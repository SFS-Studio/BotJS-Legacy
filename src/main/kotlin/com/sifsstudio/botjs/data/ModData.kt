package com.sifsstudio.botjs.data

import com.sifsstudio.botjs.BotJS
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.forge.event.lifecycle.GatherDataEvent

@Mod.EventBusSubscriber(modid = BotJS.ID, bus = Mod.EventBusSubscriber.Bus.MOD)
object ModData {


    @SubscribeEvent
    fun gatherData(event: GatherDataEvent) {
        val gen = event.generator

        gen.addProvider(ModItemModels(gen, event.existingFileHelper))
        gen.addProvider(ModLangEn(gen))

    }
}