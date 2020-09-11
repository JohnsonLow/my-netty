package com.lyf.selector

import org.apache.logging.log4j.kotlin.Logging
import java.net.InetSocketAddress
import java.nio.channels.ServerSocketChannel
import java.util.concurrent.atomic.AtomicLong

/**
 * selector 线程组，轮询的方式进行注册
 */
open class SelectorThreadGroup(threadNum: Int = 1) : Logging {
    private val currentIndex = AtomicLong(-1)
    private val threads = Array(threadNum) {
        val s = SelectorThread(this)
        Thread(s, "Thread-$it").start()
        return@Array s
    }


    /**
     * 线程组即可以bind，又可以 消费
     */
    fun bind(port: Int) {
        val channel = ServerSocketChannel.open()
        channel.configureBlocking(false)
        channel.bind(InetSocketAddress(port))
        next().registerServer(channel)
        logger.info("server start....")
    }


    fun next(): SelectorThread {
        val index = currentIndex.incrementAndGet() % threads.size
        return threads[index.toInt()]
    }
}