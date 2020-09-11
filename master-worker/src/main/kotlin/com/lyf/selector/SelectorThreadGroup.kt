package com.lyf.selector

import org.apache.logging.log4j.kotlin.Logging
import java.net.InetSocketAddress
import java.nio.channels.ServerSocketChannel
import java.util.concurrent.atomic.AtomicLong

/**
 * selector 线程组，轮询的方式进行注册
 */
open class SelectorThreadGroup(threadNum: Int = 1, private val groupName: String = "worker") : Logging {
    private val currentIndex = AtomicLong(-1)
    private val threads = Array(threadNum) {
        val s = SelectorThread(this)
        Thread(s, "$groupName-$it").start()
        return@Array s
    }

    fun next(): SelectorThread {
        val index = currentIndex.incrementAndGet() % threads.size
        return threads[index.toInt()]
    }
}