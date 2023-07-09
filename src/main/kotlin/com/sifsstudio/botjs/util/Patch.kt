package com.sifsstudio.botjs.util

import io.netty.buffer.ByteBuf
import net.minecraft.core.*
import net.minecraft.nbt.CompoundTag
import net.minecraft.nbt.ListTag
import net.minecraft.nbt.StringTag
import net.minecraft.nbt.Tag
import net.minecraft.network.FriendlyByteBuf
import net.minecraft.tags.TagKey
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.Item
import net.minecraft.world.item.ItemStack
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.math.pow
import kotlin.math.sqrt

// ItemStack
infix fun ItemStack.isItem(item: Item) = `is`(item)
infix fun ItemStack.isItem(item: TagKey<Item>) = `is`(item)

// BlockState
infix fun BlockState.isBlock(block: Block) = `is`(block)

// CompoundTag
fun CompoundTag.getList(key: String, type: Byte): ListTag = this.getList(key, type.toInt())
operator fun CompoundTag.set(key: String, value: Tag) = put(key, value)
operator fun CompoundTag.set(key: String, value: String) = putString(key, value)
operator fun CompoundTag.set(key: String, value: ByteArray) = putByteArray(key, value)

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

@Suppress("NOTHING_TO_INLINE")
inline fun Continuation<Unit>.resume() = resume(Unit)

@Suppress("UNCHECKED_CAST")
fun <T> nonnull(): T = null as T

val BlockPos.chunkIn get() = ChunkPos(this)

inline val Boolean.reversed get() = not()

inline fun Boolean.ifRun(block: () -> Unit) = this.also { if (it) block() }

@Suppress("NOTHING_TO_INLINE")
inline operator fun Runnable.invoke() = this.run()

fun FriendlyByteBuf.writeByte(byte: Byte): ByteBuf = writeByte(byte.toInt())

infix fun Double.pow(exponent: Int) = pow(exponent)
