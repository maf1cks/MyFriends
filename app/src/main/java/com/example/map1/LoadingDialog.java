package com.example.map1;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;

import androidx.appcompat.app.AlertDialog;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.Objects;

public class LoadingDialog {
    private AlertDialog dialog;
    private final Activity activity;
    private final String text;

    public LoadingDialog(Activity activity, String text) {
        this.activity = activity;
        this.text = text;
    }

    @SuppressLint("InflateParams")
    public void startDialog() {
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(activity);

        View dialogView = activity.getLayoutInflater().inflate(R.layout.dialog_loading, null);
        builder.setView(dialogView);
        builder.setCancelable(false);

        TextView textView = dialogView.findViewById(R.id.dialogLoadingText);
        textView.setText(text);

        dialog = builder.create();
        dialog.show();

        int pixelsWidth = activity.getResources().getDimensionPixelSize(R.dimen.dialog_loading_width);
        Objects.requireNonNull(dialog.getWindow()).setLayout(pixelsWidth, WindowManager.LayoutParams.WRAP_CONTENT);
    }

    public void dismissDialog() {
        dialog.dismiss();
    }
}
