package com.github.ddgrcf.yolox_demo;

import androidx.appcompat.app.AppCompatActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.blankj.utilcode.util.BarUtils;
import com.blankj.utilcode.util.ImageUtils;
import com.blankj.utilcode.util.ObjectUtils;
import com.blankj.utilcode.util.ScreenUtils;
import com.github.ddgrcf.yolox_demo.databinding.ActivitySplashBinding;
import com.github.ybq.android.spinkit.SpinKitView;
import com.jaredrummler.android.widget.AnimatedSvgView;

import net.center.blurview.ShapeBlurView;

import org.w3c.dom.Text;

import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;

import pl.droidsonroids.gif.GifDrawable;
import pl.droidsonroids.gif.GifImageView;


public class SplashActivity extends AppCompatActivity {
    public static final String TAG = SplashActivity.class.getName();
    private ActivitySplashBinding binding;

    private RelativeLayout  mSplashRootLayout;
    private RelativeLayout mSplashMainLayout;

    private GifImageView mSplashBgGifView;
    private ShapeBlurView mSplashBgMaskView;


    private Button mSplashSkipBtn;
    private AnimatedSvgView mSplashLogSvgView;
    private TextView mSplashAppName;
    private TextView mSplashAppDescription;
    private SpinKitView mSplashLoadingSpinKitView;
    private TextView mSplashLoadingInformation;

    private Handler handler = new Handler();
    private int blurTime = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivitySplashBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // components
        mSplashRootLayout = findViewById(R.id.splash_root_layout);
        mSplashMainLayout = findViewById(R.id.splash_main_layout);

        mSplashBgGifView = findViewById(R.id.splash_bg_gif_view);
        mSplashBgMaskView = findViewById(R.id.splash_bg_mask_view);

        mSplashSkipBtn = findViewById(R.id.splash_skip_btn);
        mSplashLogSvgView = findViewById(R.id.splash_animated_svg_view);

        mSplashAppName = findViewById(R.id.splash_app_name);
        mSplashAppDescription = findViewById(R.id.splash_app_description);

        mSplashLoadingSpinKitView = findViewById(R.id.splash_loading_spin_kit_view);
        mSplashLoadingInformation = findViewById(R.id.splash_loading_information);

        // set main content add transparent status bar
        BarUtils.transparentStatusBar(this); // transparent
        BarUtils.setStatusBarLightMode(this, true); // light mode
        BarUtils.addMarginTopEqualStatusBarHeight(mSplashMainLayout); // add main content height

        // activity
        initSplashAnimation();
        initSplashBgGifView();
    }

    private void initSplashBgGifView() {
        GifDrawable gifFromAssets = null;
        try {
            gifFromAssets = new GifDrawable(getAssets(), "images/demo_video.gif");
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (!ObjectUtils.isEmpty(gifFromAssets)) {
            mSplashBgGifView.setImageDrawable(gifFromAssets);
        }
    }

    private void initSplashAnimation() {
        // prepare
        int appearTime = 1500;
        mSplashSkipBtn.setClickable(false); // cancel click for error touch
        mSplashAppName.setAlpha(0.f);
        mSplashAppDescription.setAlpha(0.f);
        mSplashLoadingSpinKitView.setAlpha(0.f);
        mSplashLoadingInformation.setAlpha(0.f);
        // delay 500ms to start svg log player
        handler.postDelayed(()->{
            mSplashLogSvgView.start();
        }, 500);
        handler.postDelayed(()->{
            mSplashAppName
                    .animate()
                    .alpha(1.f)
                    .setDuration(appearTime)
                    .setListener(null);
            mSplashAppName.setVisibility(View.VISIBLE);
        }, 1000);
        handler.postDelayed(()->{
            mSplashAppDescription
                    .animate()
                    .alpha(1.f)
                    .setDuration(appearTime / 2)
                    .setListener(null);
            mSplashAppDescription.setVisibility(View.VISIBLE);
        }, 1500);
        handler.postDelayed(()->{
            mSplashLoadingSpinKitView
                    .animate()
                    .alpha(1.f)
                    .setDuration(appearTime / 5)
                    .setListener(null);
            mSplashLoadingInformation
                    .animate()
                    .alpha(1.f)
                    .setDuration(appearTime / 5)
                    .setListener(null);
            mSplashLoadingSpinKitView.setVisibility(View.VISIBLE);
            mSplashLoadingInformation.setVisibility(View.VISIBLE);
        }, 2300);
        handler.postDelayed(()->{
            new Thread(()->{
                // check asserts dir
                handler.post(()->{
                    mSplashLoadingInformation.setText("recursive the assets dir ...");
                });
                try {
                    for (String s : getAssets().list("")) {
                        handler.post(()->{
                            mSplashLoadingInformation.setText("find file: " + s);
                        });
                        // delay for seeing
                        Thread.sleep(100);
                    }
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }

                // load the model
                handler.post(()->{
                    mSplashLoadingInformation.setText("loading the model ...");
                });
                boolean ret = YoloxObbNcnn.getInstance().Init(getAssets());
                handler.post(()->{
                    if (ret) {
                        mSplashLoadingInformation.setText("load the model done .");
                        // delay for seeing
                        handler.postDelayed(()->{
                            Intent intent = new Intent(this, MainActivity.class);
                            startActivity(intent);
                        }, 200);
                    } else {
                        mSplashLoadingInformation.setText("load the model fail! please concat with author");
                        onDestroy();
                    }
                });
            }).start();
        }, 2300);

        Timer timer = new Timer(true);
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                blurTime = 500 + blurTime;
                float blurRadius = Math.min(blurTime / 2500 * 25, 25);
                handler.post(()->{
                    if (blurTime > 2500) timer.cancel();
                    mSplashBgMaskView.refreshView(
                        ShapeBlurView.build(SplashActivity.this).setBlurRadius(blurRadius)
                    );
                });
            }
        }, 500, 500);
    }

    @Override
    protected void onStop() {
        super.onStop();
        finish(); // destroy application
    }
}