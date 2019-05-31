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

package com.iflytek.cyber.iot.show.core.template

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.iflytek.cyber.iot.show.core.template.model.ListItem

class List1Adapter(private val listItems: ArrayList<ListItem>) :
        RecyclerView.Adapter<List1Adapter.ListHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ListHolder {
        return ListHolder(LayoutInflater.from(parent.context)
                .inflate(R.layout.item_list_template, parent, false))
    }

    override fun getItemCount(): Int {
        return listItems.size
    }

    override fun onBindViewHolder(holder: ListHolder, position: Int) {
        val item = listItems[holder.adapterPosition]
        holder.left.text = item.leftTextField
        holder.right.text = item.rightTextField
        if (holder.adapterPosition > 0) {
            holder.line.visibility = View.GONE
        } else {
            holder.line.visibility = View.VISIBLE
        }
    }

    class ListHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val left = itemView.findViewById<TextView>(R.id.tv_left)
        val right = itemView.findViewById<TextView>(R.id.tv_right)
        val line = itemView.findViewById<View>(R.id.line)
    }
}