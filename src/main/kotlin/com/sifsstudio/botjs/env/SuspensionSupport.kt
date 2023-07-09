package com.sifsstudio.botjs.env

import org.mozilla.javascript.Context
import org.mozilla.javascript.ContinuationPending
import org.mozilla.javascript.Script
import org.mozilla.javascript.ScriptableObject
import java.io.Closeable
import kotlin.time.ExperimentalTime
import kotlin.time.TimeSource

typealias SuspendBlock<T> = suspend (SuspensionContext) -> T
typealias SuspendContextBlock = SuspensionContext.(BotEnv.EnvContext) -> Unit

class Suspension : RuntimeException()

inline fun SuspensionContext.useWithContext(ctx: BotEnv.EnvContext, block: SuspendContextBlock) {
    use { c ->
        with(c) {
            ctx.use {
                block(it)
            }
        }
    }
}

inline fun BotEnv.suspendableContext(
    ctx: Context = Context.enter(),
    block: SuspendContextBlock
) {
    (ctx as BotEnv.EnvContext).environment = this
    SuspensionContext().useWithContext(ctx, block)
}

/*@OptIn(InternalCoroutinesApi::class)
inline fun interceptExecutor(dispatcher: ExecutorCoroutineDispatcher): ExecutorCoroutineDispatcher {
    return object: ExecutorCoroutineDispatcher(), Delay {
        private val delegate = dispatcher
        override val executor by dispatcher::executor

        private inline fun delay() = delegate as Delay

        override fun close() {
            dispatcher.close()
        }

        override fun dispatch(context: CoroutineContext, block: Runnable) {
            val currentContext = suspensionContext
            if(currentContext != null) {
                dispatcher.dispatch(context) {
                    suspensionContext = currentContext
                    block()
                }
            }
        }

        override fun scheduleResumeAfterDelay(timeMillis: Long, continuation: CancellableContinuation<Unit>) {
            TODO("Not yet implemented")
        }
    }
}

fun main() {
    var cont:Continuation<Unit>? = null
    CoroutineScope(Executors.newFixedThreadPool(1).asCoroutineDispatcher()).launch {
        suspendCoroutine {
            cont = it
        }
    }
    while(cont == null);
    cont!!.resume()
}*/

var suspensionContext: SuspensionContext?
    get() = SuspensionContext.CONTEXT.get()
    set(it) = SuspensionContext.CONTEXT.set(it)


class SuspensionContext : Closeable {
    companion object {
        val CONTEXT: ThreadLocal<SuspensionContext> = ThreadLocal()

        /**
         * Cause the current Context to suspend and notify the underlying
         * SuspensionContext object to handle the suspend function call
         */
        fun <T : Any> invokeSuspend(block: SuspendBlock<T>): T {
            val ctx = CONTEXT.get()
            check(ctx != null) { "There is no SuspensionContext bound to current thread" }
            ctx.breakpoint = block
            throw Context.getCurrentContext().captureContinuation()
        }

        suspend inline fun <T> withContext(sc: SuspensionContext, block: () -> T) = sc.withContext(block)
    }

    var breakpoint: SuspendBlock<Any>? = null
        private set
    var context: Context? = null
        private set

    init {
        check(CONTEXT.get() == null) { "There is already a com.sifsstudio.botjs.env.SuspensionContext bound to current thread" }
        CONTEXT.set(this)
    }

    @Suppress("RedundantSuspendModifier")
    suspend inline fun <T> withContext(block: () -> T): T {
        try {
            suspensionContext = this
            return block()
        } finally {
            suspensionContext = null
        }
    }

    suspend fun Context.runScriptSuspend(script: Script, scope: ScriptableObject): Any? {
        suspensionContext = this@SuspensionContext
        return try {
            context = this
            executeScriptWithContinuations(script, scope)
        } catch (suspend: ContinuationPending) {
            suspensionContext = null
            val bkp = breakpoint ?: throw suspend
            breakpoint = null
            val rhinoCont = suspend.continuation
            val result = try {
                bkp.invoke(this@SuspensionContext)
            } catch (_: Suspension) {
                context = null
                suspensionContext = null
                throw suspend
            }
            resumeSuspend(rhinoCont, scope, result)
        } finally {
            suspensionContext = null
        }
    }

    @OptIn(ExperimentalTime::class)
    @Suppress("NAME_SHADOWING")
    suspend fun Context.resumeSuspend(rhinoCont: Any, scope: ScriptableObject, result: Any?): Any? {
        var rhinoCont = rhinoCont
        var result = result
        context = this
        while (true) {
            try {
                suspensionContext = this@SuspensionContext
                (this as BotEnv.EnvContext).startTime = TimeSource.Monotonic.markNow()
                return resumeContinuation(rhinoCont, scope, result).also { context = null }
            } catch (suspend: ContinuationPending) {
                suspensionContext = null
                val bkp = breakpoint ?: throw suspend
                breakpoint = null
                rhinoCont = suspend.continuation
                result = try {
                    bkp.invoke(this@SuspensionContext)
                } catch (_: Suspension) {
                    context = null
                    suspensionContext = null
                    throw suspend
                }
            } finally {
                suspensionContext = null
            }
        }
    }

    override fun close() {
        CONTEXT.set(null)
    }
}
