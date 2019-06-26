package io.nebulas.wallet.android.view

import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.support.annotation.ColorInt
import android.support.v7.widget.RecyclerView
import android.view.View
import android.view.ViewGroup

/**
 * Created by young on 2018/6/27.
 */
class LineDecoration(@ColorInt private val lineColor: Int,
                     private val linePixelHeight: Int,
                     private val horizontalPixelPadding: Int = 0) : RecyclerView.ItemDecoration() {

    val paint = Paint().apply {
        color = lineColor
    }

    private val lineRect = Rect(0, 0, 0, 0)

    override fun onDrawOver(c: Canvas?, parent: RecyclerView?, state: RecyclerView.State?) {
        if (parent == null || c == null) {
            return
        }
        val left = parent.paddingLeft + horizontalPixelPadding
        val right = parent.width - parent.paddingRight - horizontalPixelPadding
        lineRect.left = left
        lineRect.right = right
        val childCount = parent.childCount
        (0 until childCount).forEach {
            val child = parent.getChildAt(it)
            if (it == childCount - 1) {
                return@forEach
            }
            val layoutParam = child.layoutParams
            val marginBottom = if (layoutParam is ViewGroup.MarginLayoutParams) {
                layoutParam.bottomMargin
            } else {
                0
            }
            val lineTop = child.bottom + marginBottom
            val lienBottom = lineTop + linePixelHeight
            lineRect.top = lineTop
            lineRect.bottom = lienBottom
            c.drawRect(lineRect, paint)
        }
    }

    override fun getItemOffsets(outRect: Rect?, view: View?, parent: RecyclerView?, state: RecyclerView.State?) {
        if (parent == null || view == null) {
            return
        }
        val bottom = if (parent.getChildAdapterPosition(view) == parent.adapter.itemCount - 1) {
            0
        } else {
            linePixelHeight
        }
        outRect?.set(0, 0, 0, bottom)
    }
}