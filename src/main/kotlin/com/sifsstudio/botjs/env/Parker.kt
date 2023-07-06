package com.sifsstudio.botjs.env

import com.sifsstudio.botjs.util.resume
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.atomic.AtomicInteger
// fixme: check unpark && park more than once if need.
/**
 * Utility class holding park/unpark control
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
            continuation = null
        }
    }

    @Synchronized
    fun unpark() {
        val result = operatedResult.compareAndExchangeAcquire(0, 2);
        if (result == 1) {
            check(continuation != null) { "MUST NOT BE NULL" }
            continuation!!.resume()
        }
    }

    @Synchronized
    fun interrupt() {
        val result = operatedResult.compareAndExchangeAcquire(0, 3);
        if (result == 1) {
            check(continuation != null) { "MUST NOT BE NULL" }
            continuation!!.cancel()
        }
    }
}