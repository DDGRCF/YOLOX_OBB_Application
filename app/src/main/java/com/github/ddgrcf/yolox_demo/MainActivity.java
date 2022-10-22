package com.github.ddgrcf.yolox_demo;

import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.media.ExifInterface;
import android.media.Image;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.MultiAutoCompleteTextView;
import android.widget.RelativeLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.blankj.utilcode.util.BarUtils;
import com.blankj.utilcode.util.ClickUtils;
import com.blankj.utilcode.util.ImageUtils;
import com.blankj.utilcode.util.ObjectUtils;
import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.interfaces.datasets.IBarDataSet;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import pl.droidsonroids.gif.GifDrawable;
import pl.droidsonroids.gif.GifImageView;

public class MainActivity extends AppCompatActivity {

    public static final String TAG = MainActivity.class.getName();

    private RelativeLayout mMainRootLayout;
    private RelativeLayout mMainMainLayout;

    private GifImageView mMainBgGifView;
    private ImageView mMainDisplayImageView;
    private ImageView mMainTouchImageView;

    private Button mMainDetectBtn;

    private SeekBar mMainConfThreSeekBar;
    private TextView mMainConfThreSeekBarText;
    private SeekBar mMainNmsThreSeekBar;
    private TextView mMainNmsThreSeekBarText;

    private ImageView mMainTencentQQIcon;
    private ImageView mMainGithubIcon;
    private ImageView mMainZhihuIcon;

    private BarChart mMainDetectNumberBarChart;

    private Handler handler = new Handler();

    private Bitmap tmpBitmap = null;
    private Bitmap selectedBitmap = null;

    private int selectImageCode = 1001;

    private YoloxObbNcnn.Obj[] detectObjects = null;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
//        ScreenUtils.setLandscape(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mMainRootLayout = findViewById(R.id.main_root_layout);
        mMainMainLayout = findViewById(R.id.main_main_layout);

        mMainBgGifView = findViewById(R.id.main_bg_gif_view);
        mMainDisplayImageView = findViewById(R.id.main_display_image_view);
        mMainTouchImageView = findViewById(R.id.main_touch_image_view);

        mMainConfThreSeekBar = findViewById(R.id.main_conf_thre_seek_bar);
        mMainConfThreSeekBarText = findViewById(R.id.main_conf_thre_seek_bar_text);
        mMainNmsThreSeekBar = findViewById(R.id.main_nms_thre_seek_bar);
        mMainNmsThreSeekBarText = findViewById(R.id.main_nms_thre_seek_bar_text);

        mMainTencentQQIcon = findViewById(R.id.main_tencent_qq_icon);
        mMainGithubIcon = findViewById(R.id.main_github_icon);
        mMainZhihuIcon = findViewById(R.id.main_zhihu_icon);

        mMainDetectBtn = findViewById(R.id.main_detect_btn);

        mMainDetectNumberBarChart = findViewById(R.id.main_detect_num_barchart);

        // set main content add transparent status bar
        BarUtils.transparentStatusBar(this); // transparent
        BarUtils.setStatusBarLightMode(this, true); // light mode

        BarUtils.addMarginTopEqualStatusBarHeight(mMainMainLayout); // add main content height

