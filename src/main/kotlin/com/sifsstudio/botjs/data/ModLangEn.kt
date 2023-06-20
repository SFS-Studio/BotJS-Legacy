package com.sifsstudio.botjs.data

import com.sifsstudio.botjs.BotJS
import com.sifsstudio.botjs.entity.Entities
import com.sifsstudio.botjs.item.Items
import net.minecraft.data.PackOutput
import net.minecraft.world.item.BlockItem
import net.minecraftforge.common.data.LanguageProvider
import net.minecraftforge.registries.RegistryObject

class ModLangEn(packOutput: PackOutput) : LanguageProvider(packOutput, BotJS.ID, "en_us") {
    companion object {
        val <T> RegistryObject<T>.English get() = id.path.split('_').joinToString(" ") { segment ->
            segment.replaceFirstChar {
                it.uppercase()
            }
        }

        val entries = mapOf(
            "botjs.menu.programmer.title" to "Programmer",
            "botjs.menu.bot_mount_title" to "Mount",
            "item_group.botjs" to "BotJS"
        )
    }

    override fun addTranslations() {
        Items.REGISTRY.entries.forEach {
            val item = it.get()
            val name = it.English
            if (item is BlockItem) {
                addBlock({ item.block }, name)
            } else {
                addItem({ item }, name)
            }
        }
        Entities.REGISTRY.entries.forEach {
            val entity = it.get()
            val name = it.English
            addEntityType({ entity }, name)
        }
        entries.forEach { (key, value) ->
            add(key, value)
        }
    }
}