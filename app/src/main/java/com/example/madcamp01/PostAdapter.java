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

    public PostAdapter(Context context) {
        this.context = context;
        this.postList = new ArrayList<>();
    }

    // 데이터 추가용 메서드 (무한 스크롤에서 중요)
    public void addPostList(List<PostItem> newPosts) {
        //현재 리스트의 끝(size())을 startPosition에 저장함
        int startPosition = postList.size();
        //기존 리스트에 새로운 리스트 인스턴스를 합치기(ArrayList의 addAll()메서드 사용해서 이어붙이기)
        postList.addAll(newPosts);
        notifyItemRangeInserted(startPosition, newPosts.size());
    }

    // 뷰홀더 (내부 클래스로 둬도 됨)
    public static class PostViewHolder extends RecyclerView.ViewHolder {
        ImageView ivImage;
        TextView tvTitle;

        public PostViewHolder(@NonNull View itemView) {
            super(itemView);
            // item_post.xml의 ID와 일치시켜야 함
            ivImage = itemView.findViewById(R.id.ivPostImage);
            tvTitle = itemView.findViewById(R.id.tvTitle);
        }
    }

    @NonNull
    @Override
    public PostViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        //LayoutInflater -> xml에 정의된 Resource 를 View 객체로 반환해 주는 역할
        //마치 Activity를 만들 때 onCreate에 추가되는 setContentView 메서드와 유사한 역할
        View view = LayoutInflater.from(context).inflate(R.layout.item_post, parent, false);
        return new PostViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull PostViewHolder holder, int position) {
        PostItem item = postList.get(position);
        holder.tvTitle.setText(item.getTitle());

        // 1. 이미지 리스트 가져오기
        List<String> images = item.getImages();

        // 2. 이미지가 있는지 확인 (Null 체크 및 사이즈 체크)
        if (images != null && !images.isEmpty()) {
            // 3. 첫 번째 이미지(0번)만 대표로 보여줌
            String firstImageUrl = images.get(0);

            Glide.with(context)
                    .load(firstImageUrl)
                    .placeholder(R.drawable.ic_launcher_background)
                    .into(holder.ivImage);
        } else {
            // 이미지가 아예 없는 경우 처리 (기본 이미지 보여주기 등)
            holder.ivImage.setImageResource(R.drawable.ic_launcher_background);
        }

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (listener != null && position != RecyclerView.NO_POSITION) {
                    // 클릭 발생 시 인터페이스를 통해 알림
                    listener.onItemClick(postList.get(position));
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return postList.size();
    }

    //클릭 리스너 인터페이스 정의
    public interface OnItemClickListener {
        void onItemClick(PostItem item);
    }

    //클릭 리스너 객체 변수
    private OnItemClickListener listener;

    //외부에서 클릭 리스너를 세팅하는 메서드
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }
}