package com.sifsstudio.botjs.util

import net.minecraft.tags.TagKey
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack

infix fun ItemStack.isItem(item: Item) = `is`(item)
infix fun ItemStack.isItem(item: TagKey<Item>) = `is`(item)