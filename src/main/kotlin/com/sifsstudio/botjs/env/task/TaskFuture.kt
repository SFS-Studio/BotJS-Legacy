package com.sifsstudio.botjs.env.task

interface TaskFuture<T> {
    val done: Boolean
    fun result(): Result<T>?
    fun resultOrThrow() = result()?.getOrThrow()
    fun join(): Result<T>
    fun joinOrThrow() = join().getOrThrow()

    companion object {
        val successUnitFuture: TaskFuture<Unit> = successFuture(Unit)
        fun<T: Any> failedFuture(reason: Throwable) = object: TaskFuture<T> {
            override val done = true
            override fun result() = Result.failure<T>(reason)
            override fun join() = Result.failure<T>(reason)
        }
        fun<T: Any> successFuture(result: T) = object: TaskFuture<T> {
            override val done = true
            override fun result() = Result.success(result)
            override fun join() = Result.success(result)
        }
    }
}