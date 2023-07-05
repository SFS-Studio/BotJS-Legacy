package com.sifsstudio.botjs.env.storage

import com.sifsstudio.botjs.util.set
import net.minecraft.nbt.CompoundTag

data class BotSavedData(val frame: String, val tasks: CompoundTag) {
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
    }
}