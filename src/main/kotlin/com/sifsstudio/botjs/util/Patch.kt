package com.sifsstudio.botjs.util

import io.netty.buffer.ByteBuf
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Blocks
import net.minecraft.world.level.block.state.BlockState
import net.minecraftforge.common.ForgeConfigSpec
import java.lang.IllegalStateException
import java.util.concurrent.CompletableFuture
import kotlin.coroutines.Continuation
import kotlin.coroutines.resume
import kotlin.math.pow
import kotlin.math.sqrt
import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

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

@OptIn(ExperimentalCoroutinesApi::class)
@Suppress("NOTHING_TO_INLINE")
inline fun CancellableContinuation<Unit>.resumeSilent() = resume(Unit) {}

@OptIn(ExperimentalCoroutinesApi::class)
@Suppress("NOTHING_TO_INLINE", "UNUSED")
inline fun CancellableContinuation<Unit>.resumeLogging() = resume(Unit) { it.printStackTrace() }

@Suppress("UNCHECKED_CAST")
fun <T> nonnull(): T = null as T

inline fun Boolean.ifRun(block: () -> Unit) = this.also { if (it) block() }

inline fun Boolean.ifNotRun(block: () -> Unit) = this.also { if(!it) block() }

@Suppress("NOTHING_TO_INLINE")
inline operator fun Runnable.invoke() = this.run()

fun FriendlyByteBuf.writeByte(byte: Byte): ByteBuf = writeByte(byte.toInt())

infix fun Double.pow(exponent: Int) = pow(exponent)

operator fun <T> ThreadLocal<T>.setValue(t: Any?, property: KProperty<*>, t1: T?) = set(t1)

operator fun <T> ThreadLocal<T>.getValue(t: Any?, property: KProperty<*>): T? = get()

inline fun<T> describe(future: CompletableFuture<T>, block: () -> T) {
    try {
        future.complete(block())
    } catch(th: Throwable) {
        future.completeExceptionally(th)
        if(th is Error) {
            throw th
        }
    }
}

fun<T> ForgeConfigSpec.ConfigValue<T>.delegate() = object: ReadWriteProperty<Any?, T> {
    override fun getValue(thisRef: Any?, property: KProperty<*>) = get()

    override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) = set(value)
}

inline fun<T> MutableIterable<T>.pickTo(block: (T) -> Boolean, another: MutableCollection<T>) {
    val iterator = iterator()
    var now: T
    while(iterator.hasNext()) {
        now = iterator.next()
        if(block(now)) {
            another += now
            iterator.remove()
        }
    }
}

inline fun<T, K: MutableCollection<T>> MutableIterable<T>.pickTo(block: (T) -> Boolean, another: () -> K): K {
    val res = another()
    pickTo(block,res)
    return res
}

inline fun<T> MutableIterable<T>.pick(block: (T) -> Boolean) = pickTo(block, ::mutableListOf)

inline fun<T> MutableIterable<T>.pickFirst(block: (T) -> Boolean): T? {
    val iterator = iterator()
    var now: T
    while(iterator.hasNext()) {
        now = iterator.next()
        if(block(now)) {
            return now
        }
    }
    return null
}

inline fun warn(condition: Boolean, block: () -> String) {
    if(!condition) {
        IllegalStateException(block()).printStackTrace()
    }
}