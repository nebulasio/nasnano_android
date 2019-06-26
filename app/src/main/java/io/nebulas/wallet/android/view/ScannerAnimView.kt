package io.nebulas.wallet.android.view

import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.util.AttributeSet
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import com.young.scanner.IScannerView
import com.young.scanner.screenHeight
import com.young.scanner.screenWidth
import io.nebulas.wallet.android.R
import org.jetbrains.anko.dip
import org.jetbrains.anko.sp

class ScannerAnimView(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : FrameLayout(context, attrs, defStyleAttr), IScannerView {

    override fun getDecodeArea(): Rect {
        return realAnimView.getDecodeArea()
    }

    override fun onStartPreview() {
        realAnimView.onStartPreview()
    }

    override fun onStopPreview() {
        realAnimView.onStopPreview()
    }

    constructor(context: Context?, attrs: AttributeSet?) : this(context, attrs, 0)

    constructor(context: Context?) : this(context, null)

    private val scanAreaWidth = (context?.screenWidth() ?: 0) * 0.7
    private var finalRectLeft = ((context?.screenWidth() ?: 0) - scanAreaWidth) / 2
    private var finalRectRight = finalRectLeft + scanAreaWidth
    private var finalRectTop = (context?.screenHeight() ?: 0) / 2 - scanAreaWidth / 2 - (context?.dip(50)
            ?: 50)
    private var finalRectBottom = finalRectTop + scanAreaWidth

    private val textBounds = Rect(0,0,0,0)

    private val colorBg = context?.resources?.getColor(R.color.color_50P_000000)
            ?: Color.parseColor("#7f000000")

    private val mode = PorterDuffXfermode(PorterDuff.Mode.SRC_OUT)

    private val roundRectF = RectF(finalRectLeft.toFloat(), finalRectTop.toFloat(), finalRectRight.toFloat(), finalRectBottom.toFloat())
    private val roundCornerPX = context?.dip(8) ?: 10
    private val alphaBackground = Rect(0, 0, 0, 0)

    private var originLineBitmap = BitmapFactory.decodeResource(context?.resources, R.drawable.scan_line)
    private val lineBitmap = Bitmap.createScaledBitmap(originLineBitmap, scanAreaWidth.toInt(), originLineBitmap.height, true).also {
        originLineBitmap.recycle()
        originLineBitmap = null
    }
    private val lineWidth = lineBitmap.width
    private val lineHeight = lineBitmap.height
    private val linePadding = (scanAreaWidth - lineWidth) / 2
    private val lineSrcRect = Rect(0, 0, lineWidth, lineHeight)
    private val lineLeft = finalRectLeft.toInt() + linePadding.toInt()
    private val finalLineTop = finalRectTop.toInt()
    private var lineTop = finalLineTop
    private val lineDstRect = Rect(lineLeft, finalLineTop, lineLeft + lineWidth, finalLineTop + lineHeight)

    private val text = context?.getText(R.string.scan_code_tip) ?: ""

    private val realAnimView:RealAnimView = RealAnimView(context)

    init {
        addView(realAnimView, ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT))
        context?.also {
            val marginLayoutParams = MarginLayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            val hMargin = it.dip(72)
            marginLayoutParams.marginStart = hMargin
            marginLayoutParams.marginEnd = hMargin
            marginLayoutParams.topMargin = finalRectBottom.toInt() + it.dip(10)
            addView(generateTextView(it), marginLayoutParams)
        }
    }

    private fun generateTextView(context: Context):TextView{
        val textView = TextView(context)
        textView.text = text
        textView.textSize = 14f
        textView.gravity = Gravity.CENTER_HORIZONTAL
        textView.setTextColor(Color.WHITE)
        return textView
    }


    inner class RealAnimView(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : View(context, attrs, defStyleAttr),IScannerView {

        override fun getDecodeArea(): Rect {
            return Rect(finalRectLeft.toInt(), finalRectTop.toInt(), finalRectRight.toInt(), finalRectBottom.toInt())
        }

        override fun onStartPreview() {
            if (!anim.isStarted) {
                anim.start()
            }
        }

        override fun onStopPreview() {
            anim.cancel()
        }

        private val paint: Paint = Paint().apply {
            isAntiAlias = true
        }
        private val linePaint: Paint = Paint().apply {
            isAntiAlias = true
        }
        private val textPaint: Paint = Paint().apply {
            isAntiAlias = true
            color = Color.WHITE
            textSize = context?.sp(14)?.toFloat() ?: 24f
            textAlign = Paint.Align.CENTER
        }
        private val borderPaint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.STROKE
            strokeWidth = context?.dip(1)?.toFloat() ?: 1f
            color = Color.WHITE
        }

        private val anim = ValueAnimator.ofInt(finalRectTop.toInt(), finalRectBottom.toInt() - lineHeight).apply {
            duration = 1000
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
            addUpdateListener {
                lineTop = it.animatedValue as Int
                postInvalidate()
            }
        }

        constructor(context: Context?, attrs: AttributeSet?) : this(context, attrs, 0)
        constructor(context: Context?) : this(context, null)

        override fun onDetachedFromWindow() {
            super.onDetachedFromWindow()
            anim.cancel()
        }

        override fun onDraw(canvas: Canvas?) {
            super.onDraw(canvas)
            if (canvas == null) {
                return
            }
            textPaint.getTextBounds(text.toString(), 0, text.length, textBounds)
            alphaBackground.set(0, 0, width, height)
            val sc = canvas.saveLayerAlpha(0f, 0f, width.toFloat(), height.toFloat(), 255, Canvas.ALL_SAVE_FLAG)
            paint.color = colorBg
            canvas.drawRect(alphaBackground, paint)
            paint.xfermode = mode
            paint.color = Color.TRANSPARENT
            canvas.drawRoundRect(roundRectF, roundCornerPX.toFloat(), roundCornerPX.toFloat(), paint)
            paint.xfermode = null
            canvas.restoreToCount(sc)

            canvas.drawRoundRect(roundRectF, roundCornerPX.toFloat(), roundCornerPX.toFloat(), borderPaint)

            lineDstRect.top = lineTop
            lineDstRect.bottom = lineTop + lineHeight
            canvas.drawBitmap(lineBitmap, lineSrcRect, lineDstRect, linePaint)
        }
    }

}