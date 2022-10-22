package com.sifsstudio.botjs.env.api.storage;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

public interface BotStorage {
    ResourceHandle visit(String path);
    ResourceHandle visit(Path path);
    Path of(String path);
    Path getRoot();
    Map<Path, FileLock> getActiveFiles();
    Set<Viewport> getActiveViewports();

    interface Viewport {
        Path getPosition();
        Instant getWhenEstablish();
        Instant getLastOperationTime();
        RemoteType getRemoteType();
    }
}