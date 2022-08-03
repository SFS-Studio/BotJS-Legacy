package com.sifsstudio.botjs.entity

import com.sifsstudio.botjs.env.BotEnv
import net.minecraft.world.entity.EntityType
import net.minecraft.world.entity.EquipmentSlot
import net.minecraft.world.entity.HumanoidArm
import net.minecraft.world.entity.LivingEntity
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level

class BotEntity(type: EntityType<BotEntity>, level: Level): LivingEntity(type, level) {
    private val env: BotEnv = BotEnv()
    override fun getArmorSlots(): MutableIterable<ItemStack> = mutableListOf()

    override fun setItemSlot(pSlot: EquipmentSlot, pStack: ItemStack) {}

    override fun getItemBySlot(pSlot: EquipmentSlot) = ItemStack.EMPTY

    override fun getMainArm() = HumanoidArm.RIGHT
}