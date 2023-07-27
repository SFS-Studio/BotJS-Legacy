package com.sifsstudio.botjs.env

import com.sifsstudio.botjs.env.BotEnvGlobal.BOT_SCOPE
import com.sifsstudio.botjs.env.BotEnvState.*
import com.sifsstudio.botjs.env.api.ability.AbilityBase
import com.sifsstudio.botjs.env.intrinsic.EnvCharacteristic
import com.sifsstudio.botjs.env.save.BotDataContainer
import com.sifsstudio.botjs.env.save.BotDataStorage
import com.sifsstudio.botjs.env.task.TaskFuture
import com.sifsstudio.botjs.util.ThreadLoop
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicReference

fun BotEnv.createController() = object : BotEnv.Controller() {
    override var script = ""

    //R/W across threads
    override val runState: AtomicReference<BotEnvState> = AtomicReference(READY)
    override var loaded = false

    var reloadHandle: ThreadLoop.DisposableHandle? = null

    override var safepoint: AtomicReference<CancellableContinuation<Unit>?> = AtomicReference(null)

    override var runJob: Job? = null
    override var readJob: Job? = null
    override var writeJob: Job? = null

    val chars: MutableMap<EnvCharacteristic.Key<*>, EnvCharacteristic> = mutableMapOf()

    override val resume get() = runState.get() == SAFEPOINT

    override fun terminateExecution(): Boolean {
        var cur = runState.get()
        while (true) {
            if (cur == SAFEPOINT || cur == TERMINATING) {
                return false
            }
            val witness = runState.compareAndExchangeAcquire(cur, TERMINATING)
            if (witness == cur) {
                // done
                check(witness == RUNNING)

                taskHandler.suspend(true)
                runJob?.cancel()
                break
            } else {
                cur = witness
            }
        }
        return true
    }

    override fun launch() {
        if (globalSafepoint.isEmpty() && runState.compareAndSet(READY, RUNNING)) {
            runJob = BOT_SCOPE.launch {
                suspendableContext { cx ->
                    val complete = run(this, cx)
                    if (!complete) {
                        val state = runState.get()
                        check(state == UNLOADING) { "Bot execution stopped in an unexpected state: $state, should be $UNLOADING" }
                    }
                    runState.set(READY)
                }
            }
        }
    }

    private fun checkRelaunch() {
        if (data != BotDataContainer.EMPTY) {
            launch()
        }
    }

    override fun add() {
        loaded = true
        var read = readJob
        if (read == null) {
            read = scheduleRead()
            readJob = read
        }
        if (read.isActive) {
            check(runJob == null)
            read.invokeOnCompletion { ThreadLoop.Main.execute(::checkRelaunch) }
        } else {
            val run = runJob
            if (run != null && run.isActive) {
                safepointEvents -= SafepointEvent.Unload
                val handle = run.invokeOnCompletion {
                    if (it == null) {
                        ThreadLoop.Main.execute(::checkRelaunch)
                    }
                }
                safepointEvents += SafepointEvent.Execute { handle.dispose() }
            } else {
                checkRelaunch()
            }
        }
    }

    override fun remove() {
        loaded = false

        if (runState.get() in arrayOf(RUNNING, SAFEPOINT)) {
            safepointEvents += SafepointEvent.Unload
            taskHandler.suspend(false)
        }

        reloadHandle?.dispose()
        reloadHandle = null
    }

    override fun tick() {
        if (runState.get().tickable) {
            taskHandler.tick()
        }
    }

    override fun scheduleRead(): Job {
        val job = BOT_SCOPE.launch {
            suspendableContext { cx ->
                val entity = env.entity
                BotDataStorage.readData(entity, this)
                deserialize(cx)
            }
        }
        readJob = job
        return job
    }

    override fun scheduleWrite(): Job {
        val job = BOT_SCOPE.launch {
            suspendableContext {
                serialize()
                BotDataStorage.writeData(entity, data.serialize(), this)
            }
        }
        writeJob = job
        return job
    }

    override fun clearUpgrades() {
        abilities.clear()
        chars.forEach { (_, v) -> v.onRemovedFromEnv(env) }
        chars.clear()
    }

    override fun onEnable() {
        safepointEvents.clear()
        chars.forEach { (_, v) -> v.onActive(env) }
    }

    override fun onDisable() {
        safepointEvents.clear()
        chars.forEach { (_, v) -> v.onDeactive(env) }
    }

    override fun install(ability: AbilityBase) {
        abilities += ability.id to ability
    }

    override fun chars() = chars.keys

    @Suppress("UNCHECKED_CAST")
    override fun <T : EnvCharacteristic> get(key: EnvCharacteristic.Key<T>) = chars[key] as? T

    override fun <T : EnvCharacteristic> set(key: EnvCharacteristic.Key<T>, char: T?) {
        if (char == null) {
            chars.remove(key)?.onRemovedFromEnv(env)
        } else {
            chars[key] = char
            char.onAddedToEnv(env)
        }
    }

}

fun BotEnv.dumpContinuation(cx: SuspensionContext, ret: TaskFuture<*>?) {
    val ctx = cx.context
    check(ctx != null) { "Should be called inside js execution" }
    storeContinuation(cx.createPending(ret))
}