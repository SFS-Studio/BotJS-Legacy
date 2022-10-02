package com.sifsstudio.botjs.env

import com.sifsstudio.botjs.entity.BotEntity
import com.sifsstudio.botjs.env.ability.Ability
import com.sifsstudio.botjs.env.api.Bot
import com.sifsstudio.botjs.env.task.ForceShutdown
import com.sifsstudio.botjs.env.task.ServerShutdown
import com.sifsstudio.botjs.env.task.Task
import com.sifsstudio.botjs.env.task.TaskManager
import com.sifsstudio.botjs.item.UpgradeItem
import dev.latvian.mods.rhino.Context
import dev.latvian.mods.rhino.ScriptableObject
import net.minecraft.ChatFormatting
import net.minecraft.Util
import net.minecraft.network.chat.ChatType
import net.minecraft.network.chat.Style
import net.minecraft.world.SimpleContainer
import net.minecraft.world.item.ItemStack
import net.minecraftforge.event.server.ServerStoppingEvent
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.locks.Lock
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
import kotlin.reflect.KClass

class BotEnv(val entity: BotEntity) : Runnable {

    private val lock: Lock = ReentrantLock()

    private val tasks: TaskManager = TaskManager(this)

    private val abilities: MutableSet<Ability> = HashSet()

    private var botObject: Bot? = null

    var deactivatePending = false
        private set

    var forceDeactivating = false
        private set

    var running = false
        private set

    var loaded = false
        private set

    var script: String = ""

    var valid = false
        private set

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
        lock.lock()
        check(valid && loaded && !running)
        running = true
        val context = Context.enter()
        botObject = Bot(abilities)
        val root = context.initStandardObjects().apply {
            defineProperty("bot", botObject, ScriptableObject.CONST)
        }
        lock.unlock()
        try {
            context.evaluateString(root, script, "bot_script", 1, null)
        } catch (exception: Exception) {
            entity.server!!.playerList.broadcastMessage(
                net.minecraft.network.chat.TextComponent(
                    exception.message ?: ""
                ).withStyle(Style.EMPTY.withColor(ChatFormatting.RED)), ChatType.CHAT, Util.NIL_UUID
            )
        }
        lock.lock()
        botObject = null
        Context.exit()
        running = false
        lock.unlock()
    }

    fun launch(): Future<*> {
        resetState()
        return EXECUTOR.submit(this)
    }

    fun tick() = lock.withLock {
        tasks.tick()
        if (!running || !valid || !loaded || deactivatePending) return
        botObject!!.releaseWait(true)
    }

    fun onRemove(reason: Throwable) = lock.withLock {
        loaded = false
        tasks.discard(reason)
        if(running) {
            botObject!!.releaseWait(false)
        }
        valid = false
    }

    fun onUnload() = lock.withLock {
        loaded = false
    }

    fun onLoad() = lock.withLock {
        loaded = true
    }

    fun requestDeactivate() {
        if(forceDeactivating) return

        if(deactivatePending) {
            onRemove(ForceShutdown)
            forceDeactivating = true
        } else {
            deactivatePending = true
            botObject!!.releaseWait(false)
        }
    }

    fun resetState() {
        deactivatePending = false
        forceDeactivating = false
    }

    companion object {
        val ALL_ENVS: MutableSet<BotEnv> = HashSet()
        val BOT_THREAD_ID = AtomicInteger(0)
        val EXECUTOR: ExecutorService = Executors.newCachedThreadPool { Thread(it, "BotJS-BotThread-"+ BOT_THREAD_ID.getAndIncrement()) }
        fun onServerStopping(event: ServerStoppingEvent) {
            ALL_ENVS.iterator().apply {
                while(hasNext()) {
                    next().onRemove(ServerShutdown)
                    remove()
                }
            }
        }
    }
}