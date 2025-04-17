package com.example.kurso;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.SearchView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import java.util.ArrayList;
import java.util.List;

public class FriendsFragment extends Fragment implements FriendsAdapter.OnFriendClickListener {
    private RecyclerView recyclerView;
    private FriendsAdapter adapter;
    private FirebaseFirestore db;
    private FirebaseAuth auth;
    private List<User> friendsList;
    private SearchView searchView;
    private ImageButton searchButton;
    private boolean isSearchMode = false;

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_friends, container, false);

        // Инициализация компонентов
        recyclerView = view.findViewById(R.id.friendsRecyclerView);
        searchView = view.findViewById(R.id.searchView);
        searchButton = view.findViewById(R.id.searchButton);

        db = FirebaseFirestore.getInstance();
        auth = FirebaseAuth.getInstance();
        friendsList = new ArrayList<>();

        // Настройка RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new FriendsAdapter(getContext(), friendsList, this);
        recyclerView.setAdapter(adapter);

        // Настройка поиска
        searchButton.setOnClickListener(v -> toggleSearchMode());
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                if (isSearchMode) {
                    searchUsers(query);
                }
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (isSearchMode && newText.length() >= 3) {
                    searchUsers(newText);
                }
                return true;
            }
        });

        // Загрузка списка друзей
        loadFriends();

        return view;
    }

    private void toggleSearchMode() {
        isSearchMode = !isSearchMode;
        searchView.setVisibility(isSearchMode ? View.VISIBLE : View.GONE);
        searchButton.setImageResource(isSearchMode ? R.drawable.ic_close : R.drawable.ic_search);
        adapter.setSearchMode(isSearchMode);
        
        if (!isSearchMode) {
            loadFriends();
        }
    }

    private void loadFriends() {
        if (auth.getCurrentUser() == null) return;

        String currentUserId = auth.getCurrentUser().getUid();
        db.collection("users").document(currentUserId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                User currentUser = documentSnapshot.toObject(User.class);
                if (currentUser != null && currentUser.getFriendIds() != null) {
                    loadFriendsData(currentUser.getFriendIds());
                }
            });
    }

    private void loadFriendsData(List<String> friendIds) {
        if (friendIds.isEmpty()) {
            friendsList.clear();
            adapter.notifyDataSetChanged();
            return;
        }

        db.collection("users")
            .whereIn("userId", friendIds)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                friendsList.clear();
                for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                    User friend = document.toObject(User.class);
                    friendsList.add(friend);
                }
                adapter.notifyDataSetChanged();
            });
    }

    private void searchUsers(String query) {
        if (auth.getCurrentUser() == null) return;

        String currentUserId = auth.getCurrentUser().getUid();
        db.collection("users")
            .whereGreaterThanOrEqualTo("displayName", query)
            .whereLessThanOrEqualTo("displayName", query + "\uf8ff")
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                List<User> searchResults = new ArrayList<>();
                for (QueryDocumentSnapshot document : queryDocumentSnapshots) {
                    User user = document.toObject(User.class);
                    if (!user.getUserId().equals(currentUserId)) {
                        searchResults.add(user);
                    }
                }
                adapter.updateFriends(searchResults);
            });
    }

    @Override
    public void onFriendClick(User user) {
        if (getActivity() instanceof MainActivity) {
            // Открываем профиль друга
            FriendProfileFragment fragment = FriendProfileFragment.newInstance(user.getUserId());
            requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.fragmentContainer, fragment)
                .addToBackStack(null)
                .commit();
            
            // Скрываем нижнюю навигацию
            ((MainActivity) getActivity()).hideBottomNavigation();
        }
    }

    @Override
    public void onAddFriendClick(User user) {
        if (auth.getCurrentUser() == null) return;

        String currentUserId = auth.getCurrentUser().getUid();
        
        // Добавляем друга текущему пользователю
        db.collection("users").document(currentUserId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                User currentUser = documentSnapshot.toObject(User.class);
                if (currentUser != null) {
                    // Добавляем друга текущему пользователю
                    currentUser.addFriend(user.getUserId());
                    db.collection("users").document(currentUserId)
                        .update("friendIds", currentUser.getFriendIds())
                        .addOnSuccessListener(aVoid -> {
                            // После успешного добавления друга текущему пользователю,
                            // добавляем текущего пользователя в друзья другому пользователю
                            db.collection("users").document(user.getUserId())
                                .get()
                                .addOnSuccessListener(friendDoc -> {
                                    User friendUser = friendDoc.toObject(User.class);
                                    if (friendUser != null) {
                                        friendUser.addFriend(currentUserId);
                                        db.collection("users").document(user.getUserId())
                                            .update("friendIds", friendUser.getFriendIds())
                                            .addOnSuccessListener(aVoid2 -> {
                                                Toast.makeText(getContext(), "Пользователь добавлен в друзья", Toast.LENGTH_SHORT).show();
                                                
                                                // Обновляем статус дружбы в списке
                                                user.getFriendIds().add(currentUserId);
                                                adapter.notifyDataSetChanged();
                                                
                                                if (!isSearchMode) {
                                                    loadFriends();
                                                }
                                            })
                                            .addOnFailureListener(e -> {
                                                Toast.makeText(getContext(), "Ошибка при добавлении в друзья: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                            });
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(getContext(), "Ошибка при добавлении в друзья: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(getContext(), "Ошибка при добавлении в друзья: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
                }
            })
            .addOnFailureListener(e -> {
                Toast.makeText(getContext(), "Ошибка при добавлении в друзья: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }

    @Override
    public void onRemoveFriendClick(User user) {
        if (auth.getCurrentUser() == null) return;

        String currentUserId = auth.getCurrentUser().getUid();
        
        // Удаляем друга у текущего пользователя
        db.collection("users").document(currentUserId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                User currentUser = documentSnapshot.toObject(User.class);
                if (currentUser != null) {
                    // Удаляем друга у текущего пользователя
                    currentUser.removeFriend(user.getUserId());
                    db.collection("users").document(currentUserId)
                        .update("friendIds", currentUser.getFriendIds())
                        .addOnSuccessListener(aVoid -> {
                            // После успешного удаления друга у текущего пользователя,
                            // удаляем текущего пользователя из друзей у другого пользователя
                            db.collection("users").document(user.getUserId())
                                .get()
                                .addOnSuccessListener(friendDoc -> {
                                    User friendUser = friendDoc.toObject(User.class);
                                    if (friendUser != null) {
                                        friendUser.removeFriend(currentUserId);
                                        db.collection("users").document(user.getUserId())
                                            .update("friendIds", friendUser.getFriendIds())
                                            .addOnSuccessListener(aVoid2 -> {
                                                Toast.makeText(getContext(), "Пользователь удален из друзей", Toast.LENGTH_SHORT).show();
                                                loadFriends();
                                            })
                                            .addOnFailureListener(e -> {
                                                Toast.makeText(getContext(), "Ошибка при удалении из друзей: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                            });
                                    }
                                })
                                .addOnFailureListener(e -> {
                                    Toast.makeText(getContext(), "Ошибка при удалении из друзей: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                                });
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(getContext(), "Ошибка при удалении из друзей: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        });
                }
            })
            .addOnFailureListener(e -> {
                Toast.makeText(getContext(), "Ошибка при удалении из друзей: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            });
    }
} 