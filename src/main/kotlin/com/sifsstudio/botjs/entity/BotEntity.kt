package com.sifsstudio.botjs.entity

import com.sifsstudio.botjs.BotJS
import com.sifsstudio.botjs.env.BotEnv
import com.sifsstudio.botjs.inventory.BotMountMenu
import com.sifsstudio.botjs.item.Items
import com.sifsstudio.botjs.network.ClientboundOpenProgrammerScreenPacket
import com.sifsstudio.botjs.network.NetworkManager
import dev.latvian.mods.rhino.mod.util.NbtType
import net.minecraft.nbt.CompoundTag
import net.minecraft.network.chat.TranslatableComponent
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.SimpleContainer
import net.minecraft.world.SimpleMenuProvider
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.HumanoidArm
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraftforge.network.PacketDistributor
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future

class BotEntity(type: EntityType<BotEntity>, level: Level) : LivingEntity(type, level) {

    companion object {
        val EXECUTOR: ExecutorService = Executors.newCachedThreadPool()
    }

    val environment: BotEnv = BotEnv(this)
    private val inventory: SimpleContainer = SimpleContainer(9)
    private lateinit var currentRunFuture: Future<*>

    override fun getArmorSlots() = emptyList<ItemStack>()

    override fun setItemSlot(pSlot: EquipmentSlot, pStack: ItemStack) = Unit

    override fun getItemBySlot(pSlot: EquipmentSlot): ItemStack = ItemStack.EMPTY

    override fun getMainArm(): HumanoidArm = HumanoidArm.RIGHT

    override fun addAdditionalSaveData(pCompound: CompoundTag) {
        super.addAdditionalSaveData(pCompound)
        pCompound.put("upgrades", inventory.createTag())
        pCompound.putString("script", environment.script)
    }

    override fun readAdditionalSaveData(pCompound: CompoundTag) {
        super.readAdditionalSaveData(pCompound)
        inventory.fromTag(pCompound.getList("upgrades", NbtType.COMPOUND))
        environment.script = pCompound.getString("script")
    }

    override fun onAddedToWorld() {
        super.onAddedToWorld()
        environment.enable()
    }

    override fun tick() {
        super.tick()
        environment.tick()
    }

    override fun onRemovedFromWorld() {
        super.onRemovedFromWorld()
        environment.discard()
    }

    override fun interact(pPlayer: Player, pHand: InteractionHand): InteractionResult {
        if ((this::currentRunFuture.isInitialized && this.currentRunFuture.isDone) || !this::currentRunFuture.isInitialized) {
            if (pPlayer.getItemInHand(pHand).`is`(Items.MOUNTER)) {
                if (!this.level.isClientSide) {
                    pPlayer.openMenu(SimpleMenuProvider({ containerId, playerInventory, _ ->
                        BotMountMenu(
                            containerId,
                            playerInventory,
                            inventory
                        )
                    }, TranslatableComponent("${BotJS.ID}.menu.bot_mount_title")))
                }
                return InteractionResult.sidedSuccess(this.level.isClientSide)
            } else if (pPlayer.getItemInHand(pHand).`is`(Items.PROGRAMMER)) {
                if (!this.level.isClientSide) {
                    NetworkManager.INSTANCE.send(
                        PacketDistributor.PLAYER.with { pPlayer as ServerPlayer },
                        ClientboundOpenProgrammerScreenPacket(this.id)
                    )
                }
                return InteractionResult.sidedSuccess(this.level.isClientSide)
            } else if (pPlayer.getItemInHand(pHand).`is`(Items.SWITCH)) {
                if (!this.level.isClientSide) {
                    currentRunFuture = EXECUTOR.submit(environment)
                }
            }
        }
        return super.interact(pPlayer, pHand)
    }
}