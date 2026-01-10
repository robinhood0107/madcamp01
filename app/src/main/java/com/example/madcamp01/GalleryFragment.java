package com.example.madcamp01;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.List;

/**
 * SNS 게시판 역할을 하는 프래그먼트입니다.
 * ListFragment의 전체적인 구조와 기능을 대부분 재사용하며, 아래와 같은 차이점이 있습니다.
 * 1. isPublic = true 인 게시물만 필터링하여 보여줍니다.
 * 2. '최신순'/'오래된 순'으로 정렬하는 기능을 제공합니다.
 */
public class GalleryFragment extends Fragment {

    // =========================================================================================
    // == 멤버 변수: ListFragment로부터 대부분 재사용
    // =========================================================================================

    // UI 요소
    private RecyclerView recyclerView; // 게시물 목록을 보여주는 리스트
    private PostAdapter adapter;          // 리스트에 데이터를 연결하는 어댑터
    private ProgressBar loadingProgressBar; // 데이터 로딩 중에 표시되는 원형 아이콘

    // 상태 관리 변수
    private boolean isLoading = false;      // 현재 데이터를 로딩 중인지 여부 (중복 로딩 방지용)
    private boolean isInitialLoad = true;   // 이 화면에 처음 진입했는지 여부 (화면이 다시 보일 때마다 불필요하게 새로고침하는 것을 방지)

    // Firebase 관련
    private FirebaseFirestore db;           // Firestore 데이터베이스 인스턴스
    private FirebaseStorage storage;        // Firebase Storage 인스턴스 (이미지 삭제 시 필요)
    private DocumentSnapshot lastVisible;   // 페이징(무한 스크롤)을 위해, 마지막으로 화면에 보인 게시물의 정보
    private final int LIMIT = 10;           // 한 번에 불러올 게시물의 개수

    // GalleryFragment의 고유 기능: 정렬 방향을 저장하는 변수
    private Query.Direction sortDirection = Query.Direction.DESCENDING; // 기본 정렬: 최신순 (내림차순)

