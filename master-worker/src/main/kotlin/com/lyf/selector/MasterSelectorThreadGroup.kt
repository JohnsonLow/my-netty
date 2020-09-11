package com.lyf.selector

import java.net.InetSocketAddress
import java.nio.channels.ServerSocketChannel

class MasterSelectorThreadGroup(threadNum: Int, private val workerThread: Int) :
    SelectorThreadGroup(threadNum = threadNum, groupName = "master") {
    val worker: SelectorThreadGroup = SelectorThreadGroup(workerThread)

    /**
     * master 只能绑定
     */
    fun bind(port: Int) {
        val channel = ServerSocketChannel.open()
        channel.configureBlocking(false)
        channel.bind(InetSocketAddress(port))
        next().registerServer(channel)
        logger.info("server start....")
    }


}