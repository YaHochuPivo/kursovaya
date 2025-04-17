package com.example.kurso;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentManager;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {
    private FrameLayout fragmentContainer;
    private BottomNavigationView bottomNav;
    private NavController navController;
    private ActivityResultLauncher<String> requestPermissionLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        fragmentContainer = findViewById(R.id.fragmentContainer);
        bottomNav = findViewById(R.id.bottomNav);

        // Инициализация Navigation Component
        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();
            // Настройка связи между BottomNavigationView и NavController
            NavigationUI.setupWithNavController(bottomNav, navController);

            // Слушатель для изменения видимости нижней навигации
            navController.addOnDestinationChangedListener((controller, destination, arguments) -> {
                // Скрываем нижнюю навигацию для определенных фрагментов
                if (destination.getId() == R.id.friendProfileFragment || 
                    destination.getId() == R.id.friendMoodHistoryFragment) {
                    hideBottomNavigation();
                } else {
                    showBottomNavigation();
                }
            });
        }

        // Инициализация запроса разрешений
        requestPermissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(),
            isGranted -> {
                if (isGranted) {
                    // Разрешение получено, инициализируем уведомления
                    initializeNotifications();
                }
            }
        );

        // Проверяем и запрашиваем разрешение на уведомления
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                requestPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS);
            } else {
                initializeNotifications();
            }
        } else {
            initializeNotifications();
        }
    }

    private void initializeNotifications() {
        // Инициализация уведомлений
        NotificationHelper.createNotificationChannel(this);
        
        // Проверяем настройки уведомлений
        NotificationHelper notificationHelper = new NotificationHelper(this);
        if (notificationHelper.isReminderEnabled()) {
            // Если уведомления включены, планируем их с сохраненным временем
            notificationHelper.scheduleReminder(
                notificationHelper.getReminderHour(),
                notificationHelper.getReminderMinute()
            );
        }
    }

    @Override
    public void onBackPressed() {
        if (fragmentContainer.getVisibility() == View.VISIBLE) {
            if (!navController.navigateUp()) {
                showBottomNavigation();
            }
        } else {
            super.onBackPressed();
        }
    }

    public void hideBottomNavigation() {
        if (bottomNav != null) {
            bottomNav.setVisibility(View.GONE);
        }
        if (fragmentContainer != null) {
            fragmentContainer.setVisibility(View.VISIBLE);
        }
    }

    public void showBottomNavigation() {
        if (bottomNav != null) {
            bottomNav.setVisibility(View.VISIBLE);
        }
        if (fragmentContainer != null) {
            fragmentContainer.setVisibility(View.GONE);
        }
    }
}
