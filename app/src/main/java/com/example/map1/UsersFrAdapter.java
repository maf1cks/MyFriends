package com.example.map1;

import android.content.Context;
import android.content.SharedPreferences;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class UsersFrAdapter extends ArrayAdapter<String> {
    private final int layout;
    private SharedPreferences myPref;
    private DatabaseReference myRef;

    public UsersFrAdapter(Context context, int resourse, List<String> objects) {
        super(context, resourse, objects);
        layout = resourse;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
        init();
        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView = inflater.inflate(layout, parent, false);
            ViewHolder viewHolder = new ViewHolder();
            viewHolder.username = convertView.findViewById(R.id.tv_usernameUFA);
            viewHolder.userFrID = convertView.findViewById(R.id.tv_userFrIDUFA);
            viewHolder.deleteFr = convertView.findViewById(R.id.deleteFrUFA);
            viewHolder.locFr = convertView.findViewById(R.id.locFrUFA);
            if (NetworkUtils.isInternetAvailable(getContext())) {
                myRef.child("Users").child(Objects.requireNonNull(getItem(position))).get().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Map<String, Object> base = (Map<String, Object>) task.getResult().getValue();
                        viewHolder.username.setText(Objects.requireNonNull(Objects.requireNonNull(base).get("username")).toString());
                        viewHolder.userFrID.setText(Objects.requireNonNull(base.get("userFrID")).toString());
                    }
                });
                viewHolder.deleteFr.setOnClickListener(v -> myRef.child("Users").child(Objects.requireNonNull(getItem(position))).get().addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Map<String, Object> base = (Map<String, Object>) task.getResult().getValue();
                        showDeleteFrDialog(Objects.requireNonNull(Objects.requireNonNull(base).get("username")).toString(), position);
                    }
                }));
                viewHolder.locFr.setOnClickListener(v -> {
                    SharedPreferences.Editor ed = myPref.edit();
                    myRef.child("Users").child(Objects.requireNonNull(getItem(position))).child("geo").get().addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            ArrayList<Double> geo = (ArrayList<Double>) task.getResult().getValue();
                            if (geo == null) {
                                Toast.makeText(getContext(), "Местоположение недоступно", Toast.LENGTH_SHORT).show();
                            } else {
                                ed.putString("frLatitude", Objects.requireNonNull(geo).get(0).toString());
                                ed.putString("frLongitude", geo.get(1).toString());
                                ed.apply();
                                UserFriends.act.finish();
                            }
                        }
                    });
                });
            }
            convertView.setTag(viewHolder);
        } else {
            convertView.getTag();
        }
        return convertView;

    }

    private void init() {
        myRef = FirebaseDatabase.getInstance().getReference();
        myPref = getContext().getSharedPreferences("data", 0);
    }

    private void showDeleteFrDialog(String username,int position) {
            new MaterialAlertDialogBuilder(getContext())
                    .setTitle("Удалить " + username + "?")
                    .setMessage("Вы действительно хотите удалить " + username + " из друзей?")
                    .setNegativeButton("Нет", (dialog, which) -> dialog.cancel())
                    .setPositiveButton("Да", (dialog, which) -> {
                        dialog.dismiss();
                        myRef.child("Users").child(myPref.getString("userID", "")).child("userFr").child(getItem(position)).removeValue();
                        myRef.child("Users").child(getItem(position)).child("userFr").child(myPref.getString("userID", "")).removeValue();
                        Toast.makeText(getContext(), username + " удален(а) из друзей", Toast.LENGTH_SHORT).show();
                    })
                    .show();

    }
}
class ViewHolder{
    TextView username;
    TextView userFrID;
    ImageButton deleteFr;
    ImageButton locFr;
}
