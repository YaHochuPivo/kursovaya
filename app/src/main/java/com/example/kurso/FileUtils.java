package com.example.kurso;

import android.content.Context;
import android.net.Uri;
import android.widget.Toast;
import android.util.Log;
import android.os.Handler;
import android.os.Looper;
import android.graphics.pdf.PdfDocument;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;

import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Font;
import com.itextpdf.text.FontFactory;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.parser.PdfTextExtractor;
import com.itextpdf.text.Element;
import com.itextpdf.text.Chunk;

import org.json.JSONArray;
import org.json.JSONObject;
import org.json.JSONException;

import java.io.*;
import java.text.SimpleDateFormat;
import java.text.ParseException;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FileUtils {

    public static void exportData(Context context, List<Object> items, String format) {
        Log.d("FileUtils", "Начало экспорта. Формат: " + format + ", элементов: " + items.size());
        
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());
        
        executor.execute(() -> {
            try {
                // Создаем директорию для экспорта, если её нет
                File exportDir = new File(context.getExternalFilesDir(null), "export");
                if (!exportDir.exists() && !exportDir.mkdirs()) {
                    throw new IOException("Не удалось создать директорию для экспорта");
                }

                // Формируем имя файла с временной меткой
                String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
                String fileName = "export_" + timestamp + "." + format.toLowerCase();
                File exportFile = new File(exportDir, fileName);

                Log.d("FileUtils", "Подготовка к экспорту в файл: " + exportFile.getAbsolutePath());

            switch (format.toLowerCase()) {
                    case "json":
                        exportToJson(items, exportFile);
                        break;
                    case "csv":
                        exportToCsv(items, exportFile);
                        break;
                    case "pdf":
                        exportToPdf(items, exportFile);
                        break;
                    default:
                        throw new IllegalArgumentException("Неподдерживаемый формат: " + format);
                }

                handler.post(() -> {
                    String message = "Экспорт успешно завершен: " + fileName;
                    Log.d("FileUtils", message);
                    Toast.makeText(context, message, Toast.LENGTH_LONG).show();
                });

        } catch (Exception e) {
                String errorMessage = "Ошибка при экспорте: " + e.getMessage();
                Log.e("FileUtils", errorMessage, e);
                handler.post(() -> Toast.makeText(context, errorMessage, Toast.LENGTH_LONG).show());
        }
        });
    }

    private static void exportToJson(List<Object> items, File file) throws IOException {
        Log.d("FileUtils", "Начало экспорта в JSON");
        JSONArray jsonArray = new JSONArray();

        try {
            for (Object item : items) {
                JSONObject jsonObject = new JSONObject();
                if (item instanceof Note) {
                    Note note = (Note) item;
                    jsonObject.put("type", "note");
                    jsonObject.put("title", note.getTitle());
                    jsonObject.put("text", note.getText());
                    jsonObject.put("mood", note.getMood());
                    jsonObject.put("tags", new JSONArray(note.getTags()));
                    jsonObject.put("timestamp", note.getTimestamp().getSeconds() * 1000);
                    jsonObject.put("createdAt", note.getCreatedAt());
                } else if (item instanceof PlanWrapper) {
                    PlanWrapper plan = (PlanWrapper) item;
                    jsonObject.put("type", "plan");
                    JSONArray tasksArray = new JSONArray();
                    for (TaskItem task : plan.getTasks()) {
                        JSONObject taskObject = new JSONObject();
                        taskObject.put("text", task.getText());
                        taskObject.put("time", task.getTime());
                        taskObject.put("done", task.isDone());
                        tasksArray.put(taskObject);
                    }
                    jsonObject.put("tasks", tasksArray);
                }
                jsonArray.put(jsonObject);
            }

            try (FileWriter writer = new FileWriter(file)) {
                writer.write(jsonArray.toString(2)); // Форматированный JSON
            }
            Log.d("FileUtils", "JSON экспорт завершен, элементов: " + jsonArray.length());

        } catch (JSONException e) {
            throw new IOException("Ошибка создания JSON: " + e.getMessage());
        }
    }

    private static void exportToCsv(List<Object> items, File file) throws IOException {
        Log.d("FileUtils", "Начало экспорта в CSV");
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(file))) {
            // Записываем заголовки
            writer.write("type,title,text,timestamp,time,done");
            writer.newLine();

            // Записываем данные
            for (Object item : items) {
                if (item instanceof Note) {
                    Note note = (Note) item;
                    writer.write(String.format("%s,%s,%s,%d,%s,%s",
                        quote("note"),
                        quote(note.getTitle()),
                        quote(note.getText()),
                        note.getTimestamp().getSeconds() * 1000,
                        quote(""),
                        quote("")));
                    writer.newLine();
                } else if (item instanceof PlanWrapper) {
                    PlanWrapper plan = (PlanWrapper) item;
                    for (TaskItem task : plan.getTasks()) {
                        writer.write(String.format("%s,%s,%s,%s,%s,%s",
                            quote("plan"),
                            quote(""),
                            quote(task.getText()),
                            quote(""),
                            quote(task.getTime()),
                            task.isDone()));
                        writer.newLine();
                    }
                }
            }
        }
        Log.d("FileUtils", "CSV экспорт завершен");
    }

    private static String quote(String s) {
        if (s == null) return "\"\"";
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }

    private static String unquote(String s) {
        if (s == null || s.isEmpty()) return "";
        s = s.trim();
        if (s.startsWith("\"") && s.endsWith("\"")) {
            s = s.substring(1, s.length() - 1);
        }
        return s.replace("\"\"", "\"");
    }

    private static void exportToPdf(List<Object> items, File file) throws IOException {
        PdfDocument document = new PdfDocument();
        
        // Настройка параметров страницы
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create(); // A4 размер в точках
        PdfDocument.Page page = document.startPage(pageInfo);
        
        Canvas canvas = page.getCanvas();
        Paint paint = new Paint();
        paint.setTextSize(12); // Размер текста
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        
        float x = 40; // Отступ слева
        float y = 40; // Отступ сверху
        float lineHeight = paint.getTextSize() * 1.5f; // Высота строки
        
        // Добавляем заметки
        for (Object item : items) {
            if (item instanceof Note) {
                Note note = (Note) item;
                
                // Заголовок заметки
                paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
                canvas.drawText("=== ЗАМЕТКА ===", x, y, paint);
                y += lineHeight * 2;
                
                paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
                
                // Содержимое заметки
                canvas.drawText("Заголовок: " + note.getTitle(), x, y, paint);
                y += lineHeight;
                
                String[] descriptionLines = splitTextIntoLines(note.getText(), paint, pageInfo.getPageWidth() - 80);
                canvas.drawText("Описание:", x, y, paint);
                y += lineHeight;
                for (String line : descriptionLines) {
                    canvas.drawText(line, x + 20, y, paint);
                    y += lineHeight;
                }
                
                if (note.getMood() != null && !note.getMood().isEmpty()) {
                    canvas.drawText("Настроение: " + note.getMood(), x, y, paint);
                    y += lineHeight;
                }
                
                if (note.getTags() != null && !note.getTags().isEmpty()) {
                    canvas.drawText("Теги: " + String.join(", ", note.getTags()), x, y, paint);
                    y += lineHeight;
                }
                
                // Используем createdAt для форматирования даты
                String dateStr = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                        .format(new Date(note.getCreatedAt()));
                canvas.drawText("Дата: " + dateStr, x, y, paint);
                y += lineHeight * 2;
                
                // Проверяем, нужна ли новая страница
                if (y > pageInfo.getPageHeight() - 50) {
                    document.finishPage(page);
                    pageInfo = new PdfDocument.PageInfo.Builder(595, 842, document.getPages().size() + 1).create();
                    page = document.startPage(pageInfo);
                    canvas = page.getCanvas();
                    y = 40;
                }
            }
        }
        
        // Добавляем планы
        boolean hasPlans = false;
        for (Object item : items) {
            if (item instanceof PlanWrapper) {
                if (!hasPlans) {
                    // Проверяем, нужна ли новая страница
                    if (y > pageInfo.getPageHeight() - 100) {
                        document.finishPage(page);
                        pageInfo = new PdfDocument.PageInfo.Builder(595, 842, document.getPages().size() + 1).create();
                        page = document.startPage(pageInfo);
                        canvas = page.getCanvas();
                        y = 40;
                    }
                    
                    paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
                    canvas.drawText("=== ПЛАН НА ДЕНЬ ===", x, y, paint);
                    y += lineHeight * 2;
                    paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
                    hasPlans = true;
                }
                
                PlanWrapper plan = (PlanWrapper) item;
                for (TaskItem task : plan.getTasks()) {
                    String taskText = (task.isDone() ? "[X] " : "[ ] ") + task.getText();
                    if (task.getTime() != null && !task.getTime().isEmpty()) {
                        taskText += " - " + task.getTime();
                    }
                    
                    String[] taskLines = splitTextIntoLines(taskText, paint, pageInfo.getPageWidth() - 80);
                    for (String line : taskLines) {
                        // Проверяем, нужна ли новая страница
                        if (y > pageInfo.getPageHeight() - 50) {
                            document.finishPage(page);
                            pageInfo = new PdfDocument.PageInfo.Builder(595, 842, document.getPages().size() + 1).create();
                            page = document.startPage(pageInfo);
                            canvas = page.getCanvas();
                            y = 40;
                        }
                        
                        canvas.drawText(line, x, y, paint);
                        y += lineHeight;
                    }
                }
            }
        }
        
        document.finishPage(page);
        document.writeTo(new FileOutputStream(file));
        document.close();
    }
    
    private static String[] splitTextIntoLines(String text, Paint paint, float maxWidth) {
        List<String> lines = new ArrayList<>();
        String[] words = text.split("\\s+");
        StringBuilder currentLine = new StringBuilder();
        
        for (String word : words) {
            if (currentLine.length() == 0) {
                currentLine.append(word);
            } else {
                String testLine = currentLine + " " + word;
                if (paint.measureText(testLine) <= maxWidth) {
                    currentLine.append(" ").append(word);
                } else {
                    lines.add(currentLine.toString());
                    currentLine = new StringBuilder(word);
                }
            }
        }
        
        if (currentLine.length() > 0) {
            lines.add(currentLine.toString());
        }
        
        return lines.toArray(new String[0]);
    }

    public static void importFromUri(Context context, Uri uri, String format, ImportCallback callback) {
        Log.d("FileUtils", "Начало импорта. URI: " + uri + ", формат: " + format);
        
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());
        
        executor.execute(() -> {
            List<Object> importedItems = new ArrayList<>();
            try {
                InputStream inputStream = context.getContentResolver().openInputStream(uri);
                if (inputStream == null) {
                    throw new IOException("Не удалось открыть файл");
                }

                switch (format.toLowerCase()) {
                    case "json":
                        BufferedReader jsonReader = new BufferedReader(new InputStreamReader(inputStream));
                        StringBuilder jsonBuilder = new StringBuilder();
                        String line;
                        while ((line = jsonReader.readLine()) != null) {
                            jsonBuilder.append(line).append("\n");
                        }
                        importedItems = parseJsonContent(jsonBuilder.toString());
                        break;
                        
                    case "csv":
                        BufferedReader csvReader = new BufferedReader(new InputStreamReader(inputStream));
                        StringBuilder csvBuilder = new StringBuilder();
                        while ((line = csvReader.readLine()) != null) {
                            csvBuilder.append(line).append("\n");
                        }
                        importedItems = parseCsvContent(csvBuilder.toString());
                        break;
                        
                    case "pdf":
                        importedItems = parsePdfContent(inputStream);
                        break;
                        
                    default:
                        throw new IOException("Неподдерживаемый формат: " + format);
                }

                inputStream.close();
                final List<Object> finalImportedItems = importedItems;
                
                handler.post(() -> {
                    Log.d("FileUtils", "Импорт завершен успешно. Элементов: " + finalImportedItems.size());
                    callback.onImportComplete(finalImportedItems);
                });

            } catch (Exception e) {
                Log.e("FileUtils", "Ошибка при импорте: " + e.getMessage(), e);
                handler.post(() -> {
                    Toast.makeText(context, "Ошибка импорта: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    callback.onImportComplete(new ArrayList<>());
                });
            }
        });
    }

    private static List<Object> parseJsonContent(String content) throws JSONException {
        List<Object> items = new ArrayList<>();
        JSONArray jsonArray = new JSONArray(content);

        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject jsonObject = jsonArray.getJSONObject(i);
            String type = jsonObject.getString("type");

            if ("note".equals(type)) {
                Note note = new Note();
                note.setTitle(jsonObject.getString("title"));
                note.setText(jsonObject.getString("text"));
                long timestamp = jsonObject.getLong("timestamp");
                note.setTimestamp(new com.google.firebase.Timestamp(timestamp / 1000, 0));
                note.setCreatedAt(timestamp);
                items.add(note);
            } else if ("plan".equals(type)) {
                List<TaskItem> tasks = new ArrayList<>();
                JSONArray tasksArray = jsonObject.getJSONArray("tasks");
                for (int j = 0; j < tasksArray.length(); j++) {
                    JSONObject taskObject = tasksArray.getJSONObject(j);
                    TaskItem task = new TaskItem();
                    task.setText(taskObject.getString("text"));
                    task.setTime(taskObject.getString("time"));
                    task.setDone(taskObject.getBoolean("done"));
                    tasks.add(task);
                }
                items.add(new PlanWrapper(UUID.randomUUID().toString(), tasks));
            }
        }
        return items;
    }

    private static List<Object> parseCsvContent(String content) {
        List<Object> items = new ArrayList<>();
        String[] lines = content.split("\n");
        if (lines.length <= 1) return items; // Skip if only header or empty

        for (int i = 1; i < lines.length; i++) { // Start from 1 to skip header
            String[] values = lines[i].split(",(?=(?:[^\"]*\"[^\"]*\")*[^\"]*$)");
            String type = values[0];

            if (type.equals("note")) {
                Note note = new Note();
                note.setTitle(unquote(values[1]));
                note.setText(unquote(values[2]));
                note.setMood(unquote(values[3]));
                String tagsStr = unquote(values[4]);
                if (!tagsStr.isEmpty()) {
                    note.setTags(Arrays.asList(tagsStr.split(",")));
                }
                try {
                    long timestamp = Long.parseLong(values[5]);
                    note.setTimestamp(new com.google.firebase.Timestamp(timestamp / 1000, 0));
                    note.setCreatedAt(timestamp);
                } catch (NumberFormatException e) {
                    com.google.firebase.Timestamp now = com.google.firebase.Timestamp.now();
                    note.setTimestamp(now);
                    note.setCreatedAt(now.getSeconds() * 1000);
                    Log.e("FileUtils", "Error parsing timestamp from CSV: " + values[5], e);
                }
                items.add(note);
            } else if (type.equals("task")) {
                TaskItem task = new TaskItem();
                task.setText(unquote(values[2]));
                task.setTime(unquote(values[6]));
                task.setDone(Boolean.parseBoolean(values[7]));
                items.add(task);
            }
        }
        return items;
    }

    private static List<Object> parsePdfContent(InputStream inputStream) throws IOException {
        List<Object> items = new ArrayList<>();
        try {
            PdfReader reader = new PdfReader(inputStream);
            StringBuilder content = new StringBuilder();
            
            Log.d("FileUtils", "Начинаем чтение PDF файла. Количество страниц: " + reader.getNumberOfPages());
            
            // Читаем текст из всех страниц PDF
            for (int i = 1; i <= reader.getNumberOfPages(); i++) {
                String pageText = PdfTextExtractor.getTextFromPage(reader, i);
                Log.d("FileUtils", "Страница " + i + " содержит текст:\n" + pageText);
                content.append(pageText).append("\n");
            }
            
            reader.close();
            
            String fullText = content.toString().trim();
            if (fullText.isEmpty()) {
                throw new IOException("PDF файл пуст");
            }
            
            Log.d("FileUtils", "Полный текст из PDF:\n" + fullText);

            // Разбиваем текст на секции
            String[] sections = fullText.split("===\\s*[А-Я][^=]+\\s*===");
            
            // Если есть хотя бы одна секция после разделения
            if (sections.length > 0) {
                // Обрабатываем основной текст
                String mainText = fullText;
                Note currentNote = null;
                List<TaskItem> tasks = new ArrayList<>();
                
                // Разбиваем на строки для построчного анализа
                String[] lines = mainText.split("\n");
                StringBuilder description = new StringBuilder();
                boolean isReadingDescription = false;
                
                for (String line : lines) {
                    line = line.trim();
                    if (line.isEmpty()) continue;
                    
                    if (line.contains("=== ЗАМЕТКА ===")) {
                        // Если у нас уже есть заметка, сохраняем её
                        if (currentNote != null && currentNote.getTitle() != null) {
                            if (description.length() > 0) {
                                currentNote.setText(description.toString().trim());
                            }
                            items.add(currentNote);
                            description = new StringBuilder();
                        }
                        currentNote = new Note();
                        isReadingDescription = false;
                    } else if (line.contains("=== ПЛАН НА ДЕНЬ ===")) {
                        // Если у нас есть заметка, сохраняем её перед началом плана
                        if (currentNote != null && currentNote.getTitle() != null) {
                            if (description.length() > 0) {
                                currentNote.setText(description.toString().trim());
                            }
                            items.add(currentNote);
                            currentNote = null;
                            description = new StringBuilder();
                        }
                        tasks = new ArrayList<>();
                        isReadingDescription = false;
                    } else if (currentNote != null) {
                        // Обработка содержимого заметки
                        if (line.startsWith("Заголовок:")) {
                            currentNote.setTitle(line.substring("Заголовок:".length()).trim());
                            Log.d("FileUtils", "Найден заголовок: " + currentNote.getTitle());
                        } else if (line.startsWith("Описание:")) {
                            isReadingDescription = true;
                        } else if (line.startsWith("Настроение:")) {
                            isReadingDescription = false;
                            String mood = line.substring("Настроение:".length()).trim();
                            currentNote.setMood(mood);
                            Log.d("FileUtils", "Найдено настроение: " + mood);
                        } else if (line.startsWith("Теги:")) {
                            isReadingDescription = false;
                            String tagsStr = line.substring("Теги:".length()).trim();
                            if (!tagsStr.isEmpty()) {
                                List<String> tags = Arrays.asList(tagsStr.split(",\\s*"));
                                currentNote.setTags(tags);
                                Log.d("FileUtils", "Найдены теги: " + tags);
                            }
                        } else if (line.startsWith("Дата:")) {
                            isReadingDescription = false;
                            String dateStr = line.substring("Дата:".length()).trim();
                            SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
                            try {
                                Date date = dateFormat.parse(dateStr);
                                if (date != null) {
                                    long timestamp = date.getTime();
                                    currentNote.setTimestamp(new com.google.firebase.Timestamp(timestamp / 1000, 0));
                                    currentNote.setCreatedAt(timestamp);
                                }
                            } catch (ParseException e) {
                                com.google.firebase.Timestamp now = com.google.firebase.Timestamp.now();
                                currentNote.setTimestamp(now);
                                currentNote.setCreatedAt(now.getSeconds() * 1000);
                                Log.e("FileUtils", "Error parsing date: " + dateStr, e);
                            }
                        } else if (isReadingDescription) {
                            description.append(line).append("\n");
                        }
                    } else if (line.startsWith("[")) {
                        // Обработка задач плана
                        TaskItem task = new TaskItem();
                        boolean isDone = line.startsWith("[X]");
                        String taskContent = line.substring(3).trim();
                        
                        if (taskContent.contains(" - ")) {
                            String[] parts = taskContent.split(" - ", 2);
                            task.setText(parts[0].trim());
                            task.setTime(parts[1].trim());
                        } else {
                            task.setText(taskContent);
                            task.setTime("");
                        }
                        
                        task.setDone(isDone);
                        tasks.add(task);
                        Log.d("FileUtils", "Найдена задача: " + taskContent + " (выполнено: " + isDone + ")");
                    }
                }
                
                // Добавляем последнюю заметку, если она есть
                if (currentNote != null && currentNote.getTitle() != null) {
                    if (description.length() > 0) {
                        currentNote.setText(description.toString().trim());
                    }
                    items.add(currentNote);
                }
                
                // Добавляем план, если есть задачи
                if (!tasks.isEmpty()) {
                    items.add(new PlanWrapper(UUID.randomUUID().toString(), tasks));
                    Log.d("FileUtils", "Добавлен план с " + tasks.size() + " задачами");
                }
            }

            if (items.isEmpty()) {
                throw new IOException("Не удалось найти заметки или планы в PDF файле");
            }

            Log.d("FileUtils", "Импорт PDF завершен успешно. Найдено элементов: " + items.size());
            return items;

        } catch (Exception e) {
            Log.e("FileUtils", "Ошибка при чтении PDF: " + e.getMessage(), e);
            throw new IOException("Ошибка чтения PDF: " + e.getMessage());
        }
    }

    public static void exportDataToUri(Context context, List<Object> items, String format, Uri uri) {
        if (items == null || items.isEmpty()) {
            Toast.makeText(context, "Нет данных для экспорта", Toast.LENGTH_SHORT).show();
            return;
        }

        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            try {
                OutputStream outputStream = context.getContentResolver().openOutputStream(uri);
                if (outputStream == null) {
                    throw new IOException("Не удалось открыть файл для записи");
                }

                switch (format.toLowerCase()) {
                    case "json":
                        exportToJson(items, outputStream);
                        break;
                    case "csv":
                        exportToCsv(items, outputStream);
                        break;
                    case "pdf":
                        exportToPdf(items, outputStream);
                        break;
                    default:
                        throw new IOException("Неподдерживаемый формат: " + format);
                }

                outputStream.close();

                handler.post(() -> 
                    Toast.makeText(context, "Экспорт успешно завершен", Toast.LENGTH_LONG).show()
                );

            } catch (Exception e) {
                Log.e("FileUtils", "Ошибка при экспорте: " + e.getMessage(), e);
                handler.post(() -> 
                    Toast.makeText(context, "Ошибка экспорта: " + e.getMessage(), Toast.LENGTH_LONG).show()
                );
            }
        });
    }

    private static void exportToJson(List<Object> items, OutputStream outputStream) throws IOException {
        JSONArray jsonArray = new JSONArray();

        try {
            for (Object item : items) {
                JSONObject jsonObject = new JSONObject();
                if (item instanceof Note) {
                    Note note = (Note) item;
                    jsonObject.put("type", "note");
                    jsonObject.put("title", note.getTitle());
                    jsonObject.put("text", note.getText());
                    jsonObject.put("mood", note.getMood());
                    jsonObject.put("tags", new JSONArray(note.getTags()));
                    jsonObject.put("timestamp", note.getTimestamp().getSeconds() * 1000);
                    jsonObject.put("createdAt", note.getCreatedAt());
                } else if (item instanceof PlanWrapper) {
                    PlanWrapper plan = (PlanWrapper) item;
                    jsonObject.put("type", "plan");
                    JSONArray tasksArray = new JSONArray();
                    for (TaskItem task : plan.getTasks()) {
                        JSONObject taskObject = new JSONObject();
                        taskObject.put("text", task.getText());
                        taskObject.put("time", task.getTime());
                        taskObject.put("done", task.isDone());
                        tasksArray.put(taskObject);
                    }
                    jsonObject.put("tasks", tasksArray);
                }
                jsonArray.put(jsonObject);
            }

            OutputStreamWriter writer = new OutputStreamWriter(outputStream);
            writer.write(jsonArray.toString(2));
            writer.flush();

        } catch (JSONException e) {
            throw new IOException("Ошибка создания JSON: " + e.getMessage());
        }
    }

    private static void exportToCsv(List<Object> items, OutputStream outputStream) throws IOException {
        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream));

        // Записываем заголовки
        writer.write("type,title,text,mood,tags,timestamp,time,done");
        writer.newLine();

        // Записываем данные
        for (Object item : items) {
            if (item instanceof Note) {
                Note note = (Note) item;
                long timestamp = note.getTimestamp() != null ? note.getTimestamp().getSeconds() * 1000 : new Date().getTime();
                writer.write(String.format("%s,%s,%s,%s,%s,%d,%s,%s",
                    quote("note"),
                    quote(note.getTitle()),
                    quote(note.getText()),
                    quote(note.getMood()),
                    quote(String.join(",", note.getTags())),
                    timestamp,
                    quote(""),
                    quote("")));
                writer.newLine();
            } else if (item instanceof PlanWrapper) {
                PlanWrapper plan = (PlanWrapper) item;
                long currentTime = System.currentTimeMillis();
                for (TaskItem task : plan.getTasks()) {
                    writer.write(String.format("%s,%s,%s,%s,%s,%d,%s,%s",
                        quote("task"),
                        quote(""),
                        quote(task.getText()),
                        quote(""),
                        quote(""),
                        currentTime,
                        quote(task.getTime()),
                        String.valueOf(task.isDone())));
                    writer.newLine();
                }
            }
        }
        writer.flush();
    }

    private static void exportToPdf(List<Object> items, OutputStream outputStream) throws IOException {
        PdfDocument document = new PdfDocument();
        
        // Настройка параметров страницы
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create(); // A4 размер в точках
        PdfDocument.Page page = document.startPage(pageInfo);
        
        Canvas canvas = page.getCanvas();
        Paint paint = new Paint();
        paint.setTextSize(12); // Размер текста
        paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
        
        float x = 40; // Отступ слева
        float y = 40; // Отступ сверху
        float lineHeight = paint.getTextSize() * 1.5f; // Высота строки
        
        // Добавляем заметки
        for (Object item : items) {
            if (item instanceof Note) {
                Note note = (Note) item;
                
                // Заголовок заметки
                paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
                canvas.drawText("=== ЗАМЕТКА ===", x, y, paint);
                y += lineHeight * 2;
                
                paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
                
                // Содержимое заметки
                canvas.drawText("Заголовок: " + note.getTitle(), x, y, paint);
                y += lineHeight;
                
                String[] descriptionLines = splitTextIntoLines(note.getText(), paint, pageInfo.getPageWidth() - 80);
                canvas.drawText("Описание:", x, y, paint);
                y += lineHeight;
                for (String line : descriptionLines) {
                    canvas.drawText(line, x + 20, y, paint);
                    y += lineHeight;
                }
                
                if (note.getMood() != null && !note.getMood().isEmpty()) {
                    canvas.drawText("Настроение: " + note.getMood(), x, y, paint);
                    y += lineHeight;
                }
                
                if (note.getTags() != null && !note.getTags().isEmpty()) {
                    canvas.drawText("Теги: " + String.join(", ", note.getTags()), x, y, paint);
                    y += lineHeight;
                }
                
                // Используем createdAt для форматирования даты
                String dateStr = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault())
                        .format(new Date(note.getCreatedAt()));
                canvas.drawText("Дата: " + dateStr, x, y, paint);
                y += lineHeight * 2;
                
                // Проверяем, нужна ли новая страница
                if (y > pageInfo.getPageHeight() - 50) {
                    document.finishPage(page);
                    pageInfo = new PdfDocument.PageInfo.Builder(595, 842, document.getPages().size() + 1).create();
                    page = document.startPage(pageInfo);
                    canvas = page.getCanvas();
                    y = 40;
                }
            }
        }
        
        // Добавляем планы
        boolean hasPlans = false;
        for (Object item : items) {
            if (item instanceof PlanWrapper) {
                if (!hasPlans) {
                    // Проверяем, нужна ли новая страница
                    if (y > pageInfo.getPageHeight() - 100) {
                        document.finishPage(page);
                        pageInfo = new PdfDocument.PageInfo.Builder(595, 842, document.getPages().size() + 1).create();
                        page = document.startPage(pageInfo);
                        canvas = page.getCanvas();
                        y = 40;
                    }
                    
                    paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
                    canvas.drawText("=== ПЛАН НА ДЕНЬ ===", x, y, paint);
                    y += lineHeight * 2;
                    paint.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.NORMAL));
                    hasPlans = true;
                }
                
                PlanWrapper plan = (PlanWrapper) item;
                for (TaskItem task : plan.getTasks()) {
                    String taskText = (task.isDone() ? "[X] " : "[ ] ") + task.getText();
                    if (task.getTime() != null && !task.getTime().isEmpty()) {
                        taskText += " - " + task.getTime();
                    }
                    
                    String[] taskLines = splitTextIntoLines(taskText, paint, pageInfo.getPageWidth() - 80);
                    for (String line : taskLines) {
                        // Проверяем, нужна ли новая страница
                        if (y > pageInfo.getPageHeight() - 50) {
                            document.finishPage(page);
                            pageInfo = new PdfDocument.PageInfo.Builder(595, 842, document.getPages().size() + 1).create();
                            page = document.startPage(pageInfo);
                            canvas = page.getCanvas();
                            y = 40;
                        }
                        
                        canvas.drawText(line, x, y, paint);
                        y += lineHeight;
                    }
                }
            }
        }
        
        document.finishPage(page);
        document.writeTo(outputStream);
        document.close();
    }

    private static void exportToCsv(Context context, List<Note> notes, List<PlanWrapper> plans, File file) throws IOException {
        try (FileWriter writer = new FileWriter(file)) {
            // Write header
            writer.write("type,title,text,mood,tags,timestamp,time,done\n");
            
            // Write notes
            for (Note note : notes) {
                writer.write(String.format("note,%s,%s,%s,%s,%d,%s,%s\n",
                    escapeField(note.getTitle()),
                    escapeField(note.getText()),
                    escapeField(note.getMood()),
                    escapeField(String.join(",", note.getTags())),
                    note.getCreatedAt(),
                    "",  // time is not applicable for notes
                    ""   // done is not applicable for notes
                ));
            }
            
            // Write plans
            for (PlanWrapper plan : plans) {
                long currentTime = System.currentTimeMillis();
                for (TaskItem task : plan.getTasks()) {
                    writer.write(String.format("task,%s,%s,%s,%s,%d,%s,%s\n",
                        "",  // title is not applicable for tasks
                        escapeField(task.getText()),
                        "",  // mood is not applicable for tasks
                        "",  // tags are not applicable for tasks
                        currentTime,
                        escapeField(task.getTime()),
                        String.valueOf(task.isDone())));
                }
            }
        }
    }

    private static String escapeField(String field) {
        if (field == null) return "";
        // Escape quotes and wrap in quotes if contains comma
        if (field.contains(",") || field.contains("\"")) {
            return "\"" + field.replace("\"", "\"\"") + "\"";
        }
        return field;
    }

    private static String escapeField(List<String> list) {
        if (list == null || list.isEmpty()) return "";
        return escapeField(String.join(",", list));
    }
}



