package com.sifsstudio.botjs.env.api.wrapper

import com.sifsstudio.botjs.env.EnvInputStream
import com.sifsstudio.botjs.env.EnvOutputStream
import net.minecraft.world.entity.Entity
import java.io.*

@Suppress("unused")
class WrappedEntity() {

    constructor(entity: Entity) : this() {
        this.entity = entity
    }
    @Transient
    lateinit var entity: Entity
        private set

    fun getX() =
        if(::entity.isInitialized && entity.isAlive) {
            entity.x
        } else Double.MIN_VALUE
    fun getY() =
        if(::entity.isInitialized && entity.isAlive) {
            entity.y
        } else Double.MIN_VALUE
    fun getZ() =
        if(::entity.isInitialized && entity.isAlive) {
            entity.z
        } else Double.MIN_VALUE

    fun getType() =
        if(::entity.isInitialized && entity.isAlive) {
            entity.type.registryName.toString()
        } else ""

    @Throws(IOException::class, ClassNotFoundException::class)
    private fun readObject(stream: ObjectInputStream) {
        if(stream !is EnvInputStream) {
            throw InvalidObjectException("Not read from EnvInputStream")
        }
        stream.defaultReadObject()
        val id = stream.readInt()
        if(id != Int.MIN_VALUE) {
            stream.env.entity.level.getEntity(stream.readInt())?.let { entity = it }
        }
    }

    @Throws(IOException::class)
    private fun writeObject(stream: ObjectOutputStream) {
        if(stream !is EnvOutputStream) {
            throw InvalidObjectException("Not written to EnvOutputStream")
        }
        stream.defaultWriteObject()
        if(::entity.isInitialized && entity.isAlive) {
            stream.writeInt(entity.id)
        } else stream.writeInt(Int.MIN_VALUE)
    }

    @Throws(ObjectStreamException::class)
    private fun readObjectNoData(stream: ObjectInputStream): Unit = throw InvalidObjectException("No data was present")
}