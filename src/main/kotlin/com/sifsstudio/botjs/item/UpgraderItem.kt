package com.sifsstudio.botjs.item

import com.sifsstudio.botjs.env.BotEnv
import net.minecraft.world.item.Item

abstract class UpgraderItem: Item(Properties().stacksTo(1)) {
    abstract fun upgrade(env: BotEnv)
    abstract fun degrade(env: BotEnv)
}