package com.sifsstudio.botjs.env.api

import com.sifsstudio.botjs.env.task.TaskFuture

class FutureHandle<T> (private val future: TaskFuture<T>) {
    val done: Boolean by future::done
    fun result() = future.resultOrThrow()
    fun join() = future.joinOrThrow()
}