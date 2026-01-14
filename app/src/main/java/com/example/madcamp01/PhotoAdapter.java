package com.example.madcamp01;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.ArrayList;
import java.util.List;

/**
 * 일차별로 그룹화하여 사진을 표시하는 RecyclerView 어댑터.
 */
public class PhotoAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_PHOTO = 1;

    private PostItem postItem;
    private List<AdapterItem> adapterItems; // 헤더와 사진을 함께 담는 리스트
    private Context context;
    
    public interface OnPhotoDeleteListener {
        void onDelete(int position);
    }
    
    public interface OnPhotoEditListener {
        void onEdit(int position);
    }
    
    private OnPhotoDeleteListener deleteListener;
    private OnPhotoEditListener editListener;
    
    public void setOnPhotoDeleteListener(OnPhotoDeleteListener listener) {
        this.deleteListener = listener;
    }
    
    public void setOnPhotoEditListener(OnPhotoEditListener listener) {
        this.editListener = listener;
    }
    
    private static class AdapterItem {
        int type;
        String dayNumber; // 헤더인 경우
        int photoIndex; // 사진인 경우 (PostItem 내 인덱스)
        
        AdapterItem(int type, String dayNumber, int photoIndex) {
            this.type = type;
            this.dayNumber = dayNumber;
            this.photoIndex = photoIndex;
        }
    }
    
    public PhotoAdapter(PostItem postItem, Context context) {
        this.postItem = postItem;
        this.context = context;
        this.adapterItems = new ArrayList<>();
        updateAdapterItems();
    }
    
    /**
     * PostItem 참조를 업데이트 (수정 모드에서 PostItem이 변경될 때 호출).
     */
    public void setPostItem(PostItem postItem) {
        this.postItem = postItem;
        updateAdapterItems();
    }
    
    public void updateAdapterItems() {
        adapterItems.clear();
        if (postItem == null || postItem.getPhotoCount() == 0) {
            notifyDataSetChanged();
            return;
        }
        
        String currentDay = null;
        int count = postItem.getPhotoCount();
        for (int i = 0; i < count; i++) {
            String dayNumber = postItem.getImageDay(i);
            if (dayNumber == null) dayNumber = "1";
            
            if (!dayNumber.equals(currentDay)) {
                adapterItems.add(new AdapterItem(TYPE_HEADER, dayNumber, -1));
                currentDay = dayNumber;
            }
            
            adapterItems.add(new AdapterItem(TYPE_PHOTO, null, i));
        }
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return adapterItems.get(position).type;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_day_header, parent, false);
            return new DayHeaderViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_photo, parent, false);
            return new PhotoViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        AdapterItem item = adapterItems.get(position);
        
        if (item.type == TYPE_HEADER) {
            DayHeaderViewHolder headerHolder = (DayHeaderViewHolder) holder;
            headerHolder.tvDayNumber.setText(item.dayNumber + "일차");
        } else {
            PhotoViewHolder photoHolder = (PhotoViewHolder) holder;
            int photoIndex = item.photoIndex;
            
            Uri uri = postItem.getImageUri(photoIndex);
            String imageUrl = postItem.getImageUrl(photoIndex);
            
            if (imageUrl != null && !imageUrl.isEmpty() && imageUrl.startsWith("http")) {
                Glide.with(context).load(imageUrl).into(photoHolder.ivPhoto);
            } else if (uri != null) {
                Glide.with(context).load(uri).into(photoHolder.ivPhoto);
            } else if (imageUrl != null && !imageUrl.isEmpty()) {
                Glide.with(context).load(imageUrl).into(photoHolder.ivPhoto);
            }
            
            
            // 날짜 표시
            java.util.Date date = postItem.getImageDate(photoIndex);
            if (date != null) {
                java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy.MM.dd HH:mm", java.util.Locale.getDefault());
                photoHolder.tvDate.setText(sdf.format(date));
            } else {
                photoHolder.tvDate.setText("");
            }
            
            // 위치 표시
            String location = postItem.getFormattedLocation(photoIndex);
            if (location != null && !location.isEmpty()) {
                photoHolder.tvLocation.setText(location);
            } else {
                photoHolder.tvLocation.setText("위치 정보 없음");
            }

            photoHolder.btnDeletePhoto.setOnClickListener(v -> {
                if (deleteListener != null) {
                    deleteListener.onDelete(photoIndex);
                }
            });
            
            // 수정 버튼 클릭 시 편집 다이얼로그 호출
            photoHolder.btnEditPhoto.setOnClickListener(v -> {
                if (editListener != null) {
                    editListener.onEdit(photoIndex);
                }
            });
            
            // 사진 클릭 이벤트 제거 (요청사항 반영)
            photoHolder.ivPhoto.setOnClickListener(null);
        }
    }

    @Override
    public int getItemCount() {
        return adapterItems != null ? adapterItems.size() : 0;
    }

    static class DayHeaderViewHolder extends RecyclerView.ViewHolder {
        TextView tvDayNumber;
        
        DayHeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDayNumber = itemView.findViewById(R.id.tv_day_number);
        }
    }

    static class PhotoViewHolder extends RecyclerView.ViewHolder {
        ImageView ivPhoto;
        ImageView btnDeletePhoto;
        ImageView btnEditPhoto; // Added for edit button
        TextView tvDate;
        TextView tvLocation;
        
        PhotoViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPhoto = itemView.findViewById(R.id.iv_photo);
            btnDeletePhoto = itemView.findViewById(R.id.btn_delete_photo);
            btnEditPhoto = itemView.findViewById(R.id.btn_edit_photo); // Initialize
            tvDate = itemView.findViewById(R.id.tv_photo_date);
            tvLocation = itemView.findViewById(R.id.tv_photo_location);
        }
    }
}