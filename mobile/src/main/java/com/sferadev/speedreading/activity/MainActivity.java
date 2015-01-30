package com.sferadev.speedreading.activity;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;

import com.dd.processbutton.iml.SubmitProcessButton;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.rengwuxian.materialedittext.MaterialEditText;
import com.sferadev.speedreading.R;
import com.sferadev.speedreading.utils.ProgressGenerator;

public class MainActivity extends ActionBarActivity {

    private static String TAG = ".mobile.MainActivity";

    private Toolbar toolbar;

    private MaterialEditText editText;
    private SubmitProcessButton submitButton;
    private ProgressGenerator progressGenerator;

    GoogleApiClient mGoogleApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        toolbar = (Toolbar) findViewById(R.id.toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        startConnection();

        editText = (MaterialEditText) findViewById(R.id.editText);
        submitButton = (SubmitProcessButton) findViewById(R.id.submitButton);

        submitButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                sendText(editText.getText().toString());
            }
        });

        Intent intent = getIntent();
        Log.d(TAG, "Intent Action: " + intent.getAction());
        Log.d(TAG, "Intent Type: " + intent.getType());

        if (Intent.ACTION_SEND.equals(intent.getAction()) && intent.getType() != null) {
            if ("text/plain".equals(intent.getType())) {
                editText.setText(intent.getStringExtra(Intent.EXTRA_TEXT));
                sendText(intent.getStringExtra(Intent.EXTRA_TEXT));
            }
        }

    }

    private void sendText(String text) {
        submitButton.setProgress(1);
        DataMap dataMap = new DataMap();
        dataMap.putString("textString", text.replace("[ ]", "").replace("\n", " "));
        new SendToDataLayerThread("/dataPath", dataMap).start();
        progressGenerator = new ProgressGenerator();
        progressGenerator.start(submitButton);
    }

    private void startConnection() {
        mGoogleApiClient = new GoogleApiClient.Builder(this)
                .addConnectionCallbacks(new ConnectionCallbacks() {
                    @Override
                    public void onConnected(Bundle connectionHint) {
                        Log.d(TAG, "onConnected: " + connectionHint);

                    }

                    @Override
                    public void onConnectionSuspended(int cause) {
                        Log.d(TAG, "onConnectionSuspended: " + cause);
                    }
                })
                .addOnConnectionFailedListener(new OnConnectionFailedListener() {
                    @Override
                    public void onConnectionFailed(ConnectionResult result) {
                        Log.d(TAG, "onConnectionFailed: " + result);
                    }
                })
                .addApi(Wearable.API)
                .build();
    }

    @Override
    protected void onStart() {
        super.onStart();
        mGoogleApiClient.connect();
    }

    @Override
    protected void onStop() {
        mGoogleApiClient.disconnect();
        super.onStop();
    }

    class SendToDataLayerThread extends Thread {
        String path;
        DataMap dataMap;

        SendToDataLayerThread(String p, DataMap data) {
            path = p;
            dataMap = data;
        }

        public void run() {
            NodeApi.GetConnectedNodesResult nodes = Wearable.NodeApi.getConnectedNodes(mGoogleApiClient).await();
            for (Node node : nodes.getNodes()) {

                PutDataMapRequest putDMR = PutDataMapRequest.create(path);
                putDMR.getDataMap().putAll(dataMap);
                PutDataRequest request = putDMR.asPutDataRequest();
                DataApi.DataItemResult result = Wearable.DataApi.putDataItem(mGoogleApiClient, request).await();
                if (result.getStatus().isSuccess()) {
                    Log.v(TAG, "DataMap: " + dataMap + " sent to: " + node.getDisplayName());
                } else {
                    // Log an error
                    Log.v(TAG, "ERROR: failed to send DataMap");
                }
            }
        }
    }
}
