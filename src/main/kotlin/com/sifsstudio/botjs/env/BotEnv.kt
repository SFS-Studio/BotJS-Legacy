package com.sifsstudio.botjs.env

import com.sifsstudio.botjs.entity.BotEntity
import com.sifsstudio.botjs.env.ability.Ability
import com.sifsstudio.botjs.env.api.BotImpl
import com.sifsstudio.botjs.env.task.ForceShutdown
import com.sifsstudio.botjs.env.task.ServerShutdown
import com.sifsstudio.botjs.env.task.Task
import com.sifsstudio.botjs.env.task.TaskManager
import com.sifsstudio.botjs.item.UpgradeItem
import dev.latvian.mods.rhino.Context
import dev.latvian.mods.rhino.NativeFunction
import dev.latvian.mods.rhino.Script
import dev.latvian.mods.rhino.ScriptableObject
import kotlinx.coroutines.channels.ticker
import net.minecraft.ChatFormatting
import net.minecraft.Util
import net.minecraft.network.chat.ChatType
import net.minecraft.network.chat.Style
import net.minecraft.world.SimpleContainer
import net.minecraft.world.item.ItemStack
import net.minecraftforge.event.TickEvent
import net.minecraftforge.event.server.ServerStartingEvent
import net.minecraftforge.event.server.ServerStoppingEvent
import java.util.concurrent.*
import java.util.concurrent.atomic.AtomicInteger
import kotlin.reflect.KClass

/**
 * The bot environment is the hub between bot thread
 * and the server thread. Thus, it's necessary to keep
 * it synchronized at any time.
 * it's rather difficult to understand for some people.
 * Here some necessary descriptions are to be given about
 * the mechanics.
 *
 * @author InitAuther97
 */
class BotEnv(val entity: BotEntity) : Runnable {

    private val lockObject = Object()

    private val tasks: TaskManager = TaskManager(this)

    private val abilities: MutableSet<Ability> = HashSet()

    private var botObject: BotImpl? = null

    /**
     * A bot can be requested to stop at two different
     * seriousness.
     *
     * When it is requested at first, terminate it after the
     * current tick.
     *
     * When it is requested the second time before it actually
     * stops, it will discard the tasks and throw an exception
     * to terminate the execution if it's waiting on the task.
     *
     * The third one actually does nothing, but say something
     * to the player.
     */
    var deactivatePending = false
        private set

    var forceDeactivating = false
        private set

    /**
     * Whether the script engine is still running
     */
    var running = false
        private set

    /**
     * Whether the bot is ticking
     */
    var loaded = false
        private set

    /**
     * Whether the following operations(except) can be done
     */
    var valid = false
        private set

    var script: String = ""

    fun <T : Task<R>, R : Any> pending(tsk: T) = tasks.pending(tsk)

    init {
        ALL_ENVS.add(this)
        valid = true
    }

    fun install(ability: Ability) {
        ability.bind(this)
        abilities.add(ability)
    }

    fun uninstall(ability: KClass<out Ability>) {
        abilities.removeIf(ability::isInstance)
    }

    fun collectAbilities(inventory: SimpleContainer) {
        abilities.clear()
        var stack: ItemStack
        for (i in 0..8) {
            stack = inventory.getItem(i)
            if (stack.item is UpgradeItem) {
                (stack.item as UpgradeItem).upgrade(this)
            }
        }
    }

    override fun run() {
        var context: Context
        var root: ScriptableObject
        synchronized(lockObject) {
            check(valid && loaded && !running)
            running = true
            context = Context.enter()
            botObject = BotImpl(abilities, entity.uuid)
            root = context.initStandardObjects().apply {
                defineProperty("bot", botObject, ScriptableObject.CONST and ScriptableObject.PERMANENT)
            }
        }
        try {
            context.evaluateString(root, script, "bot_script", 1, null)
        } catch (exception: Exception) {
            entity.server!!.playerList.broadcastMessage(
                net.minecraft.network.chat.TextComponent(
                    exception.message ?: ""
                ).withStyle(Style.EMPTY.withColor(ChatFormatting.RED)), ChatType.CHAT, Util.NIL_UUID
            )
        }
        synchronized(lockObject) {
            botObject = null
            Context.exit()
            running = false
        }
    }

    fun launch(): Future<*> {
        resetState()
        return EXECUTOR.submit(this)
    }

    /**
     * The operations from the main thread
     */
    fun tick() = synchronized(lockObject) {
        if(!valid) return
        tasks.tick()
    }

    fun onRemove(reason: Throwable) = synchronized(lockObject) {
        loaded = false
        tasks.discard(reason)
        if(running) {
            botObject!!.tickable = false
        }
        valid = false
    }

    fun onUnload() = synchronized(lockObject) {
        loaded = false
    }

    fun onLoad() = synchronized(lockObject) {
        loaded = true
    }

    fun requestDeactivate() {
        if(forceDeactivating) return

        if(deactivatePending) {
            onRemove(ForceShutdown)
            forceDeactivating = true
        } else {
            deactivatePending = true
            botObject!!.tickable = false
        }
    }

    fun resetState() {
        deactivatePending = false
        forceDeactivating = false
    }

    companion object {
        val ALL_ENVS: MutableSet<BotEnv> = HashSet()
        private val BOT_THREAD_ID = AtomicInteger(0)
        lateinit var EXECUTOR: ExecutorService
        var accept = false
        fun onServerStopping(event: ServerStoppingEvent) {
            accept = false

            ALL_ENVS.iterator().apply {
                while(hasNext()) {
                    next().onRemove(ServerShutdown)
                    remove()
                }
            }

            if (EXECUTOR.awaitTermination(5000, TimeUnit.MILLISECONDS)) {
                println("Some bots are still running. Shutting down forcibly...")
                EXECUTOR.shutdownNow()
            }
        }

        fun onServerStarting(event: ServerStartingEvent) {
            accept = true
            EXECUTOR = Executors.newCachedThreadPool { Thread(it, "BotJS-BotThread-"+ BOT_THREAD_ID.getAndIncrement()) }

        }

        fun onTick(event: TickEvent.ServerTickEvent) {
            if(event.phase == TickEvent.Phase.START) {
                TickSynchronizer.tick()
            }
        }
    }
}