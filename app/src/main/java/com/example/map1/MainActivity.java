package com.example.map1;

import static android.content.ContentValues.TAG;

import android.Manifest;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.net.ConnectivityManager;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;
import android.view.animation.LinearInterpolator;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.yandex.mapkit.Animation;
import com.yandex.mapkit.MapKitFactory;
import com.yandex.mapkit.geometry.Point;
import com.yandex.mapkit.map.CameraPosition;
import com.yandex.mapkit.map.Map;
import com.yandex.mapkit.map.MapObject;
import com.yandex.mapkit.map.MapObjectTapListener;
import com.yandex.mapkit.map.PlacemarkMapObject;
import com.yandex.mapkit.map.TextStyle;
import com.yandex.mapkit.mapview.MapView;
import com.yandex.runtime.image.ImageProvider;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity{
    private MapView mapView;
    private ActivityResultLauncher<String> requestPermissionLauncher;
    private Handler repeatHandler;
    private Runnable repeatRunnable;
    private static final long REPEAT_INTERVAL_MILLIS = 10000;
    private double latitude;
    private double longitude;
    private FusedLocationProviderClient fusedLocationClient;
    private SharedPreferences myPref;
    private DatabaseReference myRef;

    private String isAft;
    private boolean m = true;
    private boolean isStart = false;
    private String userID;
    private Map map;
    private PlacemarkMapObject userIC;
    private java.util.Map<String,PlacemarkMapObject> friendsICArrayList;
    private java.util.Map<String, MapObjectTapListener> friendsTapArrayList;
    private ImageLoader imageLoader;
    private Bitmap defaultBitmap;
    private MapObjectTapListener userTap;

    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1;

    private void init(){
        imageLoader = new ImageLoader();
        friendsICArrayList = new HashMap<String,PlacemarkMapObject>();
        friendsTapArrayList = new HashMap<String,MapObjectTapListener>();
        myRef = FirebaseDatabase.getInstance().getReference();
        myPref = getSharedPreferences("data",0);
        isAft = myPref.getString("isAft","");
        userID = myPref.getString("userID","");
        defaultBitmap = ImageUtils.getCircularBitmapFromDrawable(ContextCompat.getDrawable(MainActivity.this,R.drawable.white),MainActivity.this,60,4,null);
        if (repeatHandler == null) {
            repeatHandler = new Handler(Looper.getMainLooper());
        }

    }
    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        init();

        if (isAft.equals("0") || isAft.isEmpty()) {
            Intent intent = new Intent(MainActivity.this, Registration.class);
            startActivity(intent);
            finish();
        } else {
            repeatRunnable = new Runnable() {
                @Override
                public void run() {

                    try {
                        if (m){
                            MapKitFactory.getInstance().onStart();
                            mapView.onStart();
                            m=false;
                        }
                        myRef.child("Users").child(userID).child("userFr").get().addOnCompleteListener(task -> {
                            if (task.isSuccessful()){
                                java.util.Map<String, String> base = (java.util.Map<String, String>) task.getResult().getValue();
                                if (base!=null) {

                                    int baseSize=base.size();
                                    String[] userFrArray;
                                    userFrArray = base.values().toArray(new String[0]);
                                    for (int i = 0; i < baseSize; i++) {
                                        int finalI = i;
                                        myRef.child("Users").child(userFrArray[i]).get().addOnCompleteListener(task1 -> {
                                            if (task1.isSuccessful()){
                                                java.util.Map<String, Object> basei = (java.util.Map<String, Object>) task1.getResult().getValue();
                                                if (Objects.requireNonNull(basei).containsKey("geo")) {
                                                    ArrayList<Double> geo = (ArrayList<Double>) basei.get("geo");
                                                    if (friendsICArrayList.get(userFrArray[finalI]) == null) {
                                                        PlacemarkMapObject friendIC = map.getMapObjects().addPlacemark(new Point(geo.get(0), geo.get(1)), ImageProvider.fromBitmap(defaultBitmap));
                                                        String frUsername = basei.get("username").toString();
                                                        String finalFrUsername = frUsername;
                                                        if (basei.containsKey("photo")){
                                                            imageLoader.loadImageAsync(MainActivity.this,friendIC,basei.get("photo").toString());
                                                        } else {
                                                            if (frUsername.length() > 11) {
                                                                frUsername = frUsername.substring(0, 9) + "..";
                                                            }
                                                            friendIC.setIcon(ImageProvider.fromBitmap(ImageUtils.getCircularBitmapFromDrawable(ContextCompat.getDrawable(MainActivity.this,R.drawable.white),MainActivity.this,60,4,frUsername)));
                                                        }
                                                        friendsICArrayList.put(userFrArray[finalI], friendIC);

                                                        friendsTapArrayList.put(userFrArray[finalI], new MapObjectTapListener() {
                                                            @Override
                                                            public boolean onMapObjectTap(@NonNull MapObject mapObject, @NonNull Point point) {
                                                                if (NetworkUtils.isInternetAvailable(MainActivity.this)) {

                                                                    myRef.child("Users").child(userFrArray[finalI]).child("geo").get().addOnCompleteListener(task2 -> {
                                                                        if (task2.isSuccessful()) {
                                                                            ArrayList<Double> base1 = (ArrayList<Double>) task2.getResult().getValue();
                                                                            double latitudeFr = base1.get(0);
                                                                            double longitudeFr = base1.get(1);
                                                                            locate(latitudeFr, longitudeFr, 1);
                                                                            Toast.makeText(MainActivity.this, finalFrUsername, Toast.LENGTH_SHORT).show();
                                                                        }
                                                                    });
                                                                    return true;
                                                                } else return true;
                                                            }
                                                        });
                                                        friendIC.addTapListener(friendsTapArrayList.get(userFrArray[finalI]));
                                                        friendIC.setZIndex(99-finalI);
                                                        TextStyle textStyle = new TextStyle();
                                                        textStyle.setPlacement(TextStyle.Placement.BOTTOM);
                                                        friendIC.setTextStyle(textStyle);


                                                    } else {
                                                        PlacemarkMapObject friendIC = friendsICArrayList.get(userFrArray[finalI]);
                                                        animatePlacemarkMapObjectToPoint(friendIC, new Point(geo.get(0), geo.get(1)));
                                                        if (myPref.getString("time","").equals("1")){
                                                                friendIC.setText(SimpleDistanceCalculator.calculateDistanceAlongCathetuses(latitude,longitude,geo.get(0),geo.get(1)));
                                                        } else friendIC.setText("");
                                                    }
                                                    getLocation();
                                                    animatePlacemarkMapObjectToPoint(userIC, new Point(latitude, longitude));
                                                } else if (friendsICArrayList.containsKey(userFrArray[finalI])){
                                                    friendsICArrayList.get(userFrArray[finalI]).setVisible(false);
                                                    friendsICArrayList.get(userFrArray[finalI]).removeTapListener(friendsTapArrayList.get(userFrArray[finalI]));
                                                    friendsICArrayList.remove(userFrArray[finalI]);
                                                    friendsTapArrayList.remove(userFrArray[finalI]);

                                                }
                                            }
                                        });
                                    }
                                }
                            }
                        });

                    } finally {
                        repeatHandler.postDelayed(this, REPEAT_INTERVAL_MILLIS);
                    }
                }
            };
            myRef.child("Users").child(userID).get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    java.util.Map<String,Object> base = (java.util.Map<String,Object>) (task.getResult().getValue());
                    SharedPreferences.Editor ed = myPref.edit();
                    ed.putString("username",base.get("username").toString());
                    ed.apply();
                }
            });
            requestPermissionLauncher =
                    registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {

                        if (isGranted) {
                            Toast.makeText(this, "Разрешение на уведомления предоставленно!", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "Отказано на разрешение уведомлений", Toast.LENGTH_SHORT).show();
                            showNotificationPermissionDeniedMessage();
                        }
                    });

        }
    }
    @Override
    public void onRequestPermissionsResult(int requestCode,
                                           String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            boolean foregroundPermissionGranted = false;
            boolean backgroundPermissionGranted = false;

            for (int i = 0; i < permissions.length; i++) {
                String permission = permissions[i];
                int grantResult = grantResults[i];

                if (permission.equals(Manifest.permission.ACCESS_FINE_LOCATION) ||
                        permission.equals(Manifest.permission.ACCESS_COARSE_LOCATION)) {
                    if (grantResult == PackageManager.PERMISSION_GRANTED) {
                        foregroundPermissionGranted = true;
                    }
                }

                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    if (permission.equals(Manifest.permission.ACCESS_BACKGROUND_LOCATION)) {
                        if (grantResult == PackageManager.PERMISSION_GRANTED) {
                            backgroundPermissionGranted = true;
                        }
                    }
                } else {
                    backgroundPermissionGranted = foregroundPermissionGranted;
                }
            }
            if (foregroundPermissionGranted &&!backgroundPermissionGranted) {
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                    showBackgroundPermissionRequiredDialog();
                }
            }
            else {
                boolean showRationale = false;
                for (String permission : permissions) {
                    if (ActivityCompat.shouldShowRequestPermissionRationale(this, permission)) {
                        showRationale = true;
                        break;
                    }
                }

                if (showRationale) {
                    showLocationPermissionExplanationDialog();
                } else {
                    showSettingsPermissionDialog();
                }
            }
        }
    }
    private void showBackgroundPermissionRequiredDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Требуется фоновое местоположение")
                .setMessage("Чтобы приложение могло отслеживать ваше местоположение, даже когда оно закрыто, необходимо разрешение на определение местоположения в фоновом режиме ('Разрешить в любом режиме').")
                .setPositiveButton("Перейти в настройки", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", getPackageName(), null);
                    intent.setData(uri);
                    startActivity(intent);
                })
                .setNegativeButton("Выйти", (dialog, which) -> {
                    dialog.dismiss();
                    finish();
                })
                .create()
                .show();
    }
    private void showSettingsPermissionDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Требуется разрешение на местоположение")
                .setMessage("Для работы этой функции требуется разрешение на определение местоположения. Пожалуйста, перейдите в настройки приложения и предоставьте разрешение (выберите 'Разрешить в любом режиме', если доступно).")
                .setPositiveButton("Настройки", (dialog, which) -> {

                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", getPackageName(), null);
                    intent.setData(uri);
                    startActivity(intent);
                })
                .setNegativeButton("Выйти", (dialog, which) -> {
                    dialog.dismiss();
                    finish();
                })
                .create()
                .show();
    }
    private void showLocationPermissionExplanationDialog() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Почему требуется местоположение?")
                .setMessage("Это приложение помогает вам делиться своим местоположением с друзьями и видеть их на карте. Для этого необходим доступ к вашему местоположению.")
                .setPositiveButton("Понятно, запросить снова", (dialog, which) -> {
                    Log.d("PermissionDebug", "DEBUG: Кнопка 'Запросить снова' нажата. Вызываем запрос только FINE_LOCATION.");
                    String[] permissionsArray = new String[]{Manifest.permission.ACCESS_FINE_LOCATION};

                    ActivityCompat.requestPermissions(this,
                            permissionsArray,
                            LOCATION_PERMISSION_REQUEST_CODE);
                })
                .setNegativeButton("Выйти", (dialog, which) -> {
                    dialog.dismiss();
                    finish();
                })
                .create()
                .show();
    }



    @Override
    protected void onStart() {
        super.onStart();
        if (NetworkUtils.isInternetAvailable(this)) {
            if (!myPref.getString("frLatitude", "").equals("null") && !(myPref.getString("frLatitude", "").isEmpty())) {
                double frLatitude = Double.parseDouble(myPref.getString("frLatitude", ""));
                double frLongitude = Double.parseDouble(myPref.getString("frLongitude", ""));
                locate(frLatitude, frLongitude, 1);
                SharedPreferences.Editor ed = myPref.edit();
                ed.putString("frLatitude", "null");
                ed.putString("frLongitude", "null");
                ed.apply();

            }
            if (myPref.getString("newPhoto", "").equals("1")) {
                SharedPreferences.Editor ed = myPref.edit();
                ed.putString("newPhoto", "0");
                ed.apply();
                myRef.child("Users").child(userID).get().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        java.util.Map<String, Object> base = (java.util.Map<String, Object>) task.getResult().getValue();
                        if (base.containsKey("photo")) {
                            imageLoader.loadImageAsync(this, userIC, base.get("photo").toString());
                        } else {
                            String username = myPref.getString("username", "");
                            if (username.length() > 11) {
                                username = username.substring(0, 9) + "..";
                            }
                            userIC.setIcon(ImageProvider.fromBitmap(ImageUtils.getCircularBitmapFromDrawable(ContextCompat.getDrawable(MainActivity.this, R.drawable.white), this, 60, 4, username)));
                        }
                    }
                });
            }
            if (!isLocate()) {
                List<String> permissionsToRequest = new ArrayList<>();
                permissionsToRequest.add(Manifest.permission.ACCESS_FINE_LOCATION);
                String[] permissionsArray = permissionsToRequest.toArray(new String[0]);
                boolean showRationale = ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.ACCESS_FINE_LOCATION);
                if (showRationale) {
                    showLocationPermissionExplanationDialog();
                } else {
                    ActivityCompat.requestPermissions(this,
                            permissionsArray,
                            LOCATION_PERMISSION_REQUEST_CODE);
                }

            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                String packageName = getPackageName();
                PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);

                boolean isIgnoring = false;
                if (pm != null) {
                    isIgnoring = pm.isIgnoringBatteryOptimizations(packageName);
                }
                Log.d(TAG, "Is ignoring battery optimizations? " + isIgnoring);

                if (!isIgnoring) {
                    Log.d(TAG, "App is NOT ignoring battery optimizations. Showing dialog.");

                    new AlertDialog.Builder(this)
                            .setTitle("Отключите оптимизацию батареи")
                            .setMessage("Для надежного отслеживания вашего местоположения в фоновом режиме, пожалуйста, отключите оптимизацию батареи для этого приложения. Это позволит сервису работать без ограничений.")
                            .setPositiveButton("Перейти в настройки", (dialog, which) -> {
                                Log.d(TAG, "User clicked 'Open Settings'. Redirecting...");
                                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                                Uri uri = Uri.fromParts("package", getPackageName(), null);
                                intent.setData(uri);
                                try {
                                    startActivity(intent);
                                    Log.d(TAG, "Sent ACTION_APPLICATION_DETAILS_SETTINGS Intent.");
                                } catch (ActivityNotFoundException e) {
                                    Log.e(TAG, "Cannot open app settings", e);
                                }
                                dialog.dismiss();
                            })
                            .setNegativeButton("Выйти", (dialog, which) -> {
                                Log.d(TAG, "User clicked 'Cancel'. Dialog dismissed.");
                                dialog.dismiss();
                                finish();
                            })
                            .setIcon(R.mipmap.app_icon)
                            .show();

                } else if (!isStart) {
                    isStart = true;
                    getLocationMain();
                }
            } else if (!isStart) {
                isStart = true;
                getLocationMain();
            }



        } else {
            Toast.makeText(this, "Интернет отсутствует!", Toast.LENGTH_SHORT).show();
        }
    }


    @Override
    protected void onStop() {
        super.onStop();
    }
    @Override
    protected void onDestroy() {

        if (repeatHandler != null && repeatRunnable != null) {
            repeatHandler.removeCallbacks(repeatRunnable);
            Log.d("RepeatTask", "Повторяющаяся задача остановлена.");
        }
        if (isLocate() && mapView!=null) {
            mapView.onStop();
            MapKitFactory.getInstance().onStop();
        }
        repeatRunnable = null;
        imageLoader.shutdown();
        super.onDestroy();
    }





    @SuppressLint("MissingPermission")
    private void getLocation() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        latitude = location.getLatitude();
                        longitude = location.getLongitude();
                    } else {
                        Log.d(TAG, "Последнее известное местоположение равно нулю.");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Не удалось получить местоположение.", e);
                });
    }
    @SuppressLint("MissingPermission")
    private void getLocationMain() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        latitude = location.getLatitude();
                        longitude = location.getLongitude();
                        mainStart();
                    } else {
                        Log.d(TAG, "Последнее известное местоположение равно нулю.");
                    }
                })
                .addOnFailureListener(e -> {
                    Log.w(TAG, "Не удалось получить местоположение.", e);
                });
    }
    private void locate(double latitude, double longitude,int time){
        Point point = new Point(latitude,longitude);
        mapView.getMapWindow().getMap().move(
                new CameraPosition(point, 16.0f, 0.0f, 0.0f),
                new Animation(Animation.Type.SMOOTH, time), null);
    }
    private boolean isLocate() {
        boolean foregroundPermissionGranted =
                ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
                        ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED;
        if (!foregroundPermissionGranted) {
            return false;
        }
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
            boolean backgroundPermissionGranted = ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_BACKGROUND_LOCATION) == PackageManager.PERMISSION_GRANTED;
            return backgroundPermissionGranted;
        } else {
            return true;
        }
    }

    private void mainStart(){
        MapKitFactory.initialize(this);
        setContentView(R.layout.activity_main);
        checkAndRequestNotificationPermission();
        mapView = findViewById(R.id.mapview);
        locate(latitude,longitude,4);
        Log.d(TAG, "SharedPreferences 'location' value: " + myPref.getString("location",""));
        if (myPref.getString("location","").equals("1")||myPref.getString("location","").isEmpty()) {

            OneTimeWorkRequest locationWorkRequest =
                    new OneTimeWorkRequest.Builder(LocationTrackingWorker.class)
                            .build();

            WorkManager.getInstance(this).enqueueUniqueWork(
                    "LocationTrackingWork",
                    ExistingWorkPolicy.KEEP,
                    locationWorkRequest);

            Log.d(TAG, "LocationTrackingWork task enqueued.");
        } else {
            Log.d(TAG, "User preference does NOT allow location tracking. Not scheduling WorkManager task.");
        }


        map = mapView.getMapWindow().getMap();
        userIC = map.getMapObjects().addPlacemark(new Point(latitude,longitude), ImageProvider.fromBitmap(defaultBitmap));
        userTap = new MapObjectTapListener() {
            @Override
            public boolean onMapObjectTap(@NonNull MapObject mapObject, @NonNull Point point) {
                locate(latitude,longitude,1);
                Toast.makeText(MainActivity.this,"Это вы",Toast.LENGTH_SHORT).show();
                return true;
            }
        };
        userIC.addTapListener(userTap);
        myRef.child("Users").child(userID).get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                java.util.Map<String, Object> base = (java.util.Map<String, Object>) task.getResult().getValue();
                if (base.containsKey("photo")) {
                    imageLoader.loadImageAsync(this, userIC, base.get("photo").toString());
                } else {
                    String username = myPref.getString("username", "");
                    if (username.length() > 11) {
                        username = username.substring(0, 9) + "..";
                    }
                    userIC.setIcon(ImageProvider.fromBitmap(ImageUtils.getCircularBitmapFromDrawable(ContextCompat.getDrawable(MainActivity.this, R.drawable.white), this, 60, 4, username)));
                }
            }
        });
        userIC.setZIndex(100);

        findViewById(R.id.accountM).setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, UserAccount.class);
            startActivity(intent);
        });
        findViewById(R.id.friendsM).setOnClickListener(view -> {
            Intent intent = new Intent(MainActivity.this, UserFriends.class);
            startActivity(intent);

        });
        findViewById(R.id.locM).setOnClickListener(view -> {
            getLocation();
            locate(latitude,longitude,1);
            animatePlacemarkMapObjectToPoint(userIC,new Point(latitude, longitude));
        });
        repeatHandler.postDelayed(repeatRunnable, 0);

    }
    private void checkAndRequestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {

            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) ==
                    PackageManager.PERMISSION_GRANTED) {
            } else if (shouldShowRequestPermissionRationale(Manifest.permission.POST_NOTIFICATIONS)) {
                showNotificationPermissionRationale();
            } else {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            }
        } else {
            Toast.makeText(this, "Разрешение на уведомление предоставленно по умолчанию", Toast.LENGTH_SHORT).show();
        }
    }

    private void showNotificationPermissionRationale() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Требуется разрешение на уведомления")
                .setMessage("Этому приложению требуется разрешение на получение уведомлений, чтобы показывать вам важные оповещения.")
                .setPositiveButton("OK", (dialog, which) -> {
                    requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showNotificationPermissionDeniedMessage() {
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Доступ запрещён")
                .setMessage("Вам отказано в разрешении на получение уведомлений. Пожалуйста, включите это в настройках приложения, чтобы получать уведомления.")
                .setPositiveButton("Open Settings", (dialog, which) -> {
                    Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    Uri uri = Uri.fromParts("package", getPackageName(), null);
                    intent.setData(uri);
                    startActivity(intent);
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
    public void animatePlacemarkMapObjectToPoint(final PlacemarkMapObject placemarkMapObject, final Point newTargetPoint) {
        ValueAnimator currentPlacemarkAnimator;
        final Point startPoint;
        startPoint = placemarkMapObject.getGeometry();
        if (startPoint.getLatitude() == newTargetPoint.getLatitude() && startPoint.getLongitude() == newTargetPoint.getLongitude()) {
            return;
        }
        currentPlacemarkAnimator = ValueAnimator.ofFloat(0f, 1f);
        currentPlacemarkAnimator.setDuration(1000);

        currentPlacemarkAnimator.setInterpolator(new LinearInterpolator());
        currentPlacemarkAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animator) {
                float fraction = (float) animator.getAnimatedValue();
                double lat = startPoint.getLatitude() + (newTargetPoint.getLatitude() - startPoint.getLatitude()) * fraction;
                double lng = startPoint.getLongitude() + (newTargetPoint.getLongitude() - startPoint.getLongitude()) * fraction;

                Point intermediatePoint = new Point(lat, lng);
                placemarkMapObject.setGeometry(intermediatePoint);
            }
        });
        currentPlacemarkAnimator.start();
    }
    class ImageLoader {

        private final ExecutorService executorService = Executors.newSingleThreadExecutor();

        public void loadImageAsync(Activity activity, PlacemarkMapObject user, String imageUrl) {
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    final Drawable drawable = LoadImageFromWebOperations(imageUrl);

                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (drawable != null) {
                                user.setIcon(ImageProvider.fromBitmap(ImageUtils.getCircularBitmapFromDrawable(drawable,activity,60,4,null)));
                            } else {
                                System.out.println("Не удалось загрузить изображение с " + imageUrl);
                            }
                        }
                    });
                }
            });
        }
        public void shutdown() {
            executorService.shutdown();
        }
        protected Drawable LoadImageFromWebOperations(String urlString) {
            HttpURLConnection urlConnection = null;
            InputStream inputStream = null;
            try {
                System.out.println("DEBUG: Attempting to load image from URL: " + urlString);

                URL url = new URL(urlString);
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestMethod("GET");
                urlConnection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36");


                urlConnection.connect();

                int responseCode = urlConnection.getResponseCode();
                System.out.println("DEBUG: HTTP Response Code for " + urlString + ": " + responseCode);

                if (responseCode == HttpURLConnection.HTTP_OK) {
                    inputStream = urlConnection.getInputStream();
                    if (inputStream != null) {
                        return Drawable.createFromStream(inputStream, "src name");
                    } else {
                        System.err.println("ERROR: Input stream is null for successful response!");
                        return null;
                    }
                } else {
                    System.err.println("ERROR: HTTP error code: " + responseCode);
                    return null;
                }

            } catch (IOException e) {
                System.err.println("ERROR: IOException during image download.");
                e.printStackTrace();
                return null;
            } finally {
                if (inputStream != null) {
                    try { inputStream.close(); } catch (IOException e) { e.printStackTrace(); }
                }
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
            }
        }
    }

}