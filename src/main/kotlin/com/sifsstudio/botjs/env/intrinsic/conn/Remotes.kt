package com.sifsstudio.botjs.env.intrinsic.conn

import com.sifsstudio.botjs.block.entity.WAPBlockEntity
import com.sifsstudio.botjs.env.BotEnv
import com.sifsstudio.botjs.env.api.conn.Remote
import com.sifsstudio.botjs.env.intrinsic.EnvCharacteristic
import com.sifsstudio.botjs.util.position
import net.minecraft.core.Position
import java.util.*

class RemoteEnv(uid: UUID, descriptors: Map<String, String>, internal val environment: BotEnv) :
    Remote(uid, descriptors) {
    override val position: Position get() = environment.entity.position()
    override val radius get() = environment.controller[EnvCharacteristic.CONNECTION]!!.range
}

class RemoteController(uid: UUID, descriptors: Map<String, String>, internal val entity: WAPBlockEntity) :
    Remote(uid, descriptors) {
    override val position by entity.blockPos::position
    override val radius = 128.0 //TODO
}