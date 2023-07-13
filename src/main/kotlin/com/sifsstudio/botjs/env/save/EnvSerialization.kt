package com.sifsstudio.botjs.env.save

import com.sifsstudio.botjs.env.BotEnv
import org.mozilla.javascript.ScriptableObject
import org.mozilla.javascript.serialize.ScriptableInputStream
import org.mozilla.javascript.serialize.ScriptableOutputStream
import java.io.InputStream
import java.io.OutputStream

class EnvInputStream(internal val env: BotEnv, input: InputStream, scope: ScriptableObject) :
    ScriptableInputStream(input, scope)

class EnvOutputStream(internal val env: BotEnv, output: OutputStream, scope: ScriptableObject) :
    ScriptableOutputStream(output, scope) {
    var simpleFuture = false
}
