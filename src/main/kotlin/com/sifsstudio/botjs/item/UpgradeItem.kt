package com.sifsstudio.botjs.item

import com.sifsstudio.botjs.env.BotEnv
import com.sifsstudio.botjs.env.api.ability.AbilityBase
import net.minecraft.world.item.Item

abstract class UpgradeItem : Item(Properties().stacksTo(1).tab(Items.TAB)) {
    abstract fun upgrade(env: BotEnv)

    companion object {
        inline fun <T : AbilityBase> withAbility(crossinline ability: (env: BotEnv) -> T) =
            object : UpgradeItem() {
                override fun upgrade(env: BotEnv) {
                    env.install(ability(env))
                }
            }
    }
}