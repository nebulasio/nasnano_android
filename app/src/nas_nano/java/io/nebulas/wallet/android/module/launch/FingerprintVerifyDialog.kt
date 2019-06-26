package io.nebulas.wallet.android.module.launch

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.View
import io.nebulas.wallet.android.R
import io.nebulas.wallet.android.util.Util
import kotlinx.android.synthetic.nas_nano.dialog_fingerprint_verify.*

class FingerprintVerifyDialog(context: Context,
                              private val type: Type,
                              private val actionListener: ActionListener) : Dialog(context, R.style.DialogStyle) {

    enum class Type(desc: String) {
        UNLOCK("用于解锁"), OPEN("用于设置页面开启指纹验证功能")
    }

    interface ActionListener {
        fun onCancelClicked()
        fun onPasswordClicked()
    }

    private var isCreated: Boolean = false
    private lateinit var desc: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setCanceledOnTouchOutside(false)
        setContentView(R.layout.dialog_fingerprint_verify)
        tv_action_cancel.setOnClickListener {
            dismiss()
            actionListener.onCancelClicked()
        }
        tv_action_password.setOnClickListener {
            dismiss()
            actionListener.onPasswordClicked()
        }
        desc = when (type) {
            Type.UNLOCK -> context.getString(R.string.text_verify_fingerprint_to_unlock)
            Type.OPEN -> context.getString(R.string.text_verify_fingerprint_to_open)
        }
        tv_desc.text = desc
        isCreated = true
    }

    override fun show() {
        super.show()
        if (context != null && context is Activity) {
            val activity = context as Activity
            if (activity.isFinishing || activity.isDestroyed) {
                return
            }
        }
        val attr = window.attributes
        attr.width = (Util.screenWidth(context) * 0.72).toInt()
        window.decorView.setPadding(0, 0, 0, 0)
        window.attributes = attr
    }

    fun reset() {
        if (isCreated) {
            tv_title.text = context.getString(R.string.tip_title_please_verify_fingerprint)
            view_vertical_line.visibility = View.GONE
            tv_action_password.visibility = View.GONE
        } else {
            show()
        }
    }

    fun fingerVerifyError() {
        tv_title.text = context.getString(R.string.tip_please_retry)
        view_vertical_line.visibility = View.VISIBLE
        tv_action_password.visibility = View.VISIBLE
    }

}