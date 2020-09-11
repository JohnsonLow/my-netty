package com.lyf.selector

import org.apache.logging.log4j.kotlin.Logging
import java.nio.ByteBuffer
import java.nio.channels.*
import java.util.concurrent.LinkedBlockingDeque

/**
 * @param group 通过线程组找到SelectorThread 分发资源
 */
class SelectorThread(private val group: SelectorThreadGroup) : Runnable, Logging {
    private val selector: Selector = Selector.open()

    /**
     * 存放待处理注册的channel
     */
    private val taskQueue = LinkedBlockingDeque<Channel>()

    override fun run() {
        while (true) {
            val num = selector.select()
            if (num > 0) {
                handleEvent()
            }
            if (!taskQueue.isEmpty()) {
                val channel = taskQueue.take()
                if (channel is ServerSocketChannel) {
                    channel.register(selector, SelectionKey.OP_ACCEPT)
                } else if (channel is SocketChannel) {
                    channel.register(selector, SelectionKey.OP_READ)
                }
            }
        }
    }

    private fun handleEvent() {
        selector.selectedKeys().run {
            forEach {
                when {
                    it.isAcceptable -> {
                        handleAccept(it)
                    }
                    it.isReadable -> {
                        handleRead(it);
                    }
                    it.isWritable -> {
                        //暂时不处理write
                    }
                }
                remove(it)
            }
        }
    }

    private fun handleAccept(key: SelectionKey) {
        val channel = key.channel() as ServerSocketChannel
        val client = channel.accept()
        logger.info("handle receive... ${client.remoteAddress}")
        client.configureBlocking(false)
        val buffer = ByteBuffer.allocateDirect(4096)
        if (group is MasterSelectorThreadGroup) {
            group.worker.next().registerRead(client, buffer)
        } else {
            group.next().registerRead(client, buffer)
        }

    }

    private fun registerRead(client: SocketChannel, buffer: ByteBuffer) {
        client.register(selector, SelectionKey.OP_READ, buffer)
        selector.wakeup()
    }

    fun registerServer(channel: ServerSocketChannel) {
        channel.register(selector, SelectionKey.OP_ACCEPT)
        selector.wakeup()
    }

    private fun handleRead(key: SelectionKey) {
        val client = key.channel() as SocketChannel
        logger.info("handle read ... ${client.remoteAddress}")
        val buffer = key.attachment() as ByteBuffer
        buffer.clear()
        while (true) {
            val read = client.read(buffer)
            if (read > 0) {
                buffer.flip()
                //echo
                while (buffer.hasRemaining()) {
                    client.write(buffer)
                }
            } else if (read == 0) {
                //未读到数据
                break
            } else {
                client.close()
                break
            }
        }
    }
}