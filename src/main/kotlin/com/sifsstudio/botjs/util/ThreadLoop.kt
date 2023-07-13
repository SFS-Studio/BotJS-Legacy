@file:Suppress("unused", "unused_parameter")

package com.sifsstudio.botjs.util

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.suspendCancellableCoroutine
import net.minecraftforge.event.TickEvent
import net.minecraftforge.event.server.ServerStartingEvent
import net.minecraftforge.event.server.ServerStoppedEvent
import net.minecraftforge.event.server.ServerStoppingEvent
import java.util.*
import java.util.concurrent.*
import kotlin.concurrent.thread
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

sealed class ThreadLoop : Executor {
    inner class DisposableHandle(private val value: Any) {
        val markers: MutableSet<Any> = mutableSetOf()
        var onDone: (() -> Unit)? = null
        fun dispose() = tasks.remove(value)
        var done: Boolean = false
            set(value) {
                onDone?.invoke()
                field = value
            }
    }

    abstract val tasks: Deque<() -> Boolean>

    abstract val inLoop: Boolean //TODO: Fast path to reduce object creation and order tasks

    fun handleThrowable(th: Throwable) {
        th.printStackTrace()
    }

    fun schedule(block: () -> Boolean): DisposableHandle {
        if(inLoop) {
            tasks.offerFirst(block)
        } else {
            tasks.offerLast(block)
        }.ifNotRun { throw RejectedExecutionException() }
        return DisposableHandle(block)
    }

    override fun execute(command: Runnable) {
        schedule {
            command()
            true
        }
    }

    object Main : ThreadLoop() {

        private var carrier: Thread? = null
        fun onStart(event: ServerStartingEvent) {
            carrier = event.server.runningThread
        }

        fun onTick(event: TickEvent) {
            if (event.phase == TickEvent.Phase.END) {
                if(tasks.isNotEmpty()) {
                    tasks.removeIf {it()}
                }
            }
        }

        fun onStop(event: ServerStoppingEvent) {
            carrier = null
        }

        fun carry(until: () -> Boolean) {
            carrier = Thread.currentThread()
            while(until()) {
                while(tasks.isEmpty() && until()) Thread.onSpinWait()
                tasks.removeIf {it()}
            }
        }

        override val tasks = ConcurrentLinkedDeque<() ->Boolean>()
        override val inLoop get() = Thread.currentThread() == carrier
    }

    object Sync : ThreadLoop() {
        private lateinit var runner: Thread

        fun onStart(event: ServerStartingEvent) {
            tasks.clear()
            runner = thread {
                while (true) {
                    val tsk = tasks.pollFirst()
                    val back = !tsk()
                    if(tasks.isNotEmpty()) {
                        tasks.removeIf { it() }
                    }
                    if(back) {
                        tasks.offerFirst(tsk)
                    }
                }
            }
        }

        fun onStopped(event: ServerStoppedEvent) {
            runner.interrupt()
            runner.join()
            tasks.clear()
        }

        override val tasks = LinkedBlockingDeque<() -> Boolean>()
        override val inLoop get() = Thread.currentThread() == runner
    }
}

fun <T> ThreadLoop.submit(block: () -> T): CompletableFuture<T> {
    val fut = CompletableFuture<T>()
    val handle = schedule {
        try {
            fut.complete(block())
        } catch (t: Throwable) {
            fut.completeExceptionally(t)
        }
        true
    }
    fut.whenComplete { _, th ->
        if (th is CancellationException) {
            handle.dispose()
        }
    }

    return fut
}

@OptIn(ExperimentalCoroutinesApi::class)
suspend inline fun <T> ThreadLoop.await(uninterruptible: Boolean, crossinline block: () -> T): T {
    if (uninterruptible) {
        return suspendCoroutine { cont ->
            schedule sc@{
                val value: T
                try {
                    value = block()
                } catch (th: Throwable) {
                    cont.resumeWithException(th)
                    return@sc true
                }
                cont.resume(value)
                true
            }
        }
    } else {
        return suspendCancellableCoroutine { cont ->
            val handle = schedule sc@{
                val value: T
                try {
                    value = block()
                } catch (th: Throwable) {
                    cont.resumeWithException(th)
                    return@sc true
                }
                cont.resume(value, ::handleThrowable)
                true
            }
            cont.invokeOnCancellation { handle.dispose() }
        }
    }
}

@Suppress("RemoveExplicitTypeArguments")
@OptIn(ExperimentalCoroutinesApi::class)
suspend fun ThreadLoop.waitUntil(uninterruptible: Boolean, block: () -> Boolean) {
    if (uninterruptible) {
        //The 'useless' type variable definition should not be deleted
        //As the compiler cannot handle such condition and do not compile
        //See https://youtrack.jetbrains.com/issue/KT-53740/Builder-inference-with-multiple-lambdas-leads-to-unsound-type
        //As it was not exactly the condition, bugs might have raised in the following codes
        suspendCoroutine<Unit> { cont ->
            schedule {
                block().ifRun { cont.resume() }
            }
        }
    } else {
        suspendCancellableCoroutine { cont ->
            val handle = schedule {
                block().ifRun { cont.resume(Unit, ::handleThrowable) }
            }
            cont.invokeOnCancellation { handle.dispose() }
        }
    }
}

suspend inline fun ThreadLoop.sync(uninterruptible: Boolean) = await(uninterruptible) {}