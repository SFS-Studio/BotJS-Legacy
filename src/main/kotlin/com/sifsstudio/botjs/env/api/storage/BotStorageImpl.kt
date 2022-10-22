package com.sifsstudio.botjs.env.api.storage

import com.sifsstudio.botjs.env.api.NioPath

class BotStorageImpl(root: NioPath): BotStorage {

    override fun visit(path: String): ResourceHandle {
        TODO("Not yet implemented")
    }

    override fun visit(path: Path): ResourceHandle {
        TODO("Not yet implemented")
    }

    override fun of(path: String): Path {
        TODO("Not yet implemented")
    }

    override fun getRoot(): Path {
        TODO("Not yet implemented")
    }

    override fun getActiveFiles(): MutableMap<Path, FileLock> {
        TODO("Not yet implemented")
    }

    override fun getActiveViewports(): MutableSet<BotStorage.Viewport> {
        TODO("Not yet implemented")
    }
}