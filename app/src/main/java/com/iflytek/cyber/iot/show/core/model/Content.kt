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

class Content {

    var title: String? = null
    var titleSubtext1: String? = null
    var titleSubtext2: String? = null
    var header: String? = null
    var headerSubtext1: String? = null
    var mediaLengthInMilliseconds: String? = null
    var art: Image? = null
    var provider: Provider? = null

    class Provider {
        var name: String? = null
        var logo: Image? = null

        override fun toString(): String {
            return "Provider{" +
                    "name='" + name + '\''.toString() +
                    ", logo=" + logo +
                    '}'.toString()
        }
    }

    inner class Control {
        var type: String? = null
        var name: String? = null
        var enabled: Boolean = false
        var selected: Boolean = false

        override fun toString(): String {
            return "Control{" +
                    "type='" + type + '\''.toString() +
                    ", name='" + name + '\''.toString() +
                    ", enabled=" + enabled +
                    ", selected=" + selected +
                    '}'.toString()
        }
    }

    override fun toString(): String {
        return "Content{" +
                "title='" + title + '\''.toString() +
                ", titleSubtext1='" + titleSubtext1 + '\''.toString() +
                ", titleSubtext2='" + titleSubtext2 + '\''.toString() +
                ", header='" + header + '\''.toString() +
                ", headerSubtext1='" + headerSubtext1 + '\''.toString() +
                ", mediaLengthInMilliseconds='" + mediaLengthInMilliseconds + '\''.toString() +
                ", art=" + art +
                ", provider=" + provider +
                '}'.toString()
    }
}
