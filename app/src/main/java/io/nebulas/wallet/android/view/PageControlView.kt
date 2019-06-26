package io.nebulas.wallet.android.view

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import io.nebulas.wallet.android.R

/**
 * Created by Heinoc on 2018/6/22.
 */
class PageControlView : LinearLayout {

    var normalResId: Int = 0
    var focusResId: Int = 0

    var count: Int = 0
        set(count) {
            field = count
            setupViews()
        }

    private var mContext: Context? = null

    constructor(context: Context) : super(context) {
        mContext = context
    }

    constructor(context: Context, attrs: AttributeSet) : this(context, attrs, 0) {
        mContext = context
    }

    constructor(context: Context, attrs: AttributeSet, defStyle: Int) : super(context, attrs) {
        mContext = context
        val a = context.obtainStyledAttributes(attrs, R.styleable.PageControlView)
        this.normalResId = a.getResourceId(R.styleable.PageControlView_normalResId, 0)
        this.focusResId = a.getResourceId(R.styleable.PageControlView_focusResId, 0)
        a.recycle()
    }

    private fun setupViews() {
        this.removeAllViews()
        this.setBackgroundColor(Color.parseColor("#00000000"))
        val params = LinearLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        params.leftMargin = (2 * mContext!!.resources.displayMetrics.density).toInt()
        params.rightMargin = (2 * mContext!!.resources.displayMetrics.density).toInt()
        params.weight = 1f
        for (i in 0 until this.count) {
            val imageView = ImageView(context)
            imageView.id = i + ID_OFFSET
            if (i == 0) {
                imageView.setImageResource(focusResId)
            } else {
                imageView.setImageResource(normalResId)
            }
            this.addView(imageView, params)
        }
    }

    fun setFocusIndex(index: Int) {

        for (i in 0 until this.childCount) {
            val imageView = this.getChildAt(i) as ImageView
            if (imageView.id == index + ID_OFFSET) {
                imageView.setImageResource(focusResId)
            } else {
                imageView.setImageResource(normalResId)
            }

        }
    }

    companion object {

        private val ID_OFFSET = 100
    }
}
