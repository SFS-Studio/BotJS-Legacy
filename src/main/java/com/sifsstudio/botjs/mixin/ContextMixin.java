package com.sifsstudio.botjs.mixin;

import com.sifsstudio.botjs.env.SuspensionContext;
import com.sifsstudio.botjs.env.SuspensionSupportKt;
import org.mozilla.javascript.Context;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Cooperate with {@link SuspensionSupportKt#getSuspensionContext()} to
 * redirect {@link Context#getCurrentContext()} and Context.getContext()
 * for coroutine-related Context acquirement
 */
@Mixin(Context.class)
public class ContextMixin {

    /**
     * As {@link Context#getCurrentContext()} actually invokes getContext(),
     * we simply patch getContext.
     */
    @Inject(
            id = "botjs.Context.inject",
            method = "getContext",
            at = @At("TAIL"),
            cancellable = true
    )
    private static void inject$getContext(CallbackInfoReturnable<Context> info) {
        //If there is a Context present, in our conditions:
        //1. It is not inside a suspendable context. In that case, we return
        //the context.
        //2. It is inside a suspendable context, and the context is currently
        //held by this thread. In that case, we return the context.
        //3. It is inside a suspendable context, but, the context is currently
        //not held by this thread. In that case, we have to relate the context
        //to this thread and return the context.
        //4. It is inside a suspendable context, but, the context is currently
        //not held by this thread, and we find a Context bound to this thread.
        //This may be caused by external modifications from other mods, and we
        //shall permit it.

        //There are three ways to transfer.
        //1. Simply transfer the ownership of context during coroutine dispatching.
        //We can wrap the ExecutorCoroutineDispatcherImpl(used by BOT_DISPATCHER)
        //and add extra logic.
        //2. Let user manage the context on their own. Simply remove the ownership of
        //current thread before a suspend invocation and grant it to the current thread
        //after execution.Make sure users are not using them inside invokeSuspend
        //, allows passing SuspensionContext into the invokeSuspend block and provide
        //a method in which runs a non-suspending block and temporarily grant the context
        //before its execution and remove it after its execution.
        //3. Let the ContextMixin do the transfer every time it is called. I have
        //no idea how to implement this.

        //Of the three methods, the first one is the most elegant but complicated.
        //The second one is easy to design and easy to use.
        //The third one, in my opinion, is actually nothing.
        if (info.getReturnValue() != null) {
            return;
        }
        SuspensionContext sc = SuspensionSupportKt.getSuspensionContext();
        if (sc != null) {
            info.setReturnValue(sc.getContext());
        }
    }
}
