package com.sifsstudio.botjs.data

import com.sifsstudio.botjs.BotJS
import com.sifsstudio.botjs.item.Items
import net.minecraft.data.PackOutput
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.BlockItem
import net.minecraftforge.client.model.generators.ItemModelProvider
import net.minecraftforge.common.data.ExistingFileHelper

class ModItemModels(packOutput: PackOutput, existingFileHelper: ExistingFileHelper) :
    ItemModelProvider(packOutput, BotJS.ID, existingFileHelper) {

    companion object {
        val handheld = ResourceLocation("item/handheld")
    }

    private fun handheld(id: ResourceLocation) {
        val path = id.path
        singleTexture(path, handheld, "layer0", modLoc("item/$path"))
    }

    private fun blockItem(id: ResourceLocation) {
        val path = id.path
        withExistingParent(path, modLoc("block/$path"))
    }

    override fun registerModels() {
        Items.REGISTRY.entries.forEach {
            val item = it.get()
            if (item is BlockItem) {
                blockItem(it.id)
            } else {
                handheld(it.id)
            }
        }
    }
}