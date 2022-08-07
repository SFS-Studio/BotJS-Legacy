package com.sifsstudio.botjs.inventory

import net.minecraft.world.Container
import net.minecraft.world.SimpleContainer
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.inventory.Slot
import net.minecraft.world.item.ItemStack

class BotMountMenu(pContainerId: Int, pPlayerInventory: Inventory, private val pContainer: Container): AbstractContainerMenu(MenuTypes.BOT_MENU, pContainerId) {

    init {
        pContainer.startOpen(pPlayerInventory.player)
        for(i in 0..8) {
            addSlot(Slot(pContainer, i, 18 * i, 18))
        }
        for (i in 0..2) {
            for (k in 0..8) {
                addSlot(Slot(pPlayerInventory, k + i * 9 + 9, 8 + k * 18, 102 + i * 18 + -18))
            }
        }

        for (j in 0..8) {
            addSlot(Slot(pPlayerInventory, j, 8 + j * 18, 142))
        }
    }

    constructor(pContainerId: Int, pPlayerInventory: Inventory) : this(pContainerId, pPlayerInventory, SimpleContainer(9))

    override fun removed(pPlayer: Player) {
        super.removed(pPlayer)
        pContainer.stopOpen(pPlayer)
    }

    override fun quickMoveStack(pPlayer: Player, pIndex: Int): ItemStack {
        return ItemStack.EMPTY
    }

    override fun stillValid(pPlayer: Player): Boolean = pContainer.stillValid(pPlayer)
}