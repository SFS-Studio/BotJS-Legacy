package com.sifsstudio.botjs.data

import com.sifsstudio.botjs.BotJS
import com.sifsstudio.botjs.item.Items
import net.minecraft.data.DataGenerator
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.BlockItem
import net.minecraft.world.item.Item
import net.minecraftforge.client.model.generators.ItemModelProvider
import net.minecraftforge.common.data.ExistingFileHelper

class ModItemModels(generator: DataGenerator, existingFileHelper: ExistingFileHelper) :
    ItemModelProvider(generator, BotJS.ID, existingFileHelper) {

    companion object {
        val handheld = ResourceLocation("item/handheld")
    }

    fun handheld(item: Item, prefix: String) {
        val path = item.registryName?.path
        singleTexture(path, handheld, "layer0", modLoc("$prefix/$path"))
    }

    fun blockItem(item: Item) {
        val path = item.registryName?.path
        withExistingParent(path, modLoc("block/$path"))
    }

    override fun registerModels() {
        Items.REGISTRY.entries.forEach {
            val item = it.get()
            if (item is BlockItem) {
                blockItem(item)
            } else {
                handheld(item, "item")
            }
        }
    }
}