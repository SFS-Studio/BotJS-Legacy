package com.sifsstudio.botjs.item

import com.sifsstudio.botjs.BotJS
import com.sifsstudio.botjs.env.BotEnv
import net.minecraft.world.item.Item

abstract class UpgradeItem : Item(Properties().stacksTo(1).tab(BotJS.TAB)) {
    abstract fun upgrade(env: BotEnv)

    companion object {
        inline fun applies(crossinline ability: (BotEnv) -> Unit) =
            object : UpgradeItem() {
                override fun upgrade(env: BotEnv) {
                    ability(env)
                }
            }
    }
}