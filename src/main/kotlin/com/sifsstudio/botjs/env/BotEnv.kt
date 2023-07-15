@file:OptIn(ExperimentalTime::class)

package com.sifsstudio.botjs.env

import com.sifsstudio.botjs.entity.BotEntity
import com.sifsstudio.botjs.env.api.Bot
import com.sifsstudio.botjs.env.api.ability.AbilityBase
import com.sifsstudio.botjs.env.intrinsic.EnvCharacteristic
import com.sifsstudio.botjs.env.save.BotSavedData
import com.sifsstudio.botjs.env.task.TaskFuture
import com.sifsstudio.botjs.env.task.TaskHandler
import com.sifsstudio.botjs.network.ClientboundBotParticlePacket
import com.sifsstudio.botjs.network.NetworkManager
import com.sifsstudio.botjs.util.pow
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import net.minecraftforge.network.PacketDistributor
import net.minecraftforge.network.PacketDistributor.TargetPoint
import org.mozilla.javascript.*
import java.util.*
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicReference
import kotlin.time.ExperimentalTime
import kotlin.time.TimeSource

class BotEnv {

    val taskHandler = TaskHandler(this)
    lateinit var scope: ScriptableObject
    lateinit var cacheScope: NativeObject
    private val abilities: MutableMap<String, AbilityBase> = mutableMapOf()
    lateinit var entity: BotEntity
    val uid: UUID by entity::uuid

    var data = BotSavedData.createEmpty()

    val controller = createController()

    var continuation: Any? = null //NativeContinuation
    var retVal: TaskFuture<*>? = null

    init {
        if(BotEnvGlobal.ALL_ENV[uid] != null) {
            throw IllegalStateException("Duplicate environment creation for bot $uid")
        }
    }

    suspend fun run(sc: SuspensionContext, context: EnvContext): Boolean {
        with(sc) {
            context.optimizationLevel = -1
            controller.onEnable()
            scope.initRuntime()
            try {
                if (continuation == null) {
                    launchClean(context, this)
                } else {
                    launchResume(context, this)
                }
            } catch (pending: ContinuationPending) {
                storeContinuation(pending)
                return false
            } catch (exception: Exception) {
                if (exception is WrappedException && exception.wrappedException is CancellationException || exception is CancellationException) {
                    NetworkManager.INSTANCE.send(PacketDistributor.NEAR.with {
                        TargetPoint(entity.x, entity.y, entity.z, 32.0 pow 2, entity.level.dimension())
                    }, ClientboundBotParticlePacket(entity, ClientboundBotParticlePacket.CANCEL))
                } else if (exception is WrappedException && exception.wrappedException is TimeoutException || exception is TimeoutException) {
                    NetworkManager.INSTANCE.send(PacketDistributor.NEAR.with {
                        TargetPoint(entity.x, entity.y, entity.z, 32.0 pow 2, entity.level.dimension())
                    }, ClientboundBotParticlePacket(entity, ClientboundBotParticlePacket.TIMEOUT))
                } else {
                    exception.printStackTrace()
                }
            } finally {
                controller.onDisable()
                scope.discardRuntime() //Depends on the bot entity
            }
            reset()
            return true
        }
    }

    fun storeContinuation(pending: ContinuationPending) {
        val aS: Any? = pending.applicationState
        check(aS is TaskFuture<*>?)
        continuation = pending.continuation
        retVal = aS
    }

    private fun reset() {
        continuation = null
        retVal = null
        taskHandler.reset()
        data.clear()
    }

    private fun ScriptableObject.initRuntime() {
        defineProperty("bot", Bot(this@BotEnv, abilities), ScriptableObject.READONLY)
    }

    private fun ScriptableObject.discardRuntime() {
        delete("bot")
    }

    private suspend fun launchClean(context: EnvContext, suspension: SuspensionContext) {
        with(suspension) {
            with(context.environment) {
                val botScript = context.compileString(controller.script, "Bot Code", 0, null)
                cacheScope = NativeObject()
                context.runScriptSuspend(botScript, scope)
            }
        }
    }

    private suspend fun launchResume(context: EnvContext, suspension: SuspensionContext) {
        with(suspension) {
            with(context.environment) {
                val continuation = continuation!!
                val ret = retVal ?: taskHandler.resume()
                context.resumeSuspend(continuation, scope, ret)
            }
        }
    }

    //Handle operation from server thread
    //All state read/write across threads shall be executed on the main thread
    abstract inner class Controller internal constructor() {
        abstract val resume: Boolean
        abstract var script: String
        // R/W across threads
        abstract val runState: AtomicReference<BotEnvState>
        abstract val loaded: Boolean
        abstract var safepoint: CancellableContinuation<Unit>?

        abstract val runJob: Job?
        abstract val readJob: Job?
        abstract val writeJob: Job?

        protected val abilities by this@BotEnv::abilities
        protected val env = this@BotEnv

        abstract fun terminateExecution(): Boolean
        abstract fun launch()
        abstract fun add()
        abstract fun remove()
        abstract fun tick()
        abstract fun scheduleRead(): Job
        abstract fun scheduleWrite(): Job
        abstract fun clearUpgrades()
        abstract fun onEnable()
        abstract fun onDisable()
        abstract fun install(ability: AbilityBase)
        inline fun install(ability: (BotEnv) -> AbilityBase) = install(ability(this@BotEnv))
        abstract fun chars(): Set<EnvCharacteristic.Key<*>>
        abstract operator fun <T : EnvCharacteristic> get(key: EnvCharacteristic.Key<T>): T?
        abstract operator fun <T : EnvCharacteristic> set(key: EnvCharacteristic.Key<T>, char: T?)
    }

    class EnvContext(factory: ContextFactory) : Context(factory) {
        var startTime: TimeSource.Monotonic.ValueTimeMark? = null
        lateinit var environment: BotEnv
    }

}
