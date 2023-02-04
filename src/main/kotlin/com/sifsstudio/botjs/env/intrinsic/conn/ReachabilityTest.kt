package com.sifsstudio.botjs.env.intrinsic.conn

import com.sifsstudio.botjs.env.api.conn.Remote
import com.sifsstudio.botjs.util.distanceTo

object ReachabilityTest {

    operator fun invoke(first: Remote, second: Remote): Boolean {
        val d = first.position.distanceTo(second.position)
        val d1 = first.radius
        val d2 = second.radius
        return d <= d1 && d <= d2
    }

    operator fun invoke(`this`: Remote): (Remote) -> Boolean {
        return { this(`this`, it) }
    }
}