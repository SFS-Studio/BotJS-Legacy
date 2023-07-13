package com.sifsstudio.botjs.env

import org.mozilla.javascript.Scriptable
import org.mozilla.javascript.ScriptableObject
import java.util.*
//TODO: Cache restoration in proper context
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