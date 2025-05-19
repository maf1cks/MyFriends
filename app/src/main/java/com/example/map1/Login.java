package com.example.map1;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthInvalidCredentialsException;
import com.google.firebase.auth.FirebaseAuthInvalidUserException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Map;
import java.util.Objects;

public class Login extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private SharedPreferences myPref;
    private EditText ETemail;
    private EditText ETpassword;
    private FirebaseUser firebaseUser;
    private DatabaseReference myRef;
    private LoadingDialog loadingDialog;
    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_login);

        init();

        findViewById(R.id.aftorizationL).setOnClickListener(view -> {
            String email = ETemail.getText().toString();
            String password = ETpassword.getText().toString();
            if (checkInf(email,password)) {
                loadingDialog.startDialog();
                login(email, password);
            }
        });
        findViewById(R.id.resetPasswordL).setOnClickListener(view -> {
            Intent intent = new Intent(Login.this, ResetPassword.class);
            startActivity(intent);
        });
        findViewById(R.id.closeL).setOnClickListener(view -> finish());
    }
    private void init() {
        mAuth = FirebaseAuth.getInstance();
        myPref = getSharedPreferences("data",0);
        myRef = FirebaseDatabase.getInstance().getReference();
        ETemail = findViewById(R.id.et_emailL);
        ETpassword = findViewById(R.id.et_passwordL);
        loadingDialog = new LoadingDialog(this, "Подождите немного...");
    }
    private boolean checkInf(String email, String password) {
        if ((TextUtils.isEmpty(email)) || (TextUtils.isEmpty(password))) {
            Toast.makeText(Login.this, "Заполните все поля", Toast.LENGTH_SHORT).show();
            return false;
        } else if ((IsSpace.isSpase(email)) || IsSpace.isSpase(password)) {
            Toast.makeText(Login.this, "Пробелы не допускаются", Toast.LENGTH_SHORT).show();
            return false;
        }else if ((IsSpace.check(password))) {
            Toast.makeText(Login.this, "Подобные символы не допускаются", Toast.LENGTH_SHORT).show();
            return false;
        } else return true;
    }
    private void login(String email, String password){
        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        firebaseUser = mAuth.getCurrentUser();
                        if (Objects.requireNonNull(firebaseUser).isEmailVerified()) {
                            SharedPreferences.Editor ed = myPref.edit();
                            ed.putString("isAft", "1");
                            ed.apply();
                            getUserData();

                        } else {
                            loadingDialog.dismissDialog();
                            Toast.makeText(Login.this, "Почта не подтверждена", Toast.LENGTH_SHORT).show();
                            mAuth.signOut();
                        }
                    } else {
                        try {
                            throw Objects.requireNonNull(task.getException());
                        } catch (FirebaseAuthInvalidUserException e) {
                            Toast.makeText(Login.this, "Неверная почта", Toast.LENGTH_SHORT).show();
                        } catch (FirebaseAuthInvalidCredentialsException e) {
                            Toast.makeText(Login.this, "Неверные пароль или почта", Toast.LENGTH_SHORT).show();
                        } catch (Exception e) {
                            Log.e("LoginActivity", Objects.requireNonNull(e.getMessage()));
                            Toast.makeText(Login.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                        finally {
                            loadingDialog.dismissDialog();
                        }
                    }
                });
    }
    private void getUserData(){
        String userID = Objects.requireNonNull(firebaseUser).getUid();
        myRef.child("Users").child(userID).get().addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                Map<String,Object> user = (Map<String,Object>) (task.getResult().getValue());
                SharedPreferences.Editor ed = myPref.edit();
                ed.putString("isFr", Objects.requireNonNull(Objects.requireNonNull(user).get("isFr")).toString());
                ed.putString("username", Objects.requireNonNull(Objects.requireNonNull(user).get("username")).toString());
                ed.putString("userID", userID);
                ed.apply();
                if (myPref.getString("isFr","").equals("1")) {
                    ed.putString("userFrID", Objects.requireNonNull(user.get("userFrID")).toString());
                    ed.apply();
                }
                loadingDialog.dismissDialog();
                Intent intent = new Intent(Login.this, MainActivity.class);
                startActivity(intent);
                finishAffinity();
            } else loadingDialog.dismissDialog();
        });

    }
}