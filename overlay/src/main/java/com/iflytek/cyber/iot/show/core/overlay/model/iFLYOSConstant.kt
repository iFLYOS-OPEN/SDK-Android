package com.iflytek.cyber.iot.show.core.overlay.model

class iFLYOSConstant {

    enum class DialogState(val stateName: String) {
        IDLE("IDLE"),
        START("START"),
        EXPECT("EXPECT"),
        LISTENING("LISTENING"),
        THINKING("THINKING"),
        SPEAKING("SPEAKING"),
        END("END");

        override fun toString(): String {
            return this.stateName
        }
    }

    enum class MediaState constructor(val stateName: String) {
        BUFFERING("buffering"),
        PLAYING("play"),
        PAUSED("pause"),
        STOPPED("stop"),
        FINISH("finish"),
        UNKNOWN("unknown");

        companion object {
            fun pickValueOf(value: String): MediaState {
                var state = UNKNOWN
                for (perState in values()) {
                    if (perState.name.toLowerCase() == value.toLowerCase()) {
                        state = perState
                        break
                    }
                }
                return state
            }
        }

        override fun toString(): String {
            return this.stateName
        }
    }

    enum class AlertState(val stateName: String) {
        READY("READY"),
        STARTED("STARTED"),
        STOPPED("STOPPED"),
        SNOOZED("SNOOZED"),
        COMPLETED("COMPLETED"),
        PAST_DUE("PAST_DUE"),
        FOCUS_ENTERED_FOREGROUND("FOCUS_ENTERED_FOREGROUND"),
        FOCUS_ENTERED_BACKGROUND("FOCUS_ENTERED_BACKGROUND"),
        ERROR("ERROR");

        override fun toString(): String {
            return this.stateName
        }
    }
}