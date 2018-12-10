/*
 * Copyright (C) 2018 iFLYTEK CO.,LTD.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.iflytek.cyber.iot.show.core.impl.MediaPlayer

import android.content.Context
import android.content.Intent
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioTrack
import android.net.Uri
import android.os.Build
import android.util.Log
import cn.iflyos.iace.iflyos.MediaPlayer
import cn.iflyos.iace.iflyos.Speaker
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.iflytek.cyber.iot.show.core.impl.Logger.LoggerHandler
import com.iflytek.cyber.iot.show.core.impl.PlaybackController.PlaybackControllerHandler
import com.iflytek.cyber.iot.show.core.utils.ConnectivityUtils
import javazoom.jl.decoder.Bitstream
import javazoom.jl.decoder.Decoder
import javazoom.jl.decoder.Header
import javazoom.jl.decoder.SampleBuffer
import okio.*
import java.io.Closeable
import java.io.IOException


class MediaPlayerHandler(private val mContext: Context,
                         private val mLogger: LoggerHandler?,
                         private val mName: String,
                         speakerType: SpeakerType?,
                         controller: PlaybackControllerHandler?) : cn.iflyos.iace.iflyos.MediaPlayer() {
    private val mSpeaker: SpeakerHandler
    private val mMediaSourceFactory: MediaSourceFactory
    private var mPlaybackController: PlaybackControllerHandler? = null
    private var mAudioTrack: AudioTrack? = null
    private var usingAudioTrack = false
    private var cache: BufferedSink? = null
    private var source: BufferedSource? = null
    var player: SimpleExoPlayer? = null
        private set
    private var onMediaChangeListenerSet = HashSet<OnMediaStateChangedListener>()
    private var start = 0L
    private var mAudioTrackPosition = 0L

    val isPlaying: Boolean
        get() {
            return if (usingAudioTrack) {
                mAudioTrack?.playState == AudioTrack.PLAYSTATE_PLAYING
            } else
                (player != null && player?.playWhenReady ?: false
                        && (player?.playbackState == Player.STATE_BUFFERING || player?.playbackState == Player.STATE_READY))
        }

    val duration: Long
        get() {
            val duration = player?.duration
            return if (duration != C.TIME_UNSET) duration ?: 0 else 0
        }

    val speaker: Speaker
        get() = mSpeaker

    // For volume controls
    enum class SpeakerType {
        SYNCED, LOCAL
    }

    init {
        mSpeaker = SpeakerHandler(speakerType)
        mMediaSourceFactory = MediaSourceFactory(mContext, mLogger, mName)

        if (controller != null) {
            mPlaybackController = controller
            mPlaybackController?.mediaPlayer = this
        }
        initializePlayer()
    }

    private fun initializePlayer() {
        player = ExoPlayerFactory.newSimpleInstance(mContext, DefaultTrackSelector())
        player?.addListener(PlayerEventListener())
        player?.playWhenReady = false

        mAudioTrack =
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                    // deprecated since api 23
                    @Suppress("DEPRECATION")
                    AudioTrack(
                            AudioManager.STREAM_MUSIC, SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT,
                            MIN_BUFFER_SIZE,
                            AudioTrack.MODE_STREAM)
                } else {
                    AudioTrack.Builder()
                            .setTransferMode(AudioTrack.MODE_STREAM)
                            .setAudioFormat(AudioFormat.Builder()
                                    .setSampleRate(SAMPLE_RATE)
                                    .setChannelMask(CHANNEL_CONFIG)
                                    .setEncoding(AUDIO_FORMAT)
                                    .build())
                            .setAudioAttributes(AudioAttributes.Builder()
                                    .setLegacyStreamType(AudioManager.STREAM_MUSIC)
                                    .build())
                            .setBufferSizeInBytes(MIN_BUFFER_SIZE)
                            .build()
                }
        val volume = player?.volume ?: 0.5f
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            mAudioTrack?.setVolume(volume)
        } else {
            @Suppress("DEPRECATION")
            mAudioTrack?.setStereoVolume(volume, volume)
        }
    }

    private fun resetPlayer() {
        player?.repeatMode = Player.REPEAT_MODE_OFF
        player?.playWhenReady = false

        mAudioTrack?.flush()
    }

    //
    // Handle playback directives from AAC engine
    //

    override fun prepare(isOpusDong: Boolean): Boolean {
        usingAudioTrack = true

        mLogger?.postVerbose(sTag, String.format("(%s) Handling prepare()", mName))
        resetPlayer()
//        mContext.getFileStreamPath(sFileName).delete()
//        try {
//            RandomAccessFile(mContext.getFileStreamPath(sFileName), "rw").use { os ->
//                if (isOpusDong) {
//                    try {
//                        ByteArrayOutputStream().use { bufStream ->
//                            os.channel.use { fc ->
//                                val buffer = ByteArray(4096)
//                                while (!isClosed) {
//                                    var size = read(buffer)
//                                    while (size > 0) {
//                                        bufStream.write(buffer, 0, size)
//                                        size = read(buffer)
//                                    }
//                                }
//                                OpusDongHelper.decodeArrayToChannel(bufStream.toByteArray(), fc)
//                            }
//                        }
//                    } catch (e: Exception) {
//                        mLogger?.postError(sTag, e)
//                        return false
//                    }
//
//                } else {
//                    val buffer = ByteArray(4096)
//                    while (!isClosed) {
//                        var size = read(buffer)
//                        while (size > 0) {
//                            os.write(buffer, 0, size)
//                            size = read(buffer)
//                        }
//                    }
//                }
//            }
//        } catch (e: IOException) {
//            mLogger?.postError(sTag, e)
//            return false
//        }
//
//        return try {
//            val uri = Uri.fromFile(mContext.getFileStreamPath(sFileName))
//            val mediaSource = mMediaSourceFactory.createFileMediaSource(uri)
//            player?.prepare(mediaSource, true, false)
//            true
//        } catch (e: Exception) {
//            mLogger?.postError(sTag, e)
//            val message = if (e.message != null) e.message else ""
//            mediaError(MediaPlayer.MediaError.MEDIA_ERROR_UNKNOWN, message)
//            false
//        }
        val pipe = Pipe(4 * 1024 * 1024)
        cache = Okio.buffer(pipe.sink())
        source = Okio.buffer(pipe.source())

        Thread(Runnable {
            try {
                readAudioTrack()
            } catch (e: Exception) {
                e.printStackTrace()
                mLogger?.postError(sTag, e)
            }
        }).start()
        Thread(Runnable {
            try {
                writeAudioTrack(isOpusDong)
            } catch (e: Exception) {
                e.printStackTrace()
                mLogger?.postError(sTag, e)
            }
        }).start()
        return true
    }

    private fun writeAudioTrack(isOpusDong: Boolean) {
        onPlaybackStarted()
        start = System.currentTimeMillis()
        if (isOpusDong) {
            val decoder = OpusDecoderSource(source)
            val buffer = Buffer()
            var length: Long
            //val file = RandomAccessFile(mContext.getFileStreamPath(sDecodedFileName), "rw")
            try {
                length = decoder.read(buffer, 640)
                while (length != -1L) {
                    val size = buffer.size()
                    val array = buffer.readByteArray()
                    mAudioTrack?.write(array, 0, size.toInt())
                    mAudioTrackPosition = System.currentTimeMillis() - start
                    //file.write(array, 0, size.toInt())
                    length = decoder.read(buffer, 640)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                //file.close()
                decoder.close()
                mAudioTrackPosition = 0L
                onPlaybackFinished()
            }
        } else {
            val decoder = Decoder()
            val bitStream = Bitstream(source?.inputStream())
            var header: Header?
            var frameRead = Int.MAX_VALUE
            try {
                header = bitStream.readFrame()
                while (frameRead > 0) {
                    val sampleBuffer = decoder.decodeFrame(header, bitStream) as SampleBuffer
                    val buffer = sampleBuffer.buffer
                    mAudioTrack?.write(buffer, 0, sampleBuffer.bufferLength)
                    mAudioTrackPosition = System.currentTimeMillis() - start
                    bitStream.closeFrame()

                    header = bitStream.readFrame()
                    if (header == null) {
                        break
                    }
                    frameRead--
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                bitStream.close()
                mAudioTrackPosition = 0L
                onPlaybackFinished()
            }
        }
    }

    @Throws(IOException::class)
    private fun readAudioTrack() {
        val buffer = ByteArray(4096)
        //val file = RandomAccessFile(mContext.getFileStreamPath(sSourceFileName), "rw")
        while (!isClosed) {
            var size = read(buffer)
            while (size > 0) {
                cache?.write(buffer, 0, size)
                //file.write(buffer, 0, size)
                size = read(buffer)
            }
        }
        //file.close()
        safeClose(cache)
    }

    override fun prepare(url: String?): Boolean {
        usingAudioTrack = false

        mLogger?.postVerbose(sTag, String.format("(%s) Handling prepare(url)", mName))
        resetPlayer()
        val uri = Uri.parse(url)
        return try {
            val mediaSource = mMediaSourceFactory.createHttpMediaSource(uri)
            player?.prepare(mediaSource, true, false)
            true
        } catch (e: Exception) {
            val message = if (e.message != null) e.message else ""
            mLogger?.postError(sTag, e)
            mediaError(MediaPlayer.MediaError.MEDIA_ERROR_UNKNOWN, message)
            false
        }

    }

    override fun play(): Boolean {
        mLogger?.postVerbose(sTag, String.format("(%s) Handling play()", mName))

        if (usingAudioTrack) {
            mAudioTrack?.play()
        } else {
            player?.playWhenReady = true
        }

        return true
    }

    override fun stop(): Boolean {
        mLogger?.postVerbose(sTag, String.format("(%s) Handling stop()", mName))

        if (usingAudioTrack) {
            safeClose(source)
            source = null
            safeClose(cache)
            cache = null

            mAudioTrack?.stop()
            onPlaybackStopped()
        } else {
            player?.playWhenReady = false
        }

        return true
    }

    private fun safeClose(closeable: Closeable?) {
        try {
            closeable?.close()
        } catch (e: Exception) {
            // ignore
        }
    }

    override fun pause(): Boolean {
        mLogger?.postVerbose(sTag, String.format("(%s) Handling pause()", mName))
        player?.playWhenReady = false

        mAudioTrack?.pause()

        return true
    }

    override fun resume(): Boolean {
        mLogger?.postVerbose(sTag, String.format("(%s) Handling resume()", mName))
        player?.playWhenReady = true

        mAudioTrack?.play()

        return true
    }

    override fun setPosition(position: Long): Boolean {
        mLogger?.postVerbose(sTag, String.format("(%s) Handling setPosition(%s)", mName, position))
        player?.seekTo(position)
        return true
    }

    override fun getPosition(): Long {
        return if (usingAudioTrack) {
            mAudioTrackPosition
        } else
            Math.abs(player?.currentPosition ?: 0)
    }

    //
    // Handle ExoPlayer state changes and notify AAC engine
    //

    private fun onPlaybackStarted() {
        mLogger?.postVerbose(sTag, String.format("(%s) Media State Changed. STATE: PLAYING", mName))
        onMediaStateChanged(MediaPlayer.MediaState.PLAYING)
        if (mPlaybackController != null) {
            mPlaybackController?.start()
        }
    }

    private fun onPlaybackStopped() {
        mLogger?.postVerbose(sTag, String.format("(%s) Media State Changed. STATE: STOPPED", mName))
        onMediaStateChanged(MediaPlayer.MediaState.STOPPED)
        if (mPlaybackController != null) {
            mPlaybackController?.stop()
        }
    }

    private fun onPlaybackFinished() {
        Log.d(sTag, "onPlaybackFinished")
        if (isRepeating) {
            player?.seekTo(0)
            player?.repeatMode = Player.REPEAT_MODE_ONE
        } else {
            player?.repeatMode = Player.REPEAT_MODE_OFF
            if (mPlaybackController != null) {
                mPlaybackController?.reset()
            }
            mLogger?.postVerbose(sTag, String.format("(%s) Media State Changed. STATE: STOPPED", mName))
            onMediaStateChanged(MediaPlayer.MediaState.STOPPED)
            if (mPlaybackController != null) {
                mPlaybackController?.stop()
            }
        }
    }

    private fun onPlaybackBuffering() {
        mLogger?.postVerbose(sTag, String.format("(%s) Media State Changed. STATE: BUFFERING", mName))
        onMediaStateChanged(MediaPlayer.MediaState.BUFFERING)
    }

    fun localStop() {
        mLogger?.postVerbose(sTag, String.format("(%s) Handling localStop()", mName))

        if (usingAudioTrack) {
            safeClose(source)
            source = null
            safeClose(cache)
            cache = null

            mAudioTrack?.stop()
            onPlaybackStopped()
        } else {
            if (!ConnectivityUtils.isNetworkAvailable(mContext)) {
                player?.playWhenReady = false
            }
        }
    }

    //
    // ExoPlayer event listener
    //
    private inner class PlayerEventListener : Player.DefaultEventListener() {

        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            Log.i("PlayerEventListener", "playback state: " + playbackState)
            when (playbackState) {
                Player.STATE_ENDED -> if (playWhenReady) onPlaybackFinished()
                Player.STATE_READY -> if (playWhenReady)
                    onPlaybackStarted()
                else
                    onPlaybackStopped()
                Player.STATE_BUFFERING -> if (playWhenReady) onPlaybackBuffering()
                else -> {
                }
            }// Disregard other states
        }

        override fun onPlayerError(e: ExoPlaybackException?) {
            val message: String = when {
                e?.type == ExoPlaybackException.TYPE_SOURCE ->
                    "ExoPlayer Source Error: " + e.sourceException.message
                e?.type == ExoPlaybackException.TYPE_RENDERER ->
                    "ExoPlayer Renderer Error: " + e.rendererException.message
                e?.type == ExoPlaybackException.TYPE_UNEXPECTED ->
                    "ExoPlayer Unexpected Error: " + e.unexpectedException.message
                else -> e?.message ?: ""
            }
            mLogger?.postError(sTag, "PLAYER ERROR: $message")
            mediaError(MediaPlayer.MediaError.MEDIA_ERROR_INTERNAL_DEVICE_ERROR, message)
        }
    }

    //
    // SpeakerHandler
    //

    inner class SpeakerHandler internal constructor(type: SpeakerType?) : Speaker() {

        //        private SeekBar mVolumeControl;
        //        private TextView mMuteButton;
        private var mVolume: Byte = 50
        private var mIsMuted = false

        init {
            if (type != SpeakerType.SYNCED) {
                // Link all non synced speakers with the same UI control
                localSpeakers.add(this)
            } else {
                // Link mute button to synced speakers only
            }
            setupUIVolumeControls(type)
        }

        override fun setVolume(volume: Byte): Boolean {
            mLogger?.postInfo(sTag, String.format("(%s) Handling setVolume(%s)", mName, volume))
            mVolume = volume
            if (mIsMuted) {
                player?.volume = 0f
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    mAudioTrack?.setVolume(0f)
                } else {
                    @Suppress("DEPRECATION")
                    mAudioTrack?.setStereoVolume(0f, 0f)
                }
                updateUIVolume(0.toByte())
            } else {
                val channelVolume = volume / 100f
                player?.volume = channelVolume
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    mAudioTrack?.setVolume(channelVolume)
                } else {
                    @Suppress("DEPRECATION")
                    mAudioTrack?.setStereoVolume(channelVolume, channelVolume)
                }
                updateUIVolume(volume)
            }
            return true
        }

        override fun adjustVolume(value: Byte): Boolean {
            return setVolume((mVolume + value).toByte())
        }

        override fun getVolume(): Byte {
            return if (mIsMuted)
                0
            else
                mVolume
        }

        override fun setMute(mute: Boolean): Boolean {
            if (mute && !mIsMuted) {
                mLogger?.postInfo(sTag, String.format("Handling mute (%s)", mName))
                updateMuteButton(true)
            } else if (!mute && mIsMuted) {
                mLogger?.postInfo(sTag, String.format("Handling unmute (%s)", mName))
                updateMuteButton(false)
            }

            mIsMuted = mute
            if (mute) {
                player?.volume = 0f
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    mAudioTrack?.setVolume(0f)
                } else {
                    @Suppress("DEPRECATION")
                    mAudioTrack?.setStereoVolume(0f, 0f)
                }
                updateUIVolume(0.toByte())
            } else {
                player?.volume = mVolume / 100f
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    mAudioTrack?.setVolume(mVolume / 100f)
                } else {
                    @Suppress("DEPRECATION")
                    mAudioTrack?.setStereoVolume(mVolume / 100f, mVolume / 100f)
                }
                updateUIVolume(mVolume)
            }
            return true
        }

        override fun isMuted(): Boolean {
            return mIsMuted
        }

        private fun setupUIVolumeControls(type: SpeakerType?) {
            updateUIVolume(mVolume)
            updateMuteButton(mIsMuted)
        }

        private fun updateMuteButton(isMuted: Boolean) {
            val intent = Intent(ACTION_MUTE_CHANGED)
            intent.putExtra(EXTRA_MUTE, isMuted)
            mContext.sendBroadcast(intent)
        }

        private fun updateUIVolume(volume: Byte) {
            val intent = Intent(ACTION_VOLUME_CHANGED)
            intent.putExtra(EXTRA_VOLUME, volume)
            mContext.sendBroadcast(intent)
        }


    }

    fun addOnMediaStateChangedListener(listener: OnMediaStateChangedListener) {
        onMediaChangeListenerSet.add(listener)
    }

    fun removeOnMediaStateChangedListener(listener: OnMediaStateChangedListener): Boolean {
        return onMediaChangeListenerSet.remove(listener)
    }

    private fun onMediaStateChanged(state: MediaPlayer.MediaState) {
        onMediaChangeListenerSet.map {
            it.onMediaStateChanged(state)
        }
        mediaStateChanged(state)
    }

    interface OnMediaStateChangedListener {
        fun onMediaStateChanged(state: MediaPlayer.MediaState)
    }

    companion object {

        private const val sTag = "MediaPlayer"
        private const val sFileName = "iflyos_media" // Note: not thread safe
        private const val sSourceFileName = "source_media"
        private const val sDecodedFileName = "decoded_media.pcm"

        private val localSpeakers = ArrayList<SpeakerHandler>()

        const val ACTION_MUTE_CHANGED = "com.iflytek.cyber.iot.show.core.ACTION_MUTE_CHANGED"
        const val ACTION_VOLUME_CHANGED = "com.iflytek.cyber.iot.show.core.ACTION_VOLUME_CHANGED"
        const val EXTRA_MUTE = "mute"
        const val EXTRA_VOLUME = "volume"

        private const val SAMPLE_RATE = 16000
        private const val CHANNEL_CONFIG = AudioFormat.CHANNEL_OUT_MONO
        private const val AUDIO_FORMAT = AudioFormat.ENCODING_PCM_16BIT
        private val MIN_BUFFER_SIZE = AudioTrack.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
    }
}
