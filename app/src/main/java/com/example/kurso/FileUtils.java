package com.example.kurso;

import android.content.Context;
import android.os.Environment;
import android.widget.Toast;

import com.example.kurso.PlanWrapper; // ✅ правильный импорт

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class FileUtils {

    public static void exportData(Context context, List<Object> items, String format) {
        String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
        File dir = context.getExternalFilesDir(null);
        if (dir == null) {
            Toast.makeText(context, "Ошибка доступа к памяти", Toast.LENGTH_SHORT).show();
            return;
        }

        File file;
        switch (format.toLowerCase()) {
            case "csv":
                file = new File(dir, "export_" + timestamp + ".csv");
                exportToCsv(context, items, file);
                break;
            case "json":
                file = new File(dir, "export_" + timestamp + ".json");
                exportToJson(context, items, file);
                break;
            case "pdf":
                file = new File(dir, "export_" + timestamp + ".pdf");
                exportToPdf(context, items, file);
                break;
        }
    }

    public static void importData(Context context, String format, DataImportCallback callback) {
        File dir = context.getExternalFilesDir(null);
        File[] files = dir != null ? dir.listFiles() : null;

        if (files == null || files.length == 0) {
            Toast.makeText(context, "Нет файлов для импорта", Toast.LENGTH_SHORT).show();
            return;
        }

        List<Object> importedItems = new ArrayList<>();

        for (File file : files) {
            if (file.getName().endsWith(format.toLowerCase())) {
                try {
                    if (format.equalsIgnoreCase("json")) {
                        Gson gson = new Gson();
                        BufferedReader reader = new BufferedReader(new FileReader(file));
                        Object[] data = gson.fromJson(reader, Object[].class);
                        importedItems.addAll(Arrays.asList(data));
                        reader.close();
                    } else if (format.equalsIgnoreCase("csv")) {
                        BufferedReader reader = new BufferedReader(new FileReader(file));
                        String line;
                        while ((line = reader.readLine()) != null) {
                            String[] parts = line.split(",");
                            if (parts.length >= 3) {
                                Note note = new Note();
                                note.setTitle(parts[0]);
                                note.setContent(parts[1]);
                                note.setDateTime(parts[2]);
                                importedItems.add(note);
                            }
                        }
                        reader.close();
                    }
                } catch (IOException e) {
                    Toast.makeText(context, "Ошибка импорта", Toast.LENGTH_SHORT).show();
                }
                break;
            }
        }

        callback.onDataImported(importedItems);
    }

    private static void exportToCsv(Context context, List<Object> items, File file) {
        try {
            FileWriter writer = new FileWriter(file);
            for (Object obj : items) {
                if (obj instanceof Note) {
                    Note note = (Note) obj;
                    writer.append(note.getTitle()).append(",");
                    writer.append(note.getContent()).append(",");
                    writer.append(note.getDateTime()).append("\n");
                } else if (obj instanceof PlanWrapper) {
                    PlanWrapper plan = (PlanWrapper) obj;
                    for (String task : plan.getTasks()) {
                        writer.append("Задача,").append(task).append(",\n");
                    }
                }
            }
            writer.flush();
            writer.close();
            Toast.makeText(context, "CSV сохранён: " + file.getName(), Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(context, "Ошибка сохранения CSV", Toast.LENGTH_SHORT).show();
        }
    }

    private static void exportToJson(Context context, List<Object> items, File file) {
        try {
            Gson gson = new GsonBuilder().setPrettyPrinting().create();
            FileWriter writer = new FileWriter(file);
            gson.toJson(items, writer);
            writer.flush();
            writer.close();
            Toast.makeText(context, "JSON сохранён: " + file.getName(), Toast.LENGTH_SHORT).show();
        } catch (IOException e) {
            Toast.makeText(context, "Ошибка сохранения JSON", Toast.LENGTH_SHORT).show();
        }
    }

    private static void exportToPdf(Context context, List<Object> items, File file) {
        // Можно подключить iText или PdfDocument API
        Toast.makeText(context, "PDF экспорт пока не реализован", Toast.LENGTH_SHORT).show();
    }

    public interface DataImportCallback {
        void onDataImported(List<Object> importedItems);
    }
}
