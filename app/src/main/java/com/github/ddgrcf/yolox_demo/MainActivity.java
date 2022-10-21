package com.github.ddgrcf.yolox_demo;

import androidx.appcompat.app.AppCompatActivity;

import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.RelativeLayout;
import android.widget.VideoView;

import com.blankj.utilcode.util.BarUtils;
import com.blankj.utilcode.util.ImageUtils;
import com.blankj.utilcode.util.ObjectUtils;
import com.blankj.utilcode.util.ScreenUtils;

import java.io.IOException;

import pl.droidsonroids.gif.GifDrawable;
import pl.droidsonroids.gif.GifImageView;

public class MainActivity extends AppCompatActivity {

    private RelativeLayout mMainRootLayout;
    private RelativeLayout mMainMainLayout;

    private GifImageView mMainBgGifView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
//        ScreenUtils.setLandscape(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mMainRootLayout = findViewById(R.id.main_root_layout);
        mMainMainLayout = findViewById(R.id.main_main_layout);

        mMainBgGifView = findViewById(R.id.main_bg_gif_view);

        // set main content add transparent status bar
        BarUtils.transparentStatusBar(this); // transparent
        BarUtils.setStatusBarLightMode(this, true); // light mode

        BarUtils.addMarginTopEqualStatusBarHeight(mMainMainLayout); // add main content height

        // activity
        initMainBgGifView();
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


}