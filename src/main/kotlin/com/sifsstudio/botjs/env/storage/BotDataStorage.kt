package com.sifsstudio.botjs.env.storage

import com.sifsstudio.botjs.entity.BotEntity
import com.sifsstudio.botjs.util.ThreadLoop
import com.sifsstudio.botjs.util.set
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
        val perDimStorage: MutableMap<ResourceKey<Level>, BotDataStorage> = mutableMapOf()
        val chunkSet = mutableSetOf<ChunkPos>()
        fun onServerStarted(event: ServerStartedEvent) {
            val server = event.server
            val lvlR = server.getWorldPath(LevelResource.ROOT)
            server.levelKeys().forEach {
                val f = DimensionType.getStorageFolder(it, lvlR)
                perDimStorage[it] = BotDataStorage(f.resolve(BOT_DATA))
            }
        }

        fun onServerStopped(@Suppress("UNUSED_PARAMETER") event: ServerStoppedEvent) {
            perDimStorage.values.forEach { it.close() }
        }

        suspend fun readData(entity: BotEntity): BotSavedData? {
            val chunkPos = entity.chunkPosition()
            ThreadLoop.Sync.waitUntil(true) {
                if (!chunkSet.contains(chunkPos)) {
                    chunkSet.add(chunkPos)
                    false
                } else true
            }
            val key = entity.stringUUID
            val storage = perDimStorage[entity.getLevel().dimension()]!!
            return storage.readChunk(chunkPos)
                .filter { it.contains(key) }
                .map { it.getCompound(key) }
                .map(BotSavedData.Companion::deserialize)
                .getOrNull()
        }

        suspend fun writeData(entity: BotEntity, tag: CompoundTag) {
            val key = entity.stringUUID
            val storage = perDimStorage[entity.getLevel().dimension()]!!
            val chunkPos = entity.chunkPosition()
            val result = storage.readChunk(chunkPos).orElse(CompoundTag())
            result[key] = tag
            storage.writeChunk(chunkPos, result)
            ThreadLoop.Sync.await(true) {
                chunkSet.remove(chunkPos)
            }
        }

        fun refresh() {
            perDimStorage.values.forEach { it.worker.synchronize(true).join() }
        }
    }
}
