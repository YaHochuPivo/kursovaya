package com.example.kurso;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.kurso.NotesAdapter.PlanWrapper;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;
import java.util.*;

public class NotesFragment extends Fragment {

    private RecyclerView recyclerView;
    private NotesAdapter adapter;
    private List<Object> itemList;
    private FirebaseFirestore db;

    private ActivityResultLauncher<Intent> filePickerLauncher;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notes, container, false);

        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        Button btnAddNote = view.findViewById(R.id.btnAddNote);
        Button btnAddPlan = view.findViewById(R.id.btnAddPlan);
        Button btnImport = view.findViewById(R.id.btnImport);
        Button btnExport = view.findViewById(R.id.btnExport);

        db = FirebaseFirestore.getInstance();
        itemList = new ArrayList<>();
        adapter = new NotesAdapter(getContext(), itemList);
        recyclerView.setAdapter(adapter);

        btnAddNote.setOnClickListener(v -> startActivity(new Intent(getContext(), CreateNoteActivity.class)));
        btnAddPlan.setOnClickListener(v -> startActivity(new Intent(getContext(), DailyPlanActivity.class)));
        btnImport.setOnClickListener(v -> openFilePicker());
        btnExport.setOnClickListener(v -> showExportFormatDialog());

        // 🔹 Регистрируем filePicker
        filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null) {
                            String path = uri.getPath();
                            if (path != null && path.endsWith(".csv")) importFromCSV(uri);
                            else if (path != null && path.endsWith(".json")) importFromJSON(uri);
                            else Toast.makeText(getContext(), "Поддерживаются только .csv и .json", Toast.LENGTH_SHORT).show();
                        }
                    }
                });

        loadData();
        return view;
    }

    private void loadData() {
        itemList.clear();

        db.collection("notes").get().addOnSuccessListener(snapshot -> {
            for (QueryDocumentSnapshot doc : snapshot) {
                Note note = doc.toObject(Note.class);
                itemList.add(note);
            }
            adapter.notifyDataSetChanged();
        });

        db.collection("daily_plans")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.DESCENDING)
                .limit(1)
                .get()
                .addOnSuccessListener(snapshot -> {
                    if (!snapshot.isEmpty()) {
                        DocumentSnapshot doc = snapshot.getDocuments().get(0);
                        String id = doc.getId();
                        Map<String, Object> data = doc.getData();
                        List<String> tasks = new ArrayList<>();
                        if (data != null && data.containsKey("tasks")) {
                            Object taskList = data.get("tasks");
                            if (taskList instanceof List<?>) {
                                for (Object task : (List<?>) taskList) {
                                    tasks.add(String.valueOf(task));
                                }
                            }
                        }
                        itemList.add(new PlanWrapper(id, tasks));
                        adapter.notifyDataSetChanged();
                    }
                });
    }

    private void openFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE); // 🔹 важно!
        filePickerLauncher.launch(intent);
    }

    private void importFromCSV(Uri uri) {
        try (InputStream inputStream = requireContext().getContentResolver().openInputStream(uri);
             BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(";");
                if (parts.length >= 3) {
                    String title = parts[0];
                    String content = parts[1];
                    String dateTime = parts[2];
                    String mood = parts.length > 3 ? parts[3] : null;
                    List<String> tags = parts.length > 4 ? Arrays.asList(parts[4].split(",")) : new ArrayList<>();

                    Map<String, Object> note = new HashMap<>();
                    note.put("title", title);
                    note.put("content", content);
                    note.put("dateTime", dateTime);
                    note.put("mood", mood);
                    note.put("tags", tags);
                    note.put("userId", FirebaseAuth.getInstance().getCurrentUser().getUid());
                    db.collection("notes").add(note);
                }
            }
            Toast.makeText(getContext(), "Импорт CSV завершён", Toast.LENGTH_SHORT).show();
            loadData();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Ошибка импорта CSV", Toast.LENGTH_SHORT).show();
        }
    }

    private void importFromJSON(Uri uri) {
        try (InputStream inputStream = requireContext().getContentResolver().openInputStream(uri)) {
            StringBuilder builder = new StringBuilder();
            int ch;
            while ((ch = inputStream.read()) != -1) builder.append((char) ch);

            JSONArray array = new JSONArray(builder.toString());
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                String title = obj.getString("title");
                String content = obj.getString("content");
                String dateTime = obj.getString("dateTime");
                String mood = obj.optString("mood", null);
                List<String> tags = new ArrayList<>();
                JSONArray tagsArr = obj.optJSONArray("tags");
                if (tagsArr != null) {
                    for (int j = 0; j < tagsArr.length(); j++) {
                        tags.add(tagsArr.getString(j));
                    }
                }

                Map<String, Object> map = new HashMap<>();
                map.put("title", title);
                map.put("content", content);
                map.put("dateTime", dateTime);
                map.put("mood", mood);
                map.put("tags", tags);
                map.put("userId", FirebaseAuth.getInstance().getCurrentUser().getUid());

                db.collection("notes").add(map);
            }

            Toast.makeText(getContext(), "Импорт JSON завершён", Toast.LENGTH_SHORT).show();
            loadData();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Ошибка импорта JSON", Toast.LENGTH_SHORT).show();
        }
    }

    private void showExportFormatDialog() {
        String[] formats = {"CSV", "JSON", "PDF"};
        new AlertDialog.Builder(requireContext())
                .setTitle("Выберите формат")
                .setItems(formats, (dialog, which) -> {
                    switch (which) {
                        case 0: exportToCSV(); break;
                        case 1: exportToJSON(); break;
                        case 2: exportToPDF(); break;
                    }
                }).show();
    }

    private void exportToCSV() {
        db.collection("notes").get().addOnSuccessListener(snapshot -> {
            StringBuilder csv = new StringBuilder("Title;Content;Date;Mood;Tags\n");
            for (DocumentSnapshot doc : snapshot) {
                Note note = doc.toObject(Note.class);
                if (note != null) {
                    csv.append(sanitize(note.getTitle())).append(";")
                            .append(sanitize(note.getContent())).append(";")
                            .append(sanitize(note.getDateTime())).append(";")
                            .append(sanitize(note.getMood())).append(";")
                            .append(String.join(",", note.getTags())).append("\n");
                }
            }
            saveToFile("notes_export.csv", csv.toString());
        });
    }

    private void exportToJSON() {
        db.collection("notes").get().addOnSuccessListener(snapshot -> {
            JSONArray array = new JSONArray();
            for (DocumentSnapshot doc : snapshot) {
                Note note = doc.toObject(Note.class);
                if (note != null) {
                    JSONObject obj = new JSONObject();
                    try {
                        obj.put("title", note.getTitle());
                        obj.put("content", note.getContent());
                        obj.put("dateTime", note.getDateTime());
                        obj.put("mood", note.getMood());
                        obj.put("tags", new JSONArray(note.getTags()));
                        array.put(obj);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }
            saveToFile("notes_export.json", array.toString());
        });
    }

    private void exportToPDF() {
        db.collection("notes").get().addOnSuccessListener(snapshot -> {
            try {
                File file = new File(requireContext().getExternalFilesDir(null), "notes_export.pdf");
                PdfDocument pdf = new PdfDocument();
                Paint paint = new Paint();
                Paint bold = new Paint();
                bold.setFakeBoldText(true);
                bold.setTextSize(16);
                paint.setTextSize(14);

                int x = 40, y = 60, line = 25;
                PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
                PdfDocument.Page page = pdf.startPage(pageInfo);
                Canvas canvas = page.getCanvas();

                for (DocumentSnapshot doc : snapshot) {
                    Note note = doc.toObject(Note.class);
                    if (note == null) continue;

                    canvas.drawText("Заголовок: " + note.getTitle(), x, y, bold); y += line;
                    canvas.drawText("Содержимое: " + note.getContent(), x, y, paint); y += line;
                    canvas.drawText("Дата: " + note.getDateTime(), x, y, paint); y += line;
                    canvas.drawText("Настроение: " + note.getMood(), x, y, paint); y += line;
                    canvas.drawText("Теги: " + String.join(", ", note.getTags()), x, y, paint); y += line * 2;

                    if (y > 760) {
                        pdf.finishPage(page);
                        pageInfo = new PdfDocument.PageInfo.Builder(595, 842, pdf.getPages().size() + 1).create();
                        page = pdf.startPage(pageInfo);
                        canvas = page.getCanvas();
                        y = 60;
                    }
                }

                pdf.finishPage(page);
                FileOutputStream out = new FileOutputStream(file);
                pdf.writeTo(out);
                pdf.close();
                out.close();

                Toast.makeText(getContext(), "PDF сохранён: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
            } catch (Exception e) {
                e.printStackTrace();
                Toast.makeText(getContext(), "Ошибка PDF", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void saveToFile(String name, String content) {
        try {
            File file = new File(requireContext().getExternalFilesDir(null), name);
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(content.getBytes());
            fos.close();
            Toast.makeText(getContext(), "Сохранено: " + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Ошибка сохранения", Toast.LENGTH_SHORT).show();
        }
    }

    private String sanitize(String input) {
        return input != null ? input.replaceAll("[;\"]", " ") : "";
    }
}
