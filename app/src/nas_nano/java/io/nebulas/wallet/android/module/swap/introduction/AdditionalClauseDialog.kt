package io.nebulas.wallet.android.module.swap.introduction

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.Gravity
import android.view.WindowManager
import com.google.firebase.analytics.FirebaseAnalytics
import io.nebulas.wallet.android.R
import io.nebulas.wallet.android.common.Constants
import io.nebulas.wallet.android.util.Util
import kotlinx.android.synthetic.nas_nano.dialog_swap_additional_clause.*

class AdditionalClauseDialog(context: Context?, private val onAgree:(Dialog)->Unit) : Dialog(context, R.style.AppDialog) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_swap_additional_clause)
        setCanceledOnTouchOutside(true)

        tv_agree.setOnClickListener {
            FirebaseAnalytics.getInstance(context).logEvent(Constants.Exchange_Agreement_Click,Bundle())
            onAgree(this)
        }
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
        attr.height = (Util.screenHeight(context) * 0.88).toInt()
        window.decorView.setPadding(0, 0, 0, 0)
        window.attributes = attr
    }

}