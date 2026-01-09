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

    public static final int VIEW_TYPE_FOOTER = 1;
    // 뷰 타입 상수 정의 (일반 이미지 vs 마지막 푸터)
    private static final int TYPE_IMAGE = 0;
    private static final int TYPE_FOOTER = 1;

    private Context context;
    private List<String> imageUrls;

    public DetailAdapter(Context context, List<String> imageUrls) {
        this.context = context;
        this.imageUrls = imageUrls;
    }

    //포지션에 따라 뷰 타입 결정
    @Override
    public int getItemViewType(int position) {
        // 마지막 아이템이면 FOOTER 타입 반환, 아니면 IMAGE 타입 반환
        if (position == imageUrls.size()) {
            return TYPE_FOOTER;
        } else {
            return TYPE_IMAGE;
        }
    }

    //전체 아이템 개수는 '이미지 개수 + 푸터 1개'
    @Override
    public int getItemCount() {
        return imageUrls.size() + 1;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // 뷰 타입에 따라 다른 XML을 팽창(inflate)시킴
        if (viewType == TYPE_IMAGE) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_detail_image, parent, false);
            return new ImageViewHolder(view);
        } else {
            // TYPE_FOOTER
            View view = LayoutInflater.from(context).inflate(R.layout.item_detail_footer, parent, false);
            return new FooterViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        // 타입에 따라 데이터 바인딩 (푸터는 데이터 바인딩 필요 없음)
        if (getItemViewType(position) == TYPE_IMAGE) {
            String url = imageUrls.get(position);
            ImageViewHolder imageHolder = (ImageViewHolder) holder;

            Glide.with(context)
                    .load(url)
                    .placeholder(R.drawable.ic_launcher_background)
                    .into(imageHolder.ivDetailImage);
        }
    }

    // 이미지용 뷰홀더
    static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView ivDetailImage;
        public ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            ivDetailImage = itemView.findViewById(R.id.ivDetailImage);
        }
    }

    // 푸터용 뷰홀더 (아직은 하는 일 없음 지도 넣을 생각)
    static class FooterViewHolder extends RecyclerView.ViewHolder {
        public FooterViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}
