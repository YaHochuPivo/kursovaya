package com.example.kurso;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.DocumentsContract;
import android.util.Log;
import android.view.*;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.SearchView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.ListenerRegistration;

import java.text.SimpleDateFormat;
import java.util.*;

public class NotesFragment extends Fragment {

    private RecyclerView recyclerView;
    private NotesAdapter adapter;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private final List<Object> allItems = new ArrayList<>();

    private String importFormat = null;
    private String exportFormat = null;
    private boolean allowFilePicker = false;
    private TextView selectedCountText;
    private Button btnCancelSelection;
    private List<Object> itemsToExport;

    private ActivityResultLauncher<Intent> filePickerLauncher;
    private ActivityResultLauncher<Intent> folderPickerLauncher;

    private SearchView searchView;

    private ListenerRegistration notesListener;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        setHasOptionsMenu(true);
        View view = inflater.inflate(R.layout.fragment_notes, container, false);

        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new NotesAdapter(getContext(), new ArrayList<>());
        recyclerView.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        Button btnAddNote = view.findViewById(R.id.btnAddNote);
        Button btnAddPlan = view.findViewById(R.id.btnAddPlan);
        Button btnImport = view.findViewById(R.id.btnImport);
        Button btnExport = view.findViewById(R.id.btnExport);
        btnCancelSelection = view.findViewById(R.id.btnCancelSelection);
        selectedCountText = view.findViewById(R.id.selectedCountText);

        searchView = view.findViewById(R.id.searchView);

        btnAddNote.setOnClickListener(v -> startActivity(new Intent(getContext(), CreateNoteActivity.class)));
        btnAddPlan.setOnClickListener(v -> startActivity(new Intent(getContext(), DailyPlanActivity.class)));
        btnImport.setOnClickListener(v -> showFormatSelectionDialog("import"));
        
        // Обработчик обычного клика для экспорта
        btnExport.setOnClickListener(v -> {
            if (adapter.isSelectionMode() && adapter.getSelectedCount() > 0) {
                showFormatSelectionDialog("export");
            } else {
                Toast.makeText(getContext(), "Выберите элементы для экспорта", Toast.LENGTH_SHORT).show();
            }
        });

        // Обработчик длительного нажатия для включения режима выбора
        btnExport.setOnLongClickListener(v -> {
            if (!adapter.isSelectionMode()) {
                adapter.setSelectionMode(true);
                Toast.makeText(getContext(), "Выберите элементы для экспорта", Toast.LENGTH_SHORT).show();
                return true;
            }
            return false;
        });

        adapter.setOnSelectionChangeListener(() -> {
            int selectedCount = adapter.getSelectedCount();
            if (selectedCount > 0) {
                selectedCountText.setText("Выбрано: " + selectedCount);
                selectedCountText.setVisibility(View.VISIBLE);
                btnCancelSelection.setVisibility(View.VISIBLE);
            } else {
                selectedCountText.setVisibility(View.GONE);
                btnCancelSelection.setVisibility(View.GONE);
            }
        });

        btnCancelSelection.setOnClickListener(v -> {
            adapter.clearSelection();
            selectedCountText.setVisibility(View.GONE);
            btnCancelSelection.setVisibility(View.GONE);
        });

        initFilePickers();
        loadData();

        setupSearchView();

