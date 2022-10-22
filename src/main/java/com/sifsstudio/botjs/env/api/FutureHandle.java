package com.sifsstudio.botjs.env.api;

public interface FutureHandle<T> {
    boolean isDone();
    T result();
    T join();
}
