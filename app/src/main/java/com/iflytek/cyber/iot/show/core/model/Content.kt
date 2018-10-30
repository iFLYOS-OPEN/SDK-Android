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
