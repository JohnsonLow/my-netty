package com.lyf.netty.rpc.server

interface HelloService {
    fun echo(name: String): String
}


class HelloServiceImpl: HelloService {
    override fun echo(name: String): String {
        return "Hi, $name"
    }

}

