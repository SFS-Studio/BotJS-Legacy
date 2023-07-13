package com.sifsstudio.botjs.env

import com.sifsstudio.botjs.env.task.TaskFuture
import com.sifsstudio.botjs.env.task.TickableTask
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.ExperimentalTime
import kotlin.time.TimeSource

@OptIn(ExperimentalTime::class)
fun <T : Any> BotEnv.submit(task: TickableTask<T>, willBlock: Boolean): TaskFuture<T> {
    if (controller.runJob?.isCancelled == true) {
        throw CancellationException()
    }
    val cx = suspensionContext!!.context!! as BotEnv.EnvContext
    cx.startTime = TimeSource.Monotonic.markNow()
    val result = taskHandler.submit(task)
    if(!willBlock)enterSafepointSubmit(result)
    return result
}

suspend fun <T : Any> BotEnv.block(future: TaskFuture<T>, cx: SuspensionContext): T {
    taskHandler.block(future, cx)
    if (!future.isDone) {
        throw Suspension()
    } else {
        enterSafepointBlock(cx)
    }
    return future.result
}