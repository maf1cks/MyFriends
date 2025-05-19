package com.example.map1;


import static android.content.ContentValues.TAG;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import okhttp3.Request;
import androidx.activity.result.ActivityResultLauncher;
import okhttp3.Call;
import androidx.activity.EdgeToEdge;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.work.ExistingWorkPolicy;
import androidx.work.OneTimeWorkRequest;
import androidx.work.WorkManager;

import okhttp3.Callback;

import android.app.AlertDialog;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.RequestBody;
import okhttp3.Response;
import android.content.ContentResolver;
import android.content.Context;
import java.io.FileNotFoundException;

public class UserAccount extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private DatabaseReference myRef;
    private FirebaseUser firebaseUser;
    private Uri uriImage;
    private String username;
    private TextView TVusername;
    private ImageView userImage;
    @SuppressLint("UseSwitchCompatOrMaterialCode")
    private Switch switch1;
    private Switch switch2;
    private SharedPreferences myPref;
    private ImageLoader imageLoader;
    private LoadingDialog loadingNewImageDialog;
    private ImageUploader imageUploader;
    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_user_account);

        init();


        ActivityResultLauncher<Intent> activityResultLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        Intent data = result.getData();
                        if (data != null) {
                            uriImage = data.getData();
                            userImage.setImageDrawable(getDrawableFromContentUri(this,uriImage));
                            Drawable imageToUpload = userImage.getDrawable();
                            if (imageToUpload != null) {
                                loadingNewImageDialog = new LoadingDialog(this,"Подождите немного...");
                                loadingNewImageDialog.startDialog();
                                String uploadScriptUrl = "http://d98762ug.beget.tech/upload_image.php";
                                String desiredFilename = "uploaded_image_" + myPref.getString("userID","") + ".png";

                                imageUploader.uploadImage(UserAccount.this, imageToUpload, uploadScriptUrl, desiredFilename);
                            } else {

                                Toast.makeText(UserAccount.this, "No image to upload!", Toast.LENGTH_SHORT).show();
                            }
                        }
                    }
                });
        findViewById(R.id.newImageUA).setOnClickListener(view -> {
            if (NetworkUtils.isInternetAvailable(this)) {
                Intent intent = new Intent();
                intent.setType("image/*");
                intent.setAction(Intent.ACTION_GET_CONTENT);
                activityResultLauncher.launch(intent);
            }
        });

        findViewById(R.id.outAccountUA).setOnClickListener(view -> {
            if (NetworkUtils.isInternetAvailable(this)) showExitAccountDialog();

        });
        findViewById(R.id.closeUA).setOnClickListener(view -> finish());

        switch1.setOnClickListener(view -> {
            if (NetworkUtils.isInternetAvailable(this)) {
                if (switch1.isChecked()) {
                    Log.d(TAG, "User preference allows location tracking. Scheduling WorkManager task.");
                    OneTimeWorkRequest locationWorkRequest =
                            new OneTimeWorkRequest.Builder(LocationTrackingWorker.class)
                                    .build();
                    WorkManager.getInstance(this).enqueueUniqueWork(
                            "LocationTrackingWork",
                            ExistingWorkPolicy.KEEP,
                            locationWorkRequest);

                    Log.d(TAG, "LocationTrackingWork task enqueued.");
                    SharedPreferences.Editor ed = myPref.edit();
                    ed.putString("location", "1");
                    ed.apply();
                } else {
                    showDisableServiceDialog();
                }
            }
        });
        switch2.setOnClickListener(view -> {
            if (NetworkUtils.isInternetAvailable(this)) {
                if (switch2.isChecked()) {
                    SharedPreferences.Editor ed = myPref.edit();
                    ed.putString("time", "1");
                    ed.apply();
                } else {
                    SharedPreferences.Editor ed = myPref.edit();
                    ed.putString("time", "0");
                    ed.apply();
                }
            }
        });

        findViewById(R.id.newUsernameUA).setOnClickListener(view -> {
            if (NetworkUtils.isInternetAvailable(this)) showNameInputDialog();
        });

    }
    @SuppressLint("WrongViewCast")
    private void init(){
        mAuth = FirebaseAuth.getInstance();
        myRef = FirebaseDatabase.getInstance().getReference("Users");
        myPref = getSharedPreferences("data",0);
        firebaseUser = mAuth.getCurrentUser();
        username = myPref.getString("username","");
        TVusername = findViewById(R.id.tv_usernameUA);
        TVusername.setText(username);
        userImage = findViewById(R.id.userImageUA);
        switch1 = findViewById(R.id.switch1UA);
        switch2 = findViewById(R.id.switch2UA);
        imageLoader = new ImageLoader();
        imageUploader = new ImageUploader();
        if (NetworkUtils.isInternetAvailable(this)) {
            myRef.child(myPref.getString("userID", "")).get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Map<String, Object> base = (Map<String, Object>) task.getResult().getValue();
                    if (base.containsKey("photo")) {
                        imageLoader.loadImageAsync(this, userImage, base.get("photo").toString());
                    } else
                        imageLoader.loadImageAsync(this, userImage, "http://d98762ug.beget.tech/photos/default.jpg");
                }
            });
        }
        if (myPref.getString("location","").isEmpty()){
            SharedPreferences.Editor ed = myPref.edit();
            ed.putString("location","1");
            ed.apply();
        } else if (myPref.getString("location","").equals("0")){
           switch1.setChecked(false);
        }
        if (myPref.getString("time","").isEmpty()){
            SharedPreferences.Editor ed = myPref.edit();
            ed.putString("time","0");
            ed.apply();
        } else if (myPref.getString("time","").equals("1")){
            switch2.setChecked(true);
        }

    }
    private boolean checkInf(String newUsername){
        if (TextUtils.isEmpty(newUsername)) {
            Toast.makeText(UserAccount.this, "Имя не может быть пустым", Toast.LENGTH_SHORT).show();
            return false;
        } else if (IsSpace.isSpase(newUsername)) {
            Toast.makeText(UserAccount.this, "Пробелы не допускаются", Toast.LENGTH_SHORT).show();
            return false;
        } else if (IsSpace.check(newUsername)){
            Toast.makeText(UserAccount.this, "Подобные символы не допускаются", Toast.LENGTH_SHORT).show();
            return false;
        } else if (newUsername.length()<4){
            Toast.makeText(UserAccount.this, "Слишком короткое имя", Toast.LENGTH_SHORT).show();
            return false;
        } else if (newUsername.length()>30){
            Toast.makeText(UserAccount.this, "Слишком длинное имя", Toast.LENGTH_SHORT).show();
            return false;
        } else return true;

    }
    private Drawable getDrawableFromContentUri(Context context, Uri uri) {
        if (uri == null) {
            return null;
        }

        Drawable drawable = null;
        InputStream inputStream = null;
        try {
            ContentResolver contentResolver = context.getContentResolver();
            inputStream = contentResolver.openInputStream(uri);

            if (inputStream != null) {
                drawable = Drawable.createFromStream(inputStream, null);
            }

        } catch (FileNotFoundException e) {
            e.printStackTrace();
            System.err.println("Error: File not found for Uri: " + uri.toString());
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error: Failed to get Drawable from Uri: " + uri.toString());
        } finally {
            if (inputStream != null) {
                try {
                    inputStream.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
        return drawable;
    }
    private void showExitAccountDialog() {
        new MaterialAlertDialogBuilder(this)
                .setTitle("Выход из аккаунта")
                .setMessage("Вы действительно хотите выйти из аккаунта?")
                .setNegativeButton("Нет", (dialog, which) -> dialog.cancel())
                .setPositiveButton("Да", (dialog, which) -> {
                    dialog.dismiss();
                    mAuth.signOut();
                    Intent intent = new Intent(UserAccount.this, Registration.class);
                    Intent finishService = new Intent(UserAccount.this, LocationService.class);
                    stopService(finishService);
                    finishAffinity();

                    startActivity(intent);
                })
                .show();
    }
    private void showDisableServiceDialog() {
        switch1.setChecked(true);
        new MaterialAlertDialogBuilder(this)
                .setTitle("Вы уверены?")
                .setMessage("Другие пользователи больше не будут знать где вы находитесь")
                .setNegativeButton("Нет", (dialog, which) -> dialog.cancel())
                .setPositiveButton("Да", (dialog, which) -> {
                    dialog.dismiss();
                    mAuth.signOut();
                    myRef.child(myPref.getString("userID","")).child("geo").setValue(null);
                    Intent finishService = new Intent(UserAccount.this, LocationService.class);
                    stopService(finishService);
                    SharedPreferences.Editor ed = myPref.edit();
                    ed.putString("location","0");
                    ed.apply();
                    switch1.setChecked(false);
                })
                .show();
    }
    private void showNameInputDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Введите ваше новое имя");
        builder.setMessage("Пожалуйста, введите имя:");
        final EditText input = new EditText(this);
        builder.setView(input);
        builder.setPositiveButton("Сохранить", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String newUsername = input.getText().toString();
                    if (checkInf(newUsername)) {
                        if (!username.equals(newUsername)) {
                            myRef.child(myPref.getString("userID","")).child("username").setValue(newUsername);
                            @SuppressLint("CommitPrefEdits") SharedPreferences.Editor ed = myPref.edit();
                            ed.putString("username", newUsername);
                            ed.putString("newPhoto", "1");
                            ed.apply();
                            username = newUsername;
                            TVusername.setText(username);
                            Toast.makeText(UserAccount.this, "Имя успешно изменено", Toast.LENGTH_SHORT).show();
                        }
                    }

            }
        });
        builder.setNegativeButton("Отмена", new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                dialog.cancel();
            }
        });
        builder.setCancelable(true);
        builder.show();
    }

    @Override
    protected void onDestroy(){
        imageLoader.shutdown();
        imageUploader.shutdown();
        super.onDestroy();
    }
    private class ImageLoader {

        private final ExecutorService executorService = Executors.newSingleThreadExecutor();

        public void loadImageAsync(Activity activity, ImageView imageView, String imageUrl) {
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    final Drawable drawable = LoadImageFromWebOperations(imageUrl);

                    activity.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (drawable != null) {
                                imageView.setImageDrawable(drawable);
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
        private Drawable LoadImageFromWebOperations(String urlString) {
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
    private class ImageUploader{
        private final ExecutorService executorService = Executors.newSingleThreadExecutor();
        private final OkHttpClient client = new OkHttpClient();
        private final Handler mainThreadHandler = new Handler(Looper.getMainLooper());


        private Bitmap drawableToBitmap(Drawable drawable) {
            if (drawable instanceof BitmapDrawable) {
                return ((BitmapDrawable) drawable).getBitmap();
            }

            Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
            Canvas canvas = new Canvas(bitmap);
            drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
            drawable.draw(canvas);

            return bitmap;
        }

        public void uploadImage(Activity activity, Drawable imageDrawable, String uploadUrl, String filenameOnServer) {
            executorService.execute(new Runnable() {
                @Override
                public void run() {
                    Bitmap bitmap = drawableToBitmap(imageDrawable);
                    if (bitmap == null) {
                        System.err.println("ERROR: Failed to convert Drawable to Bitmap.");
                        showToast(activity, "Failed to prepare image for upload.");
                        return;
                    }
                    ByteArrayOutputStream stream = new ByteArrayOutputStream();
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream);
                    byte[] imageBytes = stream.toByteArray();

                    try {
                        stream.close();
                    } catch (IOException e) {
                        loadingNewImageDialog.dismissDialog();
                        e.printStackTrace();
                    }
                    RequestBody requestBody = new MultipartBody.Builder()
                            .setType(MultipartBody.FORM)
                            .addFormDataPart("image_file", filenameOnServer,
                                    RequestBody.create(imageBytes, MediaType.parse("image/png")))
                            .build();
                    Log.e("WW",imageBytes.toString());
                    Request request = new Request.Builder()
                            .url(uploadUrl)
                            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/58.0.3029.110 Safari/537.36")
                            .post(requestBody)
                            .build();


                    client.newCall(request).enqueue(new Callback() {
                        @Override
                        public void onFailure(Call call, IOException e) {
                            loadingNewImageDialog.dismissDialog();
                            System.err.println("Upload Failed: " + e.getMessage());
                            e.printStackTrace();
                            showToast(activity, "Upload failed: " + e.getMessage());
                        }

                        @Override
                        public void onResponse(Call call, Response response) throws IOException {
                            if (response.isSuccessful()) {
                                String responseBody = response.body() != null ? response.body().string() : "No response body";
                                System.out.println("Upload Successful: " + response.code() + " - " + responseBody);
                                showToast(activity, "Фото установлено!");
                                loadingNewImageDialog.dismissDialog();
                                myRef.child(myPref.getString("userID","")).child("photo").setValue("http://d98762ug.beget.tech/photos/"+filenameOnServer);
                                SharedPreferences.Editor ed = myPref.edit();
                                ed.putString("newPhoto","1");
                                ed.apply();
                            } else {
                                loadingNewImageDialog.dismissDialog();
                                String errorBody = response.body() != null ? response.body().string() : "No error body";
                                System.err.println("Upload Failed (Server Error): " + response.code() + " - " + errorBody);
                                showToast(activity, "Upload failed: Server returned " + response.code());
                            }
                            if (response.body() != null) {
                                response.body().close();
                            }
                        }
                    });
                }
            });
        }
        private void showToast(Activity activity, String message) {
            mainThreadHandler.post(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(activity, message, Toast.LENGTH_SHORT).show();
                }
            });
        }
        public void shutdown() {
            executorService.shutdown();
        }
    }
}