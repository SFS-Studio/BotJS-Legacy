package com.sifsstudio.botjs.env

import com.sifsstudio.botjs.util.resume
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * Utility class holding park/unpark control
 */
class Parker {
    private var continuation: CancellableContinuation<Unit>? = null
    var parking = false

    suspend fun park() {
        check(!parking) { "Something is currently parking on this parker! This should not be possible." }
        parking = true
        try {
            suspendCancellableCoroutine {
                continuation = it
            }
        } finally {
            parking = false
            continuation = null
        }
    }

    @Synchronized
    fun unpark() {
        check(parking) { "Nothing is currently blocked on this parker!" }
        continuation!!.resume()
    }

    @Synchronized
    fun interrupt() {
        check(parking)
        continuation!!.cancel()
    }
}