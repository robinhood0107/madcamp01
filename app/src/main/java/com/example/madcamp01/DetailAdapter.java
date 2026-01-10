package com.example.madcamp01;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class DetailAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    // 뷰 타입 상수 정의 (일반 이미지 vs 마지막 푸터)
    public static final int VIEW_TYPE_IMAGE = 0;
    public static final int VIEW_TYPE_FOOTER = 1;

    private final Context context;
    private final List<String> imageUrls;
    private OnItemClickListener listener;

    public DetailAdapter(Context context, List<String> imageUrls) {
        this.context = context;
        this.imageUrls = imageUrls;
    }

    @Override
    public int getItemViewType(int position) {
        // 마지막 아이템이면 FOOTER 타입, 아니면 IMAGE 타입을 반환합니다.
        return (position == imageUrls.size()) ? VIEW_TYPE_FOOTER : VIEW_TYPE_IMAGE;
    }

    @Override
    public int getItemCount() {
        // 전체 아이템 개수는 '이미지 개수 + 푸터 1개' 입니다.
        return imageUrls.size() + 1;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_IMAGE) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_detail_image, parent, false);
            return new ImageViewHolder(view);
        } else {
            View view = LayoutInflater.from(context).inflate(R.layout.item_detail_footer, parent, false);
            return new FooterViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        if (holder.getItemViewType() == VIEW_TYPE_IMAGE) {
            String imageUrl = imageUrls.get(position);
            ImageViewHolder imageHolder = (ImageViewHolder) holder;

            Glide.with(context)
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_launcher_background)
                    .centerCrop() // SquareImageView와 함께 사용되어 정사각형 썸네일을 만듭니다.
                    .into(imageHolder.ivDetailImage);

            // 클릭 리스너 설정
            imageHolder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(holder.getAdapterPosition());
                }
            });
        }
        // 푸터는 데이터 바인딩이 필요 없습니다.
    }

    // --- 뷰홀더 --- //
    static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView ivDetailImage;
        public ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            ivDetailImage = itemView.findViewById(R.id.ivDetailImage);
        }
    }

    static class FooterViewHolder extends RecyclerView.ViewHolder {
        public FooterViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }

    // --- 클릭 리스너 인터페이스 --- //
    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }
}
