package com.sifsstudio.botjs.env.api.storage;

import java.util.Iterator;
import java.util.NoSuchElementException;

public sealed interface Path<T extends BotStorage<T>> extends Comparable<Path<T>>, Iterable<Path<T>> permits PathWrapper {
    T getStorage();
    Path<T> getFileName();
    Path<T> getParent();
    int getNameCount();
    Path<T> getName(int index);
    boolean startsWith(Path<T> other);
    default boolean startsWith(String other) {
        return startsWith(getStorage().of(other));
    }
    boolean endsWith(Path<T> other);
    default boolean endsWith(String other) {
        return endsWith(getStorage().of(other));
    }
    Path<T> normalize();
    Path<T> resolve(Path<T> other);
    default Path<T> resolveSibling(Path<T> other) {
        if (other == null)
            throw new NullPointerException();
        Path<T> parent = getParent();
        return (parent == null) ? other : parent.resolve(other);
    }
    default Path<T> resolveSibling(String other) {
        return resolveSibling(getStorage().of(other));
    }
    Path<T> relativize(Path<T> other);

    default Iterator<Path<T>> iterator() {
        return new Iterator<>() {
            private int i = 0;

            @Override
            public boolean hasNext() {
                return (i < getNameCount());
            }

            @Override
            public Path<T> next() {
                if (i < getNameCount()) {
                    Path<T> result = getName(i);
                    i++;
                    return result;
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
    }

    @Override
    int compareTo(Path<T> other);
    boolean equals(Object other);
    int hashCode();
}
