package com.sifsstudio.botjs.env.api.storage;

public record PathWrapper(BotStorage storage, java.nio.file.Path path) implements Path {
    @Override
    public BotStorage getStorage() {
        return storage;
    }

    @Override
    public Path getFileName() {
        return new PathWrapper(storage, path.getFileName());
    }

    @Override
    public Path getParent() {
        return new PathWrapper(storage, path.getParent());
    }

    @Override
    public int getNameCount() {
        return path.getNameCount();
    }

    @Override
    public Path getName(int index) {
        return new PathWrapper(storage, path.getName(index));
    }

    @Override
    public boolean startsWith(Path other) {
        return path.startsWith(((PathWrapper)other).path);
    }

    @Override
    public boolean endsWith(Path other) {
        return path.endsWith(((PathWrapper)other).path);
    }

    @Override
    public Path normalize() {
        return new PathWrapper(storage, path.normalize());
    }

    @Override
    public Path resolve(Path other) {
        return new PathWrapper(storage, path.resolve(((PathWrapper)other).path));
    }

    @Override
    public Path relativize(Path other) {
        return new PathWrapper(storage, path.relativize(((PathWrapper)other).path));
    }

    @Override
    public int compareTo(Path other) {
        return path.compareTo(((PathWrapper)other).path);
    }
}
