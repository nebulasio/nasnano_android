package io.nebulas.wallet.android.dialog

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import io.nebulas.wallet.android.R
import kotlinx.android.synthetic.main.dialog_feedback_choose_pic.*

class ChoosePicWayDialog(context: Context, val chooser1: ((View, ChoosePicWayDialog) -> Unit)?, val chooser2: ((View, ChoosePicWayDialog) -> Unit)?, val cancelBlock: ((View, ChoosePicWayDialog) -> Unit)?) : Dialog(context, R.style.AppDialog) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_feedback_choose_pic)
        takePhoto.setOnClickListener {
            chooser1?.invoke(it, this)
            dismiss()
        }

        selectPic.setOnClickListener {
            chooser2?.invoke(it, this)
            dismiss()
        }
        cancleBtn.setOnClickListener {
            cancelBlock?.invoke(it, this)
            dismiss()
        }
        setCanceledOnTouchOutside(true)
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