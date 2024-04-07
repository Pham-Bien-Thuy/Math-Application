package com.example.ttv.chatbox;

//import static com.example.ttv.chatbox.chatbox.objectList;

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


public class ResultViews extends View {
    public static String listedObject = "";
    private final static int TEXT_X = 40;
    private final static int TEXT_Y = 35;
    private final static int TEXT_WIDTH = 260;
    private final static int TEXT_HEIGHT = 50;

    private Paint mPaintRectangle;
    private Paint mPaintText;
    public static ArrayList<ResultDetect> mResults;

    public ResultViews(Context context) {
        super(context);
    }

    public ResultViews(Context context, AttributeSet attrs){
        super(context, attrs);
        mPaintRectangle = new Paint();
        mPaintRectangle.setColor(Color.GREEN);
        mPaintRectangle.setStyle(Paint.Style.STROKE);
        mPaintText = new Paint();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        if (mResults == null) return;
        for (ResultDetect result : mResults) {
            mPaintRectangle.setStrokeWidth(5);
            canvas.drawRect(result.rect, mPaintRectangle);
            canvas.drawRect(0, 0, canvas.getWidth(), canvas.getHeight(), mPaintRectangle);

            Path mPath = new Path();
            RectF mRectF = new RectF(result.rect.left, result.rect.top, result.rect.left + TEXT_WIDTH,  result.rect.top + TEXT_HEIGHT);
            mPath.addRect(mRectF, Path.Direction.CW);
            mPaintText.setColor(Color.MAGENTA);
            canvas.drawPath(mPath, mPaintText);

            mPaintText.setColor(Color.WHITE);
            mPaintText.setStrokeWidth(0);
            mPaintText.setStyle(Paint.Style.FILL);
            mPaintText.setTextSize(32);
            canvas.drawText(String.format("%s %.2f", com.example.ttv.chatbox.PrePostProcess.mClasses[result.classIndex], result.score), result.rect.left + TEXT_X, result.rect.top + TEXT_Y, mPaintText);
//            objectList.add(com.example.ttv.chatbox.PrePostProcess.mClasses[result.classIndex]);
            listedObject += com.example.ttv.chatbox.PrePostProcess.mClasses[result.classIndex] + " ";
        }
//        Log.d("number_object", String.valueOf(objectList.size()) + " " + listedObject);
    }

    public void setResults(ArrayList<ResultDetect> results) {
        mResults = results;
    }
}
//change activate video view
//        getvideo = (ImageButton) findViewById(R.id.getvideo);
//                getvideo.setOnClickListener(new View.OnClickListener() {
//@Override
//public void onClick(View view) {
//        Intent intent = new Intent(chatbox.this, DisplayActivity.class);
//        startActivity(intent);
//        }
//        });