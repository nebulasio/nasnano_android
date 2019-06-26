package io.nebulas.wallet.android.dialog

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.graphics.Paint
import android.graphics.Rect
import android.os.Bundle
import android.support.annotation.DrawableRes
import android.view.*
import android.widget.LinearLayout
import android.widget.RelativeLayout
import android.widget.TextView
import io.nebulas.wallet.android.R
import io.nebulas.wallet.android.dialog.adapter.GridViewAdapter
import io.nebulas.wallet.android.util.Util
import kotlinx.android.synthetic.main.dialog_nas_bottom.*
import org.jetbrains.anko.centerInParent
import org.jetbrains.anko.dip

class NasBottomDialog(context: Context,
                      val iconResId: Int?,
                      val title: String?,
                      val content: String?,
                      val cancelButtonText: String?,
                      val cancelBlock: ((View, NasBottomDialog) -> Unit)?,
                      val confirmButtonText: String?,
                      val confirmBlock: ((View, NasBottomDialog) -> Unit)?,
                      val canceledOnTouchOutsideEnable: Boolean = false,
                      val onCustomBackPressed: (() -> Unit)? = null,
                      val tokenList: MutableList<Any>? = null,
                      val customView: View? = null,
                      private val attach: List<Pair<String, Any>>? = null) : Dialog(context, R.style.AppDialog) {

    private val attachInfo: MutableMap<String, Any> = mutableMapOf()
    private val paint: Paint = Paint()
    private val textBounds: Rect = Rect(0, 0, 0, 0)
    private val screenHeight: Int = Util.screenHeight(context)
    private val lis = ViewTreeObserver.OnPreDrawListener {
        val height = rootView.measuredHeight
        if (height > context.dip(536)) {
            setMaxHeight()
        }
        true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_nas_bottom)
        var title: String? = this.title
        val content: String? = this.content
        if (title.isNullOrEmpty() && content.isNullOrEmpty()) {
            title = " "
        }
        attach?.forEach {
            attachInfo[it.first] = it.second
        }
        if (iconResId == null) {
            tv_title.setPadding(tv_title.paddingLeft, context.dip(24), tv_title.paddingRight, tv_title.paddingBottom)
            tv_title.gravity = Gravity.START
            tv_content.gravity = Gravity.START
            layout_icon.visibility = View.GONE
        } else {
            tv_title.gravity = Gravity.CENTER_HORIZONTAL
            tv_content.gravity = Gravity.CENTER_HORIZONTAL
            iv_icon.setImageResource(iconResId)
            tv_title.setPadding(tv_title.paddingLeft, context.dip(72), tv_title.paddingRight, tv_title.paddingBottom)
            layout_icon.visibility = View.VISIBLE
        }
        tv_title.visibility = if (title.isNullOrEmpty()) {
            if (iconResId == null) {
                //无图标模式：title不可见时，设置content的paddingTop为16 -- 则paddingTop+marginTop=24
                tv_content.setPadding(0, context.dip(24 - 8), 0, 0)
            } else {
                //有图标模式：title不可见时，设置content的paddingTop为72
                tv_content.setPadding(0, context.dip(72), 0, 0)
            }
            View.GONE
        } else {
            tv_content.setPadding(0, 0, 0, 0)
            tv_title.text = title
            View.VISIBLE
        }
        tv_title.text = title
        tv_content.visibility = if (content.isNullOrEmpty()) {
            View.GONE
        } else {
            tv_content.text = content
            View.VISIBLE
        }
        tv_cancel.visibility = if (cancelButtonText.isNullOrEmpty()) {
            View.GONE
        } else {
            tv_cancel.text = cancelButtonText
            tv_cancel.setOnClickListener {
                cancelBlock?.invoke(it, this)
                dismiss()
            }
            View.VISIBLE
        }
        tv_ok.visibility = if (confirmButtonText.isNullOrEmpty()) {
            View.GONE
        } else {
            tv_ok.text = confirmButtonText
            tv_ok.setOnClickListener {
                confirmBlock?.invoke(it, this)
            }
            View.VISIBLE
        }


        if (tokenList == null || tokenList.isEmpty()) {
            grideView.visibility = View.GONE
        } else {
            if (tokenList.size < 3)
                grideView.numColumns = tokenList.size
            else
                grideView.numColumns = 3


            if (tokenList.size > 6) {
                var params = grideView.layoutParams
                params.height = context.dip(243)
                grideView.layoutParams = params
            }

            grideView.visibility = View.VISIBLE
            val gridAdapter = GridViewAdapter(context, tokenList)
            grideView.adapter = gridAdapter
        }

        customView?.apply {
            customViewContainer.visibility = View.VISIBLE
            val parentView = parent
            if (parentView != null && parentView is ViewGroup) {
                parentView.removeView(this)
            }
            val param = RelativeLayout.LayoutParams(layoutParams).apply {
                centerInParent()
            }
            customViewContainer.addView(this, param)
        }

        setCanceledOnTouchOutside(canceledOnTouchOutsideEnable)
        checkButtonTextLength()
    }

    override fun show() {
        if (context != null && context is Activity) {
            val activity = context as Activity
            if (activity.isFinishing || activity.isDestroyed) {
                return
            }
        }
        super.show()
        rootView.viewTreeObserver.addOnPreDrawListener(lis)
        val attr = window.attributes
        attr.gravity = Gravity.BOTTOM
        attr.width = WindowManager.LayoutParams.MATCH_PARENT
        window.decorView.setPadding(0, 0, 0, 0)
        window.attributes = attr
    }

    override fun dismiss() {
        super.dismiss()
        rootView.viewTreeObserver.removeOnPreDrawListener(lis)
    }

    override fun onBackPressed() {
        if (null != onCustomBackPressed)
            onCustomBackPressed.invoke()
        else
            super.onBackPressed()
    }

    fun getAttach(key: String): Any? {
        return attachInfo[key]
    }

    fun setAttach(key: String, value: Any) {
        attachInfo[key] = value
    }

    private fun setMaxHeight() {
        //暂时不处理最大高度的问题
        val maxHeight = context.dip(536)
        val attr = window.attributes
        attr.height = maxHeight.toInt()
        window.attributes = attr
    }

    private fun checkButtonTextLength() {
        //备注：@18-06-21 与设计师确认过了，button文案只支持单行，不会超过一行。如果真出现这种情况，那就修改文案
        if (cancelButtonText.isNullOrEmpty() || confirmButtonText.isNullOrEmpty()) {
            //只显示一个按钮，不需要处理
            return
        }
        val horizontalMaxWidth = getHorizontalMaxWidth()
        paint.textSize = tv_cancel.textSize
        paint.getTextBounds(cancelButtonText, 0, cancelButtonText!!.length, textBounds)
        if (textBounds.width() >= horizontalMaxWidth) {
            changeButtonLayoutToVertical()
            return
        }

        paint.textSize = tv_ok.textSize
        paint.getTextBounds(confirmButtonText, 0, confirmButtonText!!.length, textBounds)
        if (textBounds.width() >= horizontalMaxWidth) {
            changeButtonLayoutToVertical()
        }
    }

    private fun changeButtonLayoutToVertical() {
        layout_buttons.orientation = LinearLayout.VERTICAL
        (0 until layout_buttons.childCount).forEach {
            val child = layout_buttons.getChildAt(it)
            if (child is TextView) {
                val param = child.layoutParams
                param.width = ViewGroup.LayoutParams.MATCH_PARENT
                child.layoutParams = param
            }
        }
    }

    private fun getHorizontalMaxWidth(): Int {
        val screenWidth = Util.screenWidth(context)
        //减去按钮外层Layout的margin
        val buttonContainerWithoutMargin = screenWidth - context.dip(18 * 2)
        //减去Layout的Divider宽度，得出两个按钮总宽度
        val totalButtonWidthX2 = buttonContainerWithoutMargin - context.dip(18)
        val perButtonWidth = totalButtonWidthX2 / 2 //两个按钮总宽度除以2，得出单个按钮宽度
        return perButtonWidth - context.dip(8 * 2) //最后结果：单个button宽度 - button圆角背景的左右padding
    }

    class Builder(val context: Context) {

        @DrawableRes
        private var iconResId: Int? = null
        private var title: String = ""
        private var content: String? = null
        private var cancelBlock: ((View, NasBottomDialog) -> Unit)? = null
        private var confirmBlock: ((View, NasBottomDialog) -> Unit)? = null
        private var cancelText: String? = null
        private var confirmText: String? = null
        private var canceledOnTouchOutsideEnable: Boolean = false
        private var onCustomBackPressed: (() -> Unit)? = null
        private var dataList: MutableList<Any>? = null
        private var customView: View? = null
        private var attachInfo: MutableList<Pair<String, Any>>? = null
        fun withIcon(@DrawableRes drawableResInt: Int): Builder {
            this.iconResId = drawableResInt
            return this
        }

        fun withTitle(title: String): Builder {
            this.title = title
            return this
        }

        fun withContent(content: String?): Builder {
            this.content = content
            return this
        }

        fun withCancelButton(buttonText: String?, block: ((View, NasBottomDialog) -> Unit)? = null): Builder {
            this.cancelText = buttonText
            this.cancelBlock = block
            return this
        }

        fun withConfirmButton(buttonText: String?, block: ((View, NasBottomDialog) -> Unit)? = null): Builder {
            this.confirmText = buttonText
            this.confirmBlock = block
            return this
        }

        fun withCanceledOnTouchOutsideEnable(enable: Boolean): Builder {
            this.canceledOnTouchOutsideEnable = enable
            return this
        }

        fun withOnCustomBackPressed(onCustomBackPressed: (() -> Unit)?): Builder {
            this.onCustomBackPressed = onCustomBackPressed
            return this
        }

        fun withOnGridDataList(dataList: MutableList<Any>?): Builder {
            this.dataList = dataList
            return this
        }

        fun withCustomView(customView: View?): Builder {
            this.customView = customView
            return this
        }

        fun withAttachInfo(vararg pair: Pair<String, Any>): Builder {
            if (attachInfo == null) {
                attachInfo = mutableListOf()
            }
            attachInfo?.addAll(pair)
            return this
        }

        fun build(): NasBottomDialog {
            return NasBottomDialog(
                    context = context,
                    iconResId = iconResId,
                    title = title,
                    content = content,
                    cancelButtonText = cancelText,
                    cancelBlock = cancelBlock,
                    confirmButtonText = confirmText,
                    confirmBlock = confirmBlock,
                    canceledOnTouchOutsideEnable = canceledOnTouchOutsideEnable,
                    onCustomBackPressed = onCustomBackPressed,
                    tokenList = dataList,
                    customView = customView,
                    attach = attachInfo
            )
        }
    }

}