package com.iflytek.cyber.iot.show.core.template

import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.KeyEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.constraintlayout.widget.Guideline
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.google.gson.*
import com.iflytek.cyber.iot.show.core.template.model.Constant
import com.iflytek.cyber.iot.show.core.template.model.Image
import com.iflytek.cyber.iot.show.core.template.utils.CompatDividerItemDecoration
import com.iflytek.cyber.iot.show.core.template.utils.RoundedCornersTransformation

class OptionTemplateFragment : TemplateFragment() {
    private var recyclerView: RecyclerView? = null
    private var mainTitleView: TextView? = null
    private var subTitleView: TextView? = null
    private var skillIconView: ImageView? = null
    private var backgroundImageView: ImageView? = null
    private var backgroundImageContainer: View? = null
    private var progressContainer: View? = null

    private var template: String? = null

    private val adapter = OptionAdapter()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_option_template, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // find view
        recyclerView = view.findViewById(R.id.recycler_view)
        mainTitleView = view.findViewById(R.id.main_title)
        subTitleView = view.findViewById(R.id.sub_title)
        skillIconView = view.findViewById(R.id.skill_icon)
        backgroundImageView = view.findViewById(R.id.background_image)
        backgroundImageContainer = view.findViewById(R.id.background_image_container)
        progressContainer = view.findViewById(R.id.progress_container)

        // get template
        template = arguments?.getString(EXTRA_TEMPLATE) ?: return
        val payload = JsonParser().parse(template).asJsonObject

        when (payload.get(Constant.PAYLOAD_TYPE).asString) {
            Constant.TYPE_OPTION_TEMPLATE_1 -> {
                recyclerView?.layoutManager = LinearLayoutManager(context)
                adapter.viewType = OptionAdapter.OPTION_1
            }
            Constant.TYPE_OPTION_TEMPLATE_2 -> {
                recyclerView?.layoutManager = GridLayoutManager(context, 4)
                adapter.viewType = OptionAdapter.OPTION_2
            }
            Constant.TYPE_OPTION_TEMPLATE_3 -> {
                recyclerView?.layoutManager = GridLayoutManager(context, 4)
                adapter.viewType = OptionAdapter.OPTION_3
            }
        }

        view.setOnTouchListener { _, event ->
            if (event.action == KeyEvent.ACTION_DOWN || event.action == KeyEvent.ACTION_UP) {
                onScrollableBodyTouched(this, template.toString())
            }
            false
        }

