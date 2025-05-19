package com.example.map1;

import android.app.Application;

import com.yandex.mapkit.MapKitFactory;

public class MainApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        String MAPKIT_API_KEY = "7286addb-fc0c-4f7d-9b86-15cf86fe2396";
        MapKitFactory.setApiKey(MAPKIT_API_KEY);
    }
}
