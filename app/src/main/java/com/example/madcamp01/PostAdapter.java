package com.example.madcamp01;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.List;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {

    private final Context context;
    private final List<PostItem> postList;
    private final boolean isMyList; // [추가] 개인/SNS 게시판 구분 플래그
    private final String currentUserId; // [추가] 현재 사용자 ID

    private OnItemClickListener clickListener;
    private OnItemLongClickListener longClickListener;

    public PostAdapter(Context context, boolean isMyList) {
        this.context = context;
        this.postList = new ArrayList<>();
        this.isMyList = isMyList;

        // 현재 로그인한 사용자 ID 가져오기
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        this.currentUserId = (user != null) ? user.getUid() : null;
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
        TextView tvUserEmail;
        TextView tvPublicStatus;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.ivPostImage);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvUserEmail = itemView.findViewById(R.id.tvUserEmail);
            tvPublicStatus = itemView.findViewById(R.id.tvPublicStatus);
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

        // 이메일 및 제목 설정
        holder.tvUserEmail.setText(item.getUserEmail() != null ? item.getUserEmail() : "unknown_email");
        holder.tvTitle.setText(item.getTitle());

        // [추가] 공개/비공개 상태 표시 로직
        if (isMyList && currentUserId != null && currentUserId.equals(item.getUserId())) {
            holder.tvPublicStatus.setVisibility(View.VISIBLE);
            if (item.isPublic()) {
                holder.tvPublicStatus.setText("(공개)");
                holder.tvPublicStatus.setTextColor(ContextCompat.getColor(context, R.color.public_status_color)); // 초록색
            } else {
                holder.tvPublicStatus.setText("(비공개)");
                holder.tvPublicStatus.setTextColor(ContextCompat.getColor(context, R.color.private_status_color)); // 회색
            }
        } else {
            holder.tvPublicStatus.setVisibility(View.GONE);
        }

        // 이미지 로딩
        List<String> images = item.getImages();
        if (images != null && !images.isEmpty()) {
            Glide.with(context).load(images.get(0)).placeholder(R.drawable.ic_launcher_background).into(holder.ivImage);
        } else {
            holder.ivImage.setImageResource(R.drawable.ic_launcher_background);
        }

        // 리스너 바인딩
        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null && holder.getAdapterPosition() != RecyclerView.NO_POSITION) {
                clickListener.onItemClick(postList.get(holder.getAdapterPosition()));
            }
        });

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
        return postList != null ? postList.size() : 0;
    }

    // 리스너 인터페이스 (기존과 동일)
    public interface OnItemClickListener { void onItemClick(PostItem item); }
    public interface OnItemLongClickListener { void onItemLongClick(View anchorView, PostItem item); }
    public void setOnItemClickListener(OnItemClickListener listener) { this.clickListener = listener; }
    public void setOnItemLongClickListener(OnItemLongClickListener listener) { this.longClickListener = listener; }
}