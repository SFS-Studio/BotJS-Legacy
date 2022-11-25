package com.sifsstudio.botjs.entity

import com.sifsstudio.botjs.BotJS
import com.sifsstudio.botjs.env.BotEnv
import com.sifsstudio.botjs.env.BotEnv.Companion.EXECUTOR_SERVICE
import com.sifsstudio.botjs.inventory.BotMountMenu
import com.sifsstudio.botjs.item.Items
import com.sifsstudio.botjs.item.UpgradeItem
import com.sifsstudio.botjs.network.ClientboundOpenProgrammerScreenPacket
import com.sifsstudio.botjs.network.NetworkManager
import com.sifsstudio.botjs.util.getList
import com.sifsstudio.botjs.util.isItem
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.Tag
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
import java.util.concurrent.Future

class BotEntity(type: EntityType<BotEntity>, level: Level) : LivingEntity(type, level) {

    private val inventory: SimpleContainer = SimpleContainer(9).apply {
        addListener {
            for (i in 0 until it.containerSize) {
                val itemStack = it.getItem(i)
                if (itemStack != ItemStack.EMPTY && itemStack.item is UpgradeItem) {
                    (itemStack.item as UpgradeItem).upgrade(environment)
                }
            }
        }
    }
    val environment = BotEnv(this)
    private lateinit var currentRunFuture: Future<*>

    override fun getArmorSlots() = emptyList<ItemStack>()

    override fun setItemSlot(pSlot: EquipmentSlot, pStack: ItemStack) = Unit

    override fun getItemBySlot(pSlot: EquipmentSlot): ItemStack = ItemStack.EMPTY

    override fun getMainArm(): HumanoidArm = HumanoidArm.RIGHT

    override fun addAdditionalSaveData(pCompound: CompoundTag) {
        super.addAdditionalSaveData(pCompound)
        pCompound.put("upgrades", inventory.createTag())
        if (!this.level.isClientSide) {
            pCompound.put("environment", environment.serialize())
        }
    }

    override fun readAdditionalSaveData(pCompound: CompoundTag) {
        super.readAdditionalSaveData(pCompound)
        inventory.removeAllItems()
        inventory.fromTag(pCompound.getList("upgrades", Tag.TAG_COMPOUND))
        if (!this.level.isClientSide) {
            environment.deserialize(pCompound.getCompound("environment"))
        }
    }

    override fun onAddedToWorld() {
        super.onAddedToWorld()
        if (!this.level.isClientSide) {
            if (environment.serializedFrame.isNotEmpty()) {
                currentRunFuture = EXECUTOR_SERVICE.submit(environment)
            }
        }
    }

    override fun tick() {
        super.tick()
        if (!this.level.isClientSide) {
            environment.tick()
        }
    }

    override fun onRemovedFromWorld() {
        super.onRemovedFromWorld()
        if (!this.level.isClientSide) {
            environment.remove()
        }
    }

    override fun interact(pPlayer: Player, pHand: InteractionHand): InteractionResult {
        if (pPlayer.getItemInHand(pHand) isItem Items.WRENCH && !environment.running) {
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
        } else if (pPlayer.getItemInHand(pHand) isItem Items.PROGRAMMER && !environment.running) {
            if (!this.level.isClientSide) {
                NetworkManager.INSTANCE.send(
                    PacketDistributor.PLAYER.with { pPlayer as ServerPlayer },
                    ClientboundOpenProgrammerScreenPacket(this.id, environment.script)
                )
            }
            return InteractionResult.sidedSuccess(this.level.isClientSide)
        } else if (pPlayer.getItemInHand(pHand) isItem Items.SWITCH) {
            if (!this.level.isClientSide) {
                if (!environment.running) {
                    currentRunFuture = EXECUTOR_SERVICE.submit(environment)
                } else {
                    if (this::currentRunFuture.isInitialized) {
                        currentRunFuture.cancel(true)
                    } else {
                        // IMPOSSIBLE
                        throw IllegalStateException()
                    }
                }
            }
            return InteractionResult.sidedSuccess(this.level.isClientSide)
        }
        return super.interact(pPlayer, pHand)
    }
}