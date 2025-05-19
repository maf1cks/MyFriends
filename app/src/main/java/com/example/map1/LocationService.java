package com.example.map1;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.location.Location;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;
import java.util.concurrent.TimeUnit;

public class LocationService extends Service {

    private static final String TAG = "LocationService";
    public static final String CHANNEL_ID = "location_service_channel";
    private static final int NOTIFICATION_ID = LocationTrackingWorker.NOTIFICATION_ID;

    private FusedLocationProviderClient fusedLocationClient;
    private LocationCallback locationCallback;
    private LocationRequest locationRequest;

    private DatabaseReference myRef;
    private SharedPreferences myPref;

    private double latitude;
    private double longitude;

    private long lastFirebaseUpdateTime = 0;
    private static final long MIN_FIREBASE_UPDATE_INTERVAL_MS = TimeUnit.SECONDS.toMillis(10);


    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "LocationService onCreate");
        myRef = FirebaseDatabase.getInstance().getReference();
        myPref = getSharedPreferences("data", 0);
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        long desiredIntervalMillis = TimeUnit.SECONDS.toMillis(10);
        long fastestIntervalMillis = TimeUnit.SECONDS.toMillis(10);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            locationRequest = new LocationRequest.Builder(Priority.PRIORITY_HIGH_ACCURACY, desiredIntervalMillis)
                    .setWaitForAccurateLocation(false)
                    .setMinUpdateIntervalMillis(fastestIntervalMillis)
                    .build();
        } else {
            locationRequest = LocationRequest.create()
                    .setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY)
                    .setInterval(desiredIntervalMillis)
                    .setFastestInterval(fastestIntervalMillis);
        }


        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(@NonNull LocationResult locationResult) {

                for (Location location : locationResult.getLocations()) {
                    Log.d(TAG, "Location received: " + location.getLatitude() + ", " + location.getLongitude());

                    long currentTime = System.currentTimeMillis();
                    if (currentTime - lastFirebaseUpdateTime >= MIN_FIREBASE_UPDATE_INTERVAL_MS) {
                        latitude = location.getLatitude();
                        longitude = location.getLongitude();

                        String userId = myPref.getString("userID", "");

                        if (!userId.isEmpty() && NetworkUtils.isInternetAvailable(LocationService.this)) {
                            myRef.child("Users").child(userId).child("geo").setValue(List.of(latitude, longitude))
                                    .addOnSuccessListener(aVoid -> Log.d(TAG, "Location updated in Firebase for user: " + userId))
                                    .addOnFailureListener(e -> Log.e(TAG, "Error updating location in Firebase for user: " + userId, e));
                            lastFirebaseUpdateTime = currentTime;
                        } else {
                            Log.w(TAG, "User ID not found in SharedPreferences. Cannot write location to Firebase.");
                        }
                    } else {
                        Log.d(TAG, "Skipping Firebase update due to throttling. Next update in "
                                + (MIN_FIREBASE_UPDATE_INTERVAL_MS - (currentTime - lastFirebaseUpdateTime)) + " ms");
                    }
                }
            }
        };

        createNotificationChannel();
    }

    @SuppressLint({"MissingPermission", "ObsoleteSdkInt"})
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.d(TAG, "LocationService onStartCommand");
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            Log.e(TAG, "Location permissions not granted when attempting to request updates. Stopping service.");
            stopSelf();
            return START_NOT_STICKY;
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                Log.w(TAG, "Background location permission not granted on API " + Build.VERSION.SDK_INT + ". Location updates may be less frequent or stop in background.");
            }
        }


        fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null)
                .addOnSuccessListener(aVoid -> Log.d(TAG, "Location updates requested successfully"))
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error requesting location updates", e);
                    stopSelf();
                });
        Notification notification = createNotification();
        startForeground(NOTIFICATION_ID, notification);
        return START_NOT_STICKY;
    }
    private Notification createNotification() {
        Intent notificationIntent = new Intent(this, MainActivity.class);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, notificationIntent, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);

        return new NotificationCompat.Builder(this, CHANNEL_ID)
                .setContentTitle("Отслеживание местоположения")
                .setContentText("Сервис отслеживает ваше местоположение")
                .setSmallIcon(R.mipmap.app_icon)
                .setContentIntent(pendingIntent)
                .setOngoing(true)
                .build();
    }


    @SuppressLint("ObsoleteSdkInt")
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel serviceChannel = new NotificationChannel(
                    CHANNEL_ID,
                    "Location Service Channel",
                    NotificationManager.IMPORTANCE_DEFAULT
            );

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(serviceChannel);
            }
        }
    }


    @Override
    public void onDestroy() {
        Log.d(TAG, "LocationService onDestroy");
        super.onDestroy();
        if (fusedLocationClient != null && locationCallback != null) {
            fusedLocationClient.removeLocationUpdates(locationCallback);
            Log.d(TAG, "Location updates removed");
        }
    }
}
