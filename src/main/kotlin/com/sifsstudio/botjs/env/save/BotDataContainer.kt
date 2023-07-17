package com.sifsstudio.botjs.env.save

import com.sifsstudio.botjs.util.set
import net.minecraft.nbt.CompoundTag

class BotDataContainer(var frame: String, var tasks: CompoundTag) {
    fun deserialize(tag: CompoundTag) {
        frame = tag.getByteArray("frame").decodeToString()
        tasks = tag.getCompound("tasks")
    }
    fun serialize(): CompoundTag {
        val tag = CompoundTag()
        tag["frame"] = frame.encodeToByteArray()
        tag["tasks"] = tasks
        return tag
    }

    fun clear() {
        frame = ""
        tasks.allKeys.forEach {tasks.remove(it)}
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is BotDataContainer) return false

        if (frame != other.frame) return false
        return tasks == other.tasks
    }

    override fun hashCode(): Int {
        var result = frame.hashCode()
        result = 31 * result + tasks.hashCode()
        return result
    }

    companion object {
        val EMPTY get() = BotDataContainer("", CompoundTag())
    }

}