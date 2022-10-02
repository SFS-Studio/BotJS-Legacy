package com.sifsstudio.botjs.env.task

object ServerShutdown: Throwable("server_shutdown", null, false, false)
object BotDead: Throwable("bot_dead", null, false, false)
object ForceShutdown: Throwable("force_shutdown", null, false, false)