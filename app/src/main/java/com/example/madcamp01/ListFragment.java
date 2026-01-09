package com.example.madcamp01;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

public class ListFragment extends Fragment { // 클래스 이름
    private RecyclerView recyclerView;
    private PostAdapter adapter;
    private boolean isLoading = false;

    // Firestore 관련 변수
    private FirebaseFirestore db;
    private DocumentSnapshot lastVisible; // 페이징을 위해 마지막으로 본 문서를 저장
    private final int LIMIT = 10;         // 한 번에 가져올 개수

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_list, container, false);

        // 1. Firestore 초기화
        db = FirebaseFirestore.getInstance();

        recyclerView = view.findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));

        adapter = new PostAdapter(getContext());
        recyclerView.setAdapter(adapter);

        //어댑터의 클릭 리스너 설정
        adapter.setOnItemClickListener(new PostAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(PostItem item) {
                // 1. 상세 화면 프래그먼트 생성
                DetailFragment detailFragment = new DetailFragment();

                // 2. 데이터(PostItem)를 번들(Bundle)이라는 보따리에 담음
                Bundle args = new Bundle();
                args.putParcelable("postData", item); // 아까 Parcelable로 만든 이유!
                detailFragment.setArguments(args);

                // 3. 화면 이동 (프래그먼트 교체)
                // requireActivity().getSupportFragmentManager()를 사용해야 함
                requireActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, detailFragment) // R.id.container는 MainActivity의 그곳
                        .addToBackStack(null) // 뒤로가기 버튼 누르면 다시 리스트로 돌아오게 함
                        .commit();
            }
        });

        // 2. 스크롤 리스너
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (!isLoading && !recyclerView.canScrollVertically(1)) {
                    loadMoreData();
                }
            }
        });

        // 3. 초기 데이터 로드
        loadMoreData();

        return view;
    }

    private void loadMoreData() {
        if (isLoading) return; // 이미 로딩 중이면 중복 실행 방지
        isLoading = true;

        Query query;

        // "TravelPosts" 컬렉션을 생성일(createdAt) 역순(최신순)으로 정렬
        if (lastVisible == null) {
            // [처음 로드]
            query = db.collection("TravelPosts")
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .limit(LIMIT);
        } else {
            // [다음 페이지] 마지막으로 본 문서 다음부터 시작
            query = db.collection("TravelPosts")
                    .orderBy("createdAt", Query.Direction.DESCENDING)
                    .startAfter(lastVisible)
                    .limit(LIMIT);
        }

        query.get().addOnSuccessListener(queryDocumentSnapshots -> {
            if (!isAdded()) { // Fragment가 Activity에 붙어있는지 확인
                return;
            }
            if (!queryDocumentSnapshots.isEmpty()) {
                // 1. 데이터를 PostItem 리스트로 변환
                List<PostItem> newPosts = queryDocumentSnapshots.toObjects(PostItem.class);

                // 2. 어댑터에 추가
                adapter.addPostList(newPosts);

                // 3. 페이징을 위해 마지막 문서 스냅샷 저장
                lastVisible = queryDocumentSnapshots.getDocuments()
                        .get(queryDocumentSnapshots.size() - 1);
            } else {
                if (lastVisible != null) { // 첫 로드가 아닐 때만 메시지 표시
                    Toast.makeText(getContext(), "더 이상 게시물이 없습니다.", Toast.LENGTH_SHORT).show();
                }
            }
            isLoading = false;
        }).addOnFailureListener(e -> {
            if (!isAdded()) { // Fragment가 Activity에 붙어있는지 확인
                return;
            }
            Log.e("Firestore", "Error loading data", e);
            Toast.makeText(getContext(), "데이터 로딩 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
            isLoading = false;
        });
    }
}
