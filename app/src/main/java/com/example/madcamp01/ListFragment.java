package com.example.madcamp01;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.List;

public class ListFragment extends Fragment { // 클래스 이름
    private RecyclerView recyclerView;
    private PostAdapter adapter;
    private TextView loadingTextView;
    private boolean isLoading = false;

    // Firestore 관련 변수
    private FirebaseFirestore db;
    private DocumentSnapshot lastVisible; // 페이징을 위해 마지막으로 본 문서를 저장
    private final int LIMIT = 10;         // 한 번에 가져올 개수

    // 로딩 애니메이션 관련 변수
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable loadingDotsRunnable;
    private int dotCount = 0;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // 레이아웃 파일을 인플레이트
        return inflater.inflate(R.layout.fragment_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1. 뷰 및 Firestore 초기화
        db = FirebaseFirestore.getInstance();
        recyclerView = view.findViewById(R.id.recyclerView);
        loadingTextView = view.findViewById(R.id.loadingTextView);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        adapter = new PostAdapter(getContext());
        recyclerView.setAdapter(adapter);

        // 2. 어댑터 클릭 리스너 설정
        adapter.setOnItemClickListener(item -> {
            DetailFragment detailFragment = new DetailFragment();
            Bundle args = new Bundle();
            args.putParcelable("postData", item);
            detailFragment.setArguments(args);

            requireActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, detailFragment)
                    .addToBackStack(null)
                    .commit();
        });

        // 3. 스크롤 리스너 설정
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (!isLoading && !recyclerView.canScrollVertically(1)) {
                    loadMoreData();
                }
            }
        });

        // 4. 초기 데이터 로드
        loadMoreData();
    }

    private void startLoadingAnimation() {
        loadingTextView.setVisibility(View.VISIBLE);
        dotCount = 0;

        loadingDotsRunnable = new Runnable() {
            @Override
            public void run() {
                dotCount = (dotCount + 1) % 4; // 0, 1, 2, 3 -> 0, 1, 2, 3...
                StringBuilder text = new StringBuilder("게시물을 가져오는 중");
                for (int i = 0; i < dotCount; i++) {
                    text.append(".");
                }
                loadingTextView.setText(text.toString());
                handler.postDelayed(this, 500); // 0.5초마다 반복
            }
        };
        handler.post(loadingDotsRunnable); // 애니메이션 시작
    }

    private void stopLoadingAnimation() {
        if (loadingDotsRunnable != null) {
            handler.removeCallbacks(loadingDotsRunnable); // 애니메이션 정지
        }
        loadingTextView.setVisibility(View.GONE); // 텍스트 숨기기
    }

    private void loadMoreData() {
        if (isLoading) return; // 이미 로딩 중이면 중복 실행 방지
        isLoading = true;

        // 첫 페이지만 로딩 애니메이션 표시
        if (lastVisible == null) {
            startLoadingAnimation();
        }

        Query query;
        if (lastVisible == null) {
            query = db.collection("TravelPosts").orderBy("createdAt", Query.Direction.DESCENDING).limit(LIMIT);
        } else {
            query = db.collection("TravelPosts").orderBy("createdAt", Query.Direction.DESCENDING).startAfter(lastVisible).limit(LIMIT);
        }

        query.get().addOnSuccessListener(queryDocumentSnapshots -> {
            stopLoadingAnimation(); // 성공 시 애니메이션 중지
            if (!isAdded()) return;

            if (!queryDocumentSnapshots.isEmpty()) {
                List<PostItem> newPosts = queryDocumentSnapshots.toObjects(PostItem.class);
                adapter.addPostList(newPosts);
                lastVisible = queryDocumentSnapshots.getDocuments().get(queryDocumentSnapshots.size() - 1);
            } else {
                if (lastVisible != null) {
                    Toast.makeText(getContext(), "더 이상 게시물이 없습니다.", Toast.LENGTH_SHORT).show();
                }
            }
            isLoading = false;
        }).addOnFailureListener(e -> {
            stopLoadingAnimation(); // 실패 시 애니메이션 중지
            if (!isAdded()) return;

            Log.e("Firestore", "Error loading data", e);
            Toast.makeText(getContext(), "데이터 로딩 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
            isLoading = false;
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // 프래그먼트가 사라질 때 핸들러 콜백을 제거하여 메모리 누수 방지
        stopLoadingAnimation();
    }
}