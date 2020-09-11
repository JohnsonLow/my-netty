package com.lyf.selector

fun main() {
    val group = SelectorThreadGroup(4)
    group.bind(7000)
    group.bind(8000)
}