package com.sifsstudio.botjs.util

import net.minecraft.core.*
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.StringTag
import net.minecraft.tags.TagKey
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.Vec3
import kotlin.math.sqrt

// ItemStack
infix fun ItemStack.isItem(item: Item) = `is`(item)
infix fun ItemStack.isItem(item: TagKey<Item>) = `is`(item)

// BlockState
infix fun BlockState.isBlock(block: Block) = `is`(block)

// CompoundTag
fun CompoundTag.getList(key: String, type: Byte): ListTag = this.getList(key, type.toInt())

// String
fun String.asStringTag(): StringTag = StringTag.valueOf(this)

// Level
fun Level.extinguishFire(player: Player?, pos: BlockPos, direction: Direction): Boolean {
    val relativePos = pos.relative(direction)
    return if (this.getBlockState(relativePos) isBlock Blocks.FIRE) {
        this.levelEvent(player, 1009, relativePos, 0)
        this.removeBlock(relativePos, false)
        true
    } else {
        false
    }
}

// Vec3
operator fun Vec3.plus(rhs: Vec3): Vec3 = add(rhs)
operator fun Vec3.minus(rhs: Vec3): Vec3 = subtract(rhs)
operator fun Vec3.times(scalar: Double): Vec3 = scale(scalar)

fun Position.distanceTo(another: Position): Double {
    val deltaX = x() - another.x()
    val deltaY = y() - another.y()
    val deltaZ = z() - another.z()
    return sqrt(deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ)
}

fun Position.dToLessEq(another: Position, maxDistance: Double): Boolean {
    val deltaX = x() - another.x()
    val deltaY = y() - another.y()
    val deltaZ = z() - another.z()
    return deltaX * deltaX + deltaY * deltaY + deltaZ * deltaZ <= maxDistance * maxDistance
}

val Vec3i.position get() = PositionImpl(x.toDouble(), y.toDouble(), z.toDouble())