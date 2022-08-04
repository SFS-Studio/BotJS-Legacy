package com.sifsstudio.botjs.entity

import com.sifsstudio.botjs.env.BotEnv
import net.minecraft.nbt.CompoundTag
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.HumanoidArm
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraftforge.common.capabilities.Capability
import net.minecraftforge.common.util.LazyOptional

class BotEntity(type: EntityType<BotEntity>, level: Level): LivingEntity(type, level) {
    lateinit var environment: BotEnv
        private set

    override fun getArmorSlots(): MutableIterable<ItemStack> = mutableListOf()

    override fun setItemSlot(pSlot: EquipmentSlot, pStack: ItemStack) {}

    override fun getItemBySlot(pSlot: EquipmentSlot) = ItemStack.EMPTY

    override fun getMainArm() = HumanoidArm.RIGHT

    override fun <T : Any?> getCapability(cap: Capability<T>): LazyOptional<T> {
        return super.getCapability(cap)
    }
}