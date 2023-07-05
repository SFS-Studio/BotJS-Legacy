@file:Suppress("unused", "unused_parameter")

package com.sifsstudio.botjs.util

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.suspendCancellableCoroutine
import net.minecraftforge.event.TickEvent
import net.minecraftforge.event.server.ServerStartingEvent
import net.minecraftforge.event.server.ServerStoppedEvent
import net.minecraftforge.event.server.ServerStoppingEvent
import java.util.concurrent.CancellationException
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.Executor
import kotlin.concurrent.thread
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

sealed class ThreadLoop: Executor {
    inner class DisposableHandle(private val value: Any) {
        fun dispose() = tasks.remove(value)
    }

    protected val tasks = ConcurrentLinkedQueue<() -> Boolean>()

    fun handleThrowable(th: Throwable) {
        th.printStackTrace()
    }

    fun schedule(block: () -> Boolean): DisposableHandle {
        tasks.add(block)
        return DisposableHandle(block)
    }

    override fun execute(command: Runnable) {
        tasks.add {
            command.run()
            true
        }
    }

    fun<T> submit(block: () -> T): CompletableFuture<T> {
        val fut = CompletableFuture<T>()
        val handle = schedule {
            try {
                fut.complete(block())
            } catch(t: Throwable) {
                fut.completeExceptionally(t)
            }
            true
        }
        fut.whenComplete { _, th ->
            if(th is CancellationException) {
                handle.dispose()
            }
        }

        return fut
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend inline fun<T> await(uninterruptible: Boolean, crossinline block: () -> T): T {
        if(uninterruptible) {
            return suspendCoroutine { cont ->
                schedule sc@ {
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
                val handle = schedule sc@ {
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

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun waitUntil(uninterruptible: Boolean, block: () -> Boolean) {
        if(uninterruptible) {
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
    suspend inline fun sync() = await(false) {}

    object Main: ThreadLoop() {
        fun onStart(event: ServerStartingEvent) {
            tasks.clear()
        }

        fun onTick(event: TickEvent) {
            if(event.phase == TickEvent.Phase.END) {
                tasks.removeIf { it() }
            }
        }

        fun onStop(event: ServerStoppingEvent) {
            tasks.clear()
        }
    }

    object Sync: ThreadLoop() {
        private val runner = thread(false) block@ {
            while(!Thread.interrupted()) {
                tasks.removeIf { it() }
            }
        }
        fun onStart(event: ServerStartingEvent) {
            tasks.clear()
            runner.start()
        }

        fun onStopped(event: ServerStoppedEvent) {
            runner.interrupt()
            runner.join()
            tasks.clear()
        }
    }
}