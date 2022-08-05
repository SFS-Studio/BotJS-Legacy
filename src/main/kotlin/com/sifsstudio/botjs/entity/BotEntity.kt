package com.sifsstudio.botjs.entity

import com.sifsstudio.botjs.env.BotEnv
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.SimpleContainer
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.HumanoidArm
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future

class BotEntity(type: EntityType<BotEntity>, level: Level): LivingEntity(type, level) {

    companion object {
        val EXECUTOR: ExecutorService = Executors.newCachedThreadPool()
    }

    private val environment: BotEnv = BotEnv(this)
    private val inventory: SimpleContainer = SimpleContainer(9)
    private lateinit var currentRunFuture: Future<*>

    override fun getArmorSlots() = emptyList<ItemStack>()

    override fun setItemSlot(pSlot: EquipmentSlot, pStack: ItemStack) = Unit

    override fun getItemBySlot(pSlot: EquipmentSlot): ItemStack = ItemStack.EMPTY

    override fun getMainArm(): HumanoidArm = HumanoidArm.RIGHT

    override fun addAdditionalSaveData(pCompound: CompoundTag) {
        super.readAdditionalSaveData(pCompound)
        pCompound.put("upgrades", inventory.createTag())
        pCompound.putString("script", environment.script)
    }

    override fun readAdditionalSaveData(pCompound: CompoundTag) {
        super.addAdditionalSaveData(pCompound)
        inventory.fromTag(pCompound.getList("upgrades", 0))
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
        if (!this.level.isClientSide && ((this::currentRunFuture.isInitialized && this.currentRunFuture.isDone) || !this::currentRunFuture.isInitialized)) {
            currentRunFuture = EXECUTOR.submit(environment)
            return InteractionResult.sidedSuccess(this.level.isClientSide)
        }
        return super.interact(pPlayer, pHand)
    }
}