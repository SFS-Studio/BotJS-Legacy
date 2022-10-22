package com.sifsstudio.botjs.env.api.storage

import java.io.DataInput
import java.io.DataOutput

class ResourceHandleImpl: ResourceHandle {
    override fun tryAcquireInput(): DataInput {
        TODO("Not yet implemented")
    }

    override fun tryAcquireOutput(): DataOutput {
        TODO("Not yet implemented")
    }

    override fun getAssociatedLock(): FileLock {
        TODO("Not yet implemented")
    }

    override fun readDescriptor(): FileDescriptor {
        TODO("Not yet implemented")
    }

    override fun getLastFailure(): Failure {
        TODO("Not yet implemented")
    }
}