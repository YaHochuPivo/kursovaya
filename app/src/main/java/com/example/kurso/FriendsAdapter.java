package com.example.kurso;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import java.io.File;
import java.util.List;

public class FriendsAdapter extends RecyclerView.Adapter<FriendsAdapter.FriendViewHolder> {
    private List<User> friends;
    private Context context;
    private OnFriendClickListener listener;
    private boolean isSearchMode = false;
    private String currentUserId;

    public interface OnFriendClickListener {
        void onFriendClick(User user);
        void onAddFriendClick(User user);
        void onRemoveFriendClick(User user);
    }

    public FriendsAdapter(Context context, List<User> friends, OnFriendClickListener listener) {
        this.context = context;
        this.friends = friends;
        this.listener = listener;
        this.currentUserId = FirebaseAuth.getInstance().getCurrentUser() != null ? 
            FirebaseAuth.getInstance().getCurrentUser().getUid() : null;
    }

    public void setSearchMode(boolean searchMode) {
        this.isSearchMode = searchMode;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public FriendViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_friend, parent, false);
        return new FriendViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull FriendViewHolder holder, int position) {
        User user = friends.get(position);
        holder.bind(user);
    }

    @Override
    public int getItemCount() {
        return friends.size();
    }

    public void updateFriends(List<User> newFriends) {
        this.friends = newFriends;
        notifyDataSetChanged();
    }

    class FriendViewHolder extends RecyclerView.ViewHolder {
        private ImageView profileImage;
        private TextView displayName;
        private TextView email;
        private ImageButton actionButton;

        FriendViewHolder(@NonNull View itemView) {
            super(itemView);
            profileImage = itemView.findViewById(R.id.profileImage);
            displayName = itemView.findViewById(R.id.displayName);
            email = itemView.findViewById(R.id.email);
            actionButton = itemView.findViewById(R.id.actionButton);

            itemView.setOnClickListener(v -> {
                int position = getAdapterPosition();
                if (position != RecyclerView.NO_POSITION) {
                    listener.onFriendClick(friends.get(position));
                }
            });
        }

        void bind(User user) {
            displayName.setText(user.getDisplayName());
            email.setText(user.getEmail());

            // Загрузка изображения профиля
            if (user.getProfileImagePath() != null && !user.getProfileImagePath().isEmpty()) {
                File imageFile = new File(user.getProfileImagePath());
                if (imageFile.exists()) {
                    Glide.with(context)
                        .load(Uri.fromFile(imageFile))
                        .placeholder(R.drawable.default_avatar)
                        .error(R.drawable.default_avatar)
                        .circleCrop()
                        .into(profileImage);
                } else {
                    profileImage.setImageResource(R.drawable.default_avatar);
                }
            } else {
                profileImage.setImageResource(R.drawable.default_avatar);
            }

            // Проверяем статус дружбы
            boolean isFriend = currentUserId != null && user.getFriendIds() != null && 
                             user.getFriendIds().contains(currentUserId);

            // В режиме поиска показываем кнопку добавления только для не-друзей
            if (isSearchMode) {
                if (isFriend) {
                    actionButton.setImageResource(R.drawable.ic_remove_friend);
                    actionButton.setOnClickListener(v -> listener.onRemoveFriendClick(user));
                } else {
                    actionButton.setImageResource(R.drawable.ic_add_friend);
                    actionButton.setOnClickListener(v -> listener.onAddFriendClick(user));
                }
            } else {
                // В режиме списка друзей всегда показываем кнопку удаления
                actionButton.setImageResource(R.drawable.ic_remove_friend);
                actionButton.setOnClickListener(v -> listener.onRemoveFriendClick(user));
            }
        }
    }
} 