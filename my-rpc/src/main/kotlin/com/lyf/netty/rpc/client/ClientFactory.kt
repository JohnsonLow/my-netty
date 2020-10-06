package com.lyf.netty.rpc.client

import com.lyf.netty.rpc.commons.SerializingUtil
import com.lyf.netty.rpc.protolcol.Header
import com.lyf.netty.rpc.protolcol.ResponseBody
import com.lyf.netty.rpc.server.ServerResponseMessage
import io.netty.bootstrap.Bootstrap
import io.netty.buffer.ByteBuf
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.channel.ChannelInitializer
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.codec.ByteToMessageDecoder
import java.net.InetSocketAddress
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentHashMap
import kotlin.random.Random

object ClientFactory {
    private val poolSize = 1
    private val clientMap = ConcurrentHashMap<InetSocketAddress, ClientPool>()
    private val worker = NioEventLoopGroup(1)

    @Synchronized
    fun client(address: InetSocketAddress): NioSocketChannel {
        var clientPool = clientMap[address]
        if (clientPool == null) {
            clientPool = ClientPool(poolSize)
            clientMap.putIfAbsent(address, clientPool)
        }
        val index = Random.nextInt(poolSize)
        if (clientPool.clients[index] != null && clientPool.clients[index]!!.isActive) {
            return clientPool.clients[index]!!
        }
        return synchronized(clientPool.locks[index]) {
            if(clientPool.clients[index] == null || !clientPool.clients[index]!!.isActive) {
                clientPool.clients[index] = createClient(address)
            }
            clientPool.clients[index]!!
        }
    }

    private fun createClient(address: InetSocketAddress): NioSocketChannel {
        val channelF = Bootstrap().group(worker).channel(NioSocketChannel::class.java)
            .handler(object : ChannelInitializer<NioSocketChannel>() {
                override fun initChannel(channel: NioSocketChannel) {
                    channel.pipeline().addLast(ServerResponseDecoder())
                        .addLast(ClientResponseHandler())
                }

            }).connect(address).sync()
        val client = channelF.channel() as NioSocketChannel
        return client
    }
}

class ClientPool(clientNum: Int) {
    val clients: Array<NioSocketChannel?> = arrayOfNulls<NioSocketChannel?>(clientNum)
    var locks: Array<Any> = Array(clientNum) {
        Any()
    }

}

class ClientResponseHandler : ChannelInboundHandlerAdapter() {
    override fun channelRead(ctx: ChannelHandlerContext?, msg: Any) {
        //获取消息后，唤醒等待的client，执行剩下的业务逻辑
        val responseMessage = msg as ServerResponseMessage
        val requestId = responseMessage.header.requestId
        ResponseCallbackHandler.runCallback(requestId, responseMessage.body.data)
    }
}

object ResponseCallbackHandler {
    private val callbackMap = ConcurrentHashMap<Long, CompletableFuture<in Any>>()

    fun addCallback(requestId: Long, callback: CompletableFuture<Any>) {
        callbackMap.putIfAbsent(requestId, callback)
    }

    fun runCallback(requestId: Long, result: Any?) {
        callbackMap.remove(requestId)?.complete(result)
    }
}

class ServerResponseDecoder : ByteToMessageDecoder(){
    override fun decode(ctx: ChannelHandlerContext, byteBuf: ByteBuf, out: MutableList<Any>) {
        while (byteBuf.readableBytes() >= 16) {
            //先获取头
            val headerBytes = ByteArray(16)
            byteBuf.getBytes(byteBuf.readerIndex(), headerBytes)
            val headerObj = SerializingUtil.deserialize(headerBytes, Header::class.java)
            if (byteBuf.readableBytes() >= headerObj.dataLen + 16) {
                byteBuf.readBytes(16) //将指针向后移动
                val bodyBytes = ByteArray(headerObj.dataLen)
                byteBuf.readBytes(bodyBytes)
                val responseMessage = ServerResponseMessage(
                    headerObj,
                    SerializingUtil.deserialize(bodyBytes, ResponseBody::class.java)
                )
                println("client get response: ${headerObj.requestId}, ${responseMessage.body}")
                out.add(responseMessage)
            } else {
                //等待与下次的包进行拼接
                break
            }
        }
    }

}
