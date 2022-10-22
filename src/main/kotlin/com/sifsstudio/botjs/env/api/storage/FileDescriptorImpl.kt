package com.sifsstudio.botjs.env.api.storage

import com.sifsstudio.botjs.env.api.storage.FileDescriptor
import com.sifsstudio.botjs.env.api.storage.Path
import java.time.Instant

data class FileDescriptorImpl(val name: String,
                              val location: Path,
                              val createTime: Instant,
                              val lastModifiedTime: Instant,
                              val lengthByBytes: Int): FileDescriptor {
    override fun name() = name

    override fun location() = location

    override fun createTime() = createTime

    override fun lastModifiedTime() = lastModifiedTime

    override fun lengthByBytes() = lengthByBytes
}
