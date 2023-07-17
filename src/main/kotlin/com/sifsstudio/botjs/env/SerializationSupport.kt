package com.sifsstudio.botjs.env

import com.sifsstudio.botjs.env.save.EnvInputStream
import com.sifsstudio.botjs.env.save.EnvOutputStream
import com.sifsstudio.botjs.env.task.TaskFuture
import org.apache.commons.codec.binary.Base64
import org.mozilla.javascript.Context
import org.mozilla.javascript.NativeObject
import org.mozilla.javascript.ScriptableObject
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

inline var BotEnv.serializedFrame
    get() = this.data.frame
    set(value) {this.data.frame = value}

//Serialization operation
fun BotEnv.writeSerializedFrame(continuation: Any, result: TaskFuture<*>?): String {
    val baos = ByteArrayOutputStream()
    val sos = EnvOutputStream(this, baos, scope)
    sos.writeObject(continuation)
    sos.writeObject(scope)
    sos.writeObject(cacheScope)
    sos.writeBoolean(result != null)
    result?.let {
        sos.simpleFuture = true
        sos.writeObject(it)
    }
    return Base64.encodeBase64String(baos.toByteArray())
}

fun BotEnv.readSerializedFrame(frame: String, cx: Context) {
    val bais = ByteArrayInputStream(Base64.decodeBase64(frame))
    val sis = EnvInputStream(this, bais, cx.initStandardObjects())
    continuation = sis.readObject()
    scope = sis.readObject() as ScriptableObject
    cacheScope = sis.readObject() as NativeObject
    retVal = if(sis.readBoolean()) {
        sis.readObject() as TaskFuture<*>
    } else null
}

fun BotEnv.serialize() = data.apply {
    val cont = continuation
    val retVal = retVal
    serializedFrame = if(cont != null) writeSerializedFrame(cont, retVal) else ""
    tasks = taskHandler.serialize()
}

fun BotEnv.deserialize(cx: Context) {
    if(serializedFrame.isNotEmpty()) {
        readSerializedFrame(serializedFrame, cx)
        taskHandler.deserialize(data.tasks)
        controller.run {
            runState.set(BotEnvState.SAFEPOINT)
        }
    }
}
