package com.example.map1;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Map;
import java.util.Objects;

public class Registration extends AppCompatActivity{
    private DatabaseReference myRef;
    private FirebaseAuth mAuth;
    private EditText ETemail;
    private EditText ETusername;
    private EditText ETpassword;
    private EditText ETpassword1;
    private FirebaseUser firebaseUser;
    private String userID;
    private LoadingDialog loadingDialog;
    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_aft);

        init();

        findViewById(R.id.registrationR).setOnClickListener(view -> {
            String email = ETemail.getText().toString();
            String username = ETusername.getText().toString();
            String password = ETpassword.getText().toString();
            String password1 = ETpassword1.getText().toString();
            if (checkInf(email,username,password,password1)) {
                checkInBase(email, username,password);
            }
        });
        findViewById(R.id.aftorizationR).setOnClickListener(view -> {
            Intent intent = new Intent(Registration.this, Login.class);
            startActivity(intent);
        });
    }
    private void init(){
        mAuth = FirebaseAuth.getInstance();
        ETemail = findViewById(R.id.et_emailR);
        ETusername = findViewById(R.id.et_usernameR);
        ETpassword = findViewById(R.id.et_passwordR);
        ETpassword1 = findViewById(R.id.et_password1R);
        myRef = FirebaseDatabase.getInstance().getReference("Users");
        SharedPreferences myPref = getSharedPreferences("data", 0);
        SharedPreferences.Editor ed = myPref.edit();
        ed.putString("isAft", "0");
        ed.putString("isFr", "0");
        ed.apply();
        loadingDialog = new LoadingDialog(this, "Подождите немного...");





    }
    private boolean checkInf(String email, String username, String password, String password1) {
        if ((TextUtils.isEmpty(email)) || (TextUtils.isEmpty(username)) || (TextUtils.isEmpty(password)) || (TextUtils.isEmpty(password1))) {
            Toast.makeText(Registration.this, "Заполните все поля", Toast.LENGTH_SHORT).show();
            return false;
        } else if ((IsSpace.isSpase(email)) || (IsSpace.isSpase(username)) || (IsSpace.isSpase(password)) || (IsSpace.isSpase(password1))) {
            Toast.makeText(Registration.this, "Пробелы не допускаются", Toast.LENGTH_SHORT).show();
            return false;
        } else if ((IsSpace.check(username)) || (IsSpace.check(password)) || (IsSpace.check(password1))) {
            Toast.makeText(Registration.this, "Подобные символы не допускаются", Toast.LENGTH_SHORT).show();
            return false;
        } else if (password.length()<6) {
            Toast.makeText(Registration.this, "Слишком короткий пароль", Toast.LENGTH_SHORT).show();
            return false;
        } else if (!password1.equals(password)) {
            Toast.makeText(Registration.this, "Пароли не совпадают", Toast.LENGTH_SHORT).show();
            return false;
        } else if (username.length()<4) {
            Toast.makeText(Registration.this, "Слишком короткое имя", Toast.LENGTH_SHORT).show();
            return false;
        } else if (username.length()>30) {
            Toast.makeText(Registration.this, "Слишком длинное имя", Toast.LENGTH_SHORT).show();
            return false;
        } else return true;
    }
    private void checkInBase(String email, String username, String password){
        myRef.get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Map<String,Object> base = (Map<String, Object>) task.getResult().getValue();
                boolean m = true;
                for(Object i : Objects.requireNonNull(base).values()){
                    Map<String,Object> j=(Map<String,Object>)(i);
                    if (Objects.equals(Objects.requireNonNull(j.get("email")).toString(), email)){
                        sendInBase(email,username,password, true);
                        m = false;
                        break;
                    }
                }
                if (m){
                    sendInBase(email, username, password,false);
                }
            }
        });
    }
    private void sendInBase(String email,String username, String password, boolean beEmail){
        if (beEmail) {
            Toast.makeText(Registration.this, "Почта уже зарегестрирована", Toast.LENGTH_SHORT).show();
        } else {
            loadingDialog.startDialog();
            registration(email, username, password);
        }
    }
    private void registration(String email,String username,String password) {
        mAuth.createUserWithEmailAndPassword(email, password)
        .addOnCompleteListener(this, task -> {
            if (task.isSuccessful()) {
                firebaseUser = mAuth.getCurrentUser();
                userID = Objects.requireNonNull(firebaseUser).getUid();
                myRef.child(userID).setValue(new User(email, username,"0"))
                        .addOnCompleteListener(databaseTask -> {
                            if (databaseTask.isSuccessful()) {
                                firebaseUser.sendEmailVerification();
                                Toast.makeText(Registration.this, "Письмо отправлено", Toast.LENGTH_SHORT).show();
                                mAuth.signOut();
                                loadingDialog.dismissDialog();
                                Intent intent = new Intent(Registration.this, Login.class);
                                startActivity(intent);
                            } else loadingDialog.dismissDialog();
                        });

            } else {
                loadingDialog.dismissDialog();
                Toast.makeText(Registration.this, "Неверная почта", Toast.LENGTH_SHORT).show();

            }
        });
    }
}
