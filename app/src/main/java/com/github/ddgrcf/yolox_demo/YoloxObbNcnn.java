package com.github.ddgrcf.yolox_demo;

import android.content.res.AssetManager;
import android.graphics.Bitmap;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

// must init before detect
public class YoloxObbNcnn {

    // load library
    static {
        System.loadLibrary("yoloxobbncnn");
    }

    // set class names
    public static final List<String> classNames = new ArrayList<>(Arrays.asList(
            "large-vehicle", "swimming-pool", "helicopter", "bridge",
            "plane", "ship", "soccer-ball-field", "basketball-court",
            "ground-track-field", "small-vehicle", "baseball-diamond",
            "tennis-court", "roundabout", "storage-tank", "harbor"
    ));

    // singleton
    private static YoloxObbNcnn instance = new YoloxObbNcnn();
    private YoloxObbNcnn(){}
    public static YoloxObbNcnn getInstance() {
        return instance;
    }

    // init cpp singleton
    public native boolean Init(AssetManager mgr);
    // detect bitmap with parameters
    public native Obj[] Detect(Bitmap bitmap, Param param);

    // rotated rectangle object
    public class Obj {
        public float x;
        public float y;
        public float w;
        public float h;
        public float theta;
        public int label;
        public float prob;

        @NonNull
        @Override
        public String toString() {
            return "Obj{" +
                    "x=" + x +
                    ", y=" + y +
                    ", w=" + w +
                    ", h=" + h +
                    ", theta=" + theta +
                    ", label=" + label +
                    ", prob=" + prob +
                    '}';
        }
    }

    // detect parameters
    public class Param {
        public float confScoreThreshold = 0.2f;
        public float nmsIoUThreshold = 0.1f;
        public boolean agnostic = false;
        public boolean useGpu = false;

        @NonNull
        @Override
        public String toString() {
            return "Param{" +
                    "confScoreThreshold=" + confScoreThreshold +
                    ", nmsIoUThreshold=" + nmsIoUThreshold +
                    ", agnostic=" + agnostic +
                    ", useGpu=" + useGpu +
                    '}';
        }
    }

}