        // init view
        recyclerView?.adapter = adapter
        view.findViewById<View>(R.id.back_icon).setOnClickListener {
            onBackPressed(this, template.toString())
        }
        adapter.onItemClickListener = object : OptionAdapter.OnItemClickListener {
            override fun onItemClick(container: ViewGroup, itemView: View, position: Int) {
                val jsonArray = adapter.jsonArray ?: return
                if (jsonArray.size() > position && position >= 0) {
                    (jsonArray.get(position) as? JsonObject)?.let { jsonObject ->
                        val token = payload.get(Constant.PAYLOAD_TOKEN).asString
                        val selectedItemToken = jsonObject.get(Constant.PAYLOAD_TOKEN).asString

                        onSelectElement(this@OptionTemplateFragment, token, selectedItemToken)

                        progressContainer?.run {
                            visibility = View.VISIBLE
                            alpha = 0f
                            animate().alpha(1f)
                                    .setDuration(300)
                                    .start()
                        }
                    }
                }
            }
        }
        view.findViewById<ProgressBar>(R.id.progress)?.progressDrawable
                ?.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)
        view.findViewById<ProgressBar>(R.id.progress)?.indeterminateDrawable
                ?.setColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)

        // do something to init ui
        mainTitleView?.post {
            // views would have width and height now
            val backIcon = view.findViewById<ImageView>(R.id.back_icon)
            val backIconSize = backIcon.height
            val backIconPadding = backIconSize * 12 / 56

            backIcon.setPadding(backIconPadding, backIconPadding, backIconPadding, backIconPadding)
            backIcon.setImageResource(R.drawable.ic_previous_white_32dp)

            val topBar = view.findViewById<View>(R.id.top_bar)
            val mainTitleSize = topBar.height * 22 / 96
            val subTitleSize = mainTitleSize * 16 / 22

            mainTitleView?.setTextSize(TypedValue.COMPLEX_UNIT_PX, mainTitleSize.toFloat())
            subTitleView?.setTextSize(TypedValue.COMPLEX_UNIT_PX, subTitleSize.toFloat())

            (subTitleView?.layoutParams as? LinearLayout.LayoutParams)?.let { layoutParams ->
                layoutParams.topMargin = mainTitleSize / 11
                subTitleView?.layoutParams = layoutParams
            }

            val recyclerViewBottom = view.findViewById<View>(R.id.bottom_cover).height
            recyclerView?.setPadding(0, 0, 0, recyclerViewBottom)
            if (adapter.viewType == OptionAdapter.OPTION_1) {
                recyclerView?.let { recyclerView ->
                    val dividerHeight = mainTitleSize * 24 / 22
                    Log.d("OptionTemplate", "divider: $dividerHeight")
                    val divider = CompatDividerItemDecoration(recyclerView.context,
                            CompatDividerItemDecoration.VERTICAL)
                    val dividerDrawable = ColorDrawable(
                            ContextCompat.getColor(recyclerView.context, android.R.color.transparent))
                    dividerDrawable.setBounds(0, 0, recyclerView.width, dividerHeight)
                    divider.setDrawable(dividerDrawable)
                    recyclerView.addItemDecoration(divider)
                }
            }

            adapter.initTitleSize(mainTitleSize.toFloat() * 20 / 22)

            showTemplate(payload)
        }
    }

    override fun getTemplatePayload(): String {
        template ?: let {
            return it.toString()
        }
        return super.getTemplatePayload()
    }

    private fun showTemplate(payload: JsonObject) {
        val gson = Gson()

        // set padding
        when (payload.get(Constant.PAYLOAD_TYPE).asString) {
            Constant.TYPE_OPTION_TEMPLATE_1 -> {
                recyclerView?.let { recyclerView ->
                    recyclerView.setPadding(0, recyclerView.paddingTop, 0, recyclerView.paddingBottom)
                }
            }
            Constant.TYPE_OPTION_TEMPLATE_2 -> {
                recyclerView?.let { recyclerView ->
                    val padding = recyclerView.width * 24 / 1024
                    recyclerView.setPadding(padding, recyclerView.paddingTop, padding, recyclerView.paddingBottom)
                }
            }
            Constant.TYPE_OPTION_TEMPLATE_3 -> {
                recyclerView?.let { recyclerView ->
                    val padding = recyclerView.width * 24 / 1024
                    recyclerView.setPadding(padding, recyclerView.paddingTop, padding, recyclerView.paddingBottom)
                }
            }
        }

        // fill data
        adapter.jsonArray = payload.getAsJsonArray("optionItems")
        adapter.notifyDataSetChanged()

        (payload.get("title") as? JsonObject)?.let { title ->
            mainTitleView?.text = (title.get("mainTitle") as? JsonPrimitive)?.asString
            subTitleView?.text = (title.get("subTitle") as? JsonPrimitive)?.asString

            subTitleView?.visibility =
                    if (subTitleView?.text.isNullOrEmpty())
                        View.GONE
                    else
                        View.VISIBLE
        }

        val skillIconJson = payload.get("skillIcon") as? JsonObject
        if (skillIconJson != null) {
            val skillImage = gson.fromJson<Image>(skillIconJson, Image::class.java)
            skillIconView?.let { skillIconView ->
                Glide.with(skillIconView)
                        .load(skillImage.sources[0].url)
                        .apply(RequestOptions()
                                .transform(RoundedCornersTransformation(skillIconView.height / 4, 0)))
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .into(skillIconView)
            }
        }

        (payload.get("backButton") as? JsonPrimitive)?.let { backButtonJson ->
            view?.findViewById<View>(R.id.back_icon)?.let { backIcon ->
                when (backButtonJson.asString) {
                    "HIDDEN" -> {
                        backIcon.visibility = View.GONE
                        val guideline = view?.findViewById<Guideline>(R.id.top_bar_title_start)
                        guideline?.setGuidelinePercent(0.031f)
                    }
                    else -> {
                        backIcon.visibility = View.VISIBLE
                        val guideline = view?.findViewById<Guideline>(R.id.top_bar_title_start)
                        guideline?.setGuidelinePercent(0.086f)
                    }
                }
            }
        } ?: run {
            view?.findViewById<View>(R.id.back_icon)?.visibility = View.VISIBLE
            val guideline = view?.findViewById<Guideline>(R.id.top_bar_title_start)
            guideline?.setGuidelinePercent(0.086f)
        }

        (payload.get("backgroundImage") as? JsonObject)?.let { backgroundImageJson ->
            val backgroundImage = gson.fromJson<Image>(backgroundImageJson, Image::class.java)
            backgroundImageView?.let { backgroundImageView ->
                Glide.with(backgroundImageView)
                        .load(backgroundImage.sources[0].url)
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .into(backgroundImageView)
                backgroundImageContainer?.visibility = View.VISIBLE
            } ?: run {
                backgroundImageContainer?.visibility = View.GONE
            }
        } ?: run {
            backgroundImageContainer?.visibility = View.GONE
        }
    }
}