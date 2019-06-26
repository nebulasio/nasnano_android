package io.nebulas.wallet.android.view;

/**
 * Created by Heinoc on 2018/3/23.
 */

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.*;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import io.nebulas.wallet.android.R;

import java.util.ArrayList;


public class GasSeekBar extends View {
    private final String TAG = "GasSeekBar";
    private int width;
    private int height;
    private int downX = 0;
    private int downY = 0;
    private int upX = 0;
    private int upY = 0;
    private int moveX = 0;
    private int moveY = 0;
    private float scale = 0;
    private int perWidth = 0;
    private Paint mPaint;
    private Paint mTextPaint;
    private Paint buttonPaint;
    private Canvas canvas;
    private Bitmap bitmap;
    private Bitmap thumb;
    //    private Bitmap spot;
//    private Bitmap spot_on;
    private int hotarea = 100;//点击的热区
    private int cur_sections = 2;
    private ResponseOnTouch responseOnTouch;
    private int bitMapHeight = 38;//第一个点的起始位置起始，图片的长宽是76，所以取一半的距离
    private int textMove = 60;//字与下方点的距离，因为字体字体是40px，再加上10的间隔
    private int[] colors = new int[]{0xff038afb, 0xffdfe2e5};//进度条的橙色,进度条的灰色,字体的灰色
    private int textSize;
    private int circleRadius;
    private ArrayList<String> section_title;

    public GasSeekBar(Context context) {
        super(context);
    }

    public GasSeekBar(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    @SuppressLint("ResourceType")
    public GasSeekBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        cur_sections = 0;
        bitmap = Bitmap.createBitmap(900, 900, Bitmap.Config.ARGB_8888);
        canvas = new Canvas();
        canvas.setBitmap(bitmap);
        //解析图片
        thumb = BitmapFactory.decodeResource(getResources(), R.drawable.drag_oval);
//        spot = BitmapFactory.decodeResource(getResources(),R.drawable.arrow);
//        spot_on = BitmapFactory.decodeResource(getResources(), R.drawable.arrow_down);
        bitMapHeight = thumb.getHeight() / 2;
        textMove = bitMapHeight + 22;
        textSize = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_SP, 12, getResources().getDisplayMetrics());
        circleRadius = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 3, getResources().getDisplayMetrics());
        mPaint = new Paint(Paint.DITHER_FLAG);
        mPaint.setAntiAlias(true);//锯齿不显示  
        mPaint.setStrokeWidth(8);
        mTextPaint = new Paint(Paint.DITHER_FLAG);
        mTextPaint.setAntiAlias(true);
        mTextPaint.setTextSize(textSize);
        mTextPaint.setColor(0xff8f8f8f);
        buttonPaint = new Paint(Paint.DITHER_FLAG);
        buttonPaint.setAntiAlias(true);
        initData(null);
    }

    /**
     * 实例化后调用，设置bar的段数和文字
     */
    public void initData(ArrayList<String> section) {
        if (null != section_title)
            section_title.clear();

        if (section != null) {
            section_title = section;
        } else {
            String[] str = new String[]{"低", "中", "高"};
            section_title = new ArrayList<String>();
            for (int i = 0; i < str.length; i++) {
                section_title.add(str[i]);
            }
        }
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);

        int widthMode = MeasureSpec.getMode(widthMeasureSpec);
        int widthSize = MeasureSpec.getSize(widthMeasureSpec);
        int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        int heightSize = MeasureSpec.getSize(heightMeasureSpec);

        width = widthSize;
        float scaleX = widthSize / 1080;
        float scaleY = heightSize / 1920;
        scale = Math.max(scaleX, scaleY);
        //控件的高度  
        //        height = 185;  
        height = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 62, getResources().getDisplayMetrics());
        setMeasuredDimension(width, height);
//        width = width - bitMapHeight / 2;
        perWidth = (width - thumb.getWidth()) / (section_title.size() - 1);
//        perWidth = (width - section_title.size() - thumb.getWidth()/2) / (section_title.size()-1);
        hotarea = perWidth / 2;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        mPaint.setColor(Color.WHITE);
        mPaint.setStyle(Paint.Style.FILL);
        mPaint.setAlpha(0);
        canvas.drawRect(0, 0, getMeasuredWidth(), getMeasuredHeight(), mPaint);
        canvas.drawBitmap(bitmap, 0, 0, null);
        mPaint.setAlpha(255);
        mPaint.setColor(colors[1]);

        // 默认横线的颜色
        canvas.drawLine(thumb.getWidth() / 2, height * 2 / 3, width - thumb.getWidth() / 2, height * 2 / 3, mPaint);

        int section = 0;
        while (section < section_title.size()) {
            if (section < cur_sections) {
                mPaint.setColor(colors[0]);
                //已选中横线的颜色
                canvas.drawLine(thumb.getWidth() / 2 + section * perWidth, height * 2 / 3,
                        thumb.getWidth() / 2 + (section + 1) * perWidth, height * 2 / 3, mPaint);
            }

            // 竖线
            if (section < cur_sections)
                mPaint.setColor(colors[0]);
            else
                mPaint.setColor(colors[1]);

            canvas.drawLine(thumb.getWidth() / 2 + section * perWidth, height * 2 / 3 - 10, thumb.getWidth() / 2 + section * perWidth, height * 2 / 3 + 10, mPaint);

            if (section == section_title.size() - 1) {
                canvas.drawText(section_title.get(section), width - thumb.getHeight() / 2 - textSize / 2, height * 2 / 3 - textMove, mTextPaint);
            } else {
                canvas.drawText(section_title.get(section), thumb.getWidth() / 2 + section * perWidth - textSize / 2, height * 2 / 3 - textMove, mTextPaint);
            }
            section++;
        }
//        if (cur_sections == section_title.size() - 1) {
//            canvas.drawBitmap(thumb, width - thumb.getHeight() / 2,
//                    height * 2 / 3 - thumb.getHeight() / 2, buttonPaint);
//        } else {
        canvas.drawBitmap(thumb, cur_sections * perWidth,
                height * 2 / 3 - thumb.getHeight() / 2, buttonPaint);
//        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        super.onTouchEvent(event);
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
//                thumb = BitmapFactory.decodeResource(getResources(), R.drawable.set_button_1);
                downX = (int) event.getX();
                downY = (int) event.getY();
                responseTouch(downX, downY);
                break;
            case MotionEvent.ACTION_MOVE:
//                thumb = BitmapFactory.decodeResource(getResources(), R.drawable.set_button_1);
                moveX = (int) event.getX();
                moveY = (int) event.getY();
                responseTouch(moveX, moveY);
                break;
            case MotionEvent.ACTION_UP:
//                thumb = BitmapFactory.decodeResource(getResources(), R.drawable.set_button_0);
                upX = (int) event.getX();
                upY = (int) event.getY();
                responseTouch(upX, upY);

                if (null != responseOnTouch)
                    responseOnTouch.onTouchResponse(cur_sections);
                break;
        }
        return true;
    }

    private void responseTouch(int x, int y) {
        if (x <= width - bitMapHeight / 2) {
            cur_sections = (x + perWidth / 3) / perWidth;
        } else {
            cur_sections = section_title.size() - 1;
        }
        invalidate();
    }

    //设置监听  
    public void setResponseOnTouch(ResponseOnTouch response) {
        responseOnTouch = response;
    }

    //设置进度  
    public void setProgress(int progress) {
        cur_sections = progress;
        invalidate();
    }


    public interface ResponseOnTouch {
        public void onTouchResponse(int volume);
    }


}