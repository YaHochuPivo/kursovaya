package com.example.kurso;

import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.FragmentManager;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {
    private FrameLayout fragmentContainer;
    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        fragmentContainer = findViewById(R.id.fragmentContainer);
        bottomNav = findViewById(R.id.bottomNav);

        // Слушатель для отслеживания изменений в стеке фрагментов
        getSupportFragmentManager().addOnBackStackChangedListener(() -> {
            int backStackCount = getSupportFragmentManager().getBackStackEntryCount();
            if (backStackCount == 0 && fragmentContainer.getVisibility() == View.VISIBLE) {
                showBottomNavigation();
            }
        });
    }

    @Override
    public void onBackPressed() {
        if (fragmentContainer.getVisibility() == View.VISIBLE) {
            FragmentManager fm = getSupportFragmentManager();
            if (fm.getBackStackEntryCount() > 0) {
                fm.popBackStackImmediate();
                if (fm.getBackStackEntryCount() == 0) {
                    showBottomNavigation();
                }
            } else {
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
            bottomNav.setSelectedItemId(R.id.nav_settings);
        }
        if (fragmentContainer != null) {
            fragmentContainer.setVisibility(View.GONE);
        }
    }
}
