package com.example.kurso;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import java.util.ArrayList;
import java.util.List;

public class MyGiftsFragment extends Fragment {
    private static final String TAG = "MyGiftsFragment";
    
    private RecyclerView giftsRecyclerView;
    private GiftAdapter giftAdapter;
    private List<Gift> gifts = new ArrayList<>();
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_my_gifts, container, false);

        // Настройка кнопки "Назад"
        ImageButton backButton = view.findViewById(R.id.backButton);
        backButton.setOnClickListener(v -> {
            if (getActivity() instanceof MainActivity) {
                MainActivity activity = (MainActivity) getActivity();
                
                // Показываем нижнюю навигацию
                activity.showBottomNavigation();
                
                // Переходим на вкладку "Заметки"
                NavController navController = Navigation.findNavController(activity, R.id.nav_host_fragment);
                navController.navigate(R.id.nav_notes);
            }
        });

        // Инициализация списка подарков
        giftsRecyclerView = view.findViewById(R.id.giftsRecyclerView);
        giftAdapter = new GiftAdapter(gifts);
        giftsRecyclerView.setLayoutManager(new GridLayoutManager(requireContext(), 3));
        giftsRecyclerView.setAdapter(giftAdapter);

        // Загрузка подарков
        loadGifts();

        return view;
    }

    private void loadGifts() {
        String currentUserId = auth.getCurrentUser() != null ? auth.getCurrentUser().getUid() : null;
        if (currentUserId == null) {
            Log.e(TAG, "Cannot load gifts: user is not logged in");
            return;
        }

        Log.d(TAG, "Loading gifts for current user: " + currentUserId);
        
        db.collection("gifts")
            .whereEqualTo("toUserId", currentUserId)
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
                // Сортируем подарки по времени
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