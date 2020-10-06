package com.lyf.netty.rpc.protolcol

import io.protostuff.LinkedBuffer
import io.protostuff.ProtostuffIOUtil
import io.protostuff.runtime.RuntimeSchema
import java.io.Serializable


/**
 * write obj后 长度为104
 */
data class Header(
    //协议标识
    var flag: Int = 1,
    var requestId: Long = 0,
    var dataLen: Int = 0,
) : Serializable


data class RequestBody(
    var name: String = "",
    var method: String = "",
    var parameterTypes: Array<Class<*>> = emptyArray(),
    var parameters: Array<Any> = emptyArray()
) : Serializable

data class ResponseBody(
    var code: Int = 0,
    var data: Any? = null
)