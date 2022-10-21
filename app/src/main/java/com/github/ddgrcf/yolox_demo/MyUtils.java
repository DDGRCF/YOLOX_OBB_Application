package com.github.ddgrcf.yolox_demo;

import android.app.Activity;
import android.content.res.AssetManager;
import android.graphics.Canvas;
import android.graphics.Paint;

import com.blankj.utilcode.util.BarUtils;

public class MyUtils {

    public static void setMyStatusBar(Activity activity) {
        BarUtils.transparentStatusBar(activity);
        BarUtils.setStatusBarLightMode(activity, true);
    }


    public static void drawCanvasRotatedRect(Canvas canvas, Paint paint, YoloxObbNcnn.Obj object) {
        canvas.save();
        canvas.rotate(object.theta, object.x, object.y);
        float left = (float) (object.x - object.w / 2.);
        float top = (float) (object.y - object.h / 2.);
        float right = (float) (object.x + object.w / 2.);
        float bottom = (float) (object.y + object.h / 2.);
        canvas.drawRect(left, top, right, bottom, paint);
        canvas.restore();
    }

}
