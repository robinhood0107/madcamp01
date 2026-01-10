package com.example.madcamp01;

import android.content.Context;
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

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {

    private final Context context;
    private final List<PostItem> postList;

    // --- 리스너 인터페이스 정의 --- //
    private OnItemClickListener clickListener;
    private OnItemLongClickListener longClickListener;

    public PostAdapter(Context context) {
        this.context = context;
        this.postList = new ArrayList<>();
    }

    public void addPostList(List<PostItem> newPosts) {
        int startPosition = postList.size();
        postList.addAll(newPosts);
        notifyItemRangeInserted(startPosition, newPosts.size());
    }

    public void clearPosts() {
        postList.clear();
        notifyDataSetChanged();
    }

    public static class PostViewHolder extends RecyclerView.ViewHolder {
        ImageView ivImage;
        TextView tvTitle;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.ivPostImage);
            tvTitle = itemView.findViewById(R.id.tvTitle);
        }
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_post, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        PostItem item = postList.get(position);
        holder.tvTitle.setText(item.getTitle());

        List<String> images = item.getImages();
        if (images != null && !images.isEmpty()) {
            Glide.with(context)
                    .load(images.get(0))
                    .placeholder(R.drawable.ic_launcher_background)
                    .into(holder.ivImage);
        } else {
            holder.ivImage.setImageResource(R.drawable.ic_launcher_background);
        }

        // --- 리스너 바인딩 --- //
        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null && holder.getAdapterPosition() != RecyclerView.NO_POSITION) {
                clickListener.onItemClick(postList.get(holder.getAdapterPosition()));
            }
        });

        // 롱클릭 리스너에서 View도 함께 전달
        holder.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null && holder.getAdapterPosition() != RecyclerView.NO_POSITION) {
                longClickListener.onItemLongClick(v, postList.get(holder.getAdapterPosition()));
                return true;
            }
            return false;
        });
    }

    @Override
    public int getItemCount() {
        return postList.size();
    }

    // --- 리스너 인터페이스 --- //
    public interface OnItemClickListener {
        void onItemClick(PostItem item);
    }

    public interface OnItemLongClickListener {
        // View를 파라미터로 추가
        void onItemLongClick(View anchorView, PostItem item);
    }

    // --- 외부에서 리스너를 설정하는 메서드 --- //
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.clickListener = listener;
    }

    public void setOnItemLongClickListener(OnItemLongClickListener listener) {
        this.longClickListener = listener;
    }
}