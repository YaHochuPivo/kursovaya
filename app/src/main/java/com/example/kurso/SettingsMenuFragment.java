package com.example.kurso;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

public class SettingsMenuFragment extends Fragment {
    private Button profileButton;
    private Button themeButton;
    private Button aboutButton;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings_menu, container, false);

        profileButton = view.findViewById(R.id.profileButton);
        themeButton = view.findViewById(R.id.themeButton);
        aboutButton = view.findViewById(R.id.aboutButton);

        profileButton.setOnClickListener(v -> openProfile());
        themeButton.setOnClickListener(v -> openThemeSettings());
        aboutButton.setOnClickListener(v -> openAbout());

        return view;
    }

    private void openProfile() {
        SettingsFragment profileFragment = new SettingsFragment();
        openFragment(profileFragment, "profile");
    }

    private void openThemeSettings() {
        AppSettingsFragment themeFragment = new AppSettingsFragment();
        openFragment(themeFragment, "theme");
    }

    private void openAbout() {
        // TODO: Реализовать экран "О приложении"
    }

    private void openFragment(Fragment fragment, String tag) {
        // Скрываем нижнюю навигацию
        View bottomNav = requireActivity().findViewById(R.id.bottomNav);
        if (bottomNav != null) {
            bottomNav.setVisibility(View.GONE);
        }

        // Показываем контейнер для фрагмента
        View fragmentContainer = requireActivity().findViewById(R.id.fragmentContainer);
        if (fragmentContainer != null) {
            fragmentContainer.setVisibility(View.VISIBLE);
        }

        // Открываем фрагмент
        FragmentTransaction transaction = requireActivity().getSupportFragmentManager().beginTransaction();
        transaction.replace(R.id.fragmentContainer, fragment);
        transaction.addToBackStack(tag);
        transaction.commit();
    }
} 