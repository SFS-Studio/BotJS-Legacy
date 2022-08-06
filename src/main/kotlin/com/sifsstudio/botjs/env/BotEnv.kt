package com.sifsstudio.botjs.env

import com.sifsstudio.botjs.entity.BotEntity
import com.sifsstudio.botjs.env.ability.Ability
import com.sifsstudio.botjs.env.ability.SpeakAbility
import com.sifsstudio.botjs.env.api.Bot
import com.sifsstudio.botjs.env.task.Task
import com.sifsstudio.botjs.env.task.TaskFuture
import dev.latvian.mods.rhino.Context
import dev.latvian.mods.rhino.ScriptableObject
import it.unimi.dsi.fastutil.objects.Object2BooleanOpenHashMap
import org.apache.logging.log4j.LogManager
import java.util.*
import kotlin.reflect.KClass

class BotEnv(val entity: BotEntity): Runnable {
    private val tasks: MutableMap<Task<*>, Boolean> = Collections.synchronizedMap(Object2BooleanOpenHashMap())
    private val abilities: MutableSet<Ability> = HashSet()
    private var active = false
    var script: String = ""
    private var available = false

    init {
        install(SpeakAbility())
    }

    fun<T: Task<*>> pending(tsk: T) =
        synchronized(this) {
            if(available && active) {
                tasks[tsk] = false
                tsk.future
            } else TaskFuture.failedFuture()
        }

    fun install(cap: Ability) = abilities.add(cap.apply { bind(this@BotEnv) })

    fun uninstall(cap: KClass<out Ability>) = abilities.removeIf(cap::isInstance)

    override fun run() {
        check(available && !active)
        active = true
        val context = Context.enter()
        val root = context.initStandardObjects().apply {
            defineProperty("bot", Bot(abilities), ScriptableObject.CONST)
        }
        try {
            context.evaluateString(root, script, "bot_script", 1, null)
        } catch(exception: Exception) {
            LOGGER.error(exception)
        }
        active = false
    }

    fun tick() = synchronized(this) {
        if(!active || !available) return
        tasks.iterator().run {
            var entry: MutableMap.MutableEntry<Task<*>, Boolean>
            var task: Task<*>
            while(hasNext()) {
                entry = next()
                task = entry.key
                if(!entry.value) {
                    if(!task.accepts(this@BotEnv)){
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

    fun discard() = synchronized(this){
        available = false
        tasks.clear()
    }

    fun enable() {
        available = true
    }

    companion object {
        private val LOGGER = LogManager.getLogger()
    }
}