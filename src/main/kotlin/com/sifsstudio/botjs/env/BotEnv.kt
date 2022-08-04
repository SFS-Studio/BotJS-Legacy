package com.sifsstudio.botjs.env

import com.sifsstudio.botjs.env.ability.Ability
import com.sifsstudio.botjs.env.ability.BasicAbility
import com.sifsstudio.botjs.env.api.Bot
import com.sifsstudio.botjs.env.task.Task
import dev.latvian.mods.rhino.Context
import dev.latvian.mods.rhino.ScriptableObject
import net.minecraft.nbt.CompoundTag
import java.util.*

class BotEnv: Runnable {

    companion object {
        fun readEnvironment(tag: CompoundTag): BotEnv {
            TODO()
        }

        fun writeEnvironment(env: BotEnv): CompoundTag {
            TODO()
        }
    }

    private val context: Context = Context.enter()!!
    private val tasks: MutableSet<Task<*>> = Collections.synchronizedSet(HashSet())
    private val abilities: MutableSet<Ability> = HashSet()
    var script: String = ""

    init {
        install(BasicAbility)
    }

    fun register(tsk: Task<*>) = tasks.add(tsk)

    fun install(cap: Ability) {
        abilities.add(cap)
    }

    fun uninstall(cap: Ability) {
        check(abilities.contains(cap))
        abilities.remove(cap)
    }

    fun createRoot() = context.initStandardObjects().apply {
        defineProperty("bot", Bot(abilities), ScriptableObject.CONST)
    }

    override fun run() {
        val root = createRoot()
        context.evaluateString(root, script, "bot_script", TODO(), TODO())
    }

    fun tick() {
        tasks.iterator().run {
            var tsk: Task<*>
            while(hasNext()) {
                tsk = next()
                tsk.tick()
                if (tsk.done) {
                    remove()
                    tsk.lock.unlock()
                }
            }
        }
    }
}