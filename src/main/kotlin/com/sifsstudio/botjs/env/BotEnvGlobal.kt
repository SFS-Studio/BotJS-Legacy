package com.sifsstudio.botjs.env

import com.sifsstudio.botjs.BotJS
import com.sifsstudio.botjs.entity.BotEntity
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.asCoroutineDispatcher
import net.minecraftforge.event.server.ServerAboutToStartEvent
import net.minecraftforge.event.server.ServerStoppedEvent
import org.mozilla.javascript.Callable
import org.mozilla.javascript.Context
import org.mozilla.javascript.ContextFactory
import org.mozilla.javascript.Scriptable
import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.Executors
import java.util.concurrent.TimeoutException
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.ExperimentalTime
import kotlin.time.TimeSource

@OptIn(ExperimentalTime::class)
object BotEnvGlobal {
    private val BOT_THREAD_ID = AtomicInteger(0)
    private val CTX_FACTORY: ContextFactory = object : ContextFactory() {
        override fun hasFeature(cx: Context, featureIndex: Int) =
                featureIndex == Context.FEATURE_ENABLE_JAVA_MAP_ACCESS || super.hasFeature(cx, featureIndex)

        override fun makeContext() = BotEnv.EnvContext(this).apply { instructionObserverThreshold = 10000 }

        override fun observeInstructionCount(cx: Context, instructionCount: Int) {
            val ecx = cx as BotEnv.EnvContext
            if (ecx.environment.controller.runJob?.isCancelled != false) {
                throw CancellationException()
            }
            if (ecx.startTime!!.elapsedNow() >= BotJS.CONFIG.executionTimeout) {
                throw TimeoutException()
            }
        }

        override fun doTopCall(
                callable: Callable,
                cx: Context,
                scope: Scriptable,
                thisObj: Scriptable?,
                args: Array<out Any>
        ): Any {
            (cx as BotEnv.EnvContext).startTime = TimeSource.Monotonic.markNow()
            return super.doTopCall(callable, cx, scope, thisObj, args)
        }
    }

    private lateinit var BOT_DISPATCHER: CoroutineDispatcher
    lateinit var BOT_SCOPE: CoroutineScope
    val ALL_ENV: MutableMap<UUID, BotEnv> = ConcurrentHashMap<UUID, BotEnv>()

    init {
        //FIXME: Another mod using Rhino might break
        ContextFactory.initGlobal(CTX_FACTORY)
    }

    fun onServerSetup(@Suppress("UNUSED_PARAMETER") event: ServerAboutToStartEvent) {
        BOT_DISPATCHER = Executors.newCachedThreadPool {
            Thread(it, "BotJS-BotThread-${BOT_THREAD_ID.getAndIncrement()}")
        }.asCoroutineDispatcher()
        BOT_SCOPE = CoroutineScope(SupervisorJob() + BOT_DISPATCHER)
    }

    fun onServerStop(@Suppress("UNUSED_PARAMETER") event: ServerStoppedEvent) {
        BOT_THREAD_ID.set(0)
    }

    fun load(entity: BotEntity) = ALL_ENV.computeIfAbsent(entity.uuid) {BotEnv()}.apply {
        this.entity = entity
    }

    fun unload(uuid: UUID) = ALL_ENV.remove(uuid)
}