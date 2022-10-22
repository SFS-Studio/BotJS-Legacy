package com.sifsstudio.botjs.env.api.storage;

public interface FileLock {
    boolean canRead();
    boolean canWrite();
}
