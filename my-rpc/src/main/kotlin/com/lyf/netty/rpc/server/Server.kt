package com.lyf.netty.rpc.server

import com.lyf.netty.rpc.protolcol.Header
import com.lyf.netty.rpc.protolcol.RequestBody
import com.lyf.netty.rpc.protolcol.ResponseBody
import com.lyf.netty.rpc.protolcol.SerializingUtil
import io.netty.bootstrap.ServerBootstrap
import io.netty.buffer.ByteBuf
import io.netty.buffer.PooledByteBufAllocator
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.channel.ChannelInitializer
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import io.netty.handler.codec.ByteToMessageDecoder

/*
    1，先假设一个需求，写一个RPC
    2，来回通信，连接数量，拆包？
    3，动态代理呀，序列化，协议封装
    4，连接池
    5，就像调用本地方法一样去调用远程的方法，面向java中就是所谓的 面向interface开发
 */

val helloService = HelloServiceImpl()

fun main() {
    val group = NioEventLoopGroup(1)
    val server = ServerBootstrap().group(group, group)
        .channel(NioServerSocketChannel::class.java)
        .childHandler(object : ChannelInitializer<NioSocketChannel>() {
            override fun initChannel(client: NioSocketChannel) {
                println("server accept cliet port: " + client.remoteAddress().getPort())
                val pipeline = client.pipeline()
                pipeline.addLast(ServerMessageDecoder())
                pipeline.addLast(ServerRequestHandler())
            }

        }).bind(9090)
    println("server start...")
    server.sync().channel().closeFuture().sync()

}

//处理粘包问题
class ServerMessageDecoder : ByteToMessageDecoder() {
    override fun decode(context: ChannelHandlerContext, byteBuf: ByteBuf, out: MutableList<Any>) {
        //1. read head
        //2. read body
        // 这里的大小可能会根据类的全路径而变化
        while (byteBuf.readableBytes() >= 16) {
            //先获取头
            val headerBytes = ByteArray(16)
            byteBuf.getBytes(byteBuf.readerIndex(), headerBytes)
            val headerObj = SerializingUtil.deserialize(headerBytes, Header::class.java)
            if (byteBuf.readableBytes() >= headerObj.dataLen + 16) {
                println("get request: ${headerObj.requestId}")
                byteBuf.readBytes(16) //将指针向后移动
                val bodyBytes = ByteArray(headerObj.dataLen)
                byteBuf.readBytes(bodyBytes)
                val requestMsg = ServerRequestMessage(
                    headerObj,
                    SerializingUtil.deserialize(bodyBytes, RequestBody::class.java)
                )
                out.add(requestMsg)
            } else {
                //等待与下次的包进行拼接
                break
            }
        }
    }

}


class ServerRequestHandler : ChannelInboundHandlerAdapter() {
    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        val requestMessage = msg as ServerRequestMessage
        val clientChannel = ctx.channel() as NioSocketChannel
        //处理业务逻辑
        ctx.executor().parent().next().execute {
            val className = requestMessage.body.name
            // 从容器中拿到对应的service bean
            println("[${Thread.currentThread().name}] server get request, try to response $className")
            if(className == "com.lyf.netty.rpc.server.HelloService") {
                val method = HelloServiceImpl::class.java.getDeclaredMethod(
                    requestMessage.body.method,
                    *requestMessage.body.parameterTypes
                )
                val resMsg = method.invoke(helloService, *requestMessage.body.parameters)
                writeToClient(requestMessage, ResponseBody(0, resMsg), clientChannel)
            } else {
                //response 404
                writeToClient(requestMessage, ResponseBody(404), clientChannel)
            }
        }

        //3. 类名和方法签名，找到对应的实现
        //4. 执行方法，写如执行结果
    }

    fun writeToClient(requestMessage: ServerRequestMessage, responseData: ResponseBody, clientChannel: NioSocketChannel) {
        val resBodyBuff = SerializingUtil.serialize(responseData)
        val responseHeader = Header(0x141424, requestMessage.header.requestId, resBodyBuff.size)
        val resHeaderBuff = SerializingUtil.serialize(responseHeader)
        val responseBuff =
            PooledByteBufAllocator.DEFAULT.directBuffer(resHeaderBuff.size + resBodyBuff.size)
        responseBuff.writeBytes(resHeaderBuff).writeBytes(resBodyBuff)
        clientChannel.writeAndFlush(responseBuff)
    }
}

data class ServerRequestMessage(
    val header: Header,
    val body: RequestBody
)

data class ServerResponseMessage(
    val header: Header,
    val body: ResponseBody
)