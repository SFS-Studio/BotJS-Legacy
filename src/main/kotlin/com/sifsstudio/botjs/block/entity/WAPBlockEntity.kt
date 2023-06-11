package com.sifsstudio.botjs.block.entity

import com.sifsstudio.botjs.block.WirelessAPBlock
import com.sifsstudio.botjs.env.api.conn.Remote
import com.sifsstudio.botjs.env.intrinsic.UidRegistry
import com.sifsstudio.botjs.env.intrinsic.conn.MessageManager
import com.sifsstudio.botjs.env.intrinsic.conn.ReachabilityTest
import com.sifsstudio.botjs.env.intrinsic.conn.RemoteController
import com.sifsstudio.botjs.env.intrinsic.conn.RemoteLocator
import net.minecraft.core.BlockPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.state.BlockState

class WAPBlockEntity(pPos: BlockPos, pBlockState: BlockState) :
    BlockEntity(BlockEntities.CONTROLLER, pPos, pBlockState) {

    private val networkUUID = UidRegistry.generate()
    private val infoOfThis = RemoteController(networkUUID, mutableMapOf(), this)
    private val detected = mutableSetOf<Remote>()
    private var tickCount = 0

    override fun setRemoved() {
        RemoteLocator.remove(infoOfThis)
        super.setRemoved()
    }

    override fun clearRemoved() {
        RemoteLocator.add(infoOfThis)
        super.clearRemoved()
    }

    fun updateScanRemote() {
        detected.clear()
        RemoteLocator.findNearby(infoOfThis, 64.0, detected)
    }

    fun getAvailableRemotes() = detected

    fun sendMessage(msg: String, remote: Remote): Boolean {
        if (ReachabilityTest(infoOfThis, remote)) {
            MessageManager.send(infoOfThis, remote, msg)
            return true
        }
        return false
    }

    object Ticker : BlockEntityTicker<WAPBlockEntity> {
        override fun tick(pLevel: Level, pPos: BlockPos, pState: BlockState, pBlockEntity: WAPBlockEntity) {
            if (pState.getValue(WirelessAPBlock.ACTIVE)) {
                if (pBlockEntity.tickCount++ == 20) {
                    pBlockEntity.tickCount = 0
                    pBlockEntity.updateScanRemote()
                }
            }
        }
    }
}