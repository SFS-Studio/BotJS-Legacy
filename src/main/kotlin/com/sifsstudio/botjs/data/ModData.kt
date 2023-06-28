package com.sifsstudio.botjs.data

import com.sifsstudio.botjs.BotJS
import net.minecraftforge.data.event.GatherDataEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod

@Mod.EventBusSubscriber(modid = BotJS.ID, bus = Mod.EventBusSubscriber.Bus.MOD)
object ModData {
    @SubscribeEvent
    fun gatherData(event: GatherDataEvent) {
        val gen = event.generator

        gen.addProvider<ModItemModels>(event.includeClient()) {
            ModItemModels(it, event.existingFileHelper)
        }
        gen.addProvider(event.includeClient(), ::ModLangEn)
    }
}