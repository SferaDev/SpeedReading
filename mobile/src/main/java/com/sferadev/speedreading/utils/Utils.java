package com.sferadev.speedreading.utils;

import android.app.AlertDialog;
import android.content.DialogInterface.OnClickListener;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;

import com.sferadev.speedreading.R;

import static com.sferadev.speedreading.App.getContext;

public class Utils {
    public static View myDialogView = null;

    // Input Dialog Creation
    public static void createInputDialog(String title, OnClickListener positiveListener,
                                         OnClickListener negativeListener) {
        LayoutInflater factory = LayoutInflater.from(getContext());
        myDialogView = factory.inflate(R.layout.input_dialog, null);
        AlertDialog dialog = new AlertDialog.Builder(getContext(),
                android.R.style.Theme_DeviceDefault_Light_Dialog_MinWidth)
                .setTitle(title)
                .setView(myDialogView)
                .setPositiveButton(getContext().getString(android.R.string.yes), positiveListener)
                .setNegativeButton(getContext().getString(android.R.string.no), negativeListener)
                .create();
        dialog.getWindow().setType(WindowManager.LayoutParams.TYPE_SYSTEM_ALERT);
        dialog.show();
    }
}
