package com.lyf.netty.rpc.proxy

import com.lyf.netty.rpc.client.ClientFactory
import com.lyf.netty.rpc.client.ResponseCallbackHandler
import com.lyf.netty.rpc.commons.SerializingUtil
import com.lyf.netty.rpc.protolcol.Header
import com.lyf.netty.rpc.protolcol.RequestBody
import io.netty.buffer.PooledByteBufAllocator
import java.lang.Math.abs
import java.lang.reflect.Proxy
import java.net.InetSocketAddress
import java.util.*
import java.util.concurrent.CompletableFuture

object ServiceProxy {
    fun <T> proxyGet(interfaceInfo: Class<T>): T {
        val classLoader = interfaceInfo.classLoader
        return Proxy.newProxyInstance(
            classLoader, arrayOf(interfaceInfo)
        ) { proxy, method, args ->
            // 1. 调用服务，方法，参数 -> 封装成msg
            val className = interfaceInfo.name
            val methodName = method.name
            // 2. requestId + message , requestId 要缓存
            val body = RequestBody(className, methodName, method.parameterTypes, args)
            val bodyArray = SerializingUtil.serialize(body)
            val requestId = abs(UUID.randomUUID().leastSignificantBits)
            val header = Header(0x141414, requestId, bodyArray.size)
            val headerArray = SerializingUtil.serialize(header)
            // 3. 连接池，取得连接
            val client = ClientFactory.client(InetSocketAddress("127.0.0.1", 9090))
            // 4. 发送走IO，如果处理结果回调
            //先注册回调
            val callback = CompletableFuture<Any>()
            ResponseCallbackHandler.addCallback(header.requestId, callback)
            val writeBuff = PooledByteBufAllocator.DEFAULT.directBuffer(headerArray.size + bodyArray.size)
            writeBuff.writeBytes(headerArray)
                .writeBytes(bodyArray)
            println("try to request $body")
            println("${client.isActive}")
            client.writeAndFlush(writeBuff).sync()
            println("waiting for response")
            callback.get()

        } as T
    }
}