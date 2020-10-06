package com.lyf.netty.demo.inner

import io.netty.buffer.UnpooledByteBufAllocator
import io.netty.channel.ChannelHandler
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioSocketChannel
import java.net.InetSocketAddress
import kotlin.math.hypot

fun main() {
    val workerGroup = NioEventLoopGroup(1)
    val client = NioSocketChannel()
    workerGroup.register(client)
    client.pipeline().addLast(ReadHandler())
    val connect = client.connect(InetSocketAddress("127.0.0.1", 7777))
    connect.sync()
    val directBuffer = UnpooledByteBufAllocator.DEFAULT.directBuffer(20)
    directBuffer.writeBytes("hello".toByteArray())
    client.writeAndFlush(directBuffer).sync()
    client.closeFuture().sync()
}