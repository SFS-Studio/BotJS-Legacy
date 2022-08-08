package com.sifsstudio.botjs.env

import com.sifsstudio.botjs.entity.BotEntity
import com.sifsstudio.botjs.env.ability.Ability
import com.sifsstudio.botjs.env.api.Bot
import com.sifsstudio.botjs.env.task.Task
import com.sifsstudio.botjs.env.task.TaskFuture
import com.sifsstudio.botjs.item.UpgradeItem
import dev.latvian.mods.rhino.Context
import dev.latvian.mods.rhino.ScriptableObject
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap
import net.minecraft.ChatFormatting
import net.minecraft.Util
import net.minecraft.network.chat.ChatType
import net.minecraft.network.chat.Style
import net.minecraft.world.SimpleContainer
import net.minecraft.world.item.ItemStack
import org.apache.logging.log4j.LogManager
import java.util.*
import kotlin.reflect.KClass

class BotEnv(val entity: BotEntity) : Runnable {
    private val tasks: MutableMap<Task<*>, Boolean> = Collections.synchronizedMap(Object2BooleanOpenHashMap())
    private var active = false
    var script: String = ""
    private val abilities: MutableSet<Ability> = HashSet()
    private var available = false

    fun <T : Task<R>, R : Any> pending(tsk: T) =
        synchronized(this) {
            if (available && active) {
                tasks[tsk] = false
                tsk.future
            } else TaskFuture.failedFuture()
        }

    fun install(ability: Ability) {
        ability.bind(this)
        abilities.add(ability)
    }

    fun uninstall(ability: KClass<out Ability>) {
        abilities.removeIf(ability::isInstance)
    }

    fun recollectAbilities(inventory: SimpleContainer) {
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
        check(available && !active)
        active = true
        val context = Context.enter()
        val root = context.initStandardObjects().apply {
            defineProperty("bot", Bot(abilities), ScriptableObject.CONST)
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
        Context.exit()
        active = false
    }

    fun tick() = synchronized(this) {
        if (!active || !available) return
        tasks.iterator().run {
            var entry: MutableMap.MutableEntry<Task<*>, Boolean>
            var task: Task<*>
            while (hasNext()) {
                entry = next()
                task = entry.key
                if (!entry.value) {
                    if (!task.accepts(this@BotEnv)) {
                        remove()
                    } else entry.setValue(true)
                } else {
                    task.tick()
                    if (task.future.done) {
                        remove()
                    }
                }
            }
        }
    }

    fun discard() = synchronized(this) {
        available = false
        tasks.clear()
    }

    fun enable() {
        available = true
    }
}