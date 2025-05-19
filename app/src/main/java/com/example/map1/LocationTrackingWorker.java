package com.example.map1;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.concurrent.futures.CallbackToFutureAdapter;
import androidx.core.app.NotificationCompat;
import androidx.work.ForegroundInfo;
import androidx.work.ListenableWorker;
import androidx.work.WorkerParameters;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.ExecutionException;

public class LocationTrackingWorker extends ListenableWorker {

    private static final String TAG = "LocationTrackingWorker";
    public static final int NOTIFICATION_ID = 1;

    public LocationTrackingWorker(
            @NonNull Context context,
            @NonNull WorkerParameters params) {
        super(context, params);
    }

    @NonNull
    @Override
    public ListenableFuture<Result> startWork() {
        Log.d(TAG, "WorkManager task started. Attempting to start LocationService.");

        ListenableFuture<ForegroundInfo> foregroundInfo = getForegroundInfoAsync();
        try {
            setForegroundAsync(foregroundInfo.get());
        } catch (ExecutionException e) {
            throw new RuntimeException(e);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        try {
            Intent serviceIntent = new Intent(getApplicationContext(), LocationService.class);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                getApplicationContext().startForegroundService(serviceIntent);
            } else {
                getApplicationContext().startService(serviceIntent);
            }
            Log.d(TAG, "LocationService started via WorkManager.");
            return CallbackToFutureAdapter.getFuture(completer -> {
                completer.set(Result.success());
                return "startWork success";
            });

        } catch (Exception e) {
            Log.e(TAG, "Error starting LocationService from WorkManager", e);
            return CallbackToFutureAdapter.getFuture(completer -> {
                completer.set(Result.failure());
                return "startWork failure";
            });
        }
    }

    @NonNull
    @Override
    public ListenableFuture<ForegroundInfo> getForegroundInfoAsync() {
        return CallbackToFutureAdapter.getFuture(completer -> {
            Notification notification = createNotification(getApplicationContext());
            int foregroundServiceType = 0;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                foregroundServiceType = ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION;
            }

            completer.set(new ForegroundInfo(NOTIFICATION_ID, notification, foregroundServiceType));
            return "ForegroundInfo creation";
        });
    }

    private Notification createNotification(Context context) {
        createNotificationChannel(context);

        Intent notificationIntent = new Intent(context, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        return new NotificationCompat.Builder(context, LocationService.CHANNEL_ID)
                .setContentTitle("Отслеживание местоположения")
                .setContentText("Сервис отслеживает ваше местоположение")
                .setSmallIcon(R.mipmap.app_icon)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();
    }

    @SuppressLint("ObsoleteSdkInt")
    private void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    LocationService.CHANNEL_ID,
                    "Location Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );

            NotificationManager manager = context.getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }
}
