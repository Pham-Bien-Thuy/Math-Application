package com.example.ttv;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;

import androidx.appcompat.widget.AppCompatImageView;

public class ZoomImageView extends AppCompatImageView {
    private ScaleGestureDetector scaleGestureDetector;

    public ZoomImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init(context);
    }

    private void init(Context context) {
        scaleGestureDetector = new ScaleGestureDetector(context, new ScaleListener());
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        // Ensure the view is focusable and focusable in touch mode to handle touch events
        setFocusable(true);
        setFocusableInTouchMode(true);

        // Pass the touch event to the scaleGestureDetector
        scaleGestureDetector.onTouchEvent(event);

        // Return true to indicate that the event has been handled
        return true;
    }

    private class ScaleListener extends ScaleGestureDetector.SimpleOnScaleGestureListener {
        @Override
        public boolean onScale(ScaleGestureDetector detector) {
            float scaleFactor = detector.getScaleFactor();
            setScaleX(scaleFactor);
            setScaleY(scaleFactor);
            return true;
        }
    }
}
