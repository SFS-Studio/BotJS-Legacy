package com.sifsstudio.botjs.env.intrinsic.conn

import com.sifsstudio.botjs.env.BotEnv
import com.sifsstudio.botjs.env.api.conn.Remote
import com.sifsstudio.botjs.util.dToLessEq
import com.sifsstudio.botjs.util.position
import net.minecraft.core.BlockPos
import net.minecraft.core.Position

object RemoteLocator {

    val loadedRemotes: MutableSet<Remote> = mutableSetOf()

    fun dispose(remote: Remote) {
        loadedRemotes.remove(remote)
    }

    fun add(remote: Remote) {
        loadedRemotes.add(remote)
    }

    inline fun findNearby(pos: Position, distance: Double, predicate: (Remote) -> Boolean): MutableSet<Remote> {
        val result = mutableSetOf<Remote>()
        for (entry in loadedRemotes) {
            if (pos.dToLessEq(entry.position, distance) && predicate(entry)) {
                result.add(entry)
            }
        }
        return result
    }

    fun findNearby(pos: BlockPos, distance: Double, predicate: (Remote) -> Boolean) =
        findNearby(pos.position, distance, predicate)

    fun findNearby(env: BotEnv, distance: Double, predicate: (Remote) -> Boolean) =
        findNearby(env.entity.position(), distance) { predicate(it) && !(it is RemoteEnv && it.environment == env) }

    fun searchRemote(here: Remote, uid: String): Remote? =
        loadedRemotes.find { it.uid == uid }?.takeIf { ReachabilityTest(here, it) }
}