    /**
     * 프래그먼트의 UI가 처음 생성될 때 호출되는 메소드입니다.
     * ListFragment와 직접적인 관련은 없지만, 여기서는 GalleryFragment의 UI를 정의하는
     * fragment_gallery.xml 파일을 화면에 표시하도록 설정합니다.
     */
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_gallery, container, false);
    }

    /**
     * onCreateView()를 통해 View가 생성된 직후에 호출됩니다.
     * 이 메소드에서는 UI 요소들을 초기화하고, 버튼 리스너를 설정하는 등 UI 관련 작업을 수행합니다.
     * ListFragment의 onViewCreated와 거의 동일한 구조를 가집니다.
     */
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // --- [재사용] Firebase 인스턴스 초기화 ---
        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();

        // --- [재사용] UI 요소 초기화 ---
        recyclerView = view.findViewById(R.id.recyclerView);
        loadingProgressBar = view.findViewById(R.id.loadingProgressBar);

        // --- [GalleryFragment 고유 기능] 정렬 버튼 초기화 ---
        Button btnSortDesc = view.findViewById(R.id.btn_sort_desc);
        Button btnSortAsc = view.findViewById(R.id.btn_sort_asc);

        // --- [재사용] RecyclerView 및 Adapter 설정 ---
        // PostAdapter 생성 시 isMyList를 'false'로 전달하여, 이 어댑터가 SNS 게시판용임을 알립니다.
        adapter = new PostAdapter(view.getContext(), false);
        recyclerView.setLayoutManager(new GridLayoutManager(view.getContext(), 2));
        recyclerView.setAdapter(adapter);

        // --- [재사용] 아이템 클릭/롱클릭 리스너 설정 ---
        // 아이템 클릭 시 -> 상세 화면(DetailFragment)으로 이동 (ListFragment와 완전히 동일한 기능)
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

        // 아이템 롱클릭 시 -> 수정/삭제 팝업 메뉴 표시 (ListFragment와 완전히 동일한 기능)
        adapter.setOnItemLongClickListener(this::showPopupMenu);

        // --- [재사용] 스크롤 리스너 설정 (무한 스크롤 구현) ---
        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                // 스크롤이 맨 아래에 도달했고, 현재 로딩 중이 아닐 때만 다음 데이터를 불러옵니다.
                if (!isLoading && !recyclerView.canScrollVertically(1)) {
                    loadMoreData(false); // 추가 로드
                }
            }
        });

        // --- [GalleryFragment 고유 기능] 정렬 버튼 리스너 설정 ---
        btnSortDesc.setOnClickListener(v -> {
            sortDirection = Query.Direction.DESCENDING; // 정렬 방향을 '최신순'으로 변경
            loadMoreData(true); // 목록을 새로고침합니다.
        });

        btnSortAsc.setOnClickListener(v -> {
            sortDirection = Query.Direction.ASCENDING; // 정렬 방향을 '오래된 순'으로 변경
            loadMoreData(true); // 목록을 새로고침합니다.
        });
    }

    /**
     * [재사용] 프래그먼트가 화면에 완전히 표시되고 사용자와 상호작용이 가능해질 때 호출됩니다.
     * ListFragment와 동일하게, 화면에 처음 진입할 때만 초기 데이터를 불러오도록 구현하여
     * 불필요한 데이터 로딩을 방지합니다.
     */
    @Override
    public void onResume() {
        super.onResume();
        if (isInitialLoad) {
            loadMoreData(true);
            isInitialLoad = false;
        }
    }

    // =========================================================================================
    // == ListFragment로부터 코드를 그대로 복사하여 재사용한 메소드들
    // == (팝업 메뉴 표시, 삭제 확인, 실제 삭제 로직)
    // =========================================================================================

    private void showPopupMenu(View anchorView, PostItem item) {
        if (getContext() == null) return;
        PopupMenu popup = new PopupMenu(getContext(), anchorView);
        popup.getMenuInflater().inflate(R.menu.post_context_menu, popup.getMenu());
        popup.setOnMenuItemClickListener(menuItem -> {
            int itemId = menuItem.getItemId();
            if (itemId == R.id.menu_edit) {
                Toast.makeText(getContext(), "수정 기능은 준비 중입니다.", Toast.LENGTH_SHORT).show();
                return true;
            } else if (itemId == R.id.menu_delete) {
                showDeleteConfirmationDialog(item);
                return true;
            }
            return false;
        });
        popup.show();
    }

    private void showDeleteConfirmationDialog(PostItem item) {
        if (getContext() == null) return;
        new AlertDialog.Builder(getContext())
                .setTitle("삭제 확인")
                .setMessage("정말로 이 게시물을 삭제하시겠습니까? 관련된 모든 사진도 함께 삭제됩니다.")
                .setPositiveButton("삭제", (dialog, which) -> deletePost(item))
                .setNegativeButton("취소", null)
                .show();
    }

    private void deletePost(PostItem item) {
        String documentId = item.getDocumentId();
        if (documentId == null || documentId.isEmpty()) {
            Toast.makeText(getContext(), "오류: 게시물 ID가 없습니다.", Toast.LENGTH_SHORT).show();
            return;
        }
        db.collection("TravelPosts").document(documentId).delete()
                .addOnSuccessListener(aVoid -> {
                    deletePostImages(item.getImages());
                    Toast.makeText(getContext(), "게시물이 삭제되었습니다.", Toast.LENGTH_SHORT).show();
                    loadMoreData(true);
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Error deleting document", e);
                    Toast.makeText(getContext(), "삭제 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
                });
    }

    private void deletePostImages(List<String> imageUrls) {
        if (imageUrls == null || imageUrls.isEmpty()) return;
        for (String imageUrl : imageUrls) {
            StorageReference photoRef = storage.getReferenceFromUrl(imageUrl);
            photoRef.delete().addOnFailureListener(e ->
                    Log.e("FirebaseStorage", "Failed to delete image: " + imageUrl, e));
        }
    }

    // =========================================================================================
    // == 데이터 로딩 메소드: ListFragment의 것을 재사용하되, Query 부분만 수정
    // =========================================================================================

    private void loadMoreData(boolean isRefresh) {
        if (isLoading) return;
        isLoading = true;

        if (isRefresh) {
            adapter.clearPosts();
            lastVisible = null;
            loadingProgressBar.setVisibility(View.VISIBLE);
        }

        // --- [수정] GalleryFragment의 핵심 로직: 필터링 및 정렬 --- //
        Query query = db.collection("TravelPosts")
                .whereEqualTo("public", true) // 조건 1: isPublic이 true인 게시물만 필터링
                .orderBy("createdAt", sortDirection);     // 조건 2: 설정된 방향(최신/오래된)으로 정렬

        if (lastVisible != null) {
            query = query.startAfter(lastVisible);
        }

        query.limit(LIMIT).get().addOnSuccessListener(queryDocumentSnapshots -> {
            loadingProgressBar.setVisibility(View.GONE);
            if (!isAdded()) return;

            if (!queryDocumentSnapshots.isEmpty()) {
                List<PostItem> newPosts = queryDocumentSnapshots.toObjects(PostItem.class);
                adapter.addPostList(newPosts);
                lastVisible = queryDocumentSnapshots.getDocuments().get(queryDocumentSnapshots.size() - 1);
            } else if (lastVisible == null) {
                Toast.makeText(getContext(), "공유된 게시물이 아직 없습니다.", Toast.LENGTH_LONG).show();
            }
            isLoading = false;
        }).addOnFailureListener(e -> {
            loadingProgressBar.setVisibility(View.GONE);
            if (!isAdded()) return;
            Log.e("Firestore", "Error loading gallery data", e);
            Toast.makeText(getContext(), "갤러리를 불러오는 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
            isLoading = false;
        });
    }
}