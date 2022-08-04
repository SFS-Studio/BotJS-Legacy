package com.sifsstudio.botjs.env.task

interface Task<T> {
    val done: Boolean
    val lock: DelegateLock
    var result: T?
    fun tick()
}