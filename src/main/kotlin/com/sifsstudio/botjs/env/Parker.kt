package com.sifsstudio.botjs.env

import com.sifsstudio.botjs.util.resume
import kotlin.coroutines.Continuation
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * Utility class holding park/unpark control
 */
class Parker {
    private var continuation: Continuation<Unit>? = null
    var parking: Boolean = false
        private set

    suspend fun park(): Boolean {
        check(!parking) { "Something is currently parking on this parker! This should not be possible." }
        parking = true
        var result = try {
            suspendCoroutine {
                continuation = it
            }
            true
        } catch (e: Exception) {
            if(e != COROUTINE_INTERRUPTED) {
                e.printStackTrace()
            }
            false
        }
        parking = false
        continuation = null
        return result
    }

    @Synchronized
    fun unpark() {
        check(parking) { "Nothing is currently blocked on this parker!" }
        continuation!!.resume()
    }

    @Synchronized
    fun interrupt() {
        check(parking) { "Nothing is currently blocked on this parker!" }
        continuation!!.resumeWithException(COROUTINE_INTERRUPTED)
    }

    companion object {
        val COROUTINE_INTERRUPTED = object: RuntimeException(null, null, false, false) {
            override fun fillInStackTrace() = this
        }
    }
}