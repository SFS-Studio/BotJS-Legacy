package com.sifsstudio.botjs.env

import com.sifsstudio.botjs.util.resume
import kotlin.coroutines.Continuation
import kotlin.coroutines.suspendCoroutine

/**
 * Utility class holding park/unpark control
 */
class Parker {
    private var continuation: Continuation<Unit>? = null
    var parking: Boolean = false
        private set

    suspend fun park() {
        check(!parking) { "Something is currently parking on this parker! This should not be possible." }
        parking = true
        try {
            suspendCoroutine {
                continuation = it
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        parking = false
        continuation = null
    }

    @Synchronized
    fun unpark() {
        check(parking) { "Nothing is currently blocked on this parker!" }
        continuation!!.resume()
    }
}