package com.sifsstudio.botjs.util

import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.StringTag
import net.minecraft.tags.TagKey
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack

// ItemStack
infix fun ItemStack.isItem(item: Item) = `is`(item)
infix fun ItemStack.isItem(item: TagKey<Item>) = `is`(item)

// CompoundTag
fun CompoundTag.getList(key: String, type: Byte): ListTag = this.getList(key, type.toInt())

// String
fun String.asStringTag(): StringTag = StringTag.valueOf(this)