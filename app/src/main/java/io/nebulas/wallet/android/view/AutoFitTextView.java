package io.nebulas.wallet.android.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import android.util.TypedValue;

/**
 * 自定义TextView，文本内容自动调整字体大小以适应TextView的大小
 *
 * Created by Heinoc on 2018/2/27.
 */
public class AutoFitTextView extends android.support.v7.widget.AppCompatTextView {

    private Paint mPaint;
    private float originTextSize;
    private float mTextSize;

    public AutoFitTextView(Context context) {
        super(context);
    }

    public AutoFitTextView(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    public AutoFitTextView(Context context, AttributeSet attrs,
                           int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        this.setSingleLine();
        this.setLines(1);
    }

    /**
     * getTextSize 返回值是以像素(px)为单位的，而 setTextSize() 默认是 sp 为单位
     * 因此我们要传入像素单位 setTextSize(TypedValue.COMPLEX_UNIT_PX, size);
     */
    private void refitText(String text, int textWidth) {
        if (textWidth > 0) {
            mTextSize = this.getTextSize();//这个返回的单位为px
            mPaint = new Paint();
            mPaint.set(this.getPaint());
            int drawWid = 0;//drawableLeft，Right，Top，Buttom 所有图片的宽
            Drawable[] draws = getCompoundDrawables();
            for (int i = 0; i < draws.length; i++) {
                if (draws[i] != null) {
                    drawWid += draws[i].getBounds().width();
                }
            }
            //获得当前TextView的有效宽度
            int availableWidth = textWidth - this.getPaddingLeft()
                    - this.getPaddingRight() - getCompoundDrawablePadding() - drawWid;
            //所有字符所占像素宽度
            float textWidths = getTextLength(mTextSize, text);
            while (textWidths > availableWidth) {
                mPaint.setTextSize(--mTextSize);//这里传入的单位是 px
                textWidths = getTextLength(mTextSize, text);
            }
            while ((textWidths + 20) < availableWidth && mTextSize < originTextSize) {
                mPaint.setTextSize(++mTextSize);
                textWidths = getTextLength(mTextSize, text);
            }
            this.setTextSize(TypedValue.COMPLEX_UNIT_PX, mTextSize);//这里设置单位为 px
        }
    }

    /**
     * @param textSize
     * @param text
     * @return 字符串所占像素宽度
     */
    private float getTextLength(float textSize, String text) {
        mPaint.setTextSize(textSize);
        return mPaint.measureText(text);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        if (isInEditMode()) {
            return;
        }
        if (originTextSize == 0) {
            originTextSize = this.getTextSize();
            this.setSingleLine();
            this.setLines(1);
        }

        refitText(getText().toString(), this.getWidth());
    }


//
//    private Paint mTextPaint;
//    private float mTextSize;
//
//    public AutoFitTextView(Context context) {
//        super(context);
//    }
//
//    public AutoFitTextView(Context context, AttributeSet attrs) {
//        super(context, attrs);
//    }
//
//    /**
//     * Re size the font so the specified text fits in the text box assuming the
//     * text box is the specified width.
//     *
//     * @param text
//     * @param textViewWidth
//     */
//    private void refitText(String text, int textViewWidth) {
//        if (text == null || textViewWidth <= 0)
//            return;
//        mTextPaint = new Paint();
//        mTextPaint.set(this.getPaint());
//        int availableTextViewWidth = getWidth() - getPaddingLeft() - getPaddingRight();
//        float[] charsWidthArr = new float[text.length()];
//        Rect boundsRect = new Rect();
//        mTextPaint.getTextBounds(text, 0, text.length(), boundsRect);
//        int textWidth = boundsRect.width();
//        mTextSize = getTextSize();
//        while (textWidth > availableTextViewWidth) {
//            mTextSize -= 1;
//            mTextPaint.setTextSize(mTextSize);
//            textWidth = mTextPaint.getTextWidths(text, charsWidthArr);
//        }
//        this.setTextSize(TypedValue.COMPLEX_UNIT_PX, mTextSize);
//    }
//
//    @Override
//    protected void onDraw(Canvas canvas) {
//        super.onDraw(canvas);
//        refitText(this.getText().toString(), this.getWidth());
//    }
}