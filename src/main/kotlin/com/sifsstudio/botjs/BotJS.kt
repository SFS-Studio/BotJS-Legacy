package com.sifsstudio.botjs

import com.sifsstudio.botjs.block.Blocks
import com.sifsstudio.botjs.block.entity.BlockEntities
import com.sifsstudio.botjs.client.gui.screen.inventory.BotMountScreen
import com.sifsstudio.botjs.entity.Entities
import com.sifsstudio.botjs.env.BotEnvGlobal
import com.sifsstudio.botjs.env.save.BotDataStorage
import com.sifsstudio.botjs.env.save.SaveHandler
import com.sifsstudio.botjs.inventory.MenuTypes
import com.sifsstudio.botjs.item.Items
import com.sifsstudio.botjs.network.NetworkManager
import com.sifsstudio.botjs.util.ThreadLoop
import com.sifsstudio.botjs.util.delegate
import net.minecraft.client.gui.screens.MenuScreens
import net.minecraft.network.chat.Component
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.item.ItemStack
import net.minecraftforge.common.ForgeConfigSpec
import net.minecraftforge.event.CreativeModeTabEvent
import net.minecraftforge.eventbus.api.SubscribeEvent
import net.minecraftforge.fml.ModLoadingContext
import net.minecraftforge.fml.common.Mod
import net.minecraftforge.fml.config.ModConfig
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent
import thedarkcolour.kotlinforforge.forge.FORGE_BUS
import thedarkcolour.kotlinforforge.forge.MOD_BUS
import kotlin.properties.ReadOnlyProperty
import kotlin.reflect.KProperty
import kotlin.time.Duration

@Mod(BotJS.ID)
@Mod.EventBusSubscriber(modid = BotJS.ID, bus = Mod.EventBusSubscriber.Bus.MOD)
object BotJS {
    const val ID = "botjs"
    val CONFIG: Config

    class Config(builder: ForgeConfigSpec.Builder) {
        private class DurationDelegate(private val entry: ForgeConfigSpec.ConfigValue<String>): ReadOnlyProperty<Any?, Duration> {
            override operator fun getValue(thisRef: Any?, property: KProperty<*>): Duration {
                return entry.get().run { Duration.parse(this) }
            }
        }

        val executionTimeout by DurationDelegate(builder.define("js_timeout", "5s"))

        var saveDurationTicks: Int by builder.defineInRange("save_duration_ticks", 30*60*20, 60*20, Integer.MAX_VALUE).delegate()

        var maxMainEventMillis: Int by builder.defineInRange("max_server_event_millis", 20, 0, Integer.MAX_VALUE).delegate()
    }

    init {
        Entities.REGISTRY.register(MOD_BUS)
        Items.REGISTRY.register(MOD_BUS)
        MenuTypes.REGISTRY.register(MOD_BUS)
        Blocks.REGISTRY.register(MOD_BUS)
        BlockEntities.REGISTRY.register(MOD_BUS)
        NetworkManager.registerPackets()
        MOD_BUS.addListener(::setupClient)
        FORGE_BUS.addListener(BotEnvGlobal::onServerSetup)
        FORGE_BUS.addListener(BotEnvGlobal::onServerStop)
        FORGE_BUS.addListener(BotDataStorage.Companion::onServerStarted)
        FORGE_BUS.addListener(BotDataStorage.Companion::onServerStopped)
        FORGE_BUS.addListener(ThreadLoop.Main::onStart)
        FORGE_BUS.addListener(ThreadLoop.Main::onTick)
        FORGE_BUS.addListener(ThreadLoop.Main::onStop)
        FORGE_BUS.addListener(ThreadLoop.Sync::onStart)
        FORGE_BUS.addListener(ThreadLoop.Sync::onStopped)
        FORGE_BUS.addListener(SaveHandler::onSave)
        FORGE_BUS.addListener(SaveHandler::onTick)
        FORGE_BUS.addListener(SaveHandler::onStart)
        FORGE_BUS.addListener(SaveHandler::onStop)
        ForgeConfigSpec.Builder().configure(::Config).apply {
            CONFIG = left
            ModLoadingContext.get().registerConfig(ModConfig.Type.SERVER, right)
        }
    }

    private fun setupClient(event: FMLClientSetupEvent) = event.enqueueWork {
        MenuScreens.register(MenuTypes.BOT_MENU, ::BotMountScreen)
    }

    @SubscribeEvent
    fun buildCreativeTab(event: CreativeModeTabEvent.Register) {
        event.registerCreativeModeTab(ResourceLocation(ID, "botjs")) {
            it.title(Component.translatable("item_group.$ID"))
                .icon { ItemStack(Items.WRENCH) }
                .displayItems { _, output ->
                    Items.REGISTRY.entries.forEach { item ->
                        output.accept(item.get())
                    }
                    Blocks.REGISTRY.entries.forEach { block ->
                        output.accept(block.get())
                    }
                }
        }
    }
}