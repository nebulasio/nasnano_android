package io.nebulas.wallet.android.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.util.AttributeSet;

/**
 * 去除TextView的上、下、左、右默认边距
 *
 * Created by Heinoc on 2018/3/19.
 */
public class TextViewNoPadding extends android.support.v7.widget.AppCompatTextView {

//    Paint.FontMetricsInt fontMetricsInt;
    private final Paint mPaint = new Paint();

    private final Rect mBounds = new Rect();

    public TextViewNoPadding(Context context) {
        super(context);
    }

    public TextViewNoPadding(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public TextViewNoPadding(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    //    @Override
//    protected void onDraw(Canvas canvas) {
//        if (fontMetricsInt == null){
//            fontMetricsInt = new Paint.FontMetricsInt();
//            getPaint().getFontMetricsInt(fontMetricsInt);
//        }
//        canvas.translate(0, fontMetricsInt.top - fontMetricsInt.ascent);
//        super.onDraw(canvas);
//
//
//
//    }
    @Override
    protected void onDraw(Canvas canvas) {
        final String text = calculateTextParams();

        final int left = mBounds.left;
        final int bottom = mBounds.bottom;
        mBounds.offset(-mBounds.left, -mBounds.top);
        mPaint.setAntiAlias(true);
        mPaint.setColor(getCurrentTextColor());
        canvas.drawText(text, -left, mBounds.bottom - bottom, mPaint);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
        calculateTextParams();
        setMeasuredDimension(mBounds.width() + 10, -mBounds.top + 6);
    }

    private String calculateTextParams() {
        final String text = getText().toString();
        final int textLength = text.length();
        mPaint.setTextSize(getTextSize());
        mPaint.getTextBounds(text, 0, textLength, mBounds);
        if (textLength == 0) {
            mBounds.right = mBounds.left;
        }
        return text;
    }
}
