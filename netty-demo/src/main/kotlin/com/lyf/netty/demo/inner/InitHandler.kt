package com.lyf.netty.demo.inner

import io.netty.buffer.ByteBuf
import io.netty.buffer.UnpooledByteBufAllocator
import io.netty.channel.ChannelHandler
import io.netty.channel.ChannelHandlerContext
import io.netty.channel.ChannelInboundHandlerAdapter
import io.netty.channel.nio.NioEventLoopGroup
import io.netty.channel.socket.nio.NioSocketChannel

/**
 * 由于要公用一个，所以需要设置为共享
 */
@ChannelHandler.Sharable
abstract class InitHandler(val group: NioEventLoopGroup) : ChannelInboundHandlerAdapter() {
    override fun channelRegistered(ctx: ChannelHandlerContext) {
        val client = ctx.channel() as NioSocketChannel
        println("get client register--: ${client.remoteAddress()}")
        val pipeline = client.pipeline()
        pipeline.addLast(clientHandler())
        //完成注册任务后，去除
        pipeline.remove(this)
        group.register(client)
    }

    abstract fun clientHandler(): ChannelHandler
}