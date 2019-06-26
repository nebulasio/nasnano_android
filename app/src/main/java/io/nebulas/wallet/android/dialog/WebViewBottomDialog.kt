package io.nebulas.wallet.android.dialog

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import io.nebulas.wallet.android.R
import kotlinx.android.synthetic.nas_nano.webview_dialog_layout.*

/**
 * Created by alina on 2018/7/16.
 */
class WebViewBottomDialog(context: Context,
                          val title: String?,
                          val content: String?,
                          val cancelButtonText: String?,
                          val cancelBlock: ((View, WebViewBottomDialog) -> Unit)?,
                          val confirmButtonText: String?,
                          val confirmBlock: ((View, WebViewBottomDialog) -> Unit)?,
                          private val canceledOnTouchOutsideEnable: Boolean = false)
    : Dialog(context, R.style.AppDialog) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.webview_dialog_layout)
        if (!title.isNullOrEmpty()) {
            titleTV.text = title
        }
        if (!content.isNullOrEmpty()) {
            webView.loadUrl(content!!)
            webView.visibility = View.VISIBLE
        }
        if (!cancelButtonText.isNullOrEmpty()) {
            negtiveTv.text = cancelButtonText
            negtiveTv.visibility = View.VISIBLE
            negtiveTv.setOnClickListener {
                cancelBlock?.invoke(it, this)
                dismiss()
            }
        }
        if (!confirmButtonText.isNullOrEmpty()) {
            positiveTV.text = confirmButtonText
            positiveTV.visibility = View.VISIBLE
            positiveTV.setOnClickListener {
                confirmBlock?.invoke(it, this)
            }
        }
        agreeCB.setOnClickListener {
            if (agreeCB.isChecked) {
                positiveTV.isClickable = true
                positiveTV.isEnabled = true
            } else {
                positiveTV.isClickable = false
                positiveTV.isEnabled = false
            }
        }
        setCanceledOnTouchOutside(canceledOnTouchOutsideEnable)

    }

    override fun onBackPressed() {
//        super.onBackPressed()
    }

    override fun show() {
        if (context != null && context is Activity) {
            val activity = context as Activity
            if (activity.isFinishing || activity.isDestroyed) {
                return
            }
        }
        super.show()
        val attr = window.attributes
        attr.gravity = Gravity.BOTTOM
        attr.width = WindowManager.LayoutParams.MATCH_PARENT
        window.decorView.setPadding(0, 0, 0, 0)
        window.attributes = attr
    }

}