        return view;
    }

    private void setupSearchView() {
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                adapter.filter(newText);
                return true;
            }
        });
    }

    private void updateSelectionUI() {
        if (adapter.isSelectionMode()) {
            btnCancelSelection.setVisibility(View.VISIBLE);
            selectedCountText.setVisibility(View.VISIBLE);
            int selectedCount = adapter.getSelectedItems().size();
            selectedCountText.setText("Выбрано: " + selectedCount);
        } else {
            btnCancelSelection.setVisibility(View.GONE);
            selectedCountText.setVisibility(View.GONE);
        }
    }

    private void initFilePickers() {
        filePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (!allowFilePicker) return;
                    allowFilePicker = false;

                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null && importFormat != null) {
                            Log.d("NotesFragment", "Начало импорта файла. URI: " + uri + ", формат: " + importFormat);
                            // Показываем прогресс
                            Toast.makeText(getContext(), "Импорт начат...", Toast.LENGTH_SHORT).show();
                            
                            FileUtils.importFromUri(requireContext(), uri, importFormat, new ImportCallback() {
                                @Override
                                public void onImportComplete(List<Object> importedItems) {
                                    if (importedItems != null && !importedItems.isEmpty()) {
                                        Log.d("NotesFragment", "Успешно импортировано элементов: " + importedItems.size());
                                        saveImportedItemsToFirebase(importedItems);
                                    } else {
                                        Log.e("NotesFragment", "Импорт вернул пустой список");
                                        Toast.makeText(getContext(), "Импорт не удался: файл пуст или имеет неверный формат", Toast.LENGTH_LONG).show();
                                    }
                                }
                            });
                        } else {
                            Log.e("NotesFragment", "URI или формат null. URI: " + uri + ", формат: " + importFormat);
                            Toast.makeText(getContext(), "Ошибка: не выбран файл или формат", Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Log.d("NotesFragment", "Пользователь отменил выбор файла");
                    }
                });

        folderPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        Uri uri = result.getData().getData();
                        if (uri != null && exportFormat != null && itemsToExport != null) {
                            requireContext().getContentResolver().takePersistableUriPermission(
                                uri, Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                            FileUtils.exportDataToUri(requireContext(), itemsToExport, exportFormat, uri);
                            adapter.setSelectionMode(false);
                            updateSelectionUI();
                        }
                    }
                });
    }

    private void saveImportedItemsToFirebase(List<Object> importedItems) {
        if (getContext() == null) {
            Log.e("NotesFragment", "Context is null, cannot proceed with import");
            return;
        }

        if (importedItems == null || importedItems.isEmpty()) {
            Toast.makeText(getContext(), "Нет данных для импорта", Toast.LENGTH_SHORT).show();
            return;
        }

        if (auth == null || auth.getCurrentUser() == null) {
            Toast.makeText(getContext(), "Необходимо войти в систему", Toast.LENGTH_SHORT).show();
            return;
        }

        String userId = auth.getCurrentUser().getUid();
        int totalItems = importedItems.size();
        final int[] savedItems = {0};
        final int[] failedItems = {0};

        for (Object item : importedItems) {
            try {
                if (item instanceof Note) {
                    saveNote((Note) item, userId, savedItems, failedItems, totalItems);
                } else if (item instanceof PlanWrapper) {
                    savePlan((PlanWrapper) item, userId, savedItems, failedItems, totalItems);
                } else {
                    Log.e("NotesFragment", "Неизвестный тип элемента: " + (item != null ? item.getClass().getName() : "null"));
                    failedItems[0]++;
                    checkImportCompletion(savedItems[0], failedItems[0], totalItems);
                }
            } catch (Exception e) {
                Log.e("NotesFragment", "Ошибка при сохранении элемента: " + e.getMessage(), e);
                failedItems[0]++;
                if (getContext() != null) {
                    Toast.makeText(getContext(), 
                        "Ошибка сохранения: " + e.getMessage(), 
                        Toast.LENGTH_SHORT).show();
                }
                checkImportCompletion(savedItems[0], failedItems[0], totalItems);
            }
        }
    }

    private void saveNote(Note note, String userId, final int[] savedItems, final int[] failedItems, int totalItems) {
        if (note == null || db == null) return;

        String noteId = UUID.randomUUID().toString();
        Map<String, Object> noteData = new HashMap<>();
        noteData.put("id", noteId);
        noteData.put("title", note.getTitle());
        noteData.put("content", note.getContent());
        noteData.put("mood", note.getMood());
        noteData.put("tags", note.getTags() != null ? note.getTags() : new ArrayList<>());
        noteData.put("userId", userId);

        // Обработка временных меток
        long timestamp = System.currentTimeMillis();
        if (note.getTimestamp() != null) {
            timestamp = note.getTimestamp().getSeconds() * 1000;
        } else if (note.getCreatedAt() > 0) {
            timestamp = note.getCreatedAt();
        }
        
        noteData.put("timestamp", new Date(timestamp));
        noteData.put("createdAt", timestamp);
        
        Log.d("NotesFragment", "Сохранение заметки: " + note.getTitle() + ", время создания: " + new Date(timestamp));
        
        db.collection("notes")
            .document(noteId)
            .set(noteData)
            .addOnSuccessListener(aVoid -> {
                savedItems[0]++;
                Log.d("NotesFragment", "Заметка сохранена успешно: " + note.getTitle());
                checkImportCompletion(savedItems[0], failedItems[0], totalItems);
            })
            .addOnFailureListener(e -> {
                failedItems[0]++;
                Log.e("NotesFragment", "Ошибка сохранения заметки: " + e.getMessage());
                if (getContext() != null) {
                    Toast.makeText(getContext(), 
                        "Ошибка сохранения заметки: " + e.getMessage(), 
                        Toast.LENGTH_SHORT).show();
                }
                checkImportCompletion(savedItems[0], failedItems[0], totalItems);
            });
    }

    private void savePlan(PlanWrapper plan, String userId, final int[] savedItems, final int[] failedItems, int totalItems) {
        if (plan == null || db == null) return;

        try {
            // Get start of day timestamp
            Calendar calendar = Calendar.getInstance();
            calendar.set(Calendar.HOUR_OF_DAY, 0);
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
            long timestamp = calendar.getTimeInMillis();

            Map<String, Object> planData = new HashMap<>();
            planData.put("timestamp", timestamp);
            planData.put("userId", userId);
            
            List<Map<String, Object>> tasksList = new ArrayList<>();
            if (plan.getTasks() != null) {
                for (TaskItem task : plan.getTasks()) {
                    if (task != null) {
                        Map<String, Object> taskMap = new HashMap<>();
                        taskMap.put("text", task.getText());
                        taskMap.put("time", task.getTime());
                        taskMap.put("done", task.isDone());
                        tasksList.add(taskMap);
                    }
                }
            }
            planData.put("tasks", tasksList);

            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
            Log.d("NotesFragment", "Сохранение плана. Timestamp: " + sdf.format(new Date(timestamp)) + 
                ", Задач: " + tasksList.size());

            db.collection("daily_plans")
                .add(planData)
                .addOnSuccessListener(documentReference -> {
                    savedItems[0]++;
                    Log.d("NotesFragment", "План сохранен с ID: " + documentReference.getId());
                    loadData(); // Перезагружаем данные после сохранения
                })
                .addOnFailureListener(e -> {
                    failedItems[0]++;
                    Log.e("NotesFragment", "Ошибка при сохранении плана", e);
                });
        } catch (Exception e) {
            Log.e("NotesFragment", "Ошибка при подготовке данных плана: " + e.getMessage(), e);
            failedItems[0]++;
            checkImportCompletion(savedItems[0], failedItems[0], totalItems);
        }
    }

    private void checkImportCompletion(int savedItems, int failedItems, int totalItems) {
        if (savedItems + failedItems == totalItems) {
            String message = String.format("Импорт завершён. Успешно: %d, С ошибками: %d", 
                savedItems, failedItems);
            Log.d("NotesFragment", message);
            Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
            
            if (savedItems > 0) {
                loadData(); // Перезагружаем данные только если что-то было сохранено
            }
        }
    }

    private void showFormatSelectionDialog(String action) {
        String[] formats = {"JSON", "CSV", "PDF"};
        new AlertDialog.Builder(requireContext())
                .setTitle(action.equals("import") ? "Выберите формат импорта" : "Выберите формат экспорта")
                .setItems(formats, (dialog, which) -> {
                    String format = formats[which].toLowerCase(Locale.ROOT);
                    if (action.equals("import")) {
                        importFormat = format;
                        allowFilePicker = true;
                        showImportFilePicker();
                    } else {
                        exportFormat = format;
                        itemsToExport = adapter.getSelectedItems();
                        showExportFolderPicker(format);
                    }
                })
                .show();
    }

    private void showImportFilePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        filePickerLauncher.launch(Intent.createChooser(intent, "Выберите файл для импорта"));
    }

    private void showExportFolderPicker(String format) {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        String mimeType;
        switch (format) {
            case "pdf":
                mimeType = "application/pdf";
                break;
            case "json":
                mimeType = "application/json";
                break;
            case "csv":
                mimeType = "text/csv";
                break;
            default:
                mimeType = "*/*";
        }
        intent.setType(mimeType);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.putExtra(Intent.EXTRA_TITLE, "export_" + 
            new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()) +
            "." + format);
        folderPickerLauncher.launch(intent);
    }

    private void loadData() {
        if (db == null || auth == null || auth.getCurrentUser() == null) return;

        String userId = auth.getCurrentUser().getUid();
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        calendar.set(Calendar.MINUTE, 0);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);
        long startOfDay = calendar.getTimeInMillis();

        calendar.add(Calendar.DAY_OF_MONTH, 1);
        long endOfDay = calendar.getTimeInMillis();

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());
        Log.d("NotesFragment", "Загрузка планов. Start: " + sdf.format(new Date(startOfDay)) + 
            ", End: " + sdf.format(new Date(endOfDay)) + ", UserId: " + userId);

        // Load daily plans
        db.collection("daily_plans")
            .whereEqualTo("userId", userId)
            .get()
            .addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    List<PlanWrapper> plans = new ArrayList<>();
                    for (QueryDocumentSnapshot document : task.getResult()) {
                        try {
                            Long timestamp = document.getLong("timestamp");
                            if (timestamp == null) {
                                Log.e("NotesFragment", "План " + document.getId() + " не содержит timestamp");
                                continue;
                            }

                            // Проверяем, что план относится к текущему дню
                            if (timestamp >= startOfDay && timestamp < endOfDay) {
                                Log.d("NotesFragment", "Найден план с timestamp: " + sdf.format(new Date(timestamp)));
                                
                                List<Map<String, Object>> tasksData = (List<Map<String, Object>>) document.get("tasks");
                                List<TaskItem> tasks = new ArrayList<>();
                                
                                if (tasksData != null) {
                                    for (Map<String, Object> taskData : tasksData) {
                                        String text = (String) taskData.get("text");
                                        String time = (String) taskData.get("time");
                                        boolean done = taskData.get("done") != null ? (boolean) taskData.get("done") : false;
                                        tasks.add(new TaskItem(text, time, done));
                                        Log.d("NotesFragment", "Добавлена задача: " + text + " (" + time + ")");
                                    }
                                }
                                
                                Plan plan = new Plan();
                                plan.setId(document.getId());
                                plan.setTasks(tasks);
                                plan.setTimestamp(timestamp);
                                plans.add(new PlanWrapper(plan));
                                
                                Log.d("NotesFragment", "План обработан: ID=" + plan.getId() + 
                                    ", задач=" + tasks.size());
                            } else {
                                Log.d("NotesFragment", "План " + document.getId() + " не относится к текущему дню: " + 
                                    sdf.format(new Date(timestamp)));
                            }
                        } catch (Exception e) {
                            Log.e("NotesFragment", "Ошибка при обработке плана: " + e.getMessage(), e);
                        }
                    }
                    Log.d("NotesFragment", "Загружено планов: " + plans.size());
                    
                    // Load notes
                    loadNotes(userId, plans);
                } else {
                    Log.e("NotesFragment", "Ошибка при загрузке планов", task.getException());
                    loadNotes(userId, new ArrayList<>());
                }
            });
    }

    private void loadNotes(String userId, List<PlanWrapper> plans) {
        if (getContext() == null) {
            Log.e("NotesFragment", "Context is null in loadNotes");
            return;
        }

        if (adapter == null) {
            Log.e("NotesFragment", "Adapter is null in loadNotes");
            adapter = new NotesAdapter(getContext(), new ArrayList<>());
            if (recyclerView != null) {
                recyclerView.setAdapter(adapter);
            }
        }
        
        notesListener = db.collection("notes")
                .whereEqualTo("userId", userId)
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .addSnapshotListener((snapshot, error) -> {
                    if (error != null) {
                        Log.e("NotesFragment", "Ошибка при получении заметок", error);
                        if (getContext() != null) {
                            Toast.makeText(getContext(), "Ошибка загрузки заметок: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                        return;
                    }

                    if (snapshot != null) {
                        List<Object> newItems = new ArrayList<>();
                        
                        // Сначала добавляем план на день, если он есть
                        if (!plans.isEmpty()) {
                            newItems.addAll(plans);
                            Log.d("NotesFragment", "Добавлен план на день с " + plans.get(0).getTasks().size() + " задачами");
                        }
                        
                        Log.d("NotesFragment", "Получены заметки. План сохранен: " + (plans.size() > 0));
                        
                        // Затем добавляем заметки
                        for (DocumentSnapshot doc : snapshot.getDocuments()) {
                            try {
                                Note note = doc.toObject(Note.class);
                                if (note != null) {
                                    note.setId(doc.getId());
                                    newItems.add(note);
                                    Log.d("NotesFragment", "Добавлена заметка: " + note.getTitle());
                                }
                            } catch (Exception e) {
                                Log.e("NotesFragment", "Ошибка при обработке заметки: " + doc.getId(), e);
                            }
                        }

                        // Обновляем список и адаптер в UI потоке
                        if (getActivity() != null) {
                            getActivity().runOnUiThread(() -> {
                                try {
                                    allItems.clear();
                                    allItems.addAll(newItems);
                                    
                                    if (adapter != null) {
                                        adapter.setItems(new ArrayList<>(allItems));
                                        adapter.notifyDataSetChanged();
                                        
                                        Log.d("NotesFragment", "Обновлен список: " + allItems.size() + 
                                            " элементов (план: " + (!plans.isEmpty()) + ")");
                                            
                                    } else {
                                        Log.e("NotesFragment", "Adapter is null during update");
                                    }
                                } catch (Exception e) {
                                    Log.e("NotesFragment", "Ошибка при обновлении UI", e);
                                }
                            });
                        }
                    }
                });
    }

    @Override
    public void onResume() {
        super.onResume();
        // Перезагружаем данные при возвращении к фрагменту
        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
            loadData();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Отписываемся от обновлений при уничтожении представления
        if (notesListener != null) {
            notesListener.remove();
        }
    }
}
