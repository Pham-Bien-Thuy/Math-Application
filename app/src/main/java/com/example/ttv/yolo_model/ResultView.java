package com.example.ttv.yolo_model;

import static com.example.ttv.MainActivity.objectList;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

import java.util.ArrayList;


public class ResultView extends View {
    private final static int TEXT_X = 40;
    private final static int TEXT_Y = 35;
    private final static int TEXT_WIDTH = 260;
    private final static int TEXT_HEIGHT = 50;

    private Paint mPaintRectangle;
    private Paint mPaintText;
    private ArrayList<ResultDetect> mResults;

    public ResultView(Context context) {
        super(context);
    }

    public ResultView(Context context, AttributeSet attrs){
        super(context, attrs);
        mPaintRectangle = new Paint();
        mPaintRectangle.setColor(Color.YELLOW);
        mPaintText = new Paint();
    }

    //    @Override
//    protected void onDraw(Canvas canvas) {
//        super.onDraw(canvas);
//
//        if (mResults == null) return;
//        for (Result result : mResults) {
//            mPaintRectangle.setStrokeWidth(5);
//            mPaintRectangle.setStyle(Paint.Style.STROKE);
//            canvas.drawRect(result.rect, mPaintRectangle);
//
//            Path mPath = new Path();
//            RectF mRectF = new RectF(result.rect.left, result.rect.top, result.rect.left + TEXT_WIDTH,  result.rect.top + TEXT_HEIGHT);
//            mPath.addRect(mRectF, Path.Direction.CW);
//            mPaintText.setColor(Color.MAGENTA);
//            canvas.drawPath(mPath, mPaintText);
//
//            mPaintText.setColor(Color.WHITE);
//            mPaintText.setStrokeWidth(0);
//            mPaintText.setStyle(Paint.Style.FILL);
//            mPaintText.setTextSize(32);
//            canvas.drawText(String.format("%s %.2f", PrePostProcessor.mClasses[result.classIndex], result.score), result.rect.left + TEXT_X, result.rect.top + TEXT_Y, mPaintText);
//            listObject += PrePostProcessor.mClasses[result.classIndex] + " ";
//            Log.d("List_object: ", listObject);
//        }
//    }
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (mResults == null) return;

        // Danh sách các màu sẽ được sử dụng cho mỗi object phát hiện
        int[] colors = {Color.BLUE, Color.RED, Color.GREEN, Color.YELLOW, Color.CYAN, Color.MAGENTA};
        int colorIndex = 0;

        for (ResultDetect result : mResults) {
            // Chọn màu cho object phát hiện hiện tại từ danh sách màu
            int currentColor = colors[colorIndex % colors.length];
            colorIndex++;

            mPaintRectangle.setStrokeWidth(5);
            mPaintRectangle.setStyle(Paint.Style.STROKE);
            mPaintRectangle.setColor(currentColor); // Đặt màu cho hình chữ nhật

            canvas.drawRect(result.rect, mPaintRectangle);

            Path mPath = new Path();
            RectF mRectF = new RectF(result.rect.left, result.rect.top, result.rect.left + TEXT_WIDTH,  result.rect.top + TEXT_HEIGHT);
            mPath.addRect(mRectF, Path.Direction.CW);
            mPaintText.setColor(currentColor); // Đặt màu cho hộp chữ
            canvas.drawPath(mPath, mPaintText);

            mPaintText.setColor(Color.WHITE);
            mPaintText.setStrokeWidth(0);
            mPaintText.setStyle(Paint.Style.FILL);
            mPaintText.setTextSize(32);
            canvas.drawText(String.format("%s %.2f", PrePostProcessor.mClasses[result.classIndex], result.score), result.rect.left + TEXT_X, result.rect.top + TEXT_Y, mPaintText);
            //listObject += PrePostProcessor.mClasses[result.classIndex] + " ";

        }
    }


    public void setResults(ArrayList<ResultDetect> results) {
        mResults = results;
    }
}
