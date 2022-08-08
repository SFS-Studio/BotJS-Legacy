package com.sifsstudio.botjs.data

import com.sifsstudio.botjs.BotJS
import com.sifsstudio.botjs.entity.Entities
import com.sifsstudio.botjs.item.Items
import net.minecraft.data.DataGenerator
import net.minecraft.world.item.BlockItem
import net.minecraftforge.common.data.LanguageProvider
import net.minecraftforge.registries.ForgeRegistryEntry
import net.minecraftforge.registries.IForgeRegistryEntry

class ModLangEn(generator: DataGenerator) : LanguageProvider(generator, BotJS.ID, "en_us") {
    companion object {
        val <V : IForgeRegistryEntry<V>> ForgeRegistryEntry<V>.English get() = registryName!!.path.split('_').joinToString(" ") { it.replaceFirstChar { it.uppercase() } }

        val entries = mapOf(
            "botjs.menu.programmer.title" to "Programmer",
            "botjs.menu.programmer.message" to "Your code here...",
            "botjs.menu.bot_mount_title" to "Mount"
        )


    }
    override fun addTranslations() {
        Items.REGISTRY.entries.forEach {
            val item = it.get()
            val name = item.English
            if (item is BlockItem) {
                addBlock({ item.block }, name)
            } else {
                addItem({ item }, name)
            }
        }
        Entities.REGISTRY.entries.forEach {
            val entity = it.get()
            val name = entity.English
            addEntityType({ entity }, name);
        }
        entries.forEach { (key, value)->
            add(key, value)
        }
    }

}