package com.sifsstudio.botjs.env

import com.sifsstudio.botjs.env.BotEnvGlobal.BOT_SCOPE
import com.sifsstudio.botjs.env.BotEnvState.*
import com.sifsstudio.botjs.env.api.ability.AbilityBase
import com.sifsstudio.botjs.env.intrinsic.EnvCharacteristic
import com.sifsstudio.botjs.env.save.BotDataStorage
import com.sifsstudio.botjs.env.save.BotSavedData
import com.sifsstudio.botjs.env.task.TaskFuture
import com.sifsstudio.botjs.util.ThreadLoop
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicReference

fun BotEnv.createController() = object: BotEnv.Controller() {
    override var script = ""

    //R/W across threads
    override val runState: AtomicReference<BotEnvState> = AtomicReference(READY)
    override var loaded = false

    var reloadHandle: ThreadLoop.DisposableHandle? = null

    override var safepoint: CancellableContinuation<Unit>? = null

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
                    if(!complete) {
                        val state = runState.get()
                        check(state == UNLOADING) { "Bot execution stopped in an unexpected state: $state, should be $UNLOADING" }
                    }
                    runState.set(READY)
                }
            }
        }
    }

    private fun relaunch(): Boolean {
        if (runState.get() != READY) {
            return false
        }
        launch()
        return true
    }

    private fun awaitLaunch(): Boolean {
        val job = readJob ?: return false
        return if(job.isActive) {
            false
        } else if(runJob == null){
            launch()
            true
        } else false
    }

    override fun add() {
        loaded = true
        val job = readJob
        if(job == null || job.isActive) {
            ThreadLoop.Main.schedule(::awaitLaunch)
        } else {
            if (runJob != null) {
                safepointEvents -= SafepointEvent.UNLOAD
                when (runState.get()) {
                    UNLOADING -> {
                        if (reloadHandle == null) {
                            reloadHandle = ThreadLoop.Main.schedule(::relaunch)
                        }
                    }

                    SAFEPOINT -> {
                        if (resume) {
                            launch()
                        }
                    }

                    else -> {}
                }
            }
        }
    }

    override fun remove() {
        loaded = false

        if (runState.get() in arrayOf(RUNNING, SAFEPOINT)) {
            safepointEvents += SafepointEvent.UNLOAD
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
                val data = BotDataStorage.readData(entity)
                data?.let { deserialize(it, cx) }
            }
        }
        readJob = job
        return job
    }

    override fun scheduleWrite(): Job {
        val job = BOT_SCOPE.launch {
            suspendableContext {
                data = serialize()
                BotDataStorage.writeData(entity, BotSavedData.serialize(data))
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
        chars.forEach { (_, v) -> v.onActive(env) }
    }

    override fun onDisable() {
        chars.forEach { (_, v) -> v.onDeactive(env) }
    }

    override fun install(ability: AbilityBase) {
        abilities += ability.id to ability
    }

    override fun chars() = chars.keys

    @Suppress("UNCHECKED_CAST")
    override fun <T : EnvCharacteristic> get(key: EnvCharacteristic.Key<T>) = chars[key] as T

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