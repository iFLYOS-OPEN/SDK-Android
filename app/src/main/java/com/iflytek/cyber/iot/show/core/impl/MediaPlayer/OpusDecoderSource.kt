package com.iflytek.cyber.iot.show.core.impl.MediaPlayer

import android.util.Log

import java.io.IOException

import cn.iflyos.iace.utils.OpusDongHelper
import okio.Buffer
import okio.BufferedSource
import okio.Source
import okio.Timeout
import kotlin.experimental.and

internal class OpusDecoderSource(private val source: BufferedSource?) : Source {

    private val opus = ByteArray(256)

    private val decoder = OpusDongHelper()

    init {
        Log.d(TAG, "Decoding stream started")
    }

    @Throws(IOException::class)
    override fun read(sink: Buffer, byteCount: Long): Long {
        val source = source ?: return -1
        var read = 0
        while (read < byteCount) {
            opus[0] = source.readByte()
            val pkgLen = opus[0].and(0xff.toByte())
            if (pkgLen <= 0) {
                return -1
            }

            var offset = 0
            do {
                val readSize = source.read(opus, offset + 1, pkgLen - offset)
                if (readSize < 0) {
                    break
                }
                offset += readSize
            } while (offset < pkgLen)

            val pcm = decoder.decode(opus)

            sink.write(pcm)
            sink.flush()

            read += pcm.size
        }

        return read.toLong()
    }

    override fun timeout(): Timeout {
        return Timeout.NONE
    }

    @Throws(IOException::class)
    override fun close() {
        Log.d(TAG, "Decoding finished")
        source?.close()
        decoder.destory()
    }

    companion object {

        private const val TAG = "OpusDecoderSource"
    }

}
