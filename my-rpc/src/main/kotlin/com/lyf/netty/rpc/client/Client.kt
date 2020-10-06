package com.lyf.netty.rpc.client

import com.lyf.netty.rpc.proxy.ServiceProxy.proxyGet
import com.lyf.netty.rpc.server.HelloService
import java.util.concurrent.CountDownLatch


fun main() {
    val countDownLatch = CountDownLatch(11)
    for (i in 0..10) {
        val echoArg = "HA$i"
        Thread(){
            val helloService = proxyGet(HelloService::class.java)
            val echo = helloService.echo(echoArg)
            println("get rpc response: $echo ")
            countDownLatch.countDown()
        }.start()
    }

    countDownLatch.await()
}
