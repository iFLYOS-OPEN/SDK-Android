package com.iflytek.cyber.iot.show.core.template

import android.os.Handler
import androidx.fragment.app.Fragment

open class TemplateFragment : Fragment() {
    private val handler = Handler()

    companion object {
        const val EXTRA_TEMPLATE = "template"
    }

    private val callbacks = HashSet<TemplateCallback>()

    fun registerCallback(callback: TemplateCallback) {
        callbacks.add(callback)
    }

    fun unregisterCallback(callback: TemplateCallback) {
        callbacks.remove(callback)
    }

    open fun getTemplatePayload(): String {
        return "{}"
    }

    fun onBackPressed(fragment: TemplateFragment, template: String) {
        callbacks.map {
            it.onBackPressed(fragment, template)
        }
        val activityCallback = activity as? TemplateCallback
        activityCallback?.onBackPressed(fragment, template)
    }

    fun onScrollableBodyTouched(fragment: TemplateFragment, template: String) {
        callbacks.map {
            it.onScrollableBodyStopped(fragment, template)
        }
        val activityCallback = activity as? TemplateCallback
        activityCallback?.onScrollableBodyStopped(fragment, template)
    }

    fun onSelectElement(fragment: TemplateFragment, token: String, selectedItemToken: String) {
        callbacks.map {
            it.onSelectElement(fragment, token, selectedItemToken)
        }
        val activityCallback = activity as? TemplateCallback
        activityCallback?.onSelectElement(fragment, token, selectedItemToken)
    }

    override fun onStart() {
        super.onStart()
        handler.post {
            callbacks.map { it.onStart(this) }
        }
        val activityCallback = activity as? TemplateCallback
        activityCallback?.onStart(this)
    }

    override fun onStop() {
        super.onStop()
        handler.post {
            callbacks.map {
                it.onStop(this)
            }
            val activityCallback = activity as? TemplateCallback
            activityCallback?.onStop(this)
        }
    }

    override fun onResume() {
        super.onResume()
        handler.post {
            callbacks.map { it.onResume(this) }
            val activityCallback = activity as? TemplateCallback
            activityCallback?.onResume(this)
        }
    }

    override fun onPause() {
        super.onPause()
        handler.post {
            callbacks.map { it.onPause(this) }
            val activityCallback = activity as? TemplateCallback
            activityCallback?.onPause(this)
        }
    }

    open class SimpleTemplateCallback : TemplateCallback {
        override fun onStart(fragment: TemplateFragment) {
        }

        override fun onStop(fragment: TemplateFragment) {
        }

        override fun onResume(fragment: TemplateFragment) {
        }

        override fun onPause(fragment: TemplateFragment) {
        }

        override fun onBackPressed(fragment: TemplateFragment, template: String) {
        }

        override fun onScrollableBodyStopped(fragment: TemplateFragment, template: String) {
        }

        override fun onSelectElement(fragment: TemplateFragment, token: String, selectedItemToken: String) {

        }
    }

    interface TemplateCallback {
        fun onStart(fragment: TemplateFragment)
        fun onStop(fragment: TemplateFragment)
        fun onResume(fragment: TemplateFragment)
        fun onPause(fragment: TemplateFragment)

        fun onBackPressed(fragment: TemplateFragment, template: String)
        fun onSelectElement(fragment: TemplateFragment, token: String, selectedItemToken: String)
        fun onScrollableBodyStopped(fragment: TemplateFragment, template: String)
    }
}