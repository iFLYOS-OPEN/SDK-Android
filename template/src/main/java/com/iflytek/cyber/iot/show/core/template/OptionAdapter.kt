package com.iflytek.cyber.iot.show.core.template

import android.annotation.SuppressLint
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.iflytek.cyber.iot.show.core.template.model.Image


class OptionAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    companion object {
        const val OPTION_1 = 0
        const val OPTION_2 = 1
        const val OPTION_3 = 2
    }

    var jsonArray: JsonArray? = null

    var viewType: Int = OPTION_1

    var onItemClickListener: OnItemClickListener? = null

    private var titleSize = 0f

    fun initTitleSize(titleSize: Float) {
        this.titleSize = titleSize
    }

    override fun getItemCount(): Int {
        return jsonArray?.size() ?: 0
    }

    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder is OptionVerticalHolder) {
            if (titleSize > 0) {
                holder.primaryTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, titleSize)
                holder.secondaryTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, titleSize * 14 / 20)
                holder.tertiaryTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, titleSize * 14 / 20)
            }
            holder.indexView.text = "${position + 1}"

            (jsonArray?.get(position)?.asJsonObject)?.let { item ->
                holder.primaryTextView.text = (item.get("primaryTextField") as? JsonPrimitive)?.asString
                holder.secondaryTextView.text = (item.get("secondaryTextField") as? JsonPrimitive)?.asString
                holder.tertiaryTextView.text = (item.get("tertiaryTextField") as? JsonPrimitive)?.asString

                val img: Image? = if (item.has("image")) {
                    (item.get("image") as? JsonObject)?.let { image ->
                        Gson().fromJson(image, Image::class.java)
                    }
                } else {
                    null
                }
                val placeholderResId = if (viewType == OPTION_2)
                    R.drawable.placeholder_option_2
                else
                    R.drawable.placeholder_option_3
                Glide.with(holder.imageView)
                        .load(img?.sources?.get(0)?.url ?: "")
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .placeholder(placeholderResId)
                        .error(placeholderResId)
                        .into(holder.imageView)
            }
        } else if (holder is OptionHorizontalHolder) {
            if (titleSize > 0) {
                holder.primaryTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, titleSize)
                holder.secondaryTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, titleSize * 16 / 20)
                holder.tertiaryTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX, titleSize)
                holder.indexView.setTextSize(TypedValue.COMPLEX_UNIT_PX, titleSize * 16 / 20)
            }

            holder.indexView.text = "${position + 1}"

            (jsonArray?.get(position)?.asJsonObject)?.let { item ->
                holder.primaryTextView.text = item.get("primaryTextField").asString
                holder.secondaryTextView.text = item.get("secondaryTextField").asString
                holder.tertiaryTextView.text = item.get("tertiaryTextField").asString

                val img: Image? = if (item.has("image")) {
                    (item.get("image") as? JsonObject)?.let { image ->
                        Gson().fromJson(image, Image::class.java)
                    }
                } else {
                    null
                }
                Glide.with(holder.imageView)
                        .load(img?.sources?.get(0)?.url ?: "")
                        .transition(DrawableTransitionOptions.withCrossFade())
                        .placeholder(R.drawable.placeholder_option_3)
                        .error(R.drawable.placeholder_option_3)
                        .into(holder.imageView)
            }
        }
    }

    override fun getItemViewType(position: Int): Int {
        return viewType
    }

    override fun onCreateViewHolder(container: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val holder = when (viewType) {
            OPTION_1 -> {
                OptionHorizontalHolder(LayoutInflater.from(container.context)
                        .inflate(R.layout.item_option_1, container, false))
            }
            OPTION_2 -> {
                OptionVerticalHolder(LayoutInflater.from(container.context)
                        .inflate(R.layout.item_option_2, container, false))
            }
            OPTION_3 -> {
                OptionVerticalHolder(LayoutInflater.from(container.context)
                        .inflate(R.layout.item_option_3, container, false))
            }
            else -> {
                OptionHorizontalHolder(LayoutInflater.from(container.context)
                        .inflate(R.layout.item_option_1, container, false))
            }
        }
        holder.itemView.setOnClickListener {
            if (holder.adapterPosition >= 0)
                onItemClickListener?.onItemClick(container, it, holder.adapterPosition)
        }
        return holder
    }

    interface OnItemClickListener {
        fun onItemClick(container: ViewGroup, itemView: View, position: Int)
    }

    open class OptionVerticalHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.image)
        val primaryTextView: TextView = itemView.findViewById(R.id.primary_text)
        val secondaryTextView: TextView = itemView.findViewById(R.id.secondary_text)
        val tertiaryTextView: TextView = itemView.findViewById(R.id.tertiary_text)
        val indexView: TextView = itemView.findViewById(R.id.index)
    }

    open class OptionHorizontalHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.image)
        val primaryTextView: TextView = itemView.findViewById(R.id.primary_text)
        val secondaryTextView: TextView = itemView.findViewById(R.id.secondary_text)
        val tertiaryTextView: TextView = itemView.findViewById(R.id.tertiary_text)
        val indexView: TextView = itemView.findViewById(R.id.index)
    }

}