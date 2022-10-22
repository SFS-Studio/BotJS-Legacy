package com.sifsstudio.botjs.env.api.storage

import com.sifsstudio.botjs.env.api.storage.FileLock

enum class FileLocks(private val writable: Boolean, private val readable: Boolean): FileLock {
    READ(false, true),
    WRITE(false, false);

    override fun canRead() = readable
    override fun canWrite() = writable
}