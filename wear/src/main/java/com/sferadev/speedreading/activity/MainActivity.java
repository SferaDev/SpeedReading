package com.sferadev.speedreading.activity;

import android.app.Activity;
import android.os.Bundle;
import android.support.wearable.view.WatchViewStub;
import android.util.Log;
import android.view.InputDevice;
import android.view.InputDevice.MotionRange;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.view.WindowManager;
import android.widget.TextView;

import com.sferadev.speedreading.R;

import static com.sferadev.speedreading.utils.PreferenceUtils.*;

public class MainActivity extends Activity implements OnTouchListener{
    private TextView mTextView;

    private boolean workState = true;
    private int pos = -1;

    private long lastTouch = 0;

    private static String KEY_SPEED = "speed";

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
        stub.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (lastTouch == 0 || (System.currentTimeMillis() - lastTouch) > 350) {
                    if (workState) {
                        workState = false;
                    } else {
                        workState = true;
                        updateText();
                    }
                }
            }
        });
        workState = true;
        updateText();
    }

    @Override
    protected void onPause() {
        super.onPause();
        workState = false;
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
                                        mTextView.setText(getString(R.string.lwl).split(" ")[pos]);
                                    } catch (ArrayIndexOutOfBoundsException e) {
                                        pos = -1; //Yea I hate myself for this
                                    } catch (NullPointerException e) {
                                        e.printStackTrace();
                                    }
                                    pos++;
                                    Log.d("SpeedReading", "Position: " + String.valueOf(pos));
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
        if (event.getAction() == MotionEvent.ACTION_MOVE) {
            Log.d("SpeedReading", "Moved to: " + event.getY());
            int value = Math.round(1250 * event.getY() / (range.getMax() - range.getMin() - 20));
            setPreference(KEY_SPEED, value);
            Log.d("SpeedReading", "Value set: " + value);
            lastTouch = System.currentTimeMillis();
            return true;
        }
        return false;
    }
}
