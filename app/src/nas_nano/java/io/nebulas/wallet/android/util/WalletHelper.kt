package io.nebulas.wallet.android.util

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.drawable.GradientDrawable
import android.support.annotation.ColorRes
import io.nebulas.wallet.android.R
import io.nebulas.wallet.android.module.wallet.create.model.Wallet
import org.jetbrains.anko.dip
import org.jetbrains.anko.sp

internal val walletColors = intArrayOf(
        R.color.color_038AFB,
        R.color.color_8350F6,
        R.color.color_FF8F00,
        R.color.color_00CB91,
        R.color.color_FF516A)

private fun getIndex(walletId: Long): Int {
    if (walletId<0){
        return 0
    }
    return Math.abs(walletId.toInt()) % 5
}

@ColorRes
fun getWalletColor(walletId: Long): Int = walletColors[getIndex(walletId)]

@ColorRes
fun getWalletColor(wallet: Wallet): Int {
    return getWalletColor(wallet.id)
}

fun getWalletColorDrawable(context: Context, walletId: Long): GradientDrawable {
    val drawable = GradientDrawable()
    drawable.setColor(context.resources.getColor(getWalletColor(walletId)))
    drawable.cornerRadius = context.dip(6).toFloat()
    return drawable
}

fun getWalletColorCircleDrawable(context: Context, walletId: Long, walletFirstChar: Char? = null, textSizeSP: Int = 24): GradientDrawable {
    val drawable = CharGradientDrawable(context, walletFirstChar, textSizeSP)
    drawable.setColor(context.resources.getColor(getWalletColor(walletId)))
    drawable.shape = GradientDrawable.OVAL
    return drawable
}


class CharGradientDrawable(val context: Context, private val char: Char?, private val textSizeSP: Int = 24) : GradientDrawable() {

    val paint = Paint().apply {
        color = Color.WHITE
        flags = Paint.FAKE_BOLD_TEXT_FLAG
        textSize = context.sp(textSizeSP).toFloat()
        isAntiAlias = true
        textAlign = Paint.Align.CENTER
    }

    override fun draw(canvas: Canvas?) {
        super.draw(canvas)
        canvas?.apply {
            if (char != null) {
                val rect = bounds
                val fontMetrics = paint.fontMetrics
                val baseline = (rect.centerY() - fontMetrics.top / 2 - fontMetrics.bottom / 2)
                drawText(char.toString().toUpperCase(), rect.centerX().toFloat(), baseline, paint)
            }
        }
    }
}