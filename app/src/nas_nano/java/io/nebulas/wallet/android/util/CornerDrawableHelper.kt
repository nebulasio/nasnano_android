package io.nebulas.wallet.android.util

import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.support.annotation.ColorInt

fun getCornerDrawable(cornerRadius: Float, @ColorInt fillColorInt: Int, strokeWidth:Int, @ColorInt strokeColor:Int):Drawable{
    val drawable = GradientDrawable()
    drawable.shape = GradientDrawable.RECTANGLE
    drawable.cornerRadius = cornerRadius
    drawable.setStroke(strokeWidth, strokeColor)
    drawable.setColor(fillColorInt)
    return drawable
}