        // activity
        initMainBgGifView();
        initMainDetectBtn();
        initMainIconBtn();
        initMainSeekBar();
        initMainStatistics();
        initMainTouchImageView();
    }

    private void initMainBgGifView() {
        GifDrawable gifFromAssets = null;
        try {
            gifFromAssets = new GifDrawable(getAssets(), "images/demo_video.gif");
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (!ObjectUtils.isEmpty(gifFromAssets)) {
            mMainBgGifView.setImageDrawable(gifFromAssets);
        }
    }

    private void initMainSeekBar() {
        mMainConfThreSeekBar.setProgress((int) (YoloxObbNcnn.parameter.confScoreThreshold * 100));
        mMainConfThreSeekBarText.setText("CONF_THRE: " + YoloxObbNcnn.parameter.confScoreThreshold);
        mMainConfThreSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float confThre = (float)(progress / 100.);
                mMainConfThreSeekBarText.setText("CONF_THRE: " + confThre);
                YoloxObbNcnn.parameter.confScoreThreshold = confThre;
                if (!ObjectUtils.isEmpty(detectObjects) && !ObjectUtils.isEmpty(mMainDisplayImageView.getDrawable())) {
                    new Thread(()->{
                        YoloxObbNcnn.Obj[] objects = YoloxObbNcnn.getInstance().Deal(detectObjects, YoloxObbNcnn.parameter);
                        showDetectObjects(objects);
                    }).start();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (!ObjectUtils.isEmpty(detectObjects) && !ObjectUtils.isEmpty(mMainDisplayImageView.getDrawable())) {
                    new Thread(()->{
                        YoloxObbNcnn.Obj[] objects = YoloxObbNcnn.getInstance().Deal(detectObjects, YoloxObbNcnn.parameter);
                        showDetectObjects(objects);
                        updateBarChartData(objects);
                    }).start();
                }
            }
        });
        mMainNmsThreSeekBar.setProgress((int) (YoloxObbNcnn.parameter.nmsIoUThreshold * 100));
        mMainNmsThreSeekBarText.setText("NMSF_THRE: " + YoloxObbNcnn.parameter.nmsIoUThreshold);
        mMainNmsThreSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float nmsThre = (float)(progress / 100.);
                mMainNmsThreSeekBarText.setText("NMSF_THRE: " + nmsThre);
                YoloxObbNcnn.parameter.nmsIoUThreshold = nmsThre;
                if (!ObjectUtils.isEmpty(detectObjects) && !ObjectUtils.isEmpty(mMainDisplayImageView.getDrawable())) {
                    new Thread(()->{
                        YoloxObbNcnn.Obj[] objects = YoloxObbNcnn.getInstance().Deal(detectObjects, YoloxObbNcnn.parameter);
                        showDetectObjects(objects);
                    }).start();
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {

            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                if (!ObjectUtils.isEmpty(detectObjects) && !ObjectUtils.isEmpty(mMainDisplayImageView.getDrawable())) {
                    new Thread(()->{
                        YoloxObbNcnn.Obj[] objects = YoloxObbNcnn.getInstance().Deal(detectObjects, YoloxObbNcnn.parameter);
                        showDetectObjects(objects);
                        updateBarChartData(objects);
                    }).start();
                }
            }
        });
    }

    private void touchImageViewDebouncingClick(View v) {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        startActivityForResult(intent, selectImageCode);
    }

    private void touchImageViewMultiClick(View v) {
        mMainTouchImageView.setAlpha(0.f);
        mMainTouchImageView.animate().alpha(1.f).setDuration(500);
        mMainTouchImageView.setVisibility(View.VISIBLE);
        mMainDisplayImageView.setImageDrawable(null);
        mMainDetectBtn.setClickable(true);
        initMainTouchImageView();
    }

    private void initMainTouchImageView() {
        mMainDisplayImageView.setOnClickListener(new ClickUtils.OnDebouncingClickListener() {
            @Override
            public void onDebouncingClick(View v) {
                touchImageViewDebouncingClick(v);
            }
        });
    }

    private void initMainIconBtn() {
        mMainTencentQQIcon.setOnClickListener(new ClickUtils.OnDebouncingClickListener() {
            @Override
            public void onDebouncingClick(View v) {
                try {
                    String url = "mqqwpa://im/chat?chat_type=wpa&uin=969609856";
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                } catch (Exception e) {
                    e.printStackTrace();
                    Toast.makeText(MainActivity.this, "Check whether installed QQ or not", Toast.LENGTH_SHORT).show();
                }
            }
        });
        mMainGithubIcon.setOnClickListener(new ClickUtils.OnDebouncingClickListener() {
            @Override
            public void onDebouncingClick(View v) {
                Intent intent = new Intent();
                intent.setAction("android.intent.action.VIEW");
                Uri url = Uri.parse("https://github.com/DDGRCF");
                intent.setData(url);
                startActivity(intent);
            }
        });

        mMainZhihuIcon.setOnClickListener(new ClickUtils.OnDebouncingClickListener() {
            @Override
            public void onDebouncingClick(View v) {
                Intent intent = new Intent();
                intent.setAction("android.intent.action.VIEW");
                Uri url = Uri.parse("https://www.zhihu.com/people/huo-yu-qiang-67");
                intent.setData(url);
                startActivity(intent);
            }
        });
    }

    private void initMainDetectBtn() {
        mMainDetectBtn.setClickable(false);
        mMainDetectBtn.setOnClickListener(new ClickUtils.OnDebouncingClickListener() {
            @Override
            public void onDebouncingClick(View v) {
                if (ObjectUtils.isEmpty(mMainDisplayImageView.getDrawable())) {
                    Toast.makeText(MainActivity.this, "please select image first!", Toast.LENGTH_SHORT).show();
                    return;
                }

                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        detectObjects = null;
                        detectObjects = YoloxObbNcnn.getInstance().Detect(selectedBitmap, YoloxObbNcnn.parameter);
                        YoloxObbNcnn.Obj[] objects = YoloxObbNcnn.getInstance().Deal(detectObjects, YoloxObbNcnn.parameter);
                        Log.d(TAG, "ori number: " + detectObjects.length + " " + "deal number: " + objects.length);
                        updateBarChartData(objects);
                        showDetectObjects(objects);
                    }
                }).start();
            }
        });
    }

    private void showDetectObjects(YoloxObbNcnn.Obj[] objects) {
        if (objects == null)
        {
            return;
        }
        // draw objects on bitmap
        Bitmap rgba = tmpBitmap.copy(Bitmap.Config.ARGB_8888, true);

        Canvas canvas = new Canvas(rgba);

        Paint paint = new Paint();
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(4);

        for (int i = 0; i < objects.length; i++)
        {
            paint.setColor(YoloxObbNcnn.classColors.get(objects[i].label));
            MyUtils.drawCanvasRotatedRect(canvas, paint, objects[i]);
        }

        handler.post(()->{
            mMainDisplayImageView.setImageBitmap(rgba);
        });
    }

    private void updateBarChartData(YoloxObbNcnn.Obj[] objects) {
        List<Integer> data = new ArrayList<>(YoloxObbNcnn.classNames.size());
        for (int i = 0; i < YoloxObbNcnn.classNames.size(); i++) {
            data.add(i, 0);
        }

        for (YoloxObbNcnn.Obj object : objects) {
            int classIndex = object.label;
            data.set(classIndex, data.get(classIndex) + 1);
        }
        setDetectNumberBarChartData(mMainDetectNumberBarChart, data, YoloxObbNcnn.classNames);
    }

    private void initMainStatistics() {
        mMainDetectNumberBarChart.setBackgroundColor(getColor(R.color.transparent));
        mMainDetectNumberBarChart.setDrawGridBackground(false);
        mMainDetectNumberBarChart.getDescription().setEnabled(false);
        mMainDetectNumberBarChart.setDrawBarShadow(false);
        mMainDetectNumberBarChart.setDrawBorders(true);

        mMainDetectNumberBarChart.animateY(1000, Easing.Linear);
        mMainDetectNumberBarChart.animateX(1000, Easing.Linear);

        Legend l = mMainDetectNumberBarChart.getLegend();
        l.setFormSize(10f);
        l.setXEntrySpace(5f);
        l.setWordWrapEnabled(true);
        l.setEnabled(true);

        YAxis leftAxis = mMainDetectNumberBarChart.getAxisLeft();
        YAxis rightAxis = mMainDetectNumberBarChart.getAxisRight();
        leftAxis.setAxisMinimum(0f);
        rightAxis.setAxisMinimum(0f);
        leftAxis.setEnabled(false);
        rightAxis.setEnabled(false);

        XAxis xAxis = mMainDetectNumberBarChart.getXAxis();

        //XY轴的设置
        //X轴设置显示位置在底部
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        //xAxis.setGranularity(1f);

        //xAxis.setDrawAxisLine(true);
        xAxis.setDrawGridLines(true);
        xAxis.setDrawLabels(false);

        xAxis.setTextColor(getColor(R.color.black));
        xAxis.setTextSize(10f);
        xAxis.setAxisLineColor(getColor(R.color.black));

        List<Integer> mBarChartInitData = new ArrayList<>();
        for (int i = 0; i < YoloxObbNcnn.classNames.size(); i++) {
            mBarChartInitData.add(0);
        }
        setDetectNumberBarChartData(mMainDetectNumberBarChart, mBarChartInitData, YoloxObbNcnn.classNames);
    }

    private void setDetectNumberBarChartData(BarChart mBarChart, List<Integer> statistics, List<String> labels) {

        final int count = statistics.size();

        ArrayList<IBarDataSet> dataSets = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            int val = statistics.get(i);
            ArrayList<BarEntry> yVals = new ArrayList<>();
            int color = YoloxObbNcnn.classColors.get(i);

            yVals.add(new BarEntry(i, val));

            BarDataSet mBarDataSet = new BarDataSet(yVals, labels.get(i));
            mBarDataSet.setDrawIcons(false);
            mBarDataSet.setColors(color);
            mBarDataSet.setValueTextSize(10f);
            mBarDataSet.setValueTextColor(getColor(R.color.black));
            dataSets.add(mBarDataSet);
        }
        BarData mBarData = new BarData(dataSets);
        mBarData.setBarWidth(0.6f);
        mBarChart.setData(mBarData);
        handler.post(()->{
//            mBarChart.invalidate();
            mBarChart.animateXY(1000, 1000);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
        super.onActivityResult(requestCode, resultCode, intent);
        if (requestCode == selectImageCode) {
            if (resultCode == Activity.RESULT_OK && !ObjectUtils.isEmpty(intent)) {
                Uri selectedImageUri = intent.getData();
                try
                {
                    tmpBitmap = decodeUri(selectedImageUri);
                    tmpBitmap = ImageUtils.toRoundCorner(tmpBitmap, 5);
                    selectedBitmap = tmpBitmap.copy(Bitmap.Config.ARGB_8888, true);
                    mMainTouchImageView.setVisibility(View.GONE);
                    mMainDisplayImageView.setAlpha(0.f);
                    mMainDisplayImageView.animate().alpha(1.f).setDuration(500);
                    mMainDisplayImageView.setImageBitmap(tmpBitmap);
                    mMainDetectBtn.setClickable(true);
                    mMainDisplayImageView.setOnClickListener(new ClickUtils.OnMultiClickListener(2) {
                        @Override
                        public void onTriggerClick(View v) {
                            touchImageViewMultiClick(v);
                        }
                        @Override
                        public void onBeforeTriggerClick(View v, int count) {

                        }
                    });
                }
                catch (FileNotFoundException e) {
                    e.printStackTrace();
                }
            } else {
               Log.d(TAG, "select image request failed!");
            }
        }
    }

    private Bitmap decodeUri(Uri selectedImage) throws FileNotFoundException
    {
        // Decode image size
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(getContentResolver().openInputStream(selectedImage), null, o);

        // The new size we want to scale to
        final int REQUIRED_SIZE = 640;

        // Find the correct scale value. It should be the power of 2.
        int width_tmp = o.outWidth, height_tmp = o.outHeight;
        int scale = 1;
        while (true) {
            if (width_tmp / 2 < REQUIRED_SIZE
                    || height_tmp / 2 < REQUIRED_SIZE) {
                break;
            }
            width_tmp /= 2;
            height_tmp /= 2;
            scale *= 2;
        }

        // Decode with inSampleSize
        BitmapFactory.Options o2 = new BitmapFactory.Options();
        o2.inSampleSize = scale;
        Bitmap bitmap = BitmapFactory.decodeStream(getContentResolver().openInputStream(selectedImage), null, o2);

        // Rotate according to EXIF
        int rotate = 0;
        try {
            ExifInterface exif = new ExifInterface(getContentResolver().openInputStream(selectedImage));
            int orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_270:
                    rotate = 270;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    rotate = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_90:
                    rotate = 90;
                    break;
            }
        }
        catch (IOException e)
        {
            Log.e("MainActivity", "ExifInterface IOException");
        }

        Matrix matrix = new Matrix();
        matrix.postRotate(rotate);
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }

}