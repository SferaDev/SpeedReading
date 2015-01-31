package com.sferadev.speedreading.utils;

import android.os.Handler;

import com.dd.processbutton.ProcessButton;

import java.util.Random;

public class ProgressGenerator {

    private int mProgress;
    private Random random = new Random();

    public void start(final ProcessButton button) {
        final Handler handler = new Handler();
        handler.postDelayed(new Runnable() {
            @Override
            public void run() {
                mProgress += 10;
                button.setProgress(mProgress);
                if (mProgress < 100) {
                    handler.postDelayed(this, generateDelay());
                }
            }
        }, generateDelay());
    }

    private int generateDelay() {
        return random.nextInt(100);
    }
}