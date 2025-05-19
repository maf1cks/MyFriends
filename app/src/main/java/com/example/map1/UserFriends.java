package com.example.map1;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class UserFriends extends AppCompatActivity{
    private TextView TVuserFrID;
    private EditText ETfriendFrID;
    private FirebaseAuth mAuth;
    private SharedPreferences myPref;
    private DatabaseReference myRef;
    private FirebaseUser firebaseUser;
    private String userID;
    private ListView userFrRequests;
    private ListView userFr;
    private TextView TVuserFrRequestsCount;
    private TextView TVuserFrCount;
    public static Activity act;

    @SuppressLint("MissingInflatedId")
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_friedns);
        act = this;
        init1();

        if (myPref.getString("isFr","").equals("0")){
            Intent intent = new Intent(UserFriends.this, UserFrID.class);
            startActivity(intent);
            finish();
        } else init2();

        findViewById(R.id.closeF).setOnClickListener(view -> finish());

        findViewById(R.id.addFrF).setOnClickListener(view -> {
            if (NetworkUtils.isInternetAvailable(this)) {
                String friendFrID = ETfriendFrID.getText().toString();
                myRef.child("UsersFrID").get().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Map<String, String> base = (Map<String, String>) task.getResult().getValue();
                        if (Objects.requireNonNull(base).containsKey(friendFrID)) {
                            String friendID = base.get(friendFrID);
                            String userFrID = myPref.getString("userFrID", "");
                            if (friendFrID.equals(userFrID)) {
                                Toast.makeText(UserFriends.this, "Самому себе?", Toast.LENGTH_SHORT).show();
                            } else {
                                myRef.child("Users").child(userID).child("userFrRequests").get().addOnCompleteListener(task1 -> {
                                    Map<String, String> base123 = (Map<String, String>) task1.getResult().getValue();
                                    if (base123 != null && base123.containsKey(friendID)) {
                                        myRef.child("Users").child(userID).child("userFr").child(friendID).setValue(friendID);
                                        myRef.child("Users").child(userID).child("userFrRequests").child(friendID).removeValue();
                                        myRef.child("Users").child(friendID).child("userFr").child(userID).setValue(userID);
                                        Toast.makeText(UserFriends.this, friendFrID + " добавлен(а) в друзья", Toast.LENGTH_SHORT).show();
                                    } else {
                                        myRef.child("Users").child(friendID).get().addOnCompleteListener(task2 -> {
                                            if (task2.isSuccessful()) {
                                                Map<String, Object> base0 = (Map<String, Object>) task2.getResult().getValue();
                                                Map<String, String> base1 = (Map<String, String>) base0.get("userFrRequests");
                                                Map<String, String> base2 = (Map<String, String>) base0.get("userFr");
                                                if ((base1 != null) && (Objects.requireNonNull(base1).containsKey(userID))) {
                                                    Toast.makeText(UserFriends.this, "Ждём ответа", Toast.LENGTH_SHORT).show();
                                                } else if ((base2 != null) && (Objects.requireNonNull(base2).containsKey(userID))) {
                                                    Toast.makeText(UserFriends.this, "Уже друзья", Toast.LENGTH_SHORT).show();
                                                } else {
                                                    myRef.child("Users").child(friendID).child("userFrRequests").child(userID).setValue(userID);
                                                    Toast.makeText(UserFriends.this, "Запрос отправлен", Toast.LENGTH_SHORT).show();
                                                }
                                            }
                                        });
                                    }
                                });

                            }
                        } else
                            Toast.makeText(UserFriends.this, "Такого ID не существует", Toast.LENGTH_SHORT).show();

                    }
                });
            }
        });
        if (NetworkUtils.isInternetAvailable(this)) {
            myRef.child("Users").child(userID).child("userFrRequests").addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    myRef.child("Users").child(userID).child("userFrRequests").get().addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Map<String, String> baseFrR = (Map<String, String>) task.getResult().getValue();
                            if (baseFrR != null) {
                                int baseFrRSize = baseFrR.size();
                                String[] userFrRequestsArray = baseFrR.values().toArray(new String[0]);
                                ArrayAdapter<String> adapter1 = new UsersFrRequestsAdapter(UserFriends.this, R.layout.users_fr_requests_adapter, List.of(userFrRequestsArray));

                                userFrRequests.setAdapter(adapter1);
                                TVuserFrRequestsCount.setText("Заявок в друзья: " + baseFrRSize);
                            } else {
                                userFrRequests.setAdapter(null);
                                TVuserFrRequestsCount.setText("Заявок в друзья: 0");
                            }
                        }
                    });
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
            myRef.child("Users").child(userID).child("userFr").addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    myRef.child("Users").child(userID).child("userFr").get().addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Map<String, Object> baseFr = (Map<String, Object>) task.getResult().getValue();
                            if (baseFr != null) {
                                int baseFrSize = baseFr.size();
                                String[] userFrArray = baseFr.values().toArray(new String[0]);
                                ArrayAdapter<String> adapter2 = new UsersFrAdapter(UserFriends.this, R.layout.users_fr_adapter, List.of(userFrArray));
                                userFr.setAdapter(adapter2);
                                TVuserFrCount.setText("Друзей: " + baseFrSize);
                            } else {
                                userFr.setAdapter(null);
                                TVuserFrCount.setText("Друзей: 0");
                            }
                        }
                    });
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {

                }
            });
        }
    }
    private void init1(){
        mAuth = FirebaseAuth.getInstance();
        myRef = FirebaseDatabase.getInstance().getReference();
        myPref = getSharedPreferences("data",0);
        TVuserFrRequestsCount = findViewById(R.id.tv_userFrRequestsCountF);
        TVuserFrCount = findViewById(R.id.tv_userFrCountF);
        TVuserFrID = findViewById(R.id.tv_userFrIDF);
        ETfriendFrID = findViewById(R.id.et_friendFrIDF);
        userFrRequests = findViewById(R.id.userFrRequestsF);
        userFr = findViewById(R.id.userFrF);
        userID = myPref.getString("userID","");
    }
    @SuppressLint("SetTextI18n")
    private void init2(){
        TVuserFrID.setText("Ваш ID: "+myPref.getString("userFrID",""));
        if (NetworkUtils.isInternetAvailable(this)) {
            myRef.child("Users").child(userID).child("userFrRequests").get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Map<String, String> baseFrR = (Map<String, String>) task.getResult().getValue();
                    if (baseFrR != null) {
                        int baseFrRSize = baseFrR.size();
                        String[] userFrRequestsArray = baseFrR.values().toArray(new String[baseFrRSize]);
                        ArrayAdapter<String> adapter1 = new UsersFrRequestsAdapter(this, R.layout.users_fr_requests_adapter, List.of(userFrRequestsArray));
                        userFrRequests.setAdapter(adapter1);
                        TVuserFrRequestsCount.setText("Заявок в друзья " + baseFrRSize);
                    } else {
                        TVuserFrRequestsCount.setText("Заявок в друзья 0");
                    }
                }
            });
            myRef.child("Users").child(userID).child("userFr").get().addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Map<String, String> baseFr = (Map<String, String>) task.getResult().getValue();
                    if (baseFr != null) {
                        int baseFrSize = baseFr.size();
                        String[] userFrArray = baseFr.values().toArray(new String[baseFrSize]);
                        ArrayAdapter<String> adapter2 = new UsersFrAdapter(this, R.layout.users_fr_adapter, List.of(userFrArray));
                        userFr.setAdapter(adapter2);
                        TVuserFrCount.setText("Друзей " + baseFrSize);
                    } else {
                        TVuserFrCount.setText("Друзей 0");
                    }
                }
            });
        }
    }
}