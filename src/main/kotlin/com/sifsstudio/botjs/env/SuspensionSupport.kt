import org.mozilla.javascript.Context
import org.mozilla.javascript.ContinuationPending
import org.mozilla.javascript.Script
import org.mozilla.javascript.ScriptableObject
import java.io.Closeable

typealias SuspendBlock = suspend () -> Any
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

inline fun suspendableCurrent(block: SuspendContextBlock) = suspendableContext(Context.getCurrentContext(), block)

class SuspensionContext : Closeable {

    companion object {
        @JvmField
        val CONTEXT: ThreadLocal<SuspensionContext> = ThreadLocal()

        fun invokeSuspend(block: SuspendBlock) {
            val ctx = CONTEXT.get()
            check(ctx != null) { "There is no SuspensionContext bound to current thread" }
            ctx.breakpoint = block
            throw Context.getCurrentContext().captureContinuation()
        }

    }

    private val thread: Thread

    private var breakpoint: SuspendBlock? = null

    init {
        check(CONTEXT.get() == null) { "There is already a SuspensionContext bound to current thread" }
        CONTEXT.set(this)
        thread = Thread.currentThread()
    }

    suspend fun Context.runScriptSuspend(script: Script, scope: ScriptableObject): Any? {
        return try {
            executeScriptWithContinuations(script, scope)
        } catch (suspend: ContinuationPending) {
            val breakpoint = breakpoint ?: throw suspend
            val rhinoCont = suspend.continuation
            val result = breakpoint()
            resumeSuspend(rhinoCont, scope, result)
        }
    }

    suspend fun Context.resumeSuspend(rhinoCont: Any, scope: ScriptableObject, result: Any?): Any {
        var rhinoCont = rhinoCont
        var result = result
        while (true) {
            try {
                return resumeContinuation(rhinoCont, scope, result)
            } catch (suspend: ContinuationPending) {
                val breakpoint = breakpoint ?: throw suspend
                rhinoCont = suspend.continuation
                result = breakpoint()
            }
        }
    }

    override fun close() {
        CONTEXT.set(null)
    }

}