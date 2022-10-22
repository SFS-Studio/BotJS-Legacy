package com.sifsstudio.botjs.env.api;

public interface Bot {
    Object getMemory(Object memId);
    Object setMemory(Object memId, Object memory);
    boolean consumeNextTick();
}