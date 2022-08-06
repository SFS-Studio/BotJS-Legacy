package com.sifsstudio.botjs.item

import com.sifsstudio.botjs.env.BotEnv
import com.sifsstudio.botjs.env.ability.Ability
import net.minecraft.world.item.Item

abstract class UpgradeItem: Item(Properties().stacksTo(1)) {
    abstract fun upgrade(env: BotEnv)
    abstract fun degrade(env: BotEnv)

    companion object {
        inline fun<reified T: Ability> withAbility(ability: T) =
            object: UpgradeItem() {
                override fun upgrade(env: BotEnv) {
                    env.install(ability)
                }

                override fun degrade(env: BotEnv) {
                    env.uninstall(T::class)
                }
            }
    }
}