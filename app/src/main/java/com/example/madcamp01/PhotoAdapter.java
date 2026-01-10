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

// 일차별로 그룹화된 사진 어댑터
public class PhotoAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static final int TYPE_HEADER = 0;
    private static final int TYPE_PHOTO = 1;

    private List<PhotoInfo> photoInfoList;
    private List<AdapterItem> adapterItems; // 헤더와 사진을 함께 담는 리스트
    private Context context;
    
    // 삭제 클릭 리스너 인터페이스
    public interface OnPhotoDeleteListener {
        void onDelete(int position);
    }
    private OnPhotoDeleteListener deleteListener;
    public void setOnPhotoDeleteListener(OnPhotoDeleteListener listener) {
        this.deleteListener = listener;
    }
    
    // 어댑터 아이템 (헤더 또는 사진)
    private static class AdapterItem {
        int type;
        String dayNumber; // 헤더인 경우
        PhotoInfo photoInfo; // 사진인 경우
        
        AdapterItem(int type, String dayNumber, PhotoInfo photoInfo) {
            this.type = type;
            this.dayNumber = dayNumber;
            this.photoInfo = photoInfo;
        }
    }
    
    // 생성자
    public PhotoAdapter(List<PhotoInfo> photoInfoList, Context context) {
        this.photoInfoList = photoInfoList;
        this.context = context;
        this.adapterItems = new ArrayList<>();
        updateAdapterItems();
    }
    
    // PhotoInfo 리스트가 변경되면 어댑터 아이템 업데이트
    public void updateAdapterItems() {
        adapterItems.clear();
        if (photoInfoList == null || photoInfoList.isEmpty()) {
            notifyDataSetChanged();
            return;
        }
        
        String currentDay = null;
        for (PhotoInfo photoInfo : photoInfoList) {
            String dayNumber = photoInfo.getDayNumber() != null ? photoInfo.getDayNumber() : "1";
            
            // 새로운 일차가 시작되면 헤더 추가
            if (!dayNumber.equals(currentDay)) {
                adapterItems.add(new AdapterItem(TYPE_HEADER, dayNumber, null));
                currentDay = dayNumber;
            }
            
            // 사진 추가
            adapterItems.add(new AdapterItem(TYPE_PHOTO, null, photoInfo));
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
            PhotoInfo photoInfo = item.photoInfo;
            
            // 이미지 로드 (URI 또는 URL)
            Uri uri = photoInfo.getUri();
            if (photoInfo.getImageUrl() != null && photoInfo.getImageUrl().startsWith("http")) {
                Glide.with(context).load(photoInfo.getImageUrl()).into(photoHolder.ivPhoto);
            } else if (uri != null) {
                Glide.with(context).load(uri).into(photoHolder.ivPhoto);
            }
            
            // 사진 클릭시 삭제
            photoHolder.btnDeletePhoto.setOnClickListener(v -> {
                if (deleteListener != null) {
                    // 실제 PhotoInfo 리스트에서의 위치 찾기
                    int actualPosition = photoInfoList.indexOf(photoInfo);
                    if (actualPosition != -1) {
                        deleteListener.onDelete(actualPosition);
                    }
                }
            });
        }
    }

    @Override
    public int getItemCount() {
        return adapterItems != null ? adapterItems.size() : 0;
    }

    // 일차 헤더 ViewHolder
    static class DayHeaderViewHolder extends RecyclerView.ViewHolder {
        TextView tvDayNumber;
        
        DayHeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDayNumber = itemView.findViewById(R.id.tv_day_number);
        }
    }

    // 사진 ViewHolder
    static class PhotoViewHolder extends RecyclerView.ViewHolder {
        ImageView ivPhoto;
        ImageView btnDeletePhoto;
        
        PhotoViewHolder(@NonNull View itemView) {
            super(itemView);
            ivPhoto = itemView.findViewById(R.id.iv_photo);
            btnDeletePhoto = itemView.findViewById(R.id.btn_delete_photo);
        }
    }
}