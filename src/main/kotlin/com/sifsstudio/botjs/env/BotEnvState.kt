package com.sifsstudio.botjs.env

enum class BotEnvState(val free: Boolean, val tickable: Boolean, val stopping: Boolean) {
    READY(true, false, false),
    RUNNING(false, true, false),
    SAFEPOINT(false, false, false),
    TERMINATING(false, false, true),
    UNLOADING(false, false, true)
}