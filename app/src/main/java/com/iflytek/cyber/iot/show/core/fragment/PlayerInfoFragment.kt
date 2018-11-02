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

package com.iflytek.cyber.iot.show.core.fragment

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.support.v4.content.ContextCompat
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.ImageView
import android.widget.TextView
import androidx.navigation.Navigation
import cn.iflyos.iace.iflyos.MediaPlayer
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.transition.Transition
import com.google.gson.Gson
import com.iflytek.cyber.iot.show.core.R
import com.iflytek.cyber.iot.show.core.impl.Logger.LogEntry
import com.iflytek.cyber.iot.show.core.impl.Logger.LoggerHandler
import com.iflytek.cyber.iot.show.core.impl.PlaybackController.PlaybackControllerHandler
import com.iflytek.cyber.iot.show.core.model.Content
import com.iflytek.cyber.iot.show.core.model.ContentStorage
import com.iflytek.cyber.iot.show.core.model.TemplateContent
import com.iflytek.cyber.iot.show.core.utils.RoundedCornersTransformation
import com.warkiz.widget.IndicatorSeekBar
import com.warkiz.widget.OnSeekChangeListener
import com.warkiz.widget.SeekParams
import jp.wasabeef.blurry.Blurry
import java.util.*

class PlayerInfoFragment : BaseFragment(), View.OnClickListener, PlaybackControllerHandler.PlaybackCallback, Observer, OnSeekChangeListener {

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

    private var playbackController: PlaybackControllerHandler? = null


    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        val view = inflater.inflate(R.layout.fragment_player_info, container, false)
        return view
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        if (null != launcher && launcher?.mEngineService != null) {
            playbackController = launcher?.mEngineService?.getPlaybackController()
            playbackController?.setPlaybackCallback(this)
        }

        if (launcher != null) {
            launcher?.addObserver(this)
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
                                .load(content.provider?.logo!!.sources[0].url)
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

        if (playbackController != null) {
            if (playbackController?.mediaPlayer?.isPlaying == true) {
                onPlaybackStateChanged(MediaPlayer.MediaState.PLAYING)
            } else {
                onPlaybackStateChanged(MediaPlayer.MediaState.STOPPED)
            }
        }

        if (null != playbackController) {
            tvCurrentPosition?.text = format(playbackController?.mediaPlayer?.position ?: 0)
            tvDuration?.text = format(playbackController?.mediaPlayer?.duration ?: 0)
            seekBar?.max = playbackController?.mediaPlayer?.duration?.toFloat()!!
            seekBar?.setProgress(playbackController?.mediaPlayer?.position?.toFloat()!!)
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
                    .load(content.art!!.sources[0].url)
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
        // id is not static-final
        if (playbackController == null) {
            return
        }
        val mediaPlayer = playbackController?.mediaPlayer
        when (v.tag.toString()) {
            TAG_PREVIOUS -> playbackController?.previousButtonPressed()
            TAG_NEXT -> playbackController?.nextButtonPressed()
            TAG_PLAY_PAUSE -> if (mediaPlayer?.isPlaying == true) {
                playbackController?.pauseButtonPressed()
            } else {
                playbackController?.playButtonPressed()
            }
        }
    }

    private fun format(duration: Long): String {
        return String.format(Locale.getDefault(), "%2d:%02d", duration / 1000 / 60, duration / 1000 % 60)
    }

    override fun onPositionUpdated(position: Long) {
        if (playbackController == null) {
            return
        }

        val mediaPlayer = playbackController?.mediaPlayer

        if (seekBarDragging) {
            return
        }

        if (position > 0) {
            ivPlayPause?.setImageResource(R.drawable.ic_pause_circle_outline_white_24dp)
            seekBar?.isEnabled = true
        } else {
            ivPlayPause?.setImageResource(R.drawable.ic_play_circle_outline_white_24dp)
        }

        if (mediaPlayer != null) {
            if (mediaPlayer.duration <= 0) {
                seekBar?.isEnabled = false
            } else {
                seekBar?.isEnabled = true
                if (seekBar?.max != mediaPlayer.duration.toFloat())
                    seekBar?.max = mediaPlayer.duration.toInt().toFloat()
                seekBar?.setProgress(position.toInt().toFloat())
            }
        } else {
            seekBar?.setProgress(0f)
        }
    }

    override fun onPlaybackStateChanged(state: MediaPlayer.MediaState) {
        if (state == MediaPlayer.MediaState.PLAYING) {
            ivPlayPause?.setImageResource(R.drawable.ic_pause_circle_outline_white_24dp)
            seekBar?.isEnabled = true
            ContentStorage.get().isMusicPlaying = true
        } else {
            ivPlayPause?.setImageResource(R.drawable.ic_play_circle_outline_white_24dp)
            ContentStorage.get().isMusicPlaying = false
        }
        if (state == MediaPlayer.MediaState.STOPPED) {
            ivPlayPause?.setImageResource(R.drawable.ic_play_circle_outline_white_24dp)
            ContentStorage.get().isMusicPlaying = false
        }

        if (null != playbackController) {
            val mediaPlayer = playbackController?.mediaPlayer
            if (mediaPlayer?.duration ?: 0 > 0) {
                tvDuration?.visibility = View.VISIBLE
                tvCurrentPosition?.visibility = View.VISIBLE
                seekBar?.isEnabled = true
            } else {
                tvCurrentPosition?.visibility = View.INVISIBLE
                tvDuration?.visibility = View.INVISIBLE
                seekBar?.isEnabled = false
            }
        }
    }

    override fun update(o: Observable, arg: Any) {
        if (o !is LoggerHandler.LoggerObservable) {
            return
        }

        if (arg !is LogEntry) {
            return
        }

        if (arg.type == LoggerHandler.RENDER_PLAYER_INFO) {
            val content = Gson().fromJson(arg.json.toString(), TemplateContent::class.java)
            ContentStorage.get().saveContent(content.template)
            ivBack?.post { setupView() }
        }
    }

    override fun onSeeking(seekParams: SeekParams) {
        if (playbackController == null) {
            return
        }

        val mediaPlayer = playbackController?.mediaPlayer
        if (mediaPlayer != null) {
            if (mediaPlayer.duration > 0) {
                tvDuration?.visibility = View.VISIBLE
                tvCurrentPosition?.visibility = View.VISIBLE
                seekBar?.isEnabled = true
                tvCurrentPosition?.text = format(seekParams.progress.toLong())
                tvDuration?.text = format(mediaPlayer.duration)
            } else {
                tvCurrentPosition?.visibility = View.INVISIBLE
                tvDuration?.visibility = View.INVISIBLE
                seekBar?.isEnabled = false
            }
            tvCurrentPosition?.text = format(seekParams.progress.toLong())
            tvDuration?.text = format(mediaPlayer.duration)
        } else {
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

        if (playbackController == null) {
            return
        }

        val mediaPlayer = playbackController?.mediaPlayer

        if (seekBarProgressTarget != -1) {
            mediaPlayer?.position = seekBarProgressTarget.toLong()
            seekBarProgressTarget = -1
        }
    }

    private fun getBitmapFromVectorDrawable(drawableId: Int): Bitmap {
        val drawable = ContextCompat.getDrawable(launcher!!, drawableId)

        val bitmap = Bitmap.createBitmap(drawable!!.intrinsicWidth,
                drawable.intrinsicHeight, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        drawable.setBounds(0, 0, canvas.width, canvas.height)
        drawable.draw(canvas)

        return bitmap
    }

    companion object {

        private const val TAG_PREVIOUS = "PREVIOUS"
        private const val TAG_NEXT = "NEXT"
        private const val TAG_PLAY_PAUSE = "PLAY_PAUSE"
    }
}
