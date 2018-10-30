package com.iflytek.cyber.iot.show.core.model

data class ListItem(var leftTextField: String?,
                    var rightTextField: String?) {
    override fun toString(): String {
        return "ListItem(leftTextField=$leftTextField, rightTextField=$rightTextField)"
    }
}