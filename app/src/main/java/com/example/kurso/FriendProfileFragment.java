package com.example.kurso;

import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import com.bumptech.glide.Glide;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.android.material.button.MaterialButton;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class FriendProfileFragment extends Fragment {
    private static final String TAG = "FriendProfileFragment";
    private static final String ARG_USER_ID = "userId";
    
    private ImageView profileImageView;
    private TextView displayNameText;
    private TextView emailText;
    private TextView bioText;
    private ImageButton backButton;
    private MaterialButton giftButton;
    private FirebaseFirestore db;
    private String userId;
    private LineChart moodChart;
    private TextView dominantMoodText;
    private TextView totalEntriesText;
    private RecyclerView giftsRecyclerView;
    private GiftAdapter giftAdapter;
    private List<Gift> gifts = new ArrayList<>();

    public static FriendProfileFragment newInstance(String userId) {
        FriendProfileFragment fragment = new FriendProfileFragment();
        Bundle args = new Bundle();
        args.putString(ARG_USER_ID, userId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            userId = getArguments().getString(ARG_USER_ID);
        }
        db = FirebaseFirestore.getInstance();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_friend_profile, container, false);

        // Скрываем нижнюю навигацию
        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).hideBottomNavigation();
        }

        // Инициализация компонентов
        profileImageView = view.findViewById(R.id.profileImageView);
        displayNameText = view.findViewById(R.id.displayNameText);
        emailText = view.findViewById(R.id.emailText);
        bioText = view.findViewById(R.id.bioText);
        backButton = view.findViewById(R.id.backButton);
        moodChart = view.findViewById(R.id.moodChart);
        dominantMoodText = view.findViewById(R.id.dominantMoodText);
        totalEntriesText = view.findViewById(R.id.totalEntriesText);
        giftButton = view.findViewById(R.id.giftButton);

        // Настройка кнопки подарка
        giftButton.setOnClickListener(v -> {
            if (userId != null) {
                GiftDialogFragment giftDialog = GiftDialogFragment.newInstance(userId);
                giftDialog.show(getParentFragmentManager(), "gift_dialog");
            }
        });

        // Настройка кнопки "Назад"
        backButton.setOnClickListener(v -> requireActivity().onBackPressed());
        
        // Настройка графика
        setupMoodChart();
        
        // Загрузка информации о пользователе
        loadUserInfo();
        
        // Загрузка данных настроения
        loadMoodData();

        // Добавляем фрагмент со статистикой
        if (userId != null) {
            FriendStatisticsFragment statisticsFragment = FriendStatisticsFragment.newInstance(userId);
            getChildFragmentManager()
                .beginTransaction()
                .replace(R.id.statisticsContainer, statisticsFragment)
                .commit();
        }

        // Инициализация списка подарков
        giftsRecyclerView = view.findViewById(R.id.giftsRecyclerView);
        giftAdapter = new GiftAdapter(gifts);
        giftsRecyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 3));
        giftsRecyclerView.setAdapter(giftAdapter);
        
        // Загрузка подарков
        loadGifts();

        return view;
    }

    private void setupMoodChart() {
        moodChart.getDescription().setEnabled(false);
        moodChart.setTouchEnabled(true);
        moodChart.setDragEnabled(true);
        moodChart.setScaleEnabled(true);
        moodChart.setPinchZoom(true);
        moodChart.setDrawGridBackground(false);

        XAxis xAxis = moodChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setTextColor(ContextCompat.getColor(requireContext(), R.color.md_theme_light_onSurface));

        YAxis leftAxis = moodChart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setAxisMinimum(1f);
        leftAxis.setAxisMaximum(5f);
        leftAxis.setGranularity(1f);
        leftAxis.setTextColor(ContextCompat.getColor(requireContext(), R.color.md_theme_light_onSurface));

        moodChart.getAxisRight().setEnabled(false);
        moodChart.getLegend().setEnabled(false);
    }

    private void loadMoodData() {
        if (userId == null) {
            Log.e(TAG, "userId is null");
            moodChart.setNoDataText("Ошибка загрузки данных");
            return;
        }

        Log.d(TAG, "Loading mood data for user: " + userId);

        // Получаем текущую дату
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 23);
        calendar.set(Calendar.MINUTE, 59);
        calendar.set(Calendar.SECOND, 59);
        Date endDate = calendar.getTime();

        // Получаем дату 7 дней назад
        calendar.add(Calendar.DAY_OF_YEAR, -7);
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        Date startDate = calendar.getTime();

        Log.d(TAG, "Date range: " + startDate + " to " + endDate);

        // Загружаем заметки пользователя
        db.collection("notes")
            .whereEqualTo("userId", userId)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                Log.d(TAG, "Found " + queryDocumentSnapshots.size() + " notes");
                
                List<Entry> entries = new ArrayList<>();
                List<String> dates = new ArrayList<>();
                Map<Integer, Integer> moodCounts = new HashMap<>();
                
                int index = 0;
                for (var doc : queryDocumentSnapshots) {
                    Note note = doc.toObject(Note.class);
                    if (note == null || note.getMood() == null || note.getCreatedAt() == 0) {
                        continue;
                    }

                    long timestamp = note.getCreatedAt();
                    String mood = note.getMood();
                    
                    // Проверяем, попадает ли запись в диапазон последних 7 дней
                    if (timestamp >= startDate.getTime() && timestamp <= endDate.getTime()) {
                        int moodValue = getMoodValue(mood);
                        if (moodValue > 0) {
                            Log.d(TAG, "Adding mood entry - date: " + new Date(timestamp) + ", mood: " + mood + ", value: " + moodValue);
                            entries.add(new Entry(index, moodValue));
                            dates.add(new SimpleDateFormat("dd.MM", Locale.getDefault()).format(new Date(timestamp)));
                            moodCounts.put(moodValue, moodCounts.getOrDefault(moodValue, 0) + 1);
                            index++;
                        }
                    }
                }

                Log.d(TAG, "Filtered entries count: " + entries.size());
                Log.d(TAG, "Mood counts: " + moodCounts);

                // Обновляем общее количество записей
                totalEntriesText.setText(getString(R.string.total_notes_format, entries.size()));

                // Определяем преобладающее настроение
                int maxCount = 0;
                int dominantMood = 0;
                for (Map.Entry<Integer, Integer> entry : moodCounts.entrySet()) {
                    if (entry.getValue() > maxCount) {
                        maxCount = entry.getValue();
                        dominantMood = entry.getKey();
                    }
                }

                Log.d(TAG, "Dominant mood: " + dominantMood + " with count: " + maxCount);

                // Устанавливаем текст преобладающего настроения
                String dominantMoodText = getMoodText(dominantMood);
                this.dominantMoodText.setText(getString(R.string.predominant_mood_format, dominantMoodText));

                if (!entries.isEmpty()) {
                    LineDataSet dataSet = new LineDataSet(entries, "Настроение");
                    int colorPrimary = ContextCompat.getColor(requireContext(), R.color.md_theme_light_primary);
                    dataSet.setColor(colorPrimary);
                    dataSet.setCircleColor(colorPrimary);
                    dataSet.setLineWidth(2f);
                    dataSet.setCircleRadius(4f);
                    dataSet.setDrawValues(true);
                    dataSet.setValueTextSize(12f);
                    dataSet.setValueTextColor(ContextCompat.getColor(requireContext(), R.color.md_theme_light_onSurface));
                    dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
                    
                    LineData lineData = new LineData(dataSet);
                    moodChart.setData(lineData);
                    
                    XAxis xAxis = moodChart.getXAxis();
                    xAxis.setValueFormatter(new IndexAxisValueFormatter(dates));
                    xAxis.setLabelRotationAngle(-45);
                    xAxis.setGranularity(1f);
                    xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
                    xAxis.setDrawGridLines(false);
                    
                    YAxis leftAxis = moodChart.getAxisLeft();
                    leftAxis.setAxisMinimum(0.5f);
                    leftAxis.setAxisMaximum(5.5f);
                    leftAxis.setGranularity(1f);
                    
                    moodChart.getAxisRight().setEnabled(false);
                    moodChart.getLegend().setEnabled(false);
                    moodChart.getDescription().setEnabled(false);
                    
                    moodChart.animateX(1000);
                    moodChart.invalidate();
                } else {
                    moodChart.setNoDataText("Нет данных о настроении за последние 7 дней");
                    moodChart.setNoDataTextColor(ContextCompat.getColor(requireContext(), R.color.md_theme_light_onSurface));
                    moodChart.invalidate();
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error loading mood data", e);
                moodChart.setNoDataText("Ошибка загрузки данных");
                moodChart.setNoDataTextColor(ContextCompat.getColor(requireContext(), R.color.md_theme_light_onSurface));
                moodChart.invalidate();
                Toast.makeText(requireContext(), "Ошибка загрузки данных: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }

    private int getMoodValue(String mood) {
        switch (mood.toLowerCase()) {
            case "злой": return 1;
            case "грустный": return 2;
            case "нейтральный": return 3;
            case "счастливый": return 4;
            case "веселый": return 5;
            default: return -1;
        }
    }

    private String getMoodText(int moodValue) {
        switch (moodValue) {
            case 1:
                return "Злой";
            case 2:
                return "Грустный";
            case 3:
                return "Нейтральный";
            case 4:
                return "Счастливый";
            case 5:
                return "Весёлый";
            default:
                return "Неизвестно";
        }
    }

    private void loadUserInfo() {
        if (userId == null) return;

        db.collection("users").document(userId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                User user = documentSnapshot.toObject(User.class);
                if (user != null) {
                    displayNameText.setText(getString(R.string.name_format, user.getDisplayName()));
                    emailText.setText(getString(R.string.email_format, user.getEmail()));
                    bioText.setText(getString(R.string.bio_format, user.getBio() != null ? user.getBio() : "Не указано"));

                    // Загрузка фото профиля
                    if (user.getProfileImagePath() != null && !user.getProfileImagePath().isEmpty()) {
                        File imageFile = new File(user.getProfileImagePath());
                        if (imageFile.exists()) {
                            Glide.with(this)
                                .load(Uri.fromFile(imageFile))
                                .placeholder(R.drawable.default_avatar)
                                .error(R.drawable.default_avatar)
                                .circleCrop()
                                .into(profileImageView);
                        } else {
                            profileImageView.setImageResource(R.drawable.default_avatar);
                        }
                    } else {
                        profileImageView.setImageResource(R.drawable.default_avatar);
                    }
                }
            });
    }

    private void loadGifts() {
        if (userId == null) {
            Log.e(TAG, "Cannot load gifts: userId is null");
            return;
        }

        Log.d(TAG, "Loading gifts for user: " + userId);
        
        db.collection("gifts")
            .whereEqualTo("toUserId", userId)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                Log.d(TAG, "Found " + queryDocumentSnapshots.size() + " gifts");
                gifts.clear();
                for (var doc : queryDocumentSnapshots) {
                    Gift gift = doc.toObject(Gift.class);
                    if (gift != null) {
                        Log.d(TAG, "Adding gift: type=" + gift.getGiftType() + 
                            ", from=" + gift.getFromUserDisplayName());
                        gifts.add(gift);
                    }
                }
                // Сортируем подарки по времени локально
                gifts.sort((g1, g2) -> Long.compare(g2.getTimestamp(), g1.getTimestamp()));
                giftAdapter.updateGifts(gifts);
                Log.d(TAG, "Updated gifts adapter with " + gifts.size() + " gifts");
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error loading gifts", e);
                Toast.makeText(requireContext(), "Ошибка при загрузке подарков: " + e.getMessage(), 
                    Toast.LENGTH_SHORT).show();
            });
    }
} 