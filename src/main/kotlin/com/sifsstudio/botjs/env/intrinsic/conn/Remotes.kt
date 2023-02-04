package com.sifsstudio.botjs.env.intrinsic.conn

import com.sifsstudio.botjs.block.entity.ControllerBlockEntity
import com.sifsstudio.botjs.env.BotEnv
import com.sifsstudio.botjs.env.api.conn.Remote
import com.sifsstudio.botjs.env.intrinsic.EnvCharacteristic
import com.sifsstudio.botjs.util.position
import net.minecraft.core.Position

class RemoteEnv(uid: String, descriptors: Map<String, String>, internal val environment: BotEnv) :
    Remote(uid, descriptors) {
    override val position: Position get() = environment.entity.position()
    override val radius get() = environment[EnvCharacteristic.CONNECTION]!!.range
}

class ControllerEnv(uid: String, descriptors: Map<String, String>, internal val entity: ControllerBlockEntity) :
    Remote(uid, descriptors) {
    override val position by entity.blockPos::position
    override val radius = 128.0 //TODO
}