package com.sifsstudio.botjs.env.api.storage;

public record PathWrapper<T extends BotStorage<T>>(T storage, java.nio.file.Path path) implements Path<T> {
    @Override
    public T getStorage() {
        return storage;
    }

    @Override
    public Path<T> getFileName() {
        return new PathWrapper<>(storage, path.getFileName());
    }

    @Override
    public Path<T> getParent() {
        return new PathWrapper<>(storage, path.getParent());
    }

    @Override
    public int getNameCount() {
        return path.getNameCount();
    }

    @Override
    public Path<T> getName(int index) {
        return new PathWrapper<>(storage, path.getName(index));
    }

    @Override
    public boolean startsWith(Path<T> other) {
        return path.startsWith(((PathWrapper<T>)other).path);
    }

    @Override
    public boolean endsWith(Path<T> other) {
        return path.endsWith(((PathWrapper<T>)other).path);
    }

    @Override
    public Path<T> normalize() {
        return new PathWrapper<>(storage, path.normalize());
    }

    @Override
    public Path<T> resolve(Path<T> other) {
        return new PathWrapper<>(storage, path.resolve(((PathWrapper<T>)other).path));
    }

    @Override
    public Path<T> relativize(Path<T> other) {
        return new PathWrapper<>(storage, path.relativize(((PathWrapper<T>)other).path));
    }

    @Override
    public int compareTo(Path<T> other) {
        return path.compareTo(((PathWrapper<T>)other).path);
    }
}
