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
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 뷰페이저 히어로 카드 전용 어댑터.
 * 기존 PostAdapter의 인터페이스(click/longClick, add/clear)를 동일하게 유지해
 * GalleryFragment의 다른 로직을 최대한 건드리지 않는다.
 */
public class HeroPostAdapter extends RecyclerView.Adapter<HeroPostAdapter.HeroViewHolder> {

    private final Context context;
    private final List<PostItem> postList;
    private final List<DocumentSnapshot> documentSnapshots;
    private final String currentUserId;

    private OnItemClickListener clickListener;
    private OnItemLongClickListener longClickListener;
    private final SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());

    public HeroPostAdapter(Context context) {
        this.context = context;
        this.postList = new ArrayList<>();
        this.documentSnapshots = new ArrayList<>();
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        this.currentUserId = (user != null) ? user.getUid() : null;
    }

    public void addPostList(List<PostItem> newPosts, List<DocumentSnapshot> snapshots) {
        int startPosition = postList.size();
        postList.addAll(newPosts);
        documentSnapshots.addAll(snapshots);
        notifyItemRangeInserted(startPosition, newPosts.size());
    }

    public void clearPosts() {
        postList.clear();
        documentSnapshots.clear();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public HeroViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_post_hero, parent, false);
        return new HeroViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull HeroViewHolder holder, int position) {
        PostItem item = postList.get(position);

        // 썸네일/대표 이미지
        List<String> images = item.getImages();
        if (images != null && !images.isEmpty()) {
            Glide.with(context)
                    .load(images.get(0))
                    .placeholder(R.drawable.ic_placeholder)
                    .error(R.drawable.ic_placeholder)
                    .transition(DrawableTransitionOptions.withCrossFade())
                    .into(holder.heroImage);
        } else {
            holder.heroImage.setImageResource(R.drawable.ic_placeholder);
        }

        // 텍스트 데이터 매핑 (필드 부족 시 기본값)
        holder.title.setText(item.getTitle() != null ? item.getTitle() : "Untitled");
        
        // 위치 정보: cities와 countries 배열에서 가져오기
        String locationText = formatLocation(position);
        holder.location.setText(locationText);

        holder.userName.setText(item.getUserEmail() != null ? item.getUserEmail() : "알 수 없음");

        // 시간 표시: "2h ago" 형식
        String timeText = formatTimeAgo(item.getCreatedAt());
        holder.time.setText(timeText);

        boolean isOwner = currentUserId != null && currentUserId.equals(item.getUserId());
        holder.memberTag.setText(isOwner ? "My post" : "Member");

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

    /**
     * DocumentSnapshot에서 cities와 countries 배열을 읽어서 예쁘게 포맷팅
     */
    private String formatLocation(int position) {
        if (position < 0 || position >= documentSnapshots.size()) {
            return "어딘가에서";
        }
        
        DocumentSnapshot snapshot = documentSnapshots.get(position);
        if (snapshot == null) {
            return "어딘가에서";
        }
        
        List<String> cities = (List<String>) snapshot.get("cities");
        List<String> countries = (List<String>) snapshot.get("countries");
        
        // 중복 제거 및 정리
        Set<String> uniqueCities = new HashSet<>();
        Set<String> uniqueCountries = new HashSet<>();
        
        if (cities != null) {
            for (String city : cities) {
                if (city != null && !city.trim().isEmpty()) {
                    uniqueCities.add(city.trim());
                }
            }
        }
        
        if (countries != null) {
            for (String country : countries) {
                if (country != null && !country.trim().isEmpty()) {
                    uniqueCountries.add(country.trim());
                }
            }
        }
        
        // 포맷팅: "도시1, 도시2 · 국가1, 국가2" 형식 (String.join 사용 대신 직접 구현)
        List<String> parts = new ArrayList<>();

        if (!uniqueCities.isEmpty()) {
            parts.add(joinWithComma(uniqueCities));
        }

        if (!uniqueCountries.isEmpty()) {
            parts.add(joinWithComma(uniqueCountries));
        }

        if (parts.isEmpty()) {
            return "어딘가에서";
        }

        return joinWithSeparator(parts, " · ");
    }

    /**
     * 시간을 "2h ago" 형식으로 포맷팅
     */
    private String formatTimeAgo(java.util.Date createdAt) {
        if (createdAt == null) {
            return "방금 전";
        }
        
        long now = System.currentTimeMillis();
        long created = createdAt.getTime();
        long diff = now - created;
        
        long seconds = diff / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        
        if (days > 0) {
            return days + "일 전";
        } else if (hours > 0) {
            return hours + "시간 전";
        } else if (minutes > 0) {
            return minutes + "분 전";
        } else {
            return "방금 전";
        }
    }

    // String.join을 직접 구현 (하위 버전 호환)
    private String joinWithComma(Iterable<String> items) {
        StringBuilder sb = new StringBuilder();
        for (String s : items) {
            if (s == null) continue;
            if (sb.length() > 0) sb.append(", ");
            sb.append(s);
        }
        return sb.toString();
    }

    private String joinWithSeparator(Iterable<String> items, String separator) {
        StringBuilder sb = new StringBuilder();
        for (String s : items) {
            if (s == null) continue;
            if (sb.length() > 0) sb.append(separator);
            sb.append(s);
        }
        return sb.toString();
    }

    public interface OnItemClickListener { void onItemClick(PostItem item); }
    public interface OnItemLongClickListener { void onItemLongClick(View anchorView, PostItem item); }
    public void setOnItemClickListener(OnItemClickListener listener) { this.clickListener = listener; }
    public void setOnItemLongClickListener(OnItemLongClickListener listener) { this.longClickListener = listener; }

    static class HeroViewHolder extends RecyclerView.ViewHolder {
        ImageView heroImage;
        TextView title;
        TextView location;
        TextView userName;
        TextView memberTag;
        TextView time;

        HeroViewHolder(@NonNull View itemView) {
            super(itemView);
            heroImage = itemView.findViewById(R.id.heroImage);
            title = itemView.findViewById(R.id.heroTitle);
            location = itemView.findViewById(R.id.heroLocation);
            userName = itemView.findViewById(R.id.heroUserName);
            memberTag = itemView.findViewById(R.id.heroMemberTag);
            time = itemView.findViewById(R.id.heroTime);
        }
    }
}
