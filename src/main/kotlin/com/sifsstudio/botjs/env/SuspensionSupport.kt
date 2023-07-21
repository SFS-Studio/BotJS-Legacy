package com.sifsstudio.botjs.env

import com.sifsstudio.botjs.util.getValue
import com.sifsstudio.botjs.util.setValue
import org.mozilla.javascript.*
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

inline fun SuspensionContext.switchAware(block: () -> Unit) {
    suspensionContext = null
    try {
        block()
    } finally {
        suspensionContext = this
    }
}

inline fun BotEnv.suspendableContext(
    ctx: Context = Context.enter(),
    block: SuspendContextBlock
) {
    (ctx as BotEnv.EnvContext).environment = this
    SuspensionContext().useWithContext(ctx, block)
}

suspend inline fun <T> withContext(sc: SuspensionContext, block: () -> T) = sc.withContext(block)

var suspensionContext: SuspensionContext?
    by SuspensionContext.CONTEXT

class SuspensionContext : Closeable {
    companion object {
        val CONTEXT: ThreadLocal<SuspensionContext> = ThreadLocal()

        /**
         * Cause the current Context to suspend and notify the underlying
         * SuspensionContext object to handle the suspend function call
         */
        fun <T : Any> invokeSuspend(block: SuspendBlock<T>): Nothing {
            val ctx = suspensionContext
            check(ctx != null) { "There is no SuspensionContext bound to current thread" }
            ctx.breakpoint = block
            throw ctx.context!!.captureContinuation()
        }
    }

    var breakpoint: SuspendBlock<Any>? = null
        private set
    var context: Context? = null
        private set

    var currentContinuation: NativeContinuation? = null
        private set

    init {
        check(suspensionContext == null) { "There is already a SuspensionContext bound to current thread" }
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
        return try {
            context = this
            suspensionContext = this@SuspensionContext
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

    fun createPending(ret: Any?): ContinuationPending =
        object: ContinuationPending(currentContinuation) {
            init { applicationState = ret }
        }

    override fun close() {
        CONTEXT.set(null)
    }
}
