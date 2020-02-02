package com.example.review.New

import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.File
import java.io.IOException

interface SaveDataInterface {
    fun toBytes(): ByteArray?
    @Throws(IOException::class)
    fun toBytes(dos: DataOutputStream)

    @Throws(IOException::class)
    fun loadWith(rawBytes: ByteArray)

    @Throws(IOException::class)
    fun loadWith(dis: DataInputStream)

    fun save(path: File)
    @Throws(IOException::class)
    fun read(path: File)
}