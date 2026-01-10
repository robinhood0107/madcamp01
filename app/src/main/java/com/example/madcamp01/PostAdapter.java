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
    private final boolean isMyList;
    private final String currentUserId;

    private OnItemClickListener clickListener;
    private OnItemLongClickListener longClickListener;

    public PostAdapter(Context context, boolean isMyList) {
        this.context = context;
        this.postList = new ArrayList<>();
        this.isMyList = isMyList;

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

        holder.tvUserEmail.setText(item.getUserEmail() != null ? item.getUserEmail() : "unknown_email");
        holder.tvTitle.setText(item.getTitle());

        if (isMyList && currentUserId != null && currentUserId.equals(item.getUserId())) {
            holder.tvPublicStatus.setVisibility(View.VISIBLE);
            // [수정] 변경된 getter 메소드 사용
            if (item.getIsPublic()) {
                holder.tvPublicStatus.setText("(공개)");
                holder.tvPublicStatus.setTextColor(ContextCompat.getColor(context, R.color.public_status_color));
            } else {
                holder.tvPublicStatus.setText("(비공개)");
                holder.tvPublicStatus.setTextColor(ContextCompat.getColor(context, R.color.private_status_color));
            }
        } else {
            holder.tvPublicStatus.setVisibility(View.GONE);
        }

        List<String> images = item.getImages();
        if (images != null && !images.isEmpty()) {
            Glide.with(context).load(images.get(0)).placeholder(R.drawable.ic_launcher_background).into(holder.ivImage);
        } else {
            holder.ivImage.setImageResource(R.drawable.ic_launcher_background);
        }

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

    public interface OnItemClickListener { void onItemClick(PostItem item); }
    public interface OnItemLongClickListener { void onItemLongClick(View anchorView, PostItem item); }
    public void setOnItemClickListener(OnItemClickListener listener) { this.clickListener = listener; }
    public void setOnItemLongClickListener(OnItemLongClickListener listener) { this.longClickListener = listener; }
}