package com.sifsstudio.botjs.env.api

import com.sifsstudio.botjs.env.task.TaskFuture

class FutureHandleImpl<T> (private val future: TaskFuture<T>): FutureHandle<T> {
    override fun result() = future.resultOrThrow()
    override fun join() = future.joinOrThrow()
    override fun isDone() = future.done
}