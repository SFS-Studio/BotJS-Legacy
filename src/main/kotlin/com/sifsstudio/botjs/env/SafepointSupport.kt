package com.sifsstudio.botjs.env

import com.sifsstudio.botjs.env.BotEnvState.*
import com.sifsstudio.botjs.env.SuspensionContext.Companion.invokeSuspend
import com.sifsstudio.botjs.env.task.TaskFuture
import com.sifsstudio.botjs.util.*
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.yield
import java.util.*
import java.util.concurrent.ConcurrentSkipListSet
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.cancellation.CancellationException

//TODO: Avoid racing
val perEnvSafepoint = mutableMapOf<UUID, MutableSet<SafepointEvent>>()
val perEnvGlobal = mutableMapOf<UUID, MutableSet<SafepointEvent>>()

inline val BotEnv.globalSafepoint get() = perEnvGlobal.computeIfAbsent(uid) { ConcurrentSkipListSet { e1, e2 -> e1.priority - e2.priority } }
inline val BotEnv.safepointEvents get() = perEnvSafepoint.computeIfAbsent(uid) { ConcurrentSkipListSet { e1, e2 -> e1.priority - e2.priority } }

var globalSafepointScope: SafepointScope? = null
    private set

/**
 * Process safe point events after block
 */
suspend inline fun BotEnv.enterSafepointBlock(cx: SuspensionContext) {
    with(controller) {
        if(uid !in perEnvSafepoint && globalSafepoint.isEmpty()) {
            return
        }
        when(runState.compareAndExchange(RUNNING, SAFEPOINT)) {
            RUNNING -> enterSafepoint(cx, null)
            else -> {}
        }
    }
}

/**
 * Process safe point events after submitting non-blocking future
 */
fun BotEnv.enterSafepointSubmit(ret: TaskFuture<*>) {
    with(controller) {
        if(safepointEvents.isEmpty() && globalSafepoint.isEmpty()) {
            return
        }
        when(runState.compareAndExchange(RUNNING, SAFEPOINT)) {
            RUNNING -> invokeSuspend {
                enterSafepoint(it, ret)
                ret
            }
            else -> {}
        }
    }
}

/**
 * Core implementation of safe point events
 */
suspend fun BotEnv.enterSafepoint(cx: SuspensionContext, ret: TaskFuture<*>?) {
    with(controller) {
        //Priority: Global > Respective
        while(safepointEvents.isNotEmpty() || globalSafepoint.isNotEmpty()) {
            while(globalSafepoint.isNotEmpty()) {
                for (e in globalSafepoint) {
                    processEvent(cx, ret, e)
                    globalSafepointScope?.signal(cx)
                }
            }
            for (e in safepointEvents) processEvent(cx, ret, e)
        }
        check(runState.compareAndSet(SAFEPOINT, RUNNING)) { "State is not SAFEPOINT after resuming from safepoint" }
    }
}

private suspend fun BotEnv.processEvent(cx: SuspensionContext, ret: TaskFuture<*>?, e: SafepointEvent) {
    with(controller) {
        when (e) {
            SafepointEvent.Suspend -> cx.switchAware {
                try {
                    suspendCancellableCoroutine {
                        safepoint = it
                    }
                } catch (cancel: CancellationException) {
                    throw cancel
                } finally {
                    safepoint = null
                }
            }

            SafepointEvent.Save -> {
                taskHandler.pause()
                dumpContinuation(cx, ret)
                cx.switchAware {
                    controller.scheduleWrite().join()
                }
                taskHandler.resumeExecution()
            }

            SafepointEvent.Unload -> {
                runState.compareAndSet(SAFEPOINT, UNLOADING)
                throw cx.createPending(ret)
            }

            is SafepointEvent.Execute -> {
                e.block()
            }
        }
    }
}

/**
 * Attempt to trigger the safe point suspension for every living BotEnv
 * and execute the operation once all the envs are paused, resuming
 * after the operation is done
 */
fun<T> globalSafepoint(block: SafepointScope.() -> T): T {
    val set = BotEnvGlobal.ALL_ENV.values
    return SafepointScope(set.toMutableSet()).use { it.block() }
}

class SafepointScope(val control: MutableCollection<BotEnv>): AutoCloseable {

    init {
        while(globalSafepointScope != null) Thread.onSpinWait()
        check(!ThreadLoop.Main.inLoop)
        globalSafepointScope = this
        control.forEach {
            it.globalSafepoint += SafepointEvent.Suspend
        }
        control.removeIf {
            val state = it.controller.runState
            while(state.get() !in stateToIgnore){
                Thread.onSpinWait()
            }
            (state.get() == SAFEPOINT).ifNotRun { it.globalSafepoint.clear() }
        }
    }

    private val tCount = AtomicInteger(0)
    private var go = false
    var open = true
        private set
    fun runSafepoint(vararg safepoint: SafepointEvent) {
        check(open)
        val count = control.size*2
        for(e in safepoint) {
            when(e) {
                SafepointEvent.Suspend ->
                    warn {
                        "Running SUSPEND in runSafepoint is of no effect." +
                        "When no safepoint is running, the are suspended."
                    }
                SafepointEvent.Unload ->
                    unloadAll()
                else ->
                    control.forEach {
                        with(it) {
                            globalSafepoint.clear()
                            globalSafepoint += e
                            globalSafepoint += SafepointEvent.Suspend
                            controller.safepoint!!.resumeSilent()
                        }
                    }
            }
            while(tCount.get() < count) Thread.onSpinWait()
        }
    }

    private fun unloadAll() {
        check(open)
        control.forEach {
            with(it) {
                globalSafepoint.clear()
                globalSafepoint += SafepointEvent.Unload
            }
        }
        control.forEach {
            while(it.controller.runState.get() != READY) {
                Thread.onSpinWait()
            }
        }
        close()
    }

    inline fun filterNot(block: (BotEnv) -> Boolean) {
        check(open)
        control.pick(block).forEach {
            with(it) {
                globalSafepoint.clear()
                controller.safepoint!!.resume()
            }
        }
    }

    suspend fun signal(cx: SuspensionContext) {
        tCount.getAndIncrement()
        cx.switchAware {
            while (!go) yield()
        }
    }

    override fun close() {
        control.forEach {
            it.globalSafepoint.clear()
            it.controller.safepoint?.resumeSilent()
        }
        globalSafepointScope = null
    }
}

private inline val stateToIgnore get() = arrayOf(READY, SAFEPOINT)

/**
 * Predefined safe point events
 * The order means priority when processed
 */
sealed class SafepointEvent(val priority: Int) {
    object Unload: SafepointEvent(4)
    object Save: SafepointEvent(3)
    object Suspend: SafepointEvent(2)

    class Execute(val block: () -> Unit): SafepointEvent(1)
}