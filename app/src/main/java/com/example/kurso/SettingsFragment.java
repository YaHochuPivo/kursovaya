package com.example.kurso;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageMetadata;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class SettingsFragment extends Fragment {
    private static final int PERMISSION_REQUEST_CODE = 123;
    private static final int PICK_IMAGE_REQUEST = 1;
    private static final String TAG = "SettingsFragment";
    
    private TextView displayNameText;
    private TextView emailText;
    private TextView bioText;
    private TextView creationDateText;
    private Button editProfileButton;
    private Button changePasswordButton;
    private Button logoutButton;
    private ImageButton backButton;
    private ImageView profileImageView;
    private FirebaseAuth auth;
    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private StorageReference storageRef;
    private FirebaseUser currentUser;
    private ActivityResultLauncher<String[]> permissionLauncher;
    private ActivityResultLauncher<Intent> imagePickerLauncher;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Инициализация Firebase компонентов
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        // Инициализация launcher'а для запроса разрешений
        permissionLauncher = registerForActivityResult(
            new ActivityResultContracts.RequestMultiplePermissions(),
            permissions -> {
                boolean allGranted = true;
                for (Boolean isGranted : permissions.values()) {
                    allGranted = allGranted && isGranted;
                }
                if (allGranted) {
                    startImagePicker();
                } else {
                    Toast.makeText(getContext(), "Необходимо разрешение для выбора фото", Toast.LENGTH_SHORT).show();
                }
            }
        );

        imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    if (imageUri != null) {
                        saveProfileImage(imageUri);
                    } else {
                        Toast.makeText(getContext(), "Не удалось получить изображение", Toast.LENGTH_SHORT).show();
                    }
                }
            }
        );
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        // Инициализация компонентов
        displayNameText = view.findViewById(R.id.displayNameText);
        emailText = view.findViewById(R.id.emailText);
        bioText = view.findViewById(R.id.bioText);
        creationDateText = view.findViewById(R.id.creationDateText);
        editProfileButton = view.findViewById(R.id.editProfileButton);
        changePasswordButton = view.findViewById(R.id.changePasswordButton);
        logoutButton = view.findViewById(R.id.logoutButton);
        backButton = view.findViewById(R.id.backButton);
        profileImageView = view.findViewById(R.id.profileImageView);

        // Загружаем информацию о пользователе
        loadUserInfo();
        // Загружаем фото профиля
        loadProfileImage();

        editProfileButton.setOnClickListener(v -> showEditProfileDialog());
        changePasswordButton.setOnClickListener(v -> showChangePasswordDialog());
        
        // Добавляем обработчик для кнопки выхода
        logoutButton.setOnClickListener(v -> {
            // Выходим из аккаунта
            auth.signOut();
            
            // Переходим на экран входа
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            
            // Закрываем текущую активность
            if (getActivity() != null) {
                getActivity().finish();
            }
        });
        
        // Обработка нажатия кнопки "Назад"
        backButton.setOnClickListener(v -> {
            if (isAdded() && getActivity() != null) {
                // Делаем кнопку временно неактивной для предотвращения двойных нажатий
                backButton.setEnabled(false);
                
                try {
                    // Возвращаемся к предыдущему фрагменту немедленно
                    getParentFragmentManager().popBackStackImmediate();
                    
                    // Отправляем сообщение в MainActivity для обновления UI
                    if (getActivity() instanceof MainActivity) {
                        ((MainActivity) getActivity()).showBottomNavigation();
                    }
                } catch (Exception e) {
                    Log.e("SettingsFragment", "Error during navigation: " + e.getMessage());
                }
                
                // Возвращаем активность кнопке через небольшую задержку
                backButton.postDelayed(() -> backButton.setEnabled(true), 300);
            }
        });

        ImageButton changePhotoButton = view.findViewById(R.id.changePhotoButton);
        changePhotoButton.setOnClickListener(v -> openImagePicker());

        // Настройка кнопки "Мои подарки"
        view.findViewById(R.id.myGiftsButton).setOnClickListener(v -> {
            if (isAdded() && getActivity() != null) {
                MyGiftsFragment myGiftsFragment = new MyGiftsFragment();
                getParentFragmentManager()
                    .beginTransaction()
                    .replace(R.id.fragmentContainer, myGiftsFragment)
                    .addToBackStack(null)
                    .commit();
            }
        });

        return view;
    }

    private void loadUserInfo() {
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Log.e(TAG, "User is not authenticated");
            return;
        }

        // Загружаем основную информацию из FirebaseUser
        String email = user.getEmail();
        String displayName = user.getDisplayName();
        
        emailText.setText(getString(R.string.email_format, email != null ? email : "Не указан"));
        displayNameText.setText(getString(R.string.name_format, displayName != null ? displayName : "Не указано"));

        // Форматируем дату создания аккаунта
        if (user.getMetadata() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm", Locale.getDefault());
            String creationDate = sdf.format(new Date(user.getMetadata().getCreationTimestamp()));
            creationDateText.setText(getString(R.string.registration_date_format, creationDate));
        }

        // Загружаем дополнительную информацию из Firestore
        db.collection("users")
                .document(user.getUid())
                .get()
                .addOnSuccessListener(document -> {
                    if (document.exists()) {
                        // Загружаем био
                        String bio = document.getString("bio");
                        bioText.setText(getString(R.string.bio_format, bio != null ? bio : "Не указано"));

                        // Проверяем наличие фото профиля
                        String imagePath = document.getString("profileImagePath");
                        if (imagePath != null && !imagePath.isEmpty()) {
                            File imageFile = new File(imagePath);
                            if (imageFile.exists()) {
                                loadImageWithGlide(Uri.fromFile(imageFile));
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading user info: " + e.getMessage());
                    if (getContext() != null) {
                        Toast.makeText(getContext(), "Ошибка загрузки информации: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void showEditProfileDialog() {
        EditProfileDialog dialog = new EditProfileDialog(requireContext());
        dialog.setOnDismissListener(dialogInterface -> loadUserInfo());
        dialog.show();
    }

    private void showChangePasswordDialog() {
        ChangePasswordDialog dialog = new ChangePasswordDialog(requireContext());
        dialog.show();
    }

    private void checkAndRequestPermissions() {
        if (getContext() == null) return;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // Android 13 и выше
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_MEDIA_IMAGES)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionLauncher.launch(new String[]{Manifest.permission.READ_MEDIA_IMAGES});
            } else {
                startImagePicker();
            }
        } else {
            // Android 12 и ниже
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionLauncher.launch(new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE
                });
            } else {
                startImagePicker();
            }
        }
    }

    private void startImagePicker() {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType("image/*");
        String[] mimeTypes = {"image/jpeg", "image/png", "image/jpg"};
        intent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);
        
        try {
            imagePickerLauncher.launch(intent);
        } catch (Exception e) {
            Toast.makeText(getContext(), "Ошибка при открытии галереи: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            e.printStackTrace();
        }
    }

    private void openImagePicker() {
        checkAndRequestPermissions();
    }

    private void saveProfileImage(Uri sourceUri) {
        if (getContext() == null) {
            Log.e(TAG, "Context is null");
            return;
        }

        // Проверяем авторизацию
        FirebaseUser user = auth.getCurrentUser();
        if (user == null) {
            Log.e(TAG, "User is not authenticated");
            Toast.makeText(getContext(), "Необходимо авторизоваться", Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            // Проверяем существование файла
            if (!isFileExists(sourceUri)) {
                Log.e(TAG, "File does not exist: " + sourceUri);
                Toast.makeText(getContext(), "Файл не существует или недоступен", Toast.LENGTH_SHORT).show();
                return;
            }

            // Проверяем MIME-тип файла
            String mimeType = requireContext().getContentResolver().getType(sourceUri);
            Log.d(TAG, "File MIME type: " + mimeType);
            
            if (mimeType == null || !mimeType.startsWith("image/")) {
                Log.e(TAG, "Invalid MIME type: " + mimeType);
                Toast.makeText(getContext(), "Пожалуйста, выберите изображение (JPG, PNG)", Toast.LENGTH_SHORT).show();
                return;
            }

            // Проверяем размер файла
            long fileSize = getFileSize(sourceUri);
            Log.d(TAG, "File size: " + fileSize + " bytes");
            
            if (fileSize == -1) {
                Log.e(TAG, "Could not determine file size");
                Toast.makeText(getContext(), "Не удалось определить размер файла", Toast.LENGTH_SHORT).show();
                return;
            }
            
            if (fileSize > 5 * 1024 * 1024) {
                Log.e(TAG, "File too large: " + fileSize + " bytes");
                Toast.makeText(getContext(), "Размер изображения не должен превышать 5MB", Toast.LENGTH_SHORT).show();
                return;
            }

            Toast.makeText(getContext(), "Подготовка изображения...", Toast.LENGTH_SHORT).show();

            // Создаем временный файл для сжатия
            Log.d(TAG, "Starting image compression");
            Bitmap bitmap = null;
            try {
                bitmap = MediaStore.Images.Media.getBitmap(requireContext().getContentResolver(), sourceUri);
                Log.d(TAG, "Original image size: " + bitmap.getWidth() + "x" + bitmap.getHeight());
            } catch (Exception e) {
                Log.e(TAG, "Error reading bitmap: " + e.getMessage());
                Toast.makeText(getContext(), "Ошибка чтения изображения: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                return;
            }

            // Сжимаем изображение
            int quality = fileSize > 1024 * 1024 ? 50 : 70;
            Log.d(TAG, "Compressing with quality: " + quality);

            // Создаем имя файла
            String fileName = "profile_" + user.getUid() + ".jpg";

            // Сохраняем файл во внутреннее хранилище
            File internalStorageDir = new File(requireContext().getFilesDir(), "profile_images");
            if (!internalStorageDir.exists()) {
                internalStorageDir.mkdirs();
            }

            File outputFile = new File(internalStorageDir, fileName);
            FileOutputStream fos = null;
            try {
                fos = new FileOutputStream(outputFile);
                bitmap.compress(Bitmap.CompressFormat.JPEG, quality, fos);
                fos.flush();
                Log.d(TAG, "Image saved to: " + outputFile.getAbsolutePath());

                // Обновляем информацию в Firestore
                updateProfileImagePath(outputFile.getAbsolutePath());

                // Загружаем изображение в ImageView
                loadImageWithGlide(Uri.fromFile(outputFile));

                Toast.makeText(getContext(), "Фото профиля обновлено", Toast.LENGTH_SHORT).show();
            } catch (Exception e) {
                Log.e(TAG, "Error saving image: " + e.getMessage());
                Toast.makeText(getContext(), "Ошибка сохранения изображения: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            } finally {
                try {
                    if (fos != null) {
                        fos.close();
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Error closing stream: " + e.getMessage());
                }
                if (bitmap != null) {
                    bitmap.recycle();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "General error: " + e.getMessage(), e);
            if (getContext() != null) {
                Toast.makeText(getContext(), "Ошибка: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void updateProfileImagePath(String imagePath) {
        if (auth.getCurrentUser() == null || getContext() == null) return;

        DocumentReference userRef = db.collection("users").document(auth.getCurrentUser().getUid());
        Map<String, Object> updates = new HashMap<>();
        updates.put("profileImagePath", imagePath);
        updates.put("lastUpdated", FieldValue.serverTimestamp());

        userRef.update(updates)
            .addOnSuccessListener(aVoid -> {
                if (getContext() != null) {
                    Log.d(TAG, "Profile image path updated in Firestore");
                }
            })
            .addOnFailureListener(e -> {
                if (getContext() != null) {
                    Log.e(TAG, "Error updating profile image path: " + e.getMessage());
                }
            });
    }

    private void loadProfileImage() {
        if (auth.getCurrentUser() == null) return;

        db.collection("users").document(auth.getCurrentUser().getUid())
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        String imagePath = documentSnapshot.getString("profileImagePath");
                        if (imagePath != null && !imagePath.isEmpty()) {
                            File imageFile = new File(imagePath);
                            if (imageFile.exists()) {
                                loadImageWithGlide(Uri.fromFile(imageFile));
                            }
                        }
                    }
                });
    }

    private void loadImageWithGlide(Uri uri) {
        if (getContext() == null || !isAdded()) return;

        try {
            Glide.with(requireContext())
                .load(uri)
                .placeholder(R.drawable.default_avatar)
                .error(R.drawable.default_avatar)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .skipMemoryCache(false)
                .into(profileImageView);
        } catch (Exception e) {
            if (getContext() != null) {
                Log.e(TAG, "Error loading image: " + e.getMessage());
                Toast.makeText(getContext(), "Ошибка загрузки изображения: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private boolean isFileExists(Uri uri) {
        try {
            ParcelFileDescriptor pfd = requireContext().getContentResolver().openFileDescriptor(uri, "r");
            if (pfd != null) {
                pfd.close();
                return true;
            }
        } catch (Exception e) {
            Log.e(TAG, "Error checking file existence: " + e.getMessage());
        }
        return false;
    }

    private long getFileSize(Uri fileUri) {
        try {
            ParcelFileDescriptor pfd = requireContext().getContentResolver().openFileDescriptor(fileUri, "r");
            if (pfd != null) {
                long size = pfd.getStatSize();
                pfd.close();
                return size;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    private String getFileExtension(String mimeType) {
        switch (mimeType.toLowerCase()) {
            case "image/jpeg":
            case "image/jpg":
                return "jpg";
            case "image/png":
                return "png";
            default:
                return "jpg";
        }
    }
} 