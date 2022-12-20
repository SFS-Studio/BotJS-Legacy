package com.sifsstudio.botjs.env

import org.mozilla.javascript.Context
import org.mozilla.javascript.Function
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject
import java.util.UUID

fun BotEnv.addCache(value: Scriptable) = synchronized(this) {
    var uid: String
    do {
        uid = UUID.randomUUID().toString()
    } while (cacheScope.containsKey(uid))
    cacheScope.defineProperty(uid, value, ScriptableObject.CONST)
    uid
}

fun BotEnv.getCache(key: String) = synchronized(this) {
    try {
        cacheScope[key] as Scriptable
    } finally {
        cacheScope.delete(key)
    }
}

inline fun<reified T: kotlin.Function<*>> wrapJsFunction(function: Function): T = Context.jsToJava(function, T::class.java) as T