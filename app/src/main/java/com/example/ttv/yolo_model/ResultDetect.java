package com.example.ttv.yolo_model;

import android.graphics.Rect;

public class ResultDetect {
    int classIndex;
    Float score;
    Rect rect;

    public ResultDetect(int cls, Float output, Rect rect) {
        this.classIndex = cls;
        this.score = output;
        this.rect = rect;
    }
}
