package com.example.map1;

import static android.content.ContentValues.TAG;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class UsersFrRequestsAdapter extends ArrayAdapter<String>{
    private final int layout;
    private SharedPreferences myPref;
    private DatabaseReference myRef;
    public UsersFrRequestsAdapter(Context context, int resourse, List<String> objects){
        super(context,resourse,objects);
        layout=resourse;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        init();
        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView = inflater.inflate(layout, parent, false);
            ViewHolderR viewHolder = new ViewHolderR();
            viewHolder.username = convertView.findViewById(R.id.tv_usernameUFRA);
            viewHolder.userFrID = convertView.findViewById(R.id.tv_userFrIDUFRA);
            viewHolder.accept = convertView.findViewById(R.id.acceptUFRA);
            viewHolder.notAccept = convertView.findViewById(R.id.deleteFrUFRA);
            if (NetworkUtils.isInternetAvailable(getContext())) {
                myRef.child("Users").child(Objects.requireNonNull(getItem(position))).get().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Map<String, Object> base = (Map<String, Object>) task.getResult().getValue();
                        viewHolder.username.setText(Objects.requireNonNull(Objects.requireNonNull(base).get("username")).toString());
                        viewHolder.userFrID.setText(Objects.requireNonNull(base.get("userFrID")).toString());
                    }
                });

                viewHolder.accept.setOnClickListener(view -> myRef.child("Users").child(Objects.requireNonNull(getItem(position))).get().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Map<String, Object> base = (Map<String, Object>) task.getResult().getValue();
                        myRef.child("Users").child(myPref.getString("userID", "")).child("userFrRequests").child(Objects.requireNonNull(getItem(position))).removeValue();
                        myRef.child("Users").child(myPref.getString("userID", "")).child("userFr").child(Objects.requireNonNull(getItem(position))).setValue(getItem(position));
                        myRef.child("Users").child(Objects.requireNonNull(getItem(position))).child("userFr").child(myPref.getString("userID", "")).setValue(myPref.getString("userID", ""));
                        Toast.makeText(getContext(), Objects.requireNonNull(base).get("username") + " добавлен(а) в друзья", Toast.LENGTH_SHORT).show();
                    }
                }));
                viewHolder.notAccept.setOnClickListener(v -> myRef.child("Users").child(Objects.requireNonNull(getItem(position))).get().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Map<String, Object> base = (Map<String, Object>) task.getResult().getValue();
                        showDeleteFrRequestDialog(Objects.requireNonNull(Objects.requireNonNull(base).get("username")).toString(), position);

                    }
                }));
            }
            convertView.setTag(viewHolder);
        } else {
            convertView.getTag();
        }
        return convertView;

    }
    private void init(){
        myRef = FirebaseDatabase.getInstance().getReference();
        myPref = getContext().getSharedPreferences("data",0);
    }
    private void showDeleteFrRequestDialog(String username, int position) {
            new MaterialAlertDialogBuilder(getContext())
                    .setTitle("Отклонить запрос  " + username + "?")
                    .setMessage("Вы действительно хотите отклонить запрос " + username + "?")
                    .setNegativeButton("Нет", (dialog, which) -> dialog.cancel())
                    .setPositiveButton("Да", (dialog, which) -> {
                        dialog.dismiss();
                        myRef.child("Users").child(myPref.getString("userID", "")).child("userFrRequests").child(Objects.requireNonNull(getItem(position))).removeValue();
                        Toast.makeText(getContext(), "Запрос " + username + " отклонён", Toast.LENGTH_SHORT).show();
                    })
                    .show();

    }
}
class ViewHolderR{
    TextView username;
    TextView userFrID;
    ImageButton accept;
    ImageButton notAccept;
}
