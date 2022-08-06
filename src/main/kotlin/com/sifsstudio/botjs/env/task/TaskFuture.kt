package com.sifsstudio.botjs.env.task

interface TaskFuture<T: Any> {
    val done: Boolean
    val result: T
    fun join(): T

    companion object {

        val successUnitFuture: TaskFuture<Unit> = successFuture(Unit)

        val failedUnitFuture: TaskFuture<Unit> = failedFuture()

        fun<T: Any> failedFuture() = object: TaskFuture<T> {
            override val done = true
            override val result: T
                get() = throw IllegalStateException()

            override fun join(): T = throw IllegalStateException()
        }

        fun<T: Any> successFuture(result: T) = object: TaskFuture<T> {
            override val done = true
            override val result = result

            override fun join() = result
        }
    }
}