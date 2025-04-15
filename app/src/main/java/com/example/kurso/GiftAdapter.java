package com.example.kurso;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class GiftAdapter extends RecyclerView.Adapter<GiftAdapter.GiftViewHolder> {
    private List<Gift> gifts;

    public GiftAdapter(List<Gift> gifts) {
        this.gifts = gifts;
    }

    @NonNull
    @Override
    public GiftViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_gift, parent, false);
        return new GiftViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GiftViewHolder holder, int position) {
        Gift gift = gifts.get(position);
        
        // Устанавливаем иконку подарка
        int iconResId = getGiftIconResource(gift.getGiftType());
        holder.giftIcon.setImageResource(iconResId);
        
        // Устанавливаем имя отправителя
        holder.fromUserText.setText(holder.itemView.getContext()
                .getString(R.string.gift_from, gift.getFromUserDisplayName()));
    }

    @Override
    public int getItemCount() {
        return gifts.size();
    }

    private int getGiftIconResource(String giftType) {
        switch (giftType) {
            case "flower": return R.drawable.ic_gift_flower;
            case "heart": return R.drawable.ic_gift_heart;
            case "star": return R.drawable.ic_gift_star;
            case "cake": return R.drawable.ic_gift_cake;
            case "gift": return R.drawable.ic_gift_present;
            default: return R.drawable.ic_gift_present;
        }
    }

    public void updateGifts(List<Gift> newGifts) {
        this.gifts = newGifts;
        notifyDataSetChanged();
    }

    static class GiftViewHolder extends RecyclerView.ViewHolder {
        ImageView giftIcon;
        TextView fromUserText;

        GiftViewHolder(View itemView) {
            super(itemView);
            giftIcon = itemView.findViewById(R.id.giftIcon);
            fromUserText = itemView.findViewById(R.id.fromUserText);
        }
    }
} 