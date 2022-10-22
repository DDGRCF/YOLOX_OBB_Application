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
    public static final List<Integer> classColors = new ArrayList<>();
    public static final List<String> classNames = new ArrayList<>(Arrays.asList(
            "LV", "SP", "HC", "BR",
            "PL", "SP", "SBF", "BC",
            "GTF", "SV", "BD",
            "TC", "RA", "ST", "HB"
    ));
    public static Param parameter = new Param();

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

    public native Obj[] Deal(Obj[] objects, Param param);

    // rotated rectangle object
    static public class Obj {
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
    public static class Param {
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
