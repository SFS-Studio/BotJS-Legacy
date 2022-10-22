package com.sifsstudio.botjs.env.api.storage;

import java.util.Iterator;
import java.util.NoSuchElementException;

public sealed interface Path extends Comparable<Path>, Iterable<Path> permits PathWrapper {
    BotStorage getStorage();
    Path getFileName();
    Path getParent();
    int getNameCount();
    Path getName(int index);
    boolean startsWith(Path other);
    default boolean startsWith(String other) {
        return startsWith(getStorage().of(other));
    }
    boolean endsWith(Path other);
    default boolean endsWith(String other) {
        return endsWith(getStorage().of(other));
    }
    Path normalize();
    Path resolve(Path other);
    default Path resolveSibling(Path other) {
        if (other == null)
            throw new NullPointerException();
        Path parent = getParent();
        return (parent == null) ? other : parent.resolve(other);
    }
    default Path resolveSibling(String other) {
        return resolveSibling(getStorage().of(other));
    }
    Path relativize(Path other);

    default Iterator<Path> iterator() {
        return new Iterator<>() {
            private int i = 0;

            @Override
            public boolean hasNext() {
                return (i < getNameCount());
            }

            @Override
            public Path next() {
                if (i < getNameCount()) {
                    Path result = getName(i);
                    i++;
                    return result;
                } else {
                    throw new NoSuchElementException();
                }
            }
        };
    }

    @Override
    int compareTo(Path other);
    boolean equals(Object other);
    int hashCode();
}
