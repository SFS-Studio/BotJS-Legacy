package com.sifsstudio.botjs.env.api.storage;

import java.io.DataInput;
import java.io.DataOutput;

public interface ResourceHandle {
    DataInput tryAcquireInput();
    DataOutput tryAcquireOutput();
    FileLock getAssociatedLock();
    FileDescriptor readDescriptor();
    Failure getLastFailure();
}
