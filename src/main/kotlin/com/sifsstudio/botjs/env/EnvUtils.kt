package com.sifsstudio.botjs.env

import org.mozilla.javascript.Context
import org.mozilla.javascript.Function
import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject
import java.util.UUID

@Synchronized
fun BotEnv.addCache(value: Scriptable): String {
    var uid: String
    do {
        uid = UUID.randomUUID().toString()
    } while (cacheScope.containsKey(uid))
    cacheScope.defineProperty(uid, value, ScriptableObject.CONST)
    return uid
}

@Synchronized
fun BotEnv.getCache(key: String): Scriptable {
    return try {
        cacheScope[key] as Scriptable
    } finally {
        cacheScope.delete(key)
    }
}

inline fun<reified T: kotlin.Function<*>> wrapJsFunction(function: Function): T = Context.jsToJava(function, T::class.java) as T