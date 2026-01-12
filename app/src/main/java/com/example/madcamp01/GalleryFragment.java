package com.example.madcamp01;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.util.ArrayList;
import java.util.List;

public class GalleryFragment extends Fragment {

    private ViewPager2 heroViewPager;
    private HeroPostAdapter adapter;
    private LinearLayout indicatorContainer;
    private ImageButton btnPrev;
    private ImageButton btnNext;
    private TextView emptyStateText;
    private ProgressBar loadingProgressBar;
    private boolean isLoading = false;
    private boolean isInitialLoad = true;

    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private DocumentSnapshot lastVisible;
    private final int LIMIT = 10;
    private Query.Direction sortDirection = Query.Direction.DESCENDING;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_gallery, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        heroViewPager = view.findViewById(R.id.heroViewPager);
        loadingProgressBar = view.findViewById(R.id.loadingProgressBar);
        indicatorContainer = view.findViewById(R.id.indicatorContainer);
        btnPrev = view.findViewById(R.id.btnPrev);
        btnNext = view.findViewById(R.id.btnNext);
        emptyStateText = view.findViewById(R.id.emptyStateText);
        ImageButton btnSortDesc = view.findViewById(R.id.btn_sort_desc);
        ImageButton btnSortAsc = view.findViewById(R.id.btn_sort_asc);

        adapter = new HeroPostAdapter(view.getContext());
        heroViewPager.setAdapter(adapter);
        heroViewPager.setOffscreenPageLimit(2);

        adapter.setOnItemClickListener(item -> {
            DetailFragment detailFragment = new DetailFragment();
            Bundle args = new Bundle();
            args.putParcelable("postData", item);
            detailFragment.setArguments(args);
            requireActivity().getSupportFragmentManager().beginTransaction()
                    .add(R.id.fragment_container, detailFragment) // 기존 화면 위에 추가
                    .hide(this) // 현재 화면(갤러리)은 잠시 숨김
                    .addToBackStack(null) // 뒤로 가기 가능하게 설정
                    .commit();
        });

        adapter.setOnItemLongClickListener(this::showPopupMenu);

        heroViewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                updateIndicators(position);
                if (!isLoading && position >= adapter.getItemCount() - 2) {
                    loadMoreData(false);
                }
            }
        });

        btnPrev.setOnClickListener(v -> {
            int prev = heroViewPager.getCurrentItem() - 1;
            if (prev >= 0) heroViewPager.setCurrentItem(prev, true);
        });

        btnNext.setOnClickListener(v -> {
            int next = heroViewPager.getCurrentItem() + 1;
            if (next < adapter.getItemCount()) heroViewPager.setCurrentItem(next, true);
        });

        btnSortDesc.setOnClickListener(v -> {
            sortDirection = Query.Direction.DESCENDING;
            loadMoreData(true);
        });

        btnSortAsc.setOnClickListener(v -> {
            sortDirection = Query.Direction.ASCENDING;
            loadMoreData(true);
        });

        // 초기 진입 시 바로 로드 (onResume 대기 없이)
        loadMoreData(true);
    }

    @Override
    public void onResume() {
        super.onResume();
        // 어댑터에 데이터가 하나도 없는 경우에만 로드하도록 변경
        if (adapter == null || adapter.getItemCount() == 0) {
            loadMoreData(true);
        }
    }

    private void showPopupMenu(View anchorView, PostItem item) {
        if (getContext() == null) return;
        // 현재 로그인한 사용자 정보 가져오기
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String currentUserId = (currentUser != null) ? currentUser.getUid() : "";

        PopupMenu popup = new PopupMenu(getContext(), anchorView);
        popup.getMenuInflater().inflate(R.menu.post_context_menu, popup.getMenu());

        // 권한 확인: 게시물의 userId와 현재 로그인한 UID 비교
        boolean isOwner = item.getUserId() != null && item.getUserId().equals(currentUserId);

        // 본인이 아니면 '수정' 및 '삭제' 메뉴 숨기기
        if (!isOwner) {
            popup.getMenu().findItem(R.id.menu_edit).setVisible(false);
            popup.getMenu().findItem(R.id.menu_delete).setVisible(false);
        }

        popup.setOnMenuItemClickListener(menuItem -> {
            int itemId = menuItem.getItemId();
            if (itemId == R.id.menu_edit) {
                // 1. WriteFragment 인스턴스 생성
                WriteFragment writeFragment = new WriteFragment();

                // 2. 수정을 위한 데이터 꾸러미(Bundle) 생성 및 전달
                Bundle args = new Bundle();
                args.putString("postId", item.getDocumentId()); // 문서 ID
                args.putString("title", item.getTitle());       // 기존 제목
                args.putStringArrayList("images", new ArrayList<>(item.getImages())); // 기존 이미지 리스트
                args.putBoolean("isPublic", item.getIsPublic());   // 공개 여부
                args.putParcelable("postItem", item); // PostItem 전체 전달

                writeFragment.setArguments(args);

                // 3. WriteFragment로 화면 전환
                requireActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, writeFragment)
                        .addToBackStack(null) // 뒤로가기 가능하게 설정
                        .commit();

                // 4. 네비게이션 바 상태 업데이트
                if (getActivity() != null) {
                    com.google.android.material.bottomnavigation.BottomNavigationView bottomNav = 
                        getActivity().findViewById(R.id.bottom_navigation);
                    if (bottomNav != null) {
                        bottomNav.getMenu().findItem(R.id.nav_write).setChecked(true);
                    }
                    // 타이틀 업데이트
                    if (getActivity() instanceof androidx.appcompat.app.AppCompatActivity) {
                        ((androidx.appcompat.app.AppCompatActivity) getActivity()).setTitle("글쓰기");
                    }
                }

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
                    // 원본 이미지와 썸네일 모두 삭제
                    deletePostImages(item.getImages(), item.getImageThumbnails());
                    Toast.makeText(getContext(), "게시물이 삭제되었습니다.", Toast.LENGTH_SHORT).show();
                    loadMoreData(true);
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Error deleting document", e);
                    Toast.makeText(getContext(), "삭제 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
                });
    }

    /**
     * Storage에서 원본 이미지와 썸네일 모두 삭제
     */
    private void deletePostImages(List<String> imageUrls, List<String> thumbnailUrls) {
        // 원본 이미지 삭제
        if (imageUrls != null && !imageUrls.isEmpty()) {
            for (String imageUrl : imageUrls) {
                if (imageUrl != null && !imageUrl.isEmpty() && imageUrl.startsWith("http")) {
                    try {
                        StorageReference photoRef = storage.getReferenceFromUrl(imageUrl);
                        photoRef.delete().addOnFailureListener(e ->
                                Log.e("FirebaseStorage", "Failed to delete image: " + imageUrl, e));
                    } catch (Exception e) {
                        Log.e("FirebaseStorage", "Error getting reference for image: " + imageUrl, e);
                    }
                }
            }
        }
        
        // 썸네일 삭제
        if (thumbnailUrls != null && !thumbnailUrls.isEmpty()) {
            for (String thumbnailUrl : thumbnailUrls) {
                if (thumbnailUrl != null && !thumbnailUrl.isEmpty() && thumbnailUrl.startsWith("http")) {
                    try {
                        StorageReference thumbnailRef = storage.getReferenceFromUrl(thumbnailUrl);
                        thumbnailRef.delete().addOnFailureListener(e ->
                                Log.e("FirebaseStorage", "Failed to delete thumbnail: " + thumbnailUrl, e));
                    } catch (Exception e) {
                        Log.e("FirebaseStorage", "Error getting reference for thumbnail: " + thumbnailUrl, e);
                    }
                }
            }
        }
    }

    private void loadMoreData(boolean isRefresh) {
        if (isLoading) return;
        isLoading = true;

        if (isRefresh) {
            adapter.clearPosts();
            lastVisible = null;
            loadingProgressBar.setVisibility(View.VISIBLE);
            indicatorContainer.removeAllViews();
        }

        // 쿼리에서 정확한 필드 이름인 "isPublic"을 사용합니다.
        Query query = db.collection("TravelPosts")
                .whereEqualTo("isPublic", true)
                .orderBy("createdAt", sortDirection);

        if (lastVisible != null) {
            query = query.startAfter(lastVisible);
        }

        query.limit(LIMIT).get().addOnSuccessListener(queryDocumentSnapshots -> {
            loadingProgressBar.setVisibility(View.GONE);
            if (!isAdded()) return;

            if (!queryDocumentSnapshots.isEmpty()) {
                emptyStateText.setVisibility(View.GONE);
                List<PostItem> newPosts = queryDocumentSnapshots.toObjects(PostItem.class);
                List<DocumentSnapshot> snapshots = new ArrayList<>();
                for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                    snapshots.add(doc);
                }
                Log.d("GalleryFragment", "Loaded posts: " + newPosts.size());
                adapter.addPostList(newPosts, snapshots);
                lastVisible = queryDocumentSnapshots.getDocuments().get(queryDocumentSnapshots.size() - 1);
                buildIndicators(adapter.getItemCount());
            } else if (lastVisible == null) {
                emptyStateText.setVisibility(View.VISIBLE);
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

    private void buildIndicators(int count) {
        if (indicatorContainer == null || count <= 0) return;
        indicatorContainer.removeAllViews();
        for (int i = 0; i < count; i++) {
            View dot = new View(getContext());
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(12, 12);
            lp.setMargins(8, 0, 8, 0);
            dot.setLayoutParams(lp);
            dot.setBackgroundResource(R.drawable.indicator_dot_inactive);
            final int position = i;
            dot.setOnClickListener(v -> {
                if (heroViewPager != null) {
                    heroViewPager.setCurrentItem(position, true);
                }
            });
            indicatorContainer.addView(dot);
        }
        updateIndicators(heroViewPager.getCurrentItem());
    }

    private void updateIndicators(int activeIndex) {
        if (indicatorContainer == null) return;
        int childCount = indicatorContainer.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View dot = indicatorContainer.getChildAt(i);
            dot.setBackgroundResource(i == activeIndex ? R.drawable.indicator_dot_active : R.drawable.indicator_dot_inactive);
        }
    }
}