package com.example.kurso;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.AxisBase;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class FriendMoodHistoryFragment extends Fragment {
    private static final String ARG_USER_ID = "userId";
    private String userId;
    private LineChart moodChart;
    private Spinner periodSpinner;
    private FirebaseFirestore db;
    private SimpleDateFormat dateFormat;

    // Создаем пользовательские форматтеры
    private class DateAxisFormatter implements IAxisValueFormatter {
        @Override
        public String getFormattedValue(float value, AxisBase axis) {
            Calendar cal = Calendar.getInstance();
            cal.add(Calendar.DAY_OF_YEAR, -(int)value);
            return dateFormat.format(cal.getTime());
        }
    }

    private class MoodAxisFormatter implements IAxisValueFormatter {
        @Override
        public String getFormattedValue(float value, AxisBase axis) {
            switch ((int)value) {
                case 1: return "Очень плохо";
                case 2: return "Плохо";
                case 3: return "Нейтрально";
                case 4: return "Хорошо";
                case 5: return "Отлично";
                default: return "";
            }
        }
    }

    public static FriendMoodHistoryFragment newInstance(String userId) {
        FriendMoodHistoryFragment fragment = new FriendMoodHistoryFragment();
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
        dateFormat = new SimpleDateFormat("dd.MM", Locale.getDefault());
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_friend_mood_history, container, false);

        moodChart = view.findViewById(R.id.moodChart);
        periodSpinner = view.findViewById(R.id.periodSpinner);

        setupChart();
        setupSpinner();

        return view;
    }

    private void setupChart() {
        moodChart.setDescription(null);
        moodChart.setDrawGridBackground(false);
        moodChart.setTouchEnabled(true);
        moodChart.setDragEnabled(true);
        moodChart.setScaleEnabled(true);
        moodChart.setPinchZoom(true);
        moodChart.setNoDataText("Нет данных для отображения");
        moodChart.setNoDataTextColor(getTextColorForCurrentTheme());

        // Настройка оси X
        XAxis xAxis = moodChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        xAxis.setTextColor(getTextColorForCurrentTheme());
        xAxis.setValueFormatter(new DateAxisFormatter());

        // Настройка оси Y
        YAxis leftAxis = moodChart.getAxisLeft();
        leftAxis.setDrawGridLines(true);
        leftAxis.setGranularity(1f);
        leftAxis.setTextColor(getTextColorForCurrentTheme());
        leftAxis.setAxisMinimum(0f);
        leftAxis.setAxisMaximum(6f);
        leftAxis.setValueFormatter(new MoodAxisFormatter());

        moodChart.getAxisRight().setEnabled(false);
        moodChart.getLegend().setTextColor(getTextColorForCurrentTheme());
    }

    private void setupSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(requireContext(),
                R.array.period_array, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        periodSpinner.setAdapter(adapter);

        periodSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                loadMoodHistory(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    private void loadMoodHistory(int periodPosition) {
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
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                List<Entry> entries = new ArrayList<>();
                int dayCount = 0;
                Date lastDate = null;

                for (var doc : queryDocumentSnapshots) {
                    Note note = doc.toObject(Note.class);
                    Date noteDate = new Date(note.getCreatedAt());
                    
                    if (lastDate == null || !isSameDay(lastDate, noteDate)) {
                        dayCount++;
                        lastDate = noteDate;
                    }

                    float moodValue = getMoodValue(note.getMood());
                    entries.add(new Entry(dayCount, moodValue));
                }

                updateChart(entries);
            });
    }

    private boolean isSameDay(Date date1, Date date2) {
        Calendar cal1 = Calendar.getInstance();
        Calendar cal2 = Calendar.getInstance();
        cal1.setTime(date1);
        cal2.setTime(date2);
        return cal1.get(Calendar.YEAR) == cal2.get(Calendar.YEAR) &&
               cal1.get(Calendar.DAY_OF_YEAR) == cal2.get(Calendar.DAY_OF_YEAR);
    }

    private float getMoodValue(String mood) {
        if (mood == null) return 3f;
        String moodLower = mood.toLowerCase();
        if (moodLower.contains("отличн") || moodLower.contains("прекрасн")) return 5f;
        if (moodLower.contains("хорош")) return 4f;
        if (moodLower.contains("нейтральн")) return 3f;
        if (moodLower.contains("плох")) return 2f;
        if (moodLower.contains("ужасн") || moodLower.contains("очень плох")) return 1f;
        return 3f;
    }

    private void updateChart(List<Entry> entries) {
        if (entries.isEmpty()) {
            moodChart.clear();
            moodChart.invalidate();
            return;
        }

        LineDataSet dataSet = new LineDataSet(entries, "Настроение");
        dataSet.setColor(Color.rgb(89, 199, 250));
        dataSet.setCircleColor(Color.rgb(89, 199, 250));
        dataSet.setLineWidth(2f);
        dataSet.setCircleRadius(4f);
        dataSet.setDrawCircleHole(false);
        dataSet.setValueTextSize(10f);
        dataSet.setValueTextColor(getTextColorForCurrentTheme());
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(Color.rgb(89, 199, 250));
        dataSet.setFillAlpha(50);

        LineData data = new LineData(dataSet);
        moodChart.setData(data);
        moodChart.invalidate();
    }

    private int getTextColorForCurrentTheme() {
        return requireContext().getResources().getColor(R.color.md_theme_light_onSurface);
    }
} 