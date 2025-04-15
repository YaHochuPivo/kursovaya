package com.example.kurso;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FriendStatisticsFragment extends Fragment {
    private static final String ARG_USER_ID = "userId";

    private String userId;
    private PieChart moodChart;
    private TextView totalNotesText;
    private TextView averageMoodText;
    private Spinner periodSpinner;
    private FirebaseFirestore db;

    public static FriendStatisticsFragment newInstance(String userId) {
        FriendStatisticsFragment fragment = new FriendStatisticsFragment();
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
        View view = inflater.inflate(R.layout.fragment_friend_statistics, container, false);

        moodChart = view.findViewById(R.id.moodChart);
        totalNotesText = view.findViewById(R.id.totalNotesText);
        averageMoodText = view.findViewById(R.id.averageMoodText);
        periodSpinner = view.findViewById(R.id.periodSpinner);

        setupChart();
        setupSpinner();

        return view;
    }

    private void setupChart() {
        moodChart.setDescription(null);
        moodChart.setHoleRadius(50f);
        moodChart.setTransparentCircleRadius(55f);
        moodChart.setDrawHoleEnabled(true);
        moodChart.setRotationEnabled(true);
        moodChart.setHighlightPerTapEnabled(true);
        
        int textColor = getTextColorForCurrentTheme();
        moodChart.setEntryLabelColor(textColor);
        moodChart.setEntryLabelTextSize(12f);
        moodChart.setNoDataText("Нет данных для отображения");
        moodChart.setNoDataTextColor(textColor);
        moodChart.getLegend().setTextColor(textColor);
    }

    private void setupSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(requireContext(),
                R.array.period_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        periodSpinner.setAdapter(adapter);

        periodSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                loadStatistics(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void loadStatistics(int periodPosition) {
        if (userId == null) return;

        Calendar calendar = Calendar.getInstance();
        Calendar startDate = Calendar.getInstance();

        switch (periodPosition) {
            case 0: // Неделя
                startDate.add(Calendar.DAY_OF_YEAR, -7);
                break;
            case 1: // Месяц
                startDate.add(Calendar.MONTH, -1);
                break;
            case 2: // Год
                startDate.add(Calendar.YEAR, -1);
                break;
        }

        db.collection("notes")
            .whereEqualTo("userId", userId)
            .whereGreaterThan("createdAt", startDate.getTimeInMillis())
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                List<Note> notes = new ArrayList<>();
                for (var doc : queryDocumentSnapshots) {
                    notes.add(doc.toObject(Note.class));
                }
                updateStatistics(notes);
            });
    }

    private void updateStatistics(List<Note> notes) {
        totalNotesText.setText(getString(R.string.total_notes_format, notes.size()));

        Map<String, Integer> moodCounts = new HashMap<>();
        for (Note note : notes) {
            String mood = note.getMood() != null ? note.getMood() : "Не указано";
            moodCounts.put(mood, moodCounts.getOrDefault(mood, 0) + 1);
        }

        // Находим преобладающее настроение
        String predominantMood = "";
        int maxCount = 0;
        for (Map.Entry<String, Integer> entry : moodCounts.entrySet()) {
            if (entry.getValue() > maxCount) {
                maxCount = entry.getValue();
                predominantMood = entry.getKey();
            }
        }

        averageMoodText.setText(getString(R.string.predominant_mood_format, 
            predominantMood.isEmpty() ? "Не определено" : predominantMood));

        updateMoodChart(moodCounts);
    }

    private void updateMoodChart(Map<String, Integer> moodCounts) {
        List<PieEntry> entries = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();

        for (Map.Entry<String, Integer> entry : moodCounts.entrySet()) {
            entries.add(new PieEntry(entry.getValue(), entry.getKey()));
            colors.add(getMoodColor(entry.getKey()));
        }

        PieDataSet dataSet = new PieDataSet(entries, "Настроения");
        dataSet.setColors(colors);
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(getTextColorForCurrentTheme());
        dataSet.setXValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE);
        dataSet.setYValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE);
        dataSet.setValueLinePart1Length(0.4f);
        dataSet.setValueLinePart2Length(0.4f);

        PieData data = new PieData(dataSet);
        data.setValueTextSize(12f);
        data.setValueTextColor(getTextColorForCurrentTheme());

        moodChart.setData(data);
        moodChart.invalidate();
    }

    private int getMoodColor(String mood) {
        String moodLower = mood.toLowerCase();
        if (moodLower.contains("счастлив") || moodLower.contains("радост")) {
            return Color.rgb(255, 223, 0); // Желтый
        } else if (moodLower.contains("грустн") || moodLower.contains("печаль")) {
            return Color.rgb(51, 153, 255); // Синий
        } else if (moodLower.contains("зл")) {
            return Color.rgb(255, 51, 51); // Красный
        } else if (moodLower.contains("спокой")) {
            return Color.rgb(102, 255, 102); // Зеленый
        } else if (moodLower.contains("нейтраль")) {
            return Color.rgb(147, 112, 219); // Светло-фиолетовый
        } else if (moodLower.contains("весел")) {
            return Color.rgb(255, 140, 0); // Оранжевый
        } else {
            return Color.rgb(169, 169, 169); // Серый для неопределенных настроений
        }
    }

    private int getTextColorForCurrentTheme() {
        int nightModeFlags = requireContext().getResources().getConfiguration().uiMode & 
                            android.content.res.Configuration.UI_MODE_NIGHT_MASK;
        return nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES ? 
               Color.WHITE : Color.BLACK;
    }
} 