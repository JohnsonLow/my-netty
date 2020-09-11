package com.lyf.selector

fun main() {
    val group = MasterSelectorThreadGroup(2, 4)
    group.bind(7000)
    group.bind(8000)
    group.bind(9000)
    group.bind(10000)
}