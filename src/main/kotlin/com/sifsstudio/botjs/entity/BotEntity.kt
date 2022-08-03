package com.sifsstudio.botjs.entity

import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.HumanoidArm
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level

class BotEntity(pEntityType: EntityType<BotEntity>, pLevel: Level): LivingEntity(pEntityType, pLevel) {
    override fun getArmorSlots(): MutableIterable<ItemStack> {
        return mutableListOf()
    }

    override fun setItemSlot(pSlot: EquipmentSlot, pStack: ItemStack) {
        return
    }

    override fun getItemBySlot(pSlot: EquipmentSlot): ItemStack {
        return ItemStack.EMPTY
    }

    override fun getMainArm(): HumanoidArm {
        return HumanoidArm.RIGHT
    }
}