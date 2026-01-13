package com.example.madcamp01;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class PostAdapter extends RecyclerView.Adapter<PostAdapter.PostViewHolder> {

    private final Context context;
    private final List<PostItem> postList;
    private final List<DocumentSnapshot> documentSnapshots;
    private final boolean isMyList;
    private final String currentUserId;

    private OnItemClickListener clickListener;
    private OnItemLongClickListener longClickListener;

    public PostAdapter(Context context, boolean isMyList) {
        this.context = context;
        this.postList = new ArrayList<>();
        this.documentSnapshots = new ArrayList<>();
        this.isMyList = isMyList;

        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        this.currentUserId = (user != null) ? user.getUid() : null;
    }

    public void addPostList(List<PostItem> newPosts) {
        addPostList(newPosts, null);
    }

    public void addPostList(List<PostItem> newPosts, List<DocumentSnapshot> snapshots) {
        int startPosition = postList.size();
        postList.addAll(newPosts);
        if (snapshots != null) {
            documentSnapshots.addAll(snapshots);
        }
        notifyItemRangeInserted(startPosition, newPosts.size());
    }

    public void clearPosts() {
        postList.clear();
        documentSnapshots.clear();
        notifyDataSetChanged();
    }

    public static class PostViewHolder extends RecyclerView.ViewHolder {
        ImageView ivImage;
        TextView tvTitle;
        TextView tvDate;
        TextView tvLocation;
        LinearLayout locationTag;
        ImageButton btnMore;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            ivImage = itemView.findViewById(R.id.ivPostImage);
            tvTitle = itemView.findViewById(R.id.tvTitle);
            tvDate = itemView.findViewById(R.id.tvDate);
            tvLocation = itemView.findViewById(R.id.tvLocation);
            locationTag = itemView.findViewById(R.id.locationTag);
            btnMore = itemView.findViewById(R.id.btnMore);
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

        // 제목 설정
        holder.tvTitle.setText(item.getTitle() != null ? item.getTitle() : "Untitled");

        // 날짜 포맷팅
        Date createdAt = item.getCreatedAt();
        if (createdAt != null) {
            SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.ENGLISH);
            String dateText = "Added on " + dateFormat.format(createdAt);
            holder.tvDate.setText(dateText);
        } else {
            holder.tvDate.setText("Date unknown");
        }

        // 위치 정보 설정 (DocumentSnapshot에서 cities와 countries 사용)
        String formattedLocation = formatLocation(position);
        if (formattedLocation != null && !formattedLocation.isEmpty() && !formattedLocation.equals("어딘가에서")) {
            holder.locationTag.setVisibility(View.VISIBLE);
            holder.tvLocation.setText(formattedLocation);
        } else {
            // 위치 정보가 없으면 태그 숨김 (또는 "어딘가에서" 표시)
            holder.locationTag.setVisibility(View.GONE);
        }

        // 이미지 로드
        List<String> images = item.getImages();
        if (images != null && !images.isEmpty()) {
            Glide.with(context)
                    .load(images.get(0))
                    .placeholder(R.drawable.ic_placeholder)
                    .error(R.drawable.ic_placeholder)
                    .into(holder.ivImage);
        } else {
            holder.ivImage.setImageResource(R.drawable.ic_placeholder);
        }

        // 더보기 버튼 클릭 리스너 (롱클릭과 동일한 동작)
        if (holder.btnMore != null) {
            holder.btnMore.setOnClickListener(v -> {
                if (longClickListener != null && holder.getAdapterPosition() != RecyclerView.NO_POSITION) {
                    longClickListener.onItemLongClick(v, postList.get(holder.getAdapterPosition()));
                }
            });
        }

        // 아이템 클릭 리스너
        holder.itemView.setOnClickListener(v -> {
            if (clickListener != null && holder.getAdapterPosition() != RecyclerView.NO_POSITION) {
                clickListener.onItemClick(postList.get(holder.getAdapterPosition()));
            }
        });

        // 롱클릭 리스너 (더보기 메뉴 표시)
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

    /**
     * DocumentSnapshot에서 cities와 countries 배열을 읽어서 예쁘게 포맷팅
     */
    private String formatLocation(int position) {
        if (position >= 0 && position < documentSnapshots.size()) {
            DocumentSnapshot snapshot = documentSnapshots.get(position);
            String result = LocationFormatter.formatLocationFromSnapshot(snapshot);
            if (!result.equals(LocationFormatter.DEFAULT_LOCATION)) {
                return result;
            }
        }
        
        if (position >= 0 && position < postList.size()) {
            return LocationFormatter.formatLocationFromImageLocations(postList.get(position));
        }
        
        return LocationFormatter.DEFAULT_LOCATION;
    }

    public interface OnItemClickListener { void onItemClick(PostItem item); }
    public interface OnItemLongClickListener { void onItemLongClick(View anchorView, PostItem item); }
    public void setOnItemClickListener(OnItemClickListener listener) { this.clickListener = listener; }
    public void setOnItemLongClickListener(OnItemLongClickListener listener) { this.longClickListener = listener; }
}