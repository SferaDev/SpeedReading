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

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.Wearable;
import com.sferadev.speedreading.R;

import static com.sferadev.speedreading.utils.PreferenceUtils.getPreference;
import static com.sferadev.speedreading.utils.PreferenceUtils.setPreference;
import static com.sferadev.speedreading.utils.Utils.KEY_SPEED;
import static com.sferadev.speedreading.utils.Utils.KEY_TEXT_STRING;

public class MainActivity extends Activity implements DataApi.DataListener,
        ConnectionCallbacks, OnConnectionFailedListener, OnTouchListener {
    private static final String TAG = ".wear.MainActivity";

    private GoogleApiClient mGoogleApiClient;

    private TextView mTextView;

    private boolean workState = false;
    private int pos = -1;

    private float startPoint = 0;
    private int lastSpeed = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addApi(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                mTextView = (TextView) stub.findViewById(R.id.text);
            }
        });
        stub.setOnTouchListener(this);
    }

    @Override
    protected void onStart() {
        super.onStart();
        workState = true;
        mGoogleApiClient.connect();
    }

    @Override
    protected void onPause() {
        super.onPause();
        workState = false;
    }

    @Override
    protected void onStop() {
        workState = false;
        if (null != mGoogleApiClient && mGoogleApiClient.isConnected()) {
            Wearable.DataApi.removeListener(mGoogleApiClient, this);
            mGoogleApiClient.disconnect();
        }
        super.onStop();
    }

    @Override
    public boolean onTouch(View v, MotionEvent event) {
        InputDevice device = event.getDevice();
        MotionRange range = device.getMotionRange(MotionEvent.AXIS_Y);

        // Check when Input starts and save the start point
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            startPoint = event.getY();
            Log.d(TAG, "Started on: " + startPoint);
        }

        // Check when Input point changes and calculate the reading speed
        if (event.getAction() == MotionEvent.ACTION_MOVE) {
            Log.d(TAG, "Moved to: " + event.getY());
            if (workState) {
                lastSpeed = Math.round(1250 * event.getY() / (range.getMax() - range.getMin() - 20));
            }
        }

        // Check when Input ends and determine if was a speed change or click
        if (event.getAction() == MotionEvent.ACTION_UP) {
            float endPoint = event.getY();
            Log.d(TAG, "Ended on: " + endPoint);
            Log.d(TAG, "Diff value: " + Math.abs(endPoint - startPoint));
            if (Math.abs(endPoint - startPoint) > 15) {
                updateSpeed(lastSpeed);
            } else { // Was a click
                switchState();
            }
        }
        return true;
    }

    @Override
    public void onConnected(Bundle connectionHint) {
        Log.d(TAG, "Connected to Google Api Service");
        Wearable.DataApi.addListener(mGoogleApiClient, this);
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "Connection to Google Api Service Suspended: " + i);
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        for (DataEvent event : dataEvents) {
            if (event.getType() == DataEvent.TYPE_DELETED) {
                Log.d(TAG, "DataItem deleted: " + event.getDataItem().getUri());
            } else if (event.getType() == DataEvent.TYPE_CHANGED) {
                Log.d(TAG, "DataItem changed: " + event.getDataItem().getUri());
                String path = event.getDataItem().getUri().getPath();
                if (path.equals("/dataPath")) {
                    DataMap dataMap = DataMapItem.fromDataItem(event.getDataItem()).getDataMap();
                    setPreference(KEY_TEXT_STRING, dataMap.getString(KEY_TEXT_STRING));
                    Log.d(TAG, "DataMap received on watch: " + dataMap);
                }
            }
        }
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Log.d(TAG, "Connection to Google Api Service Failed: " + connectionResult);
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
        Log.d(TAG, "Speed set: " + value);
    }

    private void updateText() {
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
                                    Log.d(TAG, "Word: " + String.valueOf(pos));
                                }
                            });
                            sleep(getPreference(KEY_SPEED, 800));
                        }
                        Log.d(TAG, "State: False | Out of the loop.");
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            };
            t.start();
        } catch (IllegalStateException e) {
            e.printStackTrace();
        }
    }
}
