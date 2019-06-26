package io.nebulas.wallet.android.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.os.Build
import android.util.AttributeSet
import android.view.View
import io.nebulas.wallet.android.R
import org.jetbrains.anko.dip
import org.jetbrains.anko.sp
import kotlin.math.max


class PasswordStrengthView(context: Context?, attrs: AttributeSet?) : View(context, attrs) {

    private val strengthWords = arrayOf(
            R.string.password_strength_none,
            R.string.password_strength_short,
            R.string.password_strength_medium,
            R.string.password_strength_strong
    )
    private val strengthColors = arrayOf(
            R.color.color_FF5552,
            R.color.color_FF5552,
            R.color.color_FF8F00,
            R.color.color_00CB91
    )
    private val asc2Zero = '0'.toInt()
    private val asc2Nine = '9'.toInt()
    private val asc2a = 'a'.toInt()
    private val asc2z = 'z'.toInt()
    private val asc2A = 'A'.toInt()
    private val asc2Z = 'Z'.toInt()

    private var strengthWord: Int = strengthWords[0]
    private var strengthColor: Int = strengthColors[0]
    private var count: Int = 0
    private val paint: Paint = Paint()
    private val textBounds = Rect(0, 0, 0, 0)
    private val rect = RectF(0f,
            0f,
            (context?.dip(6) ?: 0).toFloat(),
            (context?.dip(9) ?: 0).toFloat())
    private val roundCorner = (context?.dip(1) ?: 0).toFloat()
    private val rectPadding = context?.dip(3) ?: 0
    private val perRectWidth = context?.dip(6) ?: 0
    private val perRectHeight = context?.dip(9) ?: 0
    private val verticalPadding = 10
    private var maxHeight = 0

    init {
        paint.isAntiAlias = true
        paint.textSize = (context?.sp(13) ?: 0).toFloat()
        paint.textAlign = Paint.Align.LEFT
        strengthWords.forEach {
            val word = context?.getString(it)?:""
            paint.getTextBounds(word, 0, word.length, textBounds)
            if (textBounds.height() > maxHeight) {
                maxHeight = textBounds.height()
            }
        }
    }

    fun check(pwd: String) {
        val score: Int
        if (pwd.isEmpty()) {
            score = 0
            setScore(score)
            return
        }
        checkDetail(pwd)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val widthMode = MeasureSpec.getMode(widthMeasureSpec)
        val widthSize = MeasureSpec.getSize(widthMeasureSpec)
        val heightMode = MeasureSpec.getMode(heightMeasureSpec)
        val heightSize = MeasureSpec.getSize(heightMeasureSpec)
        var width = widthSize
        var height = heightSize
        paint.textSize = context.sp(13).toFloat()
        val word = context?.getString(strengthWord)?:""
        paint.getTextBounds(word, 0, word.length, textBounds)
        if (widthMode != MeasureSpec.EXACTLY) {
            val textWidth = textBounds.width()
            val rectPaddingWidth = rectPadding * 5
            val paddingBetweenWordAndRect = context.dip(6)
            width = textWidth + perRectWidth * 6 + rectPaddingWidth + paddingBetweenWordAndRect
        }
        if (heightMode != MeasureSpec.EXACTLY) {
            val rectHeight = context.dip(9)
            height = max(maxHeight, rectHeight) + verticalPadding
        }
        setMeasuredDimension(width, height)
    }

    override fun onDraw(canvas: Canvas?) {
        super.onDraw(canvas)
        paint.color = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            context.getColor(strengthColor)
        } else {
            context.resources.getColor(strengthColor)
        }
        var offset = 0f
        val fontMetrics = paint.fontMetricsInt
        val baseline = (measuredHeight - fontMetrics.bottom + fontMetrics.top) / 2 - fontMetrics.top
        val word = context?.getString(strengthWord)?:""
        canvas?.drawText(word, 0f, baseline.toFloat() - (verticalPadding / 2), paint)

