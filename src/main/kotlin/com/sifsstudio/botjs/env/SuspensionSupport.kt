package com.sifsstudio.botjs.env

import org.mozilla.javascript.Context
import org.mozilla.javascript.ContinuationPending
import org.mozilla.javascript.Script
import org.mozilla.javascript.ScriptableObject
import java.io.Closeable
import kotlin.time.ExperimentalTime
import kotlin.time.TimeSource

typealias SuspendBlock<T> = suspend () -> T
typealias SuspendContextBlock = SuspensionContext.(Context) -> Unit

class Suspension : RuntimeException()

inline fun SuspensionContext.useWithContext(ctx: Context, block: SuspendContextBlock) {
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

class SuspensionContext : Closeable {
    companion object {
        val CONTEXT: ThreadLocal<SuspensionContext> = ThreadLocal()

        /**
         * Cause the current Context to suspend and notify the underlying
         * com.sifsstudio.botjs.env.SuspensionContext object to handle the suspend function call
         */
        fun <T : Any> invokeSuspend(block: SuspendBlock<T>): T {
            val ctx = CONTEXT.get()
            check(ctx != null) { "There is no com.sifsstudio.botjs.env.SuspensionContext bound to current thread" }
            ctx.breakpoint = block
            throw Context.getCurrentContext().captureContinuation()
        }

    }

    private var breakpoint: SuspendBlock<Any>? = null

    init {
        check(CONTEXT.get() == null) { "There is already a com.sifsstudio.botjs.env.SuspensionContext bound to current thread" }
        CONTEXT.set(this)
    }

    suspend fun Context.runScriptSuspend(script: Script, scope: ScriptableObject): Any? {
        return try {
            executeScriptWithContinuations(script, scope)
        } catch (suspend: ContinuationPending) {
            val bkp = breakpoint ?: throw suspend
            breakpoint = null
            val rhinoCont = suspend.continuation
            val result = try {
                bkp.invoke()
            } catch (_: Suspension) {
                throw suspend
            }
            resumeSuspend(rhinoCont, scope, result)
        }
    }

    @OptIn(ExperimentalTime::class)
    @Suppress("NAME_SHADOWING")
    suspend fun Context.resumeSuspend(rhinoCont: Any, scope: ScriptableObject, result: Any?): Any? {
        var rhinoCont = rhinoCont
        var result = result
        while (true) {
            try {
                (this as BotEnv.EnvContext).startTime = TimeSource.Monotonic.markNow()
                return resumeContinuation(rhinoCont, scope, result)
            } catch (suspend: ContinuationPending) {
                val bkp = breakpoint ?: throw suspend
                breakpoint = null
                rhinoCont = suspend.continuation
                result = try {
                    bkp.invoke()
                } catch (_: Suspension) {
                    throw suspend
                }
            }
        }
    }

    override fun close() {
        CONTEXT.set(null)
    }
}