package com.sifsstudio.botjs.env.save

import com.sifsstudio.botjs.entity.BotEntity
import com.sifsstudio.botjs.util.ThreadLoop
import com.sifsstudio.botjs.util.set
import com.sifsstudio.botjs.util.waitUntil
import net.minecraft.nbt.CompoundTag
import net.minecraft.resources.ResourceKey
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.Level
import net.minecraft.world.level.chunk.storage.IOWorker
import net.minecraft.world.level.dimension.DimensionType
import net.minecraft.world.level.storage.LevelResource
import net.minecraftforge.event.server.ServerStartedEvent
import net.minecraftforge.event.server.ServerStoppedEvent
import java.io.Closeable
import java.nio.file.Path
import java.util.*
import java.util.concurrent.CompletionException
import kotlin.jvm.optionals.getOrNull

class BotDataStorage(root: Path) : Closeable {
    private val worker = IOWorker(root, false, "BotJS-BotData")
    private var active = true
    private fun readChunk(posIn: ChunkPos): Optional<CompoundTag> {
        return worker.loadAsync(posIn)
            .handle { tag, ex ->
                if (ex != null) {
                    throw if (ex is CompletionException) ex.cause!! else ex
                }
                tag
            }.join()
    }

    private fun writeChunk(posIn: ChunkPos, data: CompoundTag) {
        if (data.isEmpty) {
            return
        }
        worker.store(posIn, data)
    }

    override fun close() {
        check(active)
        active = false
        worker.synchronize(true).join()
        worker.close()
    }

    @Suppress("MemberVisibilityCanBePrivate")
    companion object {
        const val BOT_DATA = "botjs_memory"
        val perDimStorage = mutableMapOf<ResourceKey<Level>, BotDataStorage>()
        val perDimChunkSet = mutableMapOf<ResourceKey<Level>, MutableSet<ChunkPos>>()
        fun onServerStarted(event: ServerStartedEvent) {
            val server = event.server
            val lvlR = server.getWorldPath(LevelResource.ROOT)
            server.levelKeys().forEach {
                val f = DimensionType.getStorageFolder(it, lvlR)
                perDimStorage[it] = BotDataStorage(f.resolve(BOT_DATA))
                perDimChunkSet[it] = mutableSetOf()
            }
        }

        fun onServerStopped(@Suppress("UNUSED_PARAMETER") event: ServerStoppedEvent) {
            perDimStorage.values.forEach {
                it.close()
            }
        }

        private suspend inline fun<T> chunkLock(entity: BotEntity, block: (ChunkPos) -> T):T {
            val chunkPos = entity.chunkPosition()
            val chunkSet = perDimChunkSet[entity.level.dimension()]!!
            //lock
            ThreadLoop.Sync.waitUntil(true) {
                if (chunkPos !in chunkSet) {
                    chunkSet += chunkPos
                    false
                } else true
            }
            try {
                return block(chunkPos)
            } finally {
                //Unlock
                ThreadLoop.Sync.execute { chunkSet -= chunkPos }
            }
        }

        suspend fun readData(entity: BotEntity) = chunkLock(entity) {
            val key = entity.stringUUID
            val storage = perDimStorage[entity.level.dimension()]!!
            storage.readChunk(it)
                .filter { it.contains(key) }
                .map { it.getCompound(key) }
                .map(entity.environment.data::deserialize)
                .getOrNull()
        }

        suspend fun writeData(entity: BotEntity, tag: CompoundTag) = chunkLock(entity) {
            val key = entity.stringUUID
            val storage = perDimStorage[entity.getLevel().dimension()]!!
            val result = storage.readChunk(it).orElse(CompoundTag())
            result[key] = tag
            storage.writeChunk(it, result)
        }

        fun refresh() {
            perDimStorage.values.forEach { it.worker.synchronize(true).join() }
        }
    }
}