        paint.color = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            context.getColor(R.color.color_D4D9DE)
        } else {
            context.resources.getColor(R.color.color_D4D9DE)
        }
        offset += textBounds.width()
        offset += context.dip(6)
        val grayCount = 6 - count
        rect.top = (measuredHeight - perRectHeight) / 2f
        rect.bottom = rect.top + perRectHeight
        (0 until grayCount).forEach {
            rect.left = offset
            rect.right = offset + perRectWidth
            canvas?.drawRoundRect(rect, roundCorner, roundCorner, paint)
            offset += perRectWidth
            offset += rectPadding
        }

        paint.color = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            context.getColor(strengthColor)
        } else {
            context.resources.getColor(strengthColor)
        }
        (grayCount until 6).forEach {
            rect.left = offset
            rect.right = offset + perRectWidth
            canvas?.drawRoundRect(rect, roundCorner, roundCorner, paint)
            offset += perRectWidth
            offset += rectPadding
        }

    }

    private fun checkDetail(pwd: String) {
        val length = pwd.length
        var lowerCharCount = 0
        var upperCharCount = 0
        var numberCount = 0
        var symbolCount = 0
        val arr = pwd.toCharArray()
        arr.forEach {
            when (it.toInt()) {
                in (asc2Zero..asc2Nine) -> numberCount++
                in (asc2a..asc2z) -> lowerCharCount++
                in (asc2A..asc2Z) -> upperCharCount++
                else -> symbolCount++
            }
        }
        val score = checkLengthScore(length) +
                checkCharacterScore(lowerCharCount, upperCharCount) +
                checkNumberScore(numberCount) +
                checkSymbolScore(symbolCount) +
                checkAdditionalScore(lowerCharCount, upperCharCount, numberCount, symbolCount)
        setScore(score)
    }

    /**
     * 长度：
     * 长度 == 0 0分
     * 长度 <=4 5分
     * 长度 [5, 7] 10分
     * 长度 [8, +∞] 25分
     */
    private fun checkLengthScore(length: Int): Int {
        return when (length) {
            0 -> 0
            in (1..4) -> 5
            in (5..7) -> 10
            else -> 25
        }
    }

    /**
     * 字母：
     * 不包含字母 0分
     * 只有小写或者只有大写 10分
     * 即有小写又有大写 20
     */
    private fun checkCharacterScore(lowerCount: Int, upperCount: Int): Int {
        return if (lowerCount <= 0 && upperCount <= 0) {
            0
        } else if (lowerCount > 0 && upperCount > 0) {
            20
        } else {
            10
        }
    }

    /**
     * 数字：
     * 不包含数字 0分
     * 一个数字 10分
     * 多个数字 20
     */
    private fun checkNumberScore(numberCount: Int): Int {
        return when (numberCount) {
            0 -> 0
            1 -> 10
            else -> 20
        }
    }

    /**
     * 特殊符号：
     * 不包含符号 0分
     * 一个符号 10分
     * 多个符号 25
     */
    private fun checkSymbolScore(symbolCount: Int): Int {
        return when (symbolCount) {
            0 -> 0
            1 -> 10
            else -> 25
        }
    }

    /**
     * 附加分：
     * 既有大写又有小写字母且有数字且有符号 5分
     * 只有大或小写字母且有数字且有符号 3分
     * 只由字母和数字组成 2分
     * 其他 0分
     */
    private fun checkAdditionalScore(lowerCount: Int,
                                     upperCount: Int,
                                     numberCount: Int,
                                     symbolCount: Int): Int {
        if (lowerCount > 0 && upperCount > 0 && numberCount > 0 && symbolCount > 0) {
            // 既有大写又有小写字母且有数字且有符号 5分
            return 5
        }
        if ((lowerCount > 0 || upperCount > 0) && numberCount > 0 && symbolCount > 0) {
            // 只有大或小写字母且有数字且有符号 3分
            return 3
        }
        if ((lowerCount > 0 || upperCount > 0) && numberCount > 0 && symbolCount <= 0) {
            // 只由字母和数字组成 2分
            return 2
        }
        return 0
    }

    private fun setScore(score: Int) {
        var index: Int
        when (score) {
            0 -> {
                index = 0
                count = 0
            }
            in (1..25) -> {
                index = 1
                count = 1
            }
            in (26..40) -> {
                index = 1
                count = 2
            }
            in (41..50) -> {
                index = 2
                count = 3
            }
            in (51..70) -> {
                index = 2
                count = 4
            }
            in (71..90) -> {
                index = 3
                count = 5
            }
            else -> {
                index = 3
                count = 6
            }
        }
        strengthWord = strengthWords[index]
        strengthColor = strengthColors[index]
        requestLayout()
        invalidate()
    }

}