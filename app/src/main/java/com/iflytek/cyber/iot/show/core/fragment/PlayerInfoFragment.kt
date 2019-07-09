/*
 * Copyright (C) 2019 iFLYTEK CO.,LTD.
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

package com.iflytek.cyber.iot.show.core.fragment

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.widget.TextViewCompat
import androidx.navigation.Navigation
import cn.iflyos.sdk.android.impl.common.iFLYOSPlayerHandler
import cn.iflyos.sdk.android.impl.mediaplayer.MediaPlayerHandler
import cn.iflyos.sdk.android.impl.mediaplayer.MediaSourceFactory
import cn.iflyos.sdk.android.impl.template.SimpleTemplateDispatcher
import cn.iflyos.sdk.android.impl.template.TemplateRuntimeHandler
import cn.iflyos.sdk.android.v3.iFLYOSManager
import cn.iflyos.sdk.android.v3.iface.MediaPlayer
import cn.iflyos.sdk.android.v3.iface.PlatformInterface
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.transition.Transition
import com.google.gson.Gson
import com.iflytek.cyber.iot.show.core.LauncherActivity
import com.iflytek.cyber.iot.show.core.R
import com.iflytek.cyber.iot.show.core.model.Content
import com.iflytek.cyber.iot.show.core.model.ContentStorage
import com.iflytek.cyber.iot.show.core.model.TemplateContent
import com.iflytek.cyber.iot.show.core.utils.RoundedCornersTransformation
import com.warkiz.widget.IndicatorSeekBar
import com.warkiz.widget.OnSeekChangeListener
import com.warkiz.widget.SeekParams
import jp.wasabeef.blurry.Blurry
import java.util.*

class PlayerInfoFragment : BaseFragment(), View.OnClickListener, OnSeekChangeListener,
        MediaPlayerHandler.OnMediaStateChangedListener {

    private var tvTitle: TextView? = null
    private var tvArtist: TextView? = null
    private var ivPrevious: ImageView? = null
    private var ivPlayPause: ImageView? = null
    private var ivNext: ImageView? = null
    private var ivAlbum: ImageView? = null
    private var tvCurrentPosition: TextView? = null
    private var tvDuration: TextView? = null
    private var ivProviderLogo: ImageView? = null
    private var ivBack: ImageView? = null
    private var ivBlurBackground: ImageView? = null
    private var tvTipsTitle: TextView? = null

    private var seekBar: IndicatorSeekBar? = null
    private var seekBarDragging = false
    private var seekBarProgressTarget = -1
    private var albumHeight: Int = 0
    private var currentSourceId: String = ""

//    private val progressHandler = Handler()

    private var playerHandler: MediaPlayerHandler? = null

    private val manager = iFLYOSManager.getInstance()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_player_info, container, false)
        return view
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val iFLYOSPlayerHandler = manager.getHandler("AudioMediaPlayer") as? iFLYOSPlayerHandler
        playerHandler = iFLYOSPlayerHandler?.mediaPlayer as? MediaPlayerHandler
        playerHandler?.addOnMediaStateChangedListener(this)

        val handler = manager.getHandler(PlatformInterface.SpecialHandler.TEMPLATETUNTIME.value())
        if (handler is TemplateRuntimeHandler) {
            handler.registerTemplateDispatchedListener(simpleTemplateDispatcher)
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        ivBack = view.findViewById(R.id.iv_back)
        tvTitle = view.findViewById(R.id.title)
        tvTipsTitle = view.findViewById(R.id.tv_tips_title)
        tvArtist = view.findViewById(R.id.artist)
        seekBar = view.findViewById(R.id.seek_bar)
        ivPlayPause = view.findViewById(R.id.play_pause)
        ivNext = view.findViewById(R.id.skip_next)
        ivPrevious = view.findViewById(R.id.skip_previous)
        tvCurrentPosition = view.findViewById(R.id.current_position)
        tvDuration = view.findViewById(R.id.duration)
        ivProviderLogo = view.findViewById(R.id.music_provider_logo)
        ivAlbum = view.findViewById(R.id.album)
        ivBlurBackground = view.findViewById(R.id.iv_blur_background)

        ivBack?.setOnClickListener { v -> Navigation.findNavController(v).navigateUp() }

        ivPrevious?.setOnClickListener(this)
        ivPrevious?.tag = TAG_PREVIOUS
        ivNext?.setOnClickListener(this)
        ivNext?.tag = TAG_NEXT
        ivPlayPause?.setOnClickListener(this)
        ivPlayPause?.tag = TAG_PLAY_PAUSE

        seekBar?.max = 100f
        seekBar?.isEnabled = false
        seekBar?.onSeekChangeListener = this

        setupView()
    }

    private fun setupView() {
        if (!isAdded && launcher != null) {
            return
        }

        Blurry.with(launcher)
                .sampling(4)
                .radius(75)
                .color(Color.parseColor("#66212121"))
                .from(getBitmapFromVectorDrawable(R.drawable.cover_default))
                .into(ivBlurBackground!!)

        val content = ContentStorage.get().currentContent
        if (content == null) {
            tvTitle?.visibility = View.GONE
            tvTipsTitle?.visibility = View.VISIBLE
            tvTipsTitle?.text = getString(R.string.default_music_title)
            tvArtist?.text = null
            tvArtist?.visibility = View.GONE
            tvCurrentPosition?.text = format(0)
            tvDuration?.text = format(0)
            setDefaultCover()
            ivPlayPause?.setImageResource(R.drawable.ic_play_circle_outline_white_24dp)
        } else {
            tvTipsTitle?.visibility = View.GONE
            tvTitle?.visibility = View.VISIBLE
            tvTitle?.text = content.title
            tvArtist?.text = content.titleSubtext1
            tvArtist?.visibility = View.VISIBLE
            tvTitle?.visibility = View.VISIBLE
            if (content.art != null && content.art?.sources != null
                    && content.art?.sources?.size ?: 0 > 0) {
                if (albumHeight > 0) {
                    realSetCover(content, true, albumHeight / 16)
                } else {
                    ivAlbum?.let {
                        it.viewTreeObserver?.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
                            override fun onGlobalLayout() {
                                realSetCover(content, true, it.height / 16)
                                it.viewTreeObserver?.removeOnGlobalLayoutListener(this)
                            }
                        })
                    }
                }
            } else {
                setDefaultCover()
            }
            if (content.provider != null) {
                if (content.provider?.logo != null && content.provider?.logo?.sources?.size ?: 0 > 0) {
                    ivProviderLogo?.visibility = View.VISIBLE
                    ivProviderLogo?.let {
                        Glide.with(launcher!!)
                                .load(content.provider?.logo?.sources?.get(0)?.url)
                                .listener(object : RequestListener<Drawable> {
                                    override fun onLoadFailed(e: GlideException?, model: Any,
                                                              target: Target<Drawable>, isFirstResource: Boolean): Boolean {
                                        it.visibility = View.GONE
                                        return false
                                    }

                                    override fun onResourceReady(resource: Drawable, model: Any,
                                                                 target: Target<Drawable>, dataSource: DataSource,
                                                                 isFirstResource: Boolean): Boolean {
                                        return false
                                    }
                                })
                                .into(it)
                    }
                }
            } else {
                ivProviderLogo?.visibility = View.GONE
            }
        }

        playerHandler?.let { playerHandler ->
            if (playerHandler.isPlaying()) {
                onMediaStateChanged("AudioMediaPlayer", "", MediaPlayer.MediaState.PLAYING)
            } else {
                onMediaStateChanged("AudioMediaPlayer", "", MediaPlayer.MediaState.STOPPED)
            }
            tvCurrentPosition?.text = format(playerHandler.getPosition())
            tvDuration?.text = format(playerHandler.duration)
            seekBar?.max = playerHandler.duration.toFloat()
            seekBar?.setProgress(playerHandler.getPosition().toFloat())
        }

        ivBack?.post {
            ivBack?.let { imageView ->
                val padding = imageView.height * 12 / 56
                imageView.setPadding(padding, padding, padding, padding)
                imageView.setImageResource(R.drawable.ic_previous_white_32dp)
            }

            view?.let { view ->
                val height = view.height

                tvTitle?.let { textView ->
                    val textSize = height * 48 / 600

                    textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize.toFloat())
                    TextViewCompat.setLineHeight(textView, textSize * 56 / 48)
                }

                tvArtist?.let { textView ->
                    val textSize = height * 24 / 600

                    textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize.toFloat())
                    TextViewCompat.setLineHeight(textView, textSize * 40 / 24)

                    val layoutParams = textView.layoutParams as? ConstraintLayout.LayoutParams
                    layoutParams?.let { lp ->
                        lp.topMargin = textSize / 2
                        textView.layoutParams = lp
                    }
                }

                val timeTextSize = height * 16 / 600
                tvCurrentPosition?.setTextSize(TypedValue.COMPLEX_UNIT_PX, timeTextSize.toFloat())
                tvDuration?.setTextSize(TypedValue.COMPLEX_UNIT_PX, timeTextSize.toFloat())

                view.findViewById<View>(R.id.info_area)?.let { infoArea ->
                    val horizontalPadding = ivAlbum?.left ?: 0
                    infoArea.setPadding(horizontalPadding, 0, horizontalPadding, 0)
                }

                tvTipsTitle?.let { textView ->
                    val textSize = height * 48 / 600

                    textView.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize.toFloat())
                    TextViewCompat.setLineHeight(textView, textSize * 56 / 48)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()

        (activity as? LauncherActivity)?.run {
            setStatusType(LauncherActivity.StatusType.PLAYER)
        }
    }

    private fun setDefaultCover() {
        val radius = IntArray(1)
        if (albumHeight > 0) {
            radius[0] = albumHeight / 16
            realSetCover(null, false, radius[0])
        } else {
            ivAlbum?.viewTreeObserver?.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
                override fun onGlobalLayout() {
                    albumHeight = ivAlbum!!.height
                    radius[0] = albumHeight / 16
                    realSetCover(null, false, radius[0])
                    ivAlbum?.viewTreeObserver?.removeOnGlobalLayoutListener(this)
                }
            })
        }
    }

    @SuppressLint("CheckResult")
    private fun realSetCover(content: Content?, shouldBlur: Boolean, radius: Int) {
        if (launcher == null) {
            return
        }

        if (shouldBlur && content?.art?.sources != null &&
                content.art?.sources?.size ?: 0 > 0) {
            Glide.with(ivAlbum!!.context)
                    .asBitmap()
                    .load(content.art?.sources?.get(0)?.url)
                    .apply(RequestOptions
                            .placeholderOf(R.drawable.cover_default)
                            .transform(RoundedCornersTransformation(radius, 0))
                            .error(R.drawable.cover_default))
                    .into(object : SimpleTarget<Bitmap>() {
                        override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                            ivAlbum?.setImageBitmap(resource)
                            ivBlurBackground?.let { blur ->
                                Blurry.with(launcher)
                                        .sampling(4)
                                        .color(Color.parseColor("#66212121"))
                                        .from(resource)
                                        .into(blur)
                            }
                        }

                        override fun onLoadFailed(errorDrawable: Drawable?) {
                            super.onLoadFailed(errorDrawable)
                            ivAlbum?.setImageResource(R.drawable.cover_default)
                        }
                    })
        } else {
            ivAlbum?.let {
                Glide.with(launcher!!)
                        .load(R.drawable.cover_default)
                        .apply(RequestOptions
                                .placeholderOf(R.drawable.cover_default)
                                .transform(RoundedCornersTransformation(radius, 0)))
                        .into(it)
            }
        }
    }

    fun requestFocus() {
        if (tvTitle != null && tvTitle?.visibility == View.VISIBLE) {
            tvTitle?.requestFocus()
        }
    }

    fun abandonFocus() {
        if (tvTitle != null)
            tvTitle?.clearFocus()
    }

    override fun onClick(v: View) {
        Log.d("PlayerInfo", "onClick: ${v.tag}")
        when (v.tag.toString()) {
            TAG_PREVIOUS -> manager.executeCommand(iFLYOSManager.Command.CMD_PLAY_PREVIOUS)
            TAG_NEXT -> manager.executeCommand(iFLYOSManager.Command.CMD_PLAY_NEXT)
            TAG_PLAY_PAUSE -> if (playerHandler?.isPlaying() == true) {
                manager.executeCommand(iFLYOSManager.Command.CMD_PLAY_PAUSE)
            } else {
                manager.executeCommand(iFLYOSManager.Command.CMD_PLAY_PLAY)
            }
        }
    }

    private fun format(duration: Long): String {
        return String.format(Locale.getDefault(), "%2d:%02d", duration / 1000 / 60, duration / 1000 % 60)
    }

    private fun setSeekBarEnabled(enabled: Boolean, resetProgress: Boolean = true) {
        seekBar?.isEnabled = enabled
        if (enabled) {
            tvDuration?.visibility = View.VISIBLE
            tvCurrentPosition?.visibility = View.VISIBLE
        } else {
            if (resetProgress) {
                seekBar?.setProgress(0f)

                tvCurrentPosition?.visibility = View.INVISIBLE
                tvDuration?.visibility = View.INVISIBLE
            }
        }
    }

    override fun onMediaStateChanged(playerName: String, sourceId: String, state: MediaPlayer.MediaState) {
        if (playerName != PLAYER_NAME)
            return

        if (currentSourceId == sourceId) {
            playerHandler?.let { playerHandler ->
                if (playerHandler.currentMediaType != MediaSourceFactory.MediaType.HLS) {
                    setSeekBarEnabled(true)
                } else {
                    setSeekBarEnabled(false)
                }
            }
        }

        if (state == MediaPlayer.MediaState.PLAYING) {
            ivPlayPause?.setImageResource(R.drawable.ic_pause_circle_outline_white_24dp)
            setSeekBarEnabled(true)
            ContentStorage.get().isMusicPlaying = true
//            progressHandler.post(progressCallback)
        } else if (state == MediaPlayer.MediaState.PAUSED) {
            ivPlayPause?.setImageResource(R.drawable.ic_play_circle_outline_white_24dp)
            ContentStorage.get().isMusicPlaying = false
//            progressHandler.removeCallbacks(progressCallback)
        }
        if (state == MediaPlayer.MediaState.STOPPED
                || state == MediaPlayer.MediaState.FINISH) {
            ivPlayPause?.setImageResource(R.drawable.ic_play_circle_outline_white_24dp)
            ContentStorage.get().isMusicPlaying = false
            setSeekBarEnabled(enabled = false, resetProgress = false)
        }

        currentSourceId = sourceId
    }

    override fun onMediaError(playerName: String, mediaError: MediaPlayer.MediaError, message: String) {
        Log.e("PlayerInfo", "play media error: $mediaError   message is: $message")
    }

    override fun onPositionUpdated(playerName: String, position: Long) {
        if (playerName != PLAYER_NAME)
            return
        onPositionUpdated(position)
    }

    private val simpleTemplateDispatcher = object : SimpleTemplateDispatcher() {
        override fun onPlayerInfoDispatched(template: String) {
            val templateContent = Gson().fromJson(template, TemplateContent::class.java)
            val prevTemplate = ContentStorage.get().template
            ContentStorage.get().saveContent(templateContent.content)
            if (prevTemplate == null || prevTemplate.audioItemId != templateContent.audioItemId) {
                setupView()
                ContentStorage.get().saveTemplate(templateContent)
            }
        }
    }

    override fun onSeeking(seekParams: SeekParams) {
        playerHandler?.let { playerHandler ->
            tvCurrentPosition?.text = format(seekParams.progress.toLong())
            tvDuration?.text = format(playerHandler.duration)
        } ?: run {
            tvCurrentPosition?.text = format(0)
            tvDuration?.text = format(0)
        }
        if (seekParams.fromUser) {
            seekBarProgressTarget = seekParams.progress
        }
    }

    override fun onStartTrackingTouch(seekBar: IndicatorSeekBar) {
        seekBarDragging = true
    }

    override fun onStopTrackingTouch(seekBar: IndicatorSeekBar) {
        seekBarDragging = false

        if (seekBarProgressTarget != -1) {
            playerHandler?.setPosition(seekBarProgressTarget.toLong())
            seekBarProgressTarget = -1
        }
    }

    private fun onPositionUpdated(position: Long) {
        if (seekBarDragging) {
            return
        }

        if (position > 0) {
            ivPlayPause?.setImageResource(R.drawable.ic_pause_circle_outline_white_24dp)
            setSeekBarEnabled(true)
        } else {
            ivPlayPause?.setImageResource(R.drawable.ic_play_circle_outline_white_24dp)
        }

        playerHandler?.let { playerHandler ->
            if (playerHandler.currentMediaType == MediaSourceFactory.MediaType.HLS) {
                setSeekBarEnabled(false)
            } else {
                setSeekBarEnabled(true)
                if (seekBar?.max != playerHandler.duration.toFloat())
                    seekBar?.max = playerHandler.duration.toInt().toFloat()
                seekBar?.setProgress(position.toInt().toFloat())
            }
        } ?: run {
            seekBar?.setProgress(0f)
        }
    }

//    private val progressCallback = object : Runnable {
//        override fun run() {
//            val position = playerHandler?.getPosition()
//            if (position != null && playerHandler?.isPlaying() == true) {
//                onPositionUpdated(position)
//            }
//            progressHandler.postDelayed(this, 1000)
//        }
//    }

    private fun getBitmapFromVectorDrawable(drawableId: Int): Bitmap {
        val drawable = ContextCompat.getDrawable(launcher!!, drawableId)

        val bitmap = Bitmap.createBitmap(drawable!!.intrinsicWidth,
                drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)

        return bitmap
    }

    override fun onDestroy() {
        super.onDestroy()
        playerHandler?.removeOnMediaStateChangedListener(this)
//        progressHandler.removeCallbacks(progressCallback)
        val handler = manager.getHandler(PlatformInterface.SpecialHandler.TEMPLATETUNTIME.value())
        if (handler is TemplateRuntimeHandler) {
            handler.unregisterDispatchedListener(simpleTemplateDispatcher)
        }
    }

    companion object {
        private const val PLAYER_NAME = "AudioMediaPlayer"

        private const val TAG_PREVIOUS = "PREVIOUS"
        private const val TAG_NEXT = "NEXT"
        private const val TAG_PLAY_PAUSE = "PLAY_PAUSE"
    }
}
