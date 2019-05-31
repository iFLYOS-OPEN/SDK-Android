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

package com.iflytek.cyber.iot.show.core.model

class ContentStorage {
    var currentContent: Content? = null
        private set
    var isMusicPlaying = false
    var template: TemplateContent? = null
        private set

    fun saveContent(content: Content?) {
        this.currentContent = content
    }

    fun saveTemplate(template: TemplateContent?) {
        this.template = template
    }

    companion object {

        private var sStorage: ContentStorage? = null

        fun get(): ContentStorage {
            val current = sStorage
            return if (current == null) {
                val newStorage = ContentStorage()
                sStorage = newStorage
                newStorage
            } else
                current
        }
    }
}
