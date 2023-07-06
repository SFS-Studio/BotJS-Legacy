package com.sifsstudio.botjs.env

import com.sifsstudio.botjs.util.resume
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.atomic.AtomicInteger

/**
 * Utility class holding park/unpark control
 *
 * (park, unpark/interrupt) should be together without more than once
 *
 * The order of (park, unpark) doesn't matter
 */
class Parker {
    private var continuation: CancellableContinuation<Unit>? = null

    /**
     * 0 for init state
     * 1 for parked
     * 2 for unpark
     * 3 for cancel
     */
    private var operatedResult = AtomicInteger(0)

    suspend fun park() {

        try {
            suspendCancellableCoroutine {
                continuation = it
                val result = operatedResult.compareAndExchangeRelease(0, 1);
                if (result == 2) {
                    it.resume()
                } else if (result == 3) {
                    it.cancel()
                }
            }
        } finally {
            operatedResult.setRelease(0)
            continuation = null
        }
    }


    fun unpark() {
        val result = operatedResult.compareAndExchangeAcquire(0, 2);
        if (result == 1) {
            check(continuation != null) { "MUST NOT BE NULL" }
            continuation!!.resume()
        }
    }

    fun interrupt() {
        val result = operatedResult.compareAndExchangeAcquire(0, 3);
        if (result == 1) {
            check(continuation != null) { "MUST NOT BE NULL" }
            continuation!!.cancel()
        }
    }
}