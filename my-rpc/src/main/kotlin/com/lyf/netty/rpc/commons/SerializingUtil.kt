package com.lyf.netty.rpc.commons

import io.protostuff.LinkedBuffer
import io.protostuff.ProtostuffIOUtil
import io.protostuff.runtime.RuntimeSchema

object SerializingUtil {
    /**
     * 将目标类序列化为byte数组
     *
     * @param source
     * @param <T>
     * @return
    </T> */
    fun <T> serialize(source: T): ByteArray {
        val schema: RuntimeSchema<T>
        var buffer: LinkedBuffer? = null
        val result: ByteArray
        try {
            schema = RuntimeSchema.getSchema(source!!::class.java) as RuntimeSchema<T>
            buffer = LinkedBuffer.allocate(LinkedBuffer.DEFAULT_BUFFER_SIZE)
            result = ProtostuffIOUtil.toByteArray(source, schema, buffer)
        } catch (e: Exception) {
            e.printStackTrace()
            throw RuntimeException("serialize exception")
        } finally {
            buffer?.clear()
        }
        return result
    }

    /**
     * 将byte数组反序列化为目标类
     *
     * @param source
     * @param typeClass
     * @param <T>
     * @return
    </T> */
    fun <T> deserialize(source: ByteArray, typeClass: Class<T>): T {
        val schema: RuntimeSchema<T>
        val newInstance: T
        try {
            schema = RuntimeSchema.getSchema(typeClass) as RuntimeSchema<T>
            newInstance = typeClass.newInstance()
            ProtostuffIOUtil.mergeFrom(source, newInstance, schema)
        } catch (e: Exception) {
            throw RuntimeException("deserialize exception")
        }
        return newInstance
    }
}