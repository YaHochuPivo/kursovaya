package com.example.kurso;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import androidx.fragment.app.Fragment;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IValueFormatter;
import com.github.mikephil.charting.formatter.IAxisValueFormatter;
import com.github.mikephil.charting.utils.ViewPortHandler;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.Timestamp;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class StatisticsFragment extends Fragment {
    private PieChart moodChart;
    private LineChart moodLineChart;
    private BarChart notesChart;
    private Spinner periodSpinner;
    private TextView totalNotesText, averageMoodText;
    private FirebaseFirestore db;
    private String userId;
    private BroadcastReceiver updateReceiver;

    private final Map<String, Integer> moodValues = new HashMap<String, Integer>() {{
        put("Грустный", 1);
        put("Злой", 2);
        put("Нейтральный", 3);
        put("Счастливый", 4);
        put("Веселый", 5);
    }};

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_statistics, container, false);

        // Инициализация компонентов
        moodChart = view.findViewById(R.id.moodChart);
        moodLineChart = view.findViewById(R.id.moodLineChart);
        notesChart = view.findViewById(R.id.notesChart);
        periodSpinner = view.findViewById(R.id.periodSpinner);
        totalNotesText = view.findViewById(R.id.totalNotesText);
        averageMoodText = view.findViewById(R.id.averageMoodText);

        db = FirebaseFirestore.getInstance();
        userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        setupSpinner();
        setupCharts();

        // Регистрируем BroadcastReceiver
        updateReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if ("com.example.kurso.UPDATE_STATISTICS".equals(intent.getAction())) {
                    loadStatistics(periodSpinner.getSelectedItemPosition());
                }
            }
        };
        IntentFilter filter = new IntentFilter("com.example.kurso.UPDATE_STATISTICS");
        requireContext().registerReceiver(updateReceiver, filter, Context.RECEIVER_NOT_EXPORTED);

        return view;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (updateReceiver != null) {
            requireContext().unregisterReceiver(updateReceiver);
        }
    }

    private void setupSpinner() {
        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getContext(),
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

    private void setupCharts() {
        int textColor = getTextColorForCurrentTheme();
        
        // Настройка графика настроений (круговая диаграмма)
        moodChart.setDescription(null);
        moodChart.setHoleRadius(50f);
        moodChart.setTransparentCircleRadius(55f);
        moodChart.setDrawHoleEnabled(true);
        moodChart.setRotationEnabled(true);
        moodChart.setHighlightPerTapEnabled(true);
        moodChart.setEntryLabelColor(textColor);
        moodChart.setEntryLabelTextSize(12f);
        moodChart.setNoDataText("Нет данных для отображения");
        moodChart.setNoDataTextColor(textColor);
        moodChart.getLegend().setTextColor(textColor);

        // Настройка линейного графика настроений
        moodLineChart.setDescription(null);
        moodLineChart.setDrawGridBackground(false);
        moodLineChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        moodLineChart.getXAxis().setGranularity(1f);
        moodLineChart.getAxisLeft().setAxisMinimum(0f);
        moodLineChart.getAxisLeft().setAxisMaximum(6f);
        moodLineChart.getAxisRight().setEnabled(false);
        moodLineChart.setNoDataText("Нет данных для отображения");
        moodLineChart.setNoDataTextColor(textColor);
        moodLineChart.getLegend().setEnabled(false);
        moodLineChart.getXAxis().setTextColor(textColor);
        moodLineChart.getAxisLeft().setTextColor(textColor);

        // Настройка графика заметок
        notesChart.setDescription(null);
        notesChart.setDrawGridBackground(false);
        notesChart.setDrawBarShadow(false);
        notesChart.setHighlightFullBarEnabled(false);
        notesChart.setDrawValueAboveBar(true);
        notesChart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        notesChart.getXAxis().setGranularity(1f);
        notesChart.getAxisLeft().setAxisMinimum(0f);
        notesChart.getAxisRight().setEnabled(false);
        notesChart.setNoDataText("Нет данных для отображения");
        notesChart.setNoDataTextColor(textColor);
        notesChart.getXAxis().setTextColor(textColor);
        notesChart.getAxisLeft().setTextColor(textColor);
        notesChart.getLegend().setTextColor(textColor);
    }

    private void loadStatistics(int periodPosition) {
        Calendar calendar = Calendar.getInstance();
        Calendar startDate = Calendar.getInstance();

        // Определение периода
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

        final long startTime = startDate.getTimeInMillis();
        android.util.Log.d("StatisticsFragment", "Loading statistics from: " + new Date(startTime));
        android.util.Log.d("StatisticsFragment", "Current user ID: " + userId);

        db.collection("notes")
                .whereEqualTo("userId", userId)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    android.util.Log.d("StatisticsFragment", "Query completed. Documents found: " + queryDocumentSnapshots.size());
                    
                    List<Note> notes = new ArrayList<>();
                    for (var doc : queryDocumentSnapshots) {
                        Note note = doc.toObject(Note.class);
                        if (note != null && note.getCreatedAt() > startTime) {
                            notes.add(note);
                            android.util.Log.d("StatisticsFragment", "Added note: createdAt=" + new Date(note.getCreatedAt()) + ", mood=" + note.getMood());
                        }
                    }
                    
                    android.util.Log.d("StatisticsFragment", "Total notes processed: " + notes.size());
                    
                    if (notes.isEmpty()) {
                        android.util.Log.d("StatisticsFragment", "No notes found, clearing charts");
                        moodChart.clear();
                        moodLineChart.clear();
                        notesChart.clear();
                        totalNotesText.setText("Всего заметок: 0");
                        averageMoodText.setText("Преобладающее настроение: -");
                        moodChart.invalidate();
                        moodLineChart.invalidate();
                        notesChart.invalidate();
                    } else {
                        updateStatistics(notes, periodPosition);
                    }
                })
                .addOnFailureListener(e -> {
                    android.util.Log.e("StatisticsFragment", "Error loading notes", e);
                    moodChart.clear();
                    moodLineChart.clear();
                    notesChart.clear();
                    totalNotesText.setText("Ошибка загрузки данных");
                    averageMoodText.setText("Преобладающее настроение: -");
                    moodChart.invalidate();
                    moodLineChart.invalidate();
                    notesChart.invalidate();
                });
    }

    private void updateStatistics(List<Note> notes, int periodPosition) {
        android.util.Log.d("StatisticsFragment", "Updating statistics with " + notes.size() + " notes");
        
        // Обновление общей статистики
        totalNotesText.setText("Всего заметок: " + notes.size());

        // Подсчет настроений
        Map<String, Integer> moodCounts = new HashMap<>();
        TreeMap<Long, Integer> moodTimeline = new TreeMap<>();
        
        for (Note note : notes) {
            String mood = note.getMood() != null ? note.getMood() : "Не указано";
            moodCounts.put(mood, moodCounts.getOrDefault(mood, 0) + 1);
            
            // Добавляем настроение в timeline
            if (note.getMood() != null && moodValues.containsKey(note.getMood())) {
                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis(note.getCreatedAt());
                // Устанавливаем время на начало дня
                cal.set(Calendar.HOUR_OF_DAY, 0);
                cal.set(Calendar.MINUTE, 0);
                cal.set(Calendar.SECOND, 0);
                cal.set(Calendar.MILLISECOND, 0);
                
                long dayStart = cal.getTimeInMillis();
                int moodValue = moodValues.get(note.getMood());
                
                // Если в этот день уже есть настроение, берем среднее
                if (moodTimeline.containsKey(dayStart)) {
                    int currentValue = moodTimeline.get(dayStart);
                    moodTimeline.put(dayStart, (currentValue + moodValue) / 2);
                } else {
                    moodTimeline.put(dayStart, moodValue);
                }
            }
        }

        android.util.Log.d("StatisticsFragment", "Mood distribution: " + moodCounts);

        // Находим преобладающее настроение
        int maxCount = 0;
        List<String> predominantMoods = new ArrayList<>();
        
        // Сначала находим максимальное количество
        for (Map.Entry<String, Integer> entry : moodCounts.entrySet()) {
            if (entry.getValue() > maxCount) {
                maxCount = entry.getValue();
            }
        }
        
        // Теперь собираем все настроения с максимальным количеством
        for (Map.Entry<String, Integer> entry : moodCounts.entrySet()) {
            if (entry.getValue() == maxCount) {
                predominantMoods.add(entry.getKey());
            }
        }
        
        // Формируем текст с преобладающим настроением
        String moodText;
        if (predominantMoods.isEmpty()) {
            moodText = "-";
        } else if (predominantMoods.size() == 1) {
            moodText = predominantMoods.get(0);
        } else {
            moodText = "Несколько (" + String.join(", ", predominantMoods) + ")";
        }
        
        averageMoodText.setText("Преобладающее настроение: " + moodText);

        // Обновление графиков
        updateMoodChart(moodCounts);
        updateMoodLineChart(moodTimeline, periodPosition);
        updateNotesChart(notes, periodPosition);
    }

    private void updateMoodChart(Map<String, Integer> moodCounts) {
        android.util.Log.d("StatisticsFragment", "Updating mood chart");
        List<PieEntry> entries = new ArrayList<>();
        List<Integer> colors = new ArrayList<>();

        if (moodCounts.isEmpty()) {
            android.util.Log.d("StatisticsFragment", "No mood entries to display");
            moodChart.clear();
            moodChart.invalidate();
            return;
        }

        for (Map.Entry<String, Integer> entry : moodCounts.entrySet()) {
            entries.add(new PieEntry(entry.getValue(), entry.getKey()));
            colors.add(getMoodColor(entry.getKey()));
            android.util.Log.d("StatisticsFragment", "Adding mood entry: " + entry.getKey() + " = " + entry.getValue());
        }

        int textColor = getTextColorForCurrentTheme();
        
        PieDataSet dataSet = new PieDataSet(entries, "Настроения");
        dataSet.setColors(colors);
        dataSet.setValueTextSize(12f);
        dataSet.setValueTextColor(textColor);
        dataSet.setXValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE);
        dataSet.setYValuePosition(PieDataSet.ValuePosition.OUTSIDE_SLICE);
        dataSet.setValueLinePart1Length(0.4f);
        dataSet.setValueLinePart2Length(0.4f);
        dataSet.setValueFormatter(new IValueFormatter() {
            @Override
            public String getFormattedValue(float value, Entry entry, int dataSetIndex, ViewPortHandler viewPortHandler) {
                return String.valueOf((int) value);
            }
        });

        PieData data = new PieData(dataSet);
        data.setValueTextSize(12f);
        data.setValueTextColor(textColor);
        
        moodChart.setData(data);
        moodChart.invalidate();
        android.util.Log.d("StatisticsFragment", "Mood chart updated");
    }

    private void updateMoodLineChart(TreeMap<Long, Integer> moodTimeline, int periodPosition) {
        List<Entry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        
        if (moodTimeline.isEmpty()) {
            moodLineChart.clear();
            moodLineChart.invalidate();
            return;
        }

        // Получаем временной диапазон
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

        // Фильтруем и добавляем записи
        int index = 0;
        for (Map.Entry<Long, Integer> entry : moodTimeline.entrySet()) {
            if (entry.getKey() >= startDate.getTimeInMillis()) {
                entries.add(new Entry(index, entry.getValue()));
                Calendar cal = Calendar.getInstance();
                cal.setTimeInMillis(entry.getKey());
                labels.add(String.format("%02d.%02d", 
                    cal.get(Calendar.DAY_OF_MONTH),
                    cal.get(Calendar.MONTH) + 1));
                index++;
            }
        }

        if (entries.isEmpty()) {
            moodLineChart.clear();
            moodLineChart.invalidate();
            return;
        }

        LineDataSet dataSet = new LineDataSet(entries, "Динамика настроения");
        dataSet.setColor(Color.rgb(64, 89, 128));
        dataSet.setLineWidth(2f);
        dataSet.setCircleColor(Color.rgb(64, 89, 128));
        dataSet.setCircleRadius(4f);
        dataSet.setDrawValues(false);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);

        LineData lineData = new LineData(dataSet);
        moodLineChart.getXAxis().setValueFormatter(new IAxisValueFormatter() {
            @Override
            public String getFormattedValue(float value, com.github.mikephil.charting.components.AxisBase axis) {
                int index = (int) value;
                return index >= 0 && index < labels.size() ? labels.get(index) : "";
            }
        });
        
        moodLineChart.getAxisLeft().setValueFormatter(new IAxisValueFormatter() {
            @Override
            public String getFormattedValue(float value, com.github.mikephil.charting.components.AxisBase axis) {
                switch ((int) value) {
                    case 1: return "Грустный";
                    case 2: return "Злой";
                    case 3: return "Нейтральный";
                    case 4: return "Счастливый";
                    case 5: return "Веселый";
                    default: return "";
                }
            }
        });

        moodLineChart.setData(lineData);
        moodLineChart.invalidate();
    }

    private void updateNotesChart(List<Note> notes, int periodPosition) {
        android.util.Log.d("StatisticsFragment", "Updating notes chart for period: " + periodPosition);
        List<BarEntry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        
        int numPeriods = getNumPeriods(periodPosition);
        android.util.Log.d("StatisticsFragment", "Number of periods: " + numPeriods);
        
        for (int i = 0; i < numPeriods; i++) {
            float count = getNotesCountForPeriod(notes, i, periodPosition);
            entries.add(new BarEntry(i, count));
            String label = getPeriodLabel(i, periodPosition);
            labels.add(label);
            android.util.Log.d("StatisticsFragment", "Period " + i + ": " + label + " = " + count);
        }

        if (entries.isEmpty()) {
            android.util.Log.d("StatisticsFragment", "No entries for notes chart");
            notesChart.clear();
            notesChart.invalidate();
            return;
        }

        BarDataSet dataSet = new BarDataSet(entries, "Количество заметок");
        dataSet.setColor(Color.rgb(64, 89, 128));

        BarData data = new BarData(dataSet);
        data.setBarWidth(0.9f);

        notesChart.getXAxis().setValueFormatter(new IAxisValueFormatter() {
            @Override
            public String getFormattedValue(float value, com.github.mikephil.charting.components.AxisBase axis) {
                int index = (int) value;
                return index >= 0 && index < labels.size() ? labels.get(index) : "";
            }
        });
        notesChart.setData(data);
        notesChart.invalidate();
        android.util.Log.d("StatisticsFragment", "Notes chart updated");
    }

    private int getNumPeriods(int periodPosition) {
        switch (periodPosition) {
            case 0: return 7;  // Неделя
            case 1: return 30; // Месяц
            case 2: return 12; // Год
            default: return 7;
        }
    }

    private float getNotesCountForPeriod(List<Note> notes, int period, int periodPosition) {
        Calendar calendar = Calendar.getInstance();
        Calendar periodStart = Calendar.getInstance();
        Calendar periodEnd = Calendar.getInstance();
        
        // Устанавливаем начало и конец периода
        switch (periodPosition) {
            case 0: // Неделя
                periodStart.add(Calendar.DAY_OF_YEAR, -period);
                periodStart.set(Calendar.HOUR_OF_DAY, 0);
                periodStart.set(Calendar.MINUTE, 0);
                periodStart.set(Calendar.SECOND, 0);
                
                periodEnd.setTime(periodStart.getTime());
                periodEnd.add(Calendar.DAY_OF_YEAR, 1);
                break;
                
            case 1: // Месяц
                periodStart.add(Calendar.DAY_OF_MONTH, -period);
                periodStart.set(Calendar.HOUR_OF_DAY, 0);
                periodStart.set(Calendar.MINUTE, 0);
                periodStart.set(Calendar.SECOND, 0);
                
                periodEnd.setTime(periodStart.getTime());
                periodEnd.add(Calendar.DAY_OF_MONTH, 1);
                break;
                
            case 2: // Год
                periodStart.add(Calendar.MONTH, -period);
                periodStart.set(Calendar.DAY_OF_MONTH, 1);
                periodStart.set(Calendar.HOUR_OF_DAY, 0);
                periodStart.set(Calendar.MINUTE, 0);
                periodStart.set(Calendar.SECOND, 0);
                
                periodEnd.setTime(periodStart.getTime());
                periodEnd.add(Calendar.MONTH, 1);
                break;
        }
        
        int count = 0;
        for (Note note : notes) {
            Calendar noteDate = Calendar.getInstance();
            noteDate.setTime(new Date(note.getCreatedAt()));
            
            // Проверяем, попадает ли дата заметки в текущий период
            if (noteDate.getTimeInMillis() >= periodStart.getTimeInMillis() && 
                noteDate.getTimeInMillis() < periodEnd.getTimeInMillis()) {
                count++;
            }
        }
        
        return count;
    }

    private String getPeriodLabel(int period, int periodPosition) {
        Calendar calendar = Calendar.getInstance();
        calendar.add(getCalendarField(periodPosition), -period);
        
        switch (periodPosition) {
            case 0: // Неделя
                return String.format("%02d.%02d", 
                    calendar.get(Calendar.DAY_OF_MONTH),
                    calendar.get(Calendar.MONTH) + 1);
            case 1: // Месяц
                return String.format("%02d.%02d", 
                    calendar.get(Calendar.DAY_OF_MONTH),
                    calendar.get(Calendar.MONTH) + 1);
            case 2: // Год
                return String.format("%02d", 
                    calendar.get(Calendar.MONTH) + 1);
            default:
                return "";
        }
    }

    private int getCalendarField(int periodPosition) {
        switch (periodPosition) {
            case 0: return Calendar.DAY_OF_WEEK;
            case 1: return Calendar.DAY_OF_MONTH;
            case 2: return Calendar.MONTH;
            default: return Calendar.DAY_OF_WEEK;
        }
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