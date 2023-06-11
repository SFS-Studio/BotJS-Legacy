package com.sifsstudio.botjs.env.intrinsic.conn

import com.sifsstudio.botjs.env.api.conn.Remote
import com.sifsstudio.botjs.util.dToLessEq
import com.sifsstudio.botjs.util.position
import net.minecraft.core.BlockPos
import net.minecraft.core.Position

object RemoteLocator {

    private val activeRemotes: MutableSet<Remote> = mutableSetOf()

    fun remove(remote: Remote) {
        activeRemotes.remove(remote)
    }

    fun add(remote: Remote) {
        activeRemotes.add(remote)
    }

    fun findNearby(
        pos: Position,
        distance: Double,
        collection: MutableCollection<Remote>,
        predicate: (Remote) -> Boolean
    ) {
        for (entry in activeRemotes) {
            if (pos.dToLessEq(entry.position, distance) && predicate(entry)) {
                collection.add(entry)
            }
        }
    }

    fun findNearby(
        pos: BlockPos,
        distance: Double,
        collection: MutableCollection<Remote>,
        predicate: (Remote) -> Boolean
    ) =
        findNearby(pos.position, distance, collection, predicate)

    fun findNearby(here: Remote, distance: Double, collection: MutableCollection<Remote>) =
        findNearby(here.position, distance, collection) { it != here }

    fun findNearby(
        here: Remote,
        distance: Double,
        collection: MutableCollection<Remote>,
        predicate: (Remote) -> Boolean
    ) =
        findNearby(here.position, distance, collection) { predicate(it) && it != here }

    fun searchRemote(here: Remote, uid: String): Remote? =
        activeRemotes.find { it.uid.toString() == uid }?.takeIf { ReachabilityTest(here, it) }
}