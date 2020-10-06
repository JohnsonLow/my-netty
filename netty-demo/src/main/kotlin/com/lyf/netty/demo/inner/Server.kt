package com.lyf.netty.demo.inner

import io.netty.buffer.ByteBuf
import io.netty.buffer.UnpooledByteBufAllocator
import io.netty.channel.ChannelHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioServerSocketChannel
import io.netty.channel.socket.nio.NioSocketChannel
import java.net.InetSocketAddress

fun main() {
    val serverChannel = NioServerSocketChannel()
    val group = NioEventLoopGroup(1)
    group.register(serverChannel)
    serverChannel.pipeline().addLast(AcceptHandler(group, object : InitHandler(group) {
        override fun clientHandler(): ChannelHandler {
            return ReadHandler()
        }

    }))
    val channelFuture = serverChannel.bind(InetSocketAddress(8888))

    channelFuture.sync()
    serverChannel.closeFuture().sync()

}
class AcceptHandler(val group: NioEventLoopGroup, val initHandler: InitHandler): ChannelInboundHandlerAdapter() {
    override fun channelRead(ctx: ChannelHandlerContext?, msg: Any?) {
        val client = msg as NioSocketChannel
        println("get client register: ${client.remoteAddress()}")
        client.pipeline().addLast(initHandler)
        group.register(client)
    }
}

class ReadHandler : ChannelInboundHandlerAdapter() {
    override fun channelRead(ctx: ChannelHandlerContext, msg: Any) {
        val client = ctx.channel() as NioSocketChannel
        val buff = msg as ByteBuf

        val bytes = buff.readableBytes()
        val echoBytes = "get msg: ".toByteArray()
        val arr = ByteArray(bytes)
        buff.readBytes(arr)
        println("get msg: ${String(arr)}")
        var writeBuff = UnpooledByteBufAllocator.DEFAULT.directBuffer(echoBytes.size + arr.size)
        writeBuff.writeBytes(echoBytes)
        writeBuff.writeBytes(arr)

        client.writeAndFlush(writeBuff)
    }
}