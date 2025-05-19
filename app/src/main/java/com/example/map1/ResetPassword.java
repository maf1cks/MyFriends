package com.example.map1;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.FirebaseAuth;

public class ResetPassword extends AppCompatActivity {
    private FirebaseAuth mAuth;
    private EditText ETemail;
    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_reset_password);

        init();

        findViewById(R.id.registrationP).setOnClickListener(view -> {

            String email = ETemail.getText().toString();
            if (checkInf(email)) {
                resetPassword(email);
            }
        });
        findViewById(R.id.closeP).setOnClickListener(view -> finish());
    }
    private void init(){
        mAuth = FirebaseAuth.getInstance();
        ETemail = findViewById(R.id.et_emailP);
    }
    private boolean checkInf(String email){
        if (TextUtils.isEmpty(email)) {
            Toast.makeText(ResetPassword.this, "Заполните все поля", Toast.LENGTH_SHORT).show();
            return false;
        } else if (IsSpace.isSpase(email)) {
            Toast.makeText(ResetPassword.this, "Пробелы не допускаются", Toast.LENGTH_SHORT).show();
            return false;
        } else return true;
    }
    private void resetPassword(String email){
        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(ResetPassword.this, "Письмо отправлено", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(ResetPassword.this, "Неверная почта", Toast.LENGTH_SHORT).show();

                    }
                });
    }
}