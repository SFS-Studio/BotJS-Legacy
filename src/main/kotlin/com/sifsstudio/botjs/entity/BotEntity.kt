package com.sifsstudio.botjs.entity

import com.sifsstudio.botjs.BotJS
import com.sifsstudio.botjs.env.BotEnv
import com.sifsstudio.botjs.env.BotEnvGlobal
import com.sifsstudio.botjs.inventory.BotMountMenu
import com.sifsstudio.botjs.item.Items
import com.sifsstudio.botjs.item.UpgradeItem
import com.sifsstudio.botjs.network.ClientboundOpenProgrammerScreenPacket
import com.sifsstudio.botjs.network.NetworkManager
import com.sifsstudio.botjs.util.getList
import com.sifsstudio.botjs.util.isItem
import net.minecraft.core.particles.SimpleParticleType
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.Tag
import net.minecraft.network.chat.Component
import net.minecraft.server.level.ServerPlayer
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.SimpleContainer
import net.minecraft.world.SimpleMenuProvider
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.HumanoidArm
import net.minecraft.world.entity.Mob
import net.minecraft.world.entity.ai.control.LookControl
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraftforge.network.PacketDistributor

class BotEntity(type: EntityType<BotEntity>, level: Level) : Mob(type, level) {

    init {
        lookControl = object : LookControl(this) {
            override fun resetXRotOnTick() = false
        }
    }

    private val inventory: SimpleContainer = SimpleContainer(9)
    lateinit var environment: BotEnv

    override fun getArmorSlots() = emptyList<ItemStack>()

    override fun setItemSlot(pSlot: EquipmentSlot, pStack: ItemStack) = Unit

    override fun getItemBySlot(pSlot: EquipmentSlot): ItemStack = ItemStack.EMPTY

    override fun getMainArm(): HumanoidArm = HumanoidArm.RIGHT

    override fun getMaxHeadXRot() = 90
    override fun getMaxHeadYRot() = 180

    override fun addAdditionalSaveData(pCompound: CompoundTag) {
        super.addAdditionalSaveData(pCompound)
        pCompound.put("upgrades", inventory.createTag())
        pCompound.putString("script", environment.controller.script)
    }

    override fun readAdditionalSaveData(pCompound: CompoundTag) {
        super.readAdditionalSaveData(pCompound)
        environment = BotEnvGlobal.load(this)
        inventory.removeAllItems()
        inventory.fromTag(pCompound.getList("upgrades", Tag.TAG_COMPOUND))
        environment.controller.script = pCompound.getString("script")
    }

    override fun onAddedToWorld() {
        super.onAddedToWorld()
        if (!this.level.isClientSide) {
            if (environment.controller.resume) {
                environment.controller.clearUpgrades()
                for (i in 0 until inventory.containerSize) {
                    val itemStack = inventory.getItem(i)
                    if (itemStack != ItemStack.EMPTY && itemStack.item is UpgradeItem) {
                        (itemStack.item as UpgradeItem).upgrade(environment)
                    }
                }
            }
            environment.controller.add()
        }
    }

    override fun tick() {
        super.tick()
        if (!this.level.isClientSide) {
            environment.controller.tick()
        }
    }

    override fun onRemovedFromWorld() {
        super.onRemovedFromWorld()
        if (!this.level.isClientSide) {
            environment.controller.remove()
        }
    }

    override fun mobInteract(pPlayer: Player, pHand: InteractionHand): InteractionResult {
        if (pPlayer.getItemInHand(pHand) isItem Items.WRENCH) {
            if (!this.level.isClientSide && environment.controller.runState.get().free) {
                pPlayer.openMenu(SimpleMenuProvider({ containerId, playerInventory, _ ->
                    BotMountMenu(
                        containerId,
                        playerInventory,
                        inventory
                    )
                }, Component.translatable("${BotJS.ID}.menu.bot_mount_title")))
            }
            return InteractionResult.sidedSuccess(this.level.isClientSide)
        } else if (pPlayer.getItemInHand(pHand) isItem Items.PROGRAMMER) {
            if (!this.level.isClientSide && environment.controller.runState.get().free) {
                NetworkManager.INSTANCE.send(
                    PacketDistributor.PLAYER.with { pPlayer as ServerPlayer },
                    ClientboundOpenProgrammerScreenPacket(this.id, environment.controller.script)
                )
            }
            return InteractionResult.sidedSuccess(this.level.isClientSide)
        } else if (pPlayer.getItemInHand(pHand) isItem Items.SWITCH) {
            if (!this.level.isClientSide) {
                if (environment.controller.runState.get().free) {
                    environment.controller.clearUpgrades()
                    for (i in 0 until inventory.containerSize) {
                        val itemStack = inventory.getItem(i)
                        if (itemStack != ItemStack.EMPTY && itemStack.item is UpgradeItem) {
                            (itemStack.item as UpgradeItem).upgrade(environment)
                        }
                    }
                    environment.controller.launch()
                } else {
                    environment.controller.terminateExecution()
                }
            }
            return InteractionResult.sidedSuccess(this.level.isClientSide)
        }
        return super.mobInteract(pPlayer, pHand)
    }

    fun genIndicateParticle(type: SimpleParticleType) {
        for (i in 0..10) {
            val vx = random.nextGaussian() * 0.02
            val vy = random.nextGaussian() * 0.02
            val vz = random.nextGaussian() * 0.02
            level.addParticle(
                type,
                getRandomX(1.0),
                randomY + 1.0,
                getRandomZ(1.0),
                vx,
                vy,
                vz
            )
        }
    }
}
