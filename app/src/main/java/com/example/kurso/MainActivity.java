package com.example.kurso;

import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;
import androidx.navigation.ui.NavigationUI;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {
    private FrameLayout fragmentContainer;
    private NavController navController;
    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        fragmentContainer = findViewById(R.id.fragmentContainer);
        bottomNav = findViewById(R.id.bottomNav);

        NavHostFragment navHostFragment = (NavHostFragment) getSupportFragmentManager()
                .findFragmentById(R.id.nav_host_fragment);
        if (navHostFragment != null) {
            navController = navHostFragment.getNavController();
            NavigationUI.setupWithNavController(bottomNav, navController);
        }

        // Слушатель для отслеживания изменений в стеке фрагментов
        getSupportFragmentManager().addOnBackStackChangedListener(() -> {
            int backStackCount = getSupportFragmentManager().getBackStackEntryCount();
            if (backStackCount == 0 && fragmentContainer.getVisibility() == View.VISIBLE) {
                // Если стек пуст и контейнер фрагментов видим, возвращаемся в настройки
                fragmentContainer.setVisibility(View.GONE);
                bottomNav.setVisibility(View.VISIBLE);
                bottomNav.setSelectedItemId(R.id.nav_settings);
            }
        });

        // Слушатель для нижней навигации
        bottomNav.setOnItemSelectedListener(item -> {
            if (fragmentContainer.getVisibility() == View.VISIBLE) {
                fragmentContainer.setVisibility(View.GONE);
            }
            return NavigationUI.onNavDestinationSelected(item, navController);
        });
    }

    @Override
    public void onBackPressed() {
        if (fragmentContainer.getVisibility() == View.VISIBLE) {
            FragmentManager fm = getSupportFragmentManager();
            if (fm.getBackStackEntryCount() > 0) {
                fm.popBackStackImmediate();
                if (fm.getBackStackEntryCount() == 0) {
                    // Если после popBackStack стек пуст, возвращаемся в настройки
                    fragmentContainer.setVisibility(View.GONE);
                    bottomNav.setVisibility(View.VISIBLE);
                    bottomNav.setSelectedItemId(R.id.nav_settings);
                }
            } else {
                fragmentContainer.setVisibility(View.GONE);
                bottomNav.setVisibility(View.VISIBLE);
                bottomNav.setSelectedItemId(R.id.nav_settings);
            }
        } else {
            super.onBackPressed();
        }
    }
}
