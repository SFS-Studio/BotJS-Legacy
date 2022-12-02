package com.sifsstudio.botjs.env.api.storage;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

public interface BotStorage<T extends BotStorage<T>> {
    ResourceHandle visit(String path);
    ResourceHandle visit(Path<T> path);
    Path<T> of(String path);
    Path<T> getRoot();
    Path<T> queryWritelock();
    Set<Path<T>> queryReadlock();
    Map<?, Viewport<T>> getActiveViewports();
    Viewport<T> createViewport(Object holder);

    interface Viewport<T extends BotStorage<T>> {
        Path<T> getPosition();
        Instant getWhenEstablish();
        Instant getLastOperationTime();
        RemoteType getRemoteType();
    }
}