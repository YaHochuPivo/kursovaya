package com.example.kurso;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;

public class AppSettingsFragment extends Fragment {
    private static final String THEME_PREF = "theme_preferences";
    private static final String THEME_KEY = "is_dark_theme";
    private boolean isDarkTheme;
    private RadioGroup themeGroup;
    private RadioButton lightTheme;
    private RadioButton darkTheme;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Загружаем текущую тему
        isDarkTheme = requireContext().getSharedPreferences(THEME_PREF, Context.MODE_PRIVATE)
                .getBoolean(THEME_KEY, false);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_app_settings, container, false);

        // Инициализация компонентов
        themeGroup = view.findViewById(R.id.themeGroup);
        lightTheme = view.findViewById(R.id.lightTheme);
        darkTheme = view.findViewById(R.id.darkTheme);
        ImageButton backButton = view.findViewById(R.id.backButton);

        // Устанавливаем текущую тему
        if (isDarkTheme) {
            darkTheme.setChecked(true);
        } else {
            lightTheme.setChecked(true);
        }

        // Обработчик изменения темы
        themeGroup.setOnCheckedChangeListener((group, checkedId) -> {
            isDarkTheme = checkedId == R.id.darkTheme;
            
            // Сохраняем выбранную тему
            requireContext().getSharedPreferences(THEME_PREF, Context.MODE_PRIVATE)
                    .edit()
                    .putBoolean(THEME_KEY, isDarkTheme)
                    .apply();

            // Применяем тему
            AppCompatDelegate.setDefaultNightMode(
                isDarkTheme ? AppCompatDelegate.MODE_NIGHT_YES : AppCompatDelegate.MODE_NIGHT_NO
            );

            // Показываем уведомление
            Toast.makeText(requireContext(), 
                getString(isDarkTheme ? R.string.dark_theme_enabled : R.string.light_theme_enabled), 
                Toast.LENGTH_SHORT).show();
        });

        // Обработчик кнопки "Назад"
        backButton.setOnClickListener(v -> {
            requireActivity().getSupportFragmentManager().popBackStack();
        });

        return view;
    }
}