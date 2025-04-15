package com.example.kurso;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.GridLayout;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;

public class GiftDialogFragment extends DialogFragment {
    private String recipientUserId;
    private FirebaseFirestore db;
    private FirebaseAuth auth;

    private static final String[] GIFT_TYPES = {
        "flower", "heart", "star", "cake", "gift"
    };

    private static final int[] GIFT_ICONS = {
        R.drawable.ic_gift_flower,
        R.drawable.ic_gift_heart,
        R.drawable.ic_gift_star,
        R.drawable.ic_gift_cake,
        R.drawable.ic_gift_present
    };

    public static GiftDialogFragment newInstance(String recipientUserId) {
        GiftDialogFragment fragment = new GiftDialogFragment();
        Bundle args = new Bundle();
        args.putString("recipientUserId", recipientUserId);
        fragment.setArguments(args);
        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();

        if (getArguments() != null) {
            recipientUserId = getArguments().getString("recipientUserId");
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        LayoutInflater inflater = requireActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.dialog_gift_selection, null);

        GridLayout giftGrid = view.findViewById(R.id.giftGrid);
        setupGiftGrid(giftGrid);

        return builder.setView(view)
                     .setTitle("Выберите подарок")
                     .setNegativeButton("Отмена", (dialog, id) -> dialog.cancel())
                     .create();
    }

    private void setupGiftGrid(GridLayout giftGrid) {
        for (int i = 0; i < GIFT_TYPES.length; i++) {
            ImageView giftIcon = new ImageView(requireContext());
            giftIcon.setImageResource(GIFT_ICONS[i]);
            
            GridLayout.LayoutParams params = new GridLayout.LayoutParams();
            params.width = GridLayout.LayoutParams.WRAP_CONTENT;
            params.height = GridLayout.LayoutParams.WRAP_CONTENT;
            params.setMargins(16, 16, 16, 16);
            giftIcon.setLayoutParams(params);

            final int giftIndex = i;
            giftIcon.setOnClickListener(v -> sendGift(GIFT_TYPES[giftIndex]));
            
            giftGrid.addView(giftIcon);
        }
    }

    private void sendGift(String giftType) {
        if (auth.getCurrentUser() == null || recipientUserId == null) return;

        String currentUserId = auth.getCurrentUser().getUid();
        
        // Получаем имя текущего пользователя
        db.collection("users").document(currentUserId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                User currentUser = documentSnapshot.toObject(User.class);
                if (currentUser != null) {
                    Gift gift = new Gift(currentUserId, currentUser.getDisplayName(), recipientUserId, giftType);

                    db.collection("gifts")
                        .add(gift)
                        .addOnSuccessListener(documentReference -> {
                            gift.setGiftId(documentReference.getId());
                            documentReference.set(gift)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(getContext(), "Подарок отправлен!", Toast.LENGTH_SHORT).show();
                                    dismiss();
                                })
                                .addOnFailureListener(e -> 
                                    Toast.makeText(getContext(), "Ошибка при отправке подарка: " + e.getMessage(), 
                                        Toast.LENGTH_SHORT).show()
                                );
                        })
                        .addOnFailureListener(e -> 
                            Toast.makeText(getContext(), "Ошибка при отправке подарка: " + e.getMessage(), 
                                Toast.LENGTH_SHORT).show()
                        );
                }
            })
            .addOnFailureListener(e -> 
                Toast.makeText(getContext(), "Ошибка при получении данных пользователя: " + e.getMessage(), 
                    Toast.LENGTH_SHORT).show()
            );
    }
} 