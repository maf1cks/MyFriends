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

public class UserFrID extends AppCompatActivity {
    private DatabaseReference myRef;
    private SharedPreferences myPref;
    private FirebaseAuth mAuth;
    private EditText et_userFrID;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_user_id);

        init();

        findViewById(R.id.createFrIDUI).setOnClickListener(view -> {
            String userFrID = et_userFrID.getText().toString();
            if(checkInf(userFrID)){
                createFrID(userFrID);
            }
        });
    }
    private void init(){
        myPref = getSharedPreferences("data",0);
        mAuth = FirebaseAuth.getInstance();
        myRef = FirebaseDatabase.getInstance().getReference();
        et_userFrID = findViewById(R.id.et_userFrIDUI);
    }
    private boolean checkInf(String userFrID){
        if(TextUtils.isEmpty(userFrID)){
            Toast.makeText(UserFrID.this, "Заполните все поля", Toast.LENGTH_SHORT).show();
            return false;
        } else if (IsSpace.isSpase(userFrID)) {
            Toast.makeText(UserFrID.this, "Пробелы не допускаются", Toast.LENGTH_SHORT).show();
            return false;
        } else if (IsSpace.check(userFrID)) {
            Toast.makeText(UserFrID.this, "Подобные символы не допускаются", Toast.LENGTH_SHORT).show();
            return false;
        } else if (userFrID.length()>30) {
            Toast.makeText(UserFrID.this, "Слишком длинный", Toast.LENGTH_SHORT).show();
            return false;
        } else return true;
    }
    private void createFrID(String userFrID){
        FirebaseUser firebaseUser = mAuth.getCurrentUser();
        String userID = Objects.requireNonNull(firebaseUser).getUid();
        myRef.child("UsersFrID").get().addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                Map<String,String> base = (java.util.Map<String, String>) task.getResult().getValue();
                if ((base!=null) && (Objects.requireNonNull(base).containsKey(userFrID))){
                    Toast.makeText(UserFrID.this, "Такой ID уже существует", Toast.LENGTH_SHORT).show();
                } else {
                    myRef.child("Users").child(userID).child("isFr").setValue("1");
                    myRef.child("Users").child(userID).child("userFrID").setValue(userFrID);
                    myRef.child("UsersFrID").child(userFrID).setValue(userID);
                    SharedPreferences.Editor ed = myPref.edit();
                    ed.putString("isFr", "1");
                    ed.putString("userFrID", userFrID);
                    ed.apply();
                    Toast.makeText(UserFrID.this, "Ваш ID создан", Toast.LENGTH_SHORT).show();
                    Intent intent = new Intent(UserFrID.this,UserFriends.class);
                    startActivity(intent);
                    finish();
                }
            }
        });

    }
}