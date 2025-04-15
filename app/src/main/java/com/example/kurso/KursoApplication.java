package com.example.kurso;

import android.app.Application;
import com.google.android.gms.common.GoogleApiAvailability;
import android.util.Log;
import com.google.firebase.FirebaseApp;

public class KursoApplication extends Application {
    private static final String TAG = "KursoApplication";

    @Override
    public void onCreate() {
        super.onCreate();
        
        // Проверяем доступность Google Play Services
        int resultCode = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(this);
        if (resultCode != com.google.android.gms.common.ConnectionResult.SUCCESS) {
            Log.e(TAG, "Google Play Services не доступны: " + resultCode);
        } else {
            Log.d(TAG, "Google Play Services доступны");
        }

        // Инициализируем Firebase
        FirebaseApp.initializeApp(this);
        Log.d(TAG, "Firebase инициализирован");
    }
} 