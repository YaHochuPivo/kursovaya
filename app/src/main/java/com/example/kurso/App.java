package com.example.kurso;

import android.app.Application;
import androidx.appcompat.app.AppCompatDelegate;

public class App extends Application {
    private static final String THEME_PREF = "theme_preferences";
    private static final String THEME_KEY = "is_dark_theme";

    @Override
    public void onCreate() {
        super.onCreate();
        
        // Загружаем сохраненную тему
        boolean isDarkTheme = getSharedPreferences(THEME_PREF, MODE_PRIVATE)
                .getBoolean(THEME_KEY, false);
        
        // Применяем тему
        AppCompatDelegate.setDefaultNightMode(
            isDarkTheme ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
        );
    }
} 