package com.sferadev.speedreading.activity;

import android.app.Activity;
import android.os.Bundle;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.InputDevice;
import android.view.InputDevice.MotionRange;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.widget.TextView;

import com.sferadev.speedreading.R;

import static com.sferadev.speedreading.utils.PreferenceUtils.getPreference;
import static com.sferadev.speedreading.utils.PreferenceUtils.setPreference;
import static com.sferadev.speedreading.utils.Utils.KEY_SPEED;
import static com.sferadev.speedreading.utils.Utils.KEY_TEXT_STRING;

public class MainActivity extends Activity implements OnTouchListener{
    private TextView mTextView;

    private boolean workState = true;
    private int pos = -1;

    private float startPoint = 0;
    private float endPoint = 0;
    private int lastSpeed = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                mTextView = (TextView) stub.findViewById(R.id.text);
            }
        });
        stub.setOnTouchListener(this);
        updateText();
    }

    @Override
    protected void onStart() {
        super.onStart();
        workState = true;
    }

    @Override
    protected void onPause() {
        super.onPause();
        workState = false;
    }

    @Override
    protected void onStop() {
        super.onStop();
        workState = false;
    }

    private void switchState() {
        if (workState) {
            workState = false;
        } else {
            workState = true;
            updateText();
        }
    }

    private void updateSpeed(int value) {
        setPreference(KEY_SPEED, value);
        Log.d("SpeedReading", "Speed set: " + value);
    }

    private void updateText(){
        try {
            Thread t = new Thread() {
                @Override
                public void run() {
                    try {
                        while (workState) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        mTextView.setText(getPreference(KEY_TEXT_STRING, getString(R.string.lwl)).split(" ")[pos]);
                                    } catch (ArrayIndexOutOfBoundsException e) {
                                        pos = -1; //Yea I hate myself for this
                                    } catch (NullPointerException e) {
                                        e.printStackTrace();
                                    }
                                    pos++;
                                    Log.d("SpeedReading", "Word: " + String.valueOf(pos));
                                }
                            });
                            sleep(getPreference(KEY_SPEED, 800));
                        }
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            };
            workState = true;
            t.start();
        } catch (IllegalStateException e){
            e.printStackTrace();
        }
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        InputDevice device = event.getDevice();
        MotionRange range = device.getMotionRange(MotionEvent.AXIS_Y);

        // Check when Input starts and save the start point
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            startPoint = event.getY();
            Log.d("SpeedReading", "Started on: " + startPoint);
        }

        // Check when Input point changes and calculate the reading speed
        if (event.getAction() == MotionEvent.ACTION_MOVE) {
            Log.d("SpeedReading", "Moved to: " + event.getY());
            if (workState) {
                lastSpeed = Math.round(1250 * event.getY() / (range.getMax() - range.getMin() - 20));
            }
        }

        // Check when Input ends and determine if was a speed change or click
        if (event.getAction() == MotionEvent.ACTION_UP) {
            endPoint = event.getY();
            Log.d("SpeedReading", "Ended on: " + endPoint);
            Log.d("SpeedReading", "Diff value: " + Math.abs(endPoint - startPoint));
            if (Math.abs(endPoint - startPoint) > 15) {
                updateSpeed(lastSpeed);
            } else {
                switchState();
            }
        }
        return true;
    }
}
