package com.sifsstudio.botjs.block

import com.sifsstudio.botjs.block.entity.BlockEntities
import com.sifsstudio.botjs.block.entity.WAPBlockEntity
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.BaseEntityBlock
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.Mirror
import net.minecraft.world.level.block.Rotation
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.block.state.properties.BooleanProperty
import net.minecraft.world.level.block.state.properties.Property
import net.minecraft.world.level.material.Material
import net.minecraft.world.phys.BlockHitResult

class WirelessAPBlock : BaseEntityBlock(Properties.of(Material.METAL)) {
    override fun newBlockEntity(pPos: BlockPos, pState: BlockState) = WAPBlockEntity(pPos, pState)

    override fun <T : BlockEntity?> getTicker(
        pLevel: Level,
        pState: BlockState,
        pBlockEntityType: BlockEntityType<T>
    ): BlockEntityTicker<T>? =
        createTickerHelper(pBlockEntityType, BlockEntities.CONTROLLER, WAPBlockEntity.Ticker)

    override fun createBlockStateDefinition(pBuilder: StateDefinition.Builder<Block?, BlockState?>) {
        pBuilder.add(FACING, ACTIVE)
    }

    override fun rotate(pState: BlockState, pRot: Rotation): BlockState {
        return pState.setValue(FACING, pRot.rotate(pState.getValue(FACING)))
    }

    override fun mirror(pState: BlockState, pMirror: Mirror): BlockState {
        return pState.rotate(pMirror.getRotation(pState.getValue(FACING)))
    }

    override fun use(
        pState: BlockState,
        pLevel: Level,
        pPos: BlockPos,
        pPlayer: Player,
        pHand: InteractionHand,
        pHit: BlockHitResult
    ): InteractionResult {
        return if (pLevel.isClientSide) {
            InteractionResult.SUCCESS
        } else {

            InteractionResult.CONSUME
        }
    }

    companion object {
        val FACING: Property<Direction> = BlockStateProperties.HORIZONTAL_FACING
        val ACTIVE: Property<Boolean> = BooleanProperty.create("active")
    }
}