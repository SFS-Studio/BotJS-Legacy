import org.mozilla.javascript.Context
import org.mozilla.javascript.ContinuationPending
import org.mozilla.javascript.Script
import org.mozilla.javascript.ScriptableObject
import java.io.Closeable

typealias SuspendBlock<T> = suspend () -> T
typealias SuspendContextBlock = SuspensionContext.(Context) -> Unit

inline fun SuspensionContext.useWithContext(ctx: Context, block: SuspendContextBlock) {
    use { c ->
        with(c) {
            ctx.use {
                block(it)
            }
        }
    }
}

inline fun suspendableContext(
    ctx: Context = Context.enter(),
    block: SuspendContextBlock
) {
    SuspensionContext().useWithContext(ctx, block)
}

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

    }

    private var breakpoint: SuspendBlock<Any>? = null

    init {
        check(CONTEXT.get() == null) { "There is already a SuspensionContext bound to current thread" }
        CONTEXT.set(this)
    }

    suspend fun Context.runScriptSuspend(script: Script, scope: ScriptableObject): Any? {
        // TODO: check if wrong
        return try {
            executeScriptWithContinuations(script, scope)
        } catch (suspend: ContinuationPending) {
            val bkp = breakpoint ?: throw suspend
            breakpoint = null
            val rhinoCont = suspend.continuation
            val result = bkp.invoke()
            resumeSuspend(rhinoCont, scope, result)
        }
    }

    @Suppress("NAME_SHADOWING")
    suspend fun Context.resumeSuspend(rhinoCont: Any, scope: ScriptableObject, result: Any?): Any {
        var rhinoCont = rhinoCont
        var result = result
        while (true) {
            try {
                return resumeContinuation(rhinoCont, scope, result)
            } catch (suspend: ContinuationPending) {
                val bkp = breakpoint ?: throw suspend
                breakpoint = null
                rhinoCont = suspend.continuation
                result = bkp.invoke()
            }
        }
    }

    override fun close() {
        CONTEXT.set(null)
    }
}