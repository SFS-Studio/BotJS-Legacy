package com.sifsstudio.botjs.env.task

interface TaskFuture<T: Any> {
    val done: Boolean
    val result: T
    fun join(): T

    companion object {
        fun<T : Any> failedFuture() = object: TaskFuture<T> {
            override val done: Boolean
                get() = throw IllegalStateException()
            override val result: T
                get() = throw IllegalStateException()

            override fun join(): T = throw IllegalStateException()
        }
    }
}