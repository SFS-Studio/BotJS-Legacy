package com.sifsstudio.botjs.env.save

import com.sifsstudio.botjs.util.set
import net.minecraft.nbt.CompoundTag

class BotSavedData(var frame: String, var tasks: CompoundTag) {
    companion object {
        fun deserialize(tag: CompoundTag): BotSavedData {
            val fr = tag.getByteArray("frame")
            val tsk = tag.getCompound("tasks")
            return BotSavedData(fr.decodeToString(), tsk)
        }

        fun serialize(data: BotSavedData): CompoundTag {
            val tag = CompoundTag()
            tag["frame"] = data.frame.encodeToByteArray()
            tag["tasks"] = data.tasks
            return tag
        }
        fun createEmpty() = BotSavedData("", CompoundTag())
    }

    fun clear() {
        frame = ""
        tasks.allKeys.forEach {tasks.remove(it)}
    }
}