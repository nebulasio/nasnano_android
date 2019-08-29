package io.nebulas.wallet.android.dialog

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.text.TextUtils
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import io.nebulas.wallet.android.R
import kotlinx.android.synthetic.nas_nano.dialog_common_center.*

class CommonCenterDialog(context: Context,
                         val title: String? = null,
                         val content: String? = null,
                         val leftButtonText:String?=null,
                         val leftButtonAction:(()->Unit)?=null,
                         val rightButtonText:String?=null,
                         val rightButtonAction:(()->Unit)?=null) : Dialog(context, R.style.CenterAppDialog) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_common_center)
        setupViews()
    }

    private fun setupViews(){
        if (TextUtils.isEmpty(title)) {
            tvTitle.visibility = View.GONE
        } else {
            tvTitle.visibility = View.VISIBLE
            tvTitle.text = this.title
        }
        if (TextUtils.isEmpty(content)) {
            tvContent.visibility = View.GONE
        } else {
            tvContent.visibility = View.VISIBLE
            tvContent.text = this.content
        }
        if (TextUtils.isEmpty(leftButtonText)) {
            tvLeftButton.visibility = View.GONE
        } else {
            tvLeftButton.visibility = View.VISIBLE
            tvLeftButton.text = this.leftButtonText
            tvLeftButton.setOnClickListener {
                dismiss()
                leftButtonAction?.invoke()
            }
        }
        if (TextUtils.isEmpty(rightButtonText)) {
            tvRightButton.visibility = View.GONE
        } else {
            tvRightButton.visibility = View.VISIBLE
            tvRightButton.text = this.rightButtonText
            tvRightButton.setOnClickListener {
                dismiss()
                rightButtonAction?.invoke()
            }
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
        attr.gravity = Gravity.CENTER
        attr.width = WindowManager.LayoutParams.MATCH_PARENT
        window.decorView.setPadding(0, 0, 0, 0)
        window.attributes = attr
    }

    class Builder{
        private var title:String? = null
        private var content:String? = null
        private var leftButtonText:String? = null
        private var leftButtonAction:(()->Unit)?=null
        private var rightButtonText:String? = null
        private var rightButtonAction:(()->Unit)?=null

        public fun withTitle(title: String?):Builder{
            this.title = title
            return this
        }

        public fun withContent(content: String?):Builder{
            this.content = content
            return this
        }

        public fun withLeftButton(text:String?, action:(()->Unit)?=null):Builder{
            this.leftButtonText = text
            this.leftButtonAction = action
            return this
        }

        public fun withRightButton(text:String?, action:(()->Unit)?=null):Builder{
            this.rightButtonText = text
            this.rightButtonAction = action
            return this
        }

        public fun build(context: Context):CommonCenterDialog{
            return CommonCenterDialog(
                    context=context,
                    title = this.title,
                    content = this.content,
                    leftButtonText = this.leftButtonText,
                    leftButtonAction = this.leftButtonAction,
                    rightButtonText = this.rightButtonText,
                    rightButtonAction = this.rightButtonAction)
        }
    }
}