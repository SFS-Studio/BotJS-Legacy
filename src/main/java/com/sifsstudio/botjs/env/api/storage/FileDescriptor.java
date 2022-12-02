package com.sifsstudio.botjs.env.api.storage;

import java.time.Instant;

public interface FileDescriptor {
    String name();
    Instant createTime();
    Instant lastModifiedTime();
    int lengthByBytes();
}
