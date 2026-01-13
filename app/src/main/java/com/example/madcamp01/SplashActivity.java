package com.example.madcamp01;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatActivity;

public class SplashActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        ImageView splashLogo = findViewById(R.id.splash_logo);

        // 페이드 인 + 스케일 애니메이션
        ObjectAnimator fadeIn = ObjectAnimator.ofFloat(splashLogo, "alpha", 0f, 1f);
        fadeIn.setDuration(400);
        fadeIn.setInterpolator(new AccelerateDecelerateInterpolator());

        ObjectAnimator scaleX = ObjectAnimator.ofFloat(splashLogo, "scaleX", 0.8f, 1f);
        ObjectAnimator scaleY = ObjectAnimator.ofFloat(splashLogo, "scaleY", 0.8f, 1f);
        scaleX.setDuration(400);
        scaleY.setDuration(400);
        scaleX.setInterpolator(new DecelerateInterpolator());
        scaleY.setInterpolator(new DecelerateInterpolator());

        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.playTogether(fadeIn, scaleX, scaleY);
        animatorSet.start();

        // 1초 후 LoginActivity로 이동
        new Handler(Looper.getMainLooper()).postDelayed(() -> {
            // 페이드 아웃 애니메이션
            ObjectAnimator fadeOut = ObjectAnimator.ofFloat(splashLogo, "alpha", 1f, 0f);
            fadeOut.setDuration(200);
            fadeOut.setInterpolator(new AccelerateDecelerateInterpolator());
            fadeOut.start();

            fadeOut.addUpdateListener(animation -> {
                if (animation.getAnimatedFraction() >= 1.0f) {
                    Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
                    startActivity(intent);
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                    finish();
                }
            });
        }, 1000);
    }
}
