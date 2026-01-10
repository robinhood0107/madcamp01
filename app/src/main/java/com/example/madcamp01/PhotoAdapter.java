package com.example.madcamp01;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

// 1. RecyclerView.Adapter를 상속받을 때 <PhotoAdapter.PhotoViewHolder>를 정확히 명시해야 합니다.
public class PhotoAdapter extends RecyclerView.Adapter<PhotoAdapter.PhotoViewHolder> {

    private List<Uri> uriList;
    private Context context;
    // 삭제 클릭 리스너 인터페이스
    public interface OnPhotoDeleteListener {
        void onDelete(int position);
    }
    private OnPhotoDeleteListener deleteListener;
    public void setOnPhotoDeleteListener(OnPhotoDeleteListener listener) {
        this.deleteListener = listener;
    }
    // 생성자
    public PhotoAdapter(List<Uri> uriList, Context context) {
        this.uriList = uriList;
        this.context = context;
    }

    // [필수 구현 1] 아이템 하나를 담을 틀(ViewHolder)을 만드는 메서드
    @NonNull
    @Override
    public PhotoViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_photo, parent, false);
        return new PhotoViewHolder(view);
    }

    // [필수 구현 2] 실제로 사진 데이터를 화면에 보여주는 메서드
    // 여기서 인자 형식이 @NonNull PhotoViewHolder holder, int position 이어야 합니다.
    @Override
    public void onBindViewHolder(@NonNull PhotoViewHolder holder, int position) {
        Uri uri = uriList.get(position);

        // 이미지 주소를 ImageView에 세팅
        Glide.with(context)
                .load(uri)
                .into(holder.ivPhoto);
        //사진 클릭시 삭제
        holder.btnDeletePhoto.setOnClickListener(v -> {
            if (deleteListener != null) {
                // 정확한 현재 위치를 파악하기 위해 holder.getAdapterPosition() 권장
                int currentPos = holder.getAdapterPosition();
                if (currentPos != RecyclerView.NO_POSITION) {
                    deleteListener.onDelete(currentPos);
                }
            }
        });
    }

    // [필수 구현 3] 전체 아이템 개수를 알려주는 메서드
    @Override
    public int getItemCount() {
        return uriList != null ? uriList.size() : 0;
    }

    // 사진 한 장을 붙잡고 있을 바구니(ViewHolder) 클래스
    public static class PhotoViewHolder extends RecyclerView.ViewHolder {
        ImageView ivPhoto;
        ImageView btnDeletePhoto;
        public PhotoViewHolder(@NonNull View itemView) {
            super(itemView);
            // item_photo.xml에 있는 ImageView와 연결
            ivPhoto = itemView.findViewById(R.id.iv_photo);
            btnDeletePhoto = itemView.findViewById(R.id.btn_delete_photo);
        }
    }
}