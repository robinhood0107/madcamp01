package com.example.madcamp01;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
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
    private EditText etSearch;
    private ImageButton btnSearch;
    
    private boolean isLoading = false;
    private String currentSearchQuery = "";

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
        etSearch = view.findViewById(R.id.etSearch);
        btnSearch = view.findViewById(R.id.btnSearch);
        
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
                    .add(R.id.fragment_container, detailFragment)
                    .hide(this)
                    .addToBackStack(null)
                    .commit();
        });

        adapter.setOnItemLongClickListener(this::showPopupMenu);

        heroViewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                updateIndicators(position);
                // 검색 중이 아닐 때만 페이징 로드 (검색 시에는 전체 결과를 한번에 보여주거나 별도 처리)
                if (currentSearchQuery.isEmpty() && !isLoading && position >= adapter.getItemCount() - 2) {
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
            performSearch(currentSearchQuery);
        });

        btnSortAsc.setOnClickListener(v -> {
            sortDirection = Query.Direction.ASCENDING;
            performSearch(currentSearchQuery);
        });

        // 검색 버튼 클릭 리스너
        btnSearch.setOnClickListener(v -> {
            performSearch(etSearch.getText().toString().trim());
        });

        // 키보드 검색 버튼 리스너
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH) {
                performSearch(etSearch.getText().toString().trim());
                return true;
            }
            return false;
        });

        loadMoreData(true);
    }

    private void performSearch(String query) {
        currentSearchQuery = query;
        loadMoreData(true);
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

        Query query;
        if (currentSearchQuery.isEmpty()) {
            // 일반 모드: 최신순/오래된순 페이징 로드
            query = db.collection("TravelPosts")
                    .whereEqualTo("isPublic", true)
                    .orderBy("createdAt", sortDirection);
            
            if (lastVisible != null) {
                query = query.startAfter(lastVisible);
            }
            query = query.limit(LIMIT);
            
            query.get().addOnSuccessListener(queryDocumentSnapshots -> {
                processQueryResults(queryDocumentSnapshots.getDocuments(), isRefresh);
                if (!queryDocumentSnapshots.isEmpty()) {
                    lastVisible = queryDocumentSnapshots.getDocuments().get(queryDocumentSnapshots.size() - 1);
                }
                isLoading = false;
            }).addOnFailureListener(e -> {
                handleLoadFailure(e);
            });
        } else {
            // 검색 모드: 도시 또는 국가 리스트에 포함된 경우 검색
            // Firestore의 array-contains 한계를 극복하기 위해 모든 공개 게시물을 가져와서 클라이언트 필터링
            // (게시물이 아주 많아지면 Algolia나 별도의 검색 필드가 필요하지만, 현재 수준에선 적합한 방식)
            db.collection("TravelPosts")
                    .whereEqualTo("isPublic", true)
                    .orderBy("createdAt", sortDirection)
                    .get()
                    .addOnSuccessListener(queryDocumentSnapshots -> {
                        List<DocumentSnapshot> filteredDocs = new ArrayList<>();
                        String lowerQuery = currentSearchQuery.toLowerCase();
                        
                        for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                            List<String> cities = (List<String>) doc.get("cities");
                            List<String> countries = (List<String>) doc.get("countries");
                            
                            boolean match = false;
                            if (cities != null) {
                                for (String c : cities) {
                                    if (c.toLowerCase().contains(lowerQuery)) { match = true; break; }
                                }
                            }
                            if (!match && countries != null) {
                                for (String c : countries) {
                                    if (c.toLowerCase().contains(lowerQuery)) { match = true; break; }
                                }
                            }
                            
                            if (match) {
                                filteredDocs.add(doc);
                            }
                        }
                        processQueryResults(filteredDocs, isRefresh);
                        isLoading = false;
                    }).addOnFailureListener(e -> {
                        handleLoadFailure(e);
                    });
        }
    }

    private void processQueryResults(List<DocumentSnapshot> documents, boolean isRefresh) {
        loadingProgressBar.setVisibility(View.GONE);
        if (!isAdded()) return;

        if (!documents.isEmpty()) {
            emptyStateText.setVisibility(View.GONE);
            List<PostItem> newPosts = new ArrayList<>();
            for (DocumentSnapshot doc : documents) {
                newPosts.add(doc.toObject(PostItem.class));
            }
            adapter.addPostList(newPosts, documents);
            buildIndicators(adapter.getItemCount());
        } else if (isRefresh) {
            emptyStateText.setVisibility(View.VISIBLE);
            emptyStateText.setText(currentSearchQuery.isEmpty() ? "공유된 게시물이 아직 없습니다." : "'" + currentSearchQuery + "' 검색 결과가 없습니다.");
        }
    }

    private void handleLoadFailure(Exception e) {
        loadingProgressBar.setVisibility(View.GONE);
        if (!isAdded()) return;
        Log.e("GalleryFragment", "Error loading data", e);
        Toast.makeText(getContext(), "데이터를 불러오는 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
        isLoading = false;
    }

    private void buildIndicators(int count) {
        if (indicatorContainer == null || count <= 0) return;
        indicatorContainer.removeAllViews();
        // 인디케이터가 너무 많아지면 UI가 깨질 수 있으므로 최대 개수 제한(예: 15개)
        int displayCount = Math.min(count, 15);
        for (int i = 0; i < displayCount; i++) {
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
        if (heroViewPager.getCurrentItem() < displayCount) {
            updateIndicators(heroViewPager.getCurrentItem());
        }
    }

    private void updateIndicators(int activeIndex) {
        if (indicatorContainer == null) return;
        int childCount = indicatorContainer.getChildCount();
        for (int i = 0; i < childCount; i++) {
            View dot = indicatorContainer.getChildAt(i);
            dot.setBackgroundResource(i == activeIndex ? R.drawable.indicator_dot_active : R.drawable.indicator_dot_inactive);
        }
    }

    private void showPopupMenu(View anchorView, PostItem item) {
        if (getContext() == null) return;
        FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
        String currentUserId = (currentUser != null) ? currentUser.getUid() : "";

        PopupMenu popup = new PopupMenu(getContext(), anchorView);
        popup.getMenuInflater().inflate(R.menu.post_context_menu, popup.getMenu());

        boolean isOwner = item.getUserId() != null && item.getUserId().equals(currentUserId);

        if (!isOwner) {
            popup.getMenu().findItem(R.id.menu_edit).setVisible(false);
            popup.getMenu().findItem(R.id.menu_delete).setVisible(false);
        }

        popup.setOnMenuItemClickListener(menuItem -> {
            int itemId = menuItem.getItemId();
            if (itemId == R.id.menu_edit) {
                WriteFragment writeFragment = new WriteFragment();
                Bundle args = new Bundle();
                args.putString("postId", item.getDocumentId());
                args.putString("title", item.getTitle());
                args.putStringArrayList("images", new ArrayList<>(item.getImages()));
                args.putBoolean("isPublic", item.getIsPublic());
                args.putParcelable("postItem", item);
                writeFragment.setArguments(args);

                requireActivity().getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, writeFragment)
                        .addToBackStack(null)
                        .commit();
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
                    deletePostImages(item.getImages(), item.getImageThumbnails());
                    Toast.makeText(getContext(), "게시물이 삭제되었습니다.", Toast.LENGTH_SHORT).show();
                    loadMoreData(true);
                })
                .addOnFailureListener(e -> {
                    Log.e("Firestore", "Error deleting document", e);
                    Toast.makeText(getContext(), "삭제 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
                });
    }

    private void deletePostImages(List<String> imageUrls, List<String> thumbnailUrls) {
        if (imageUrls != null) {
            for (String imageUrl : imageUrls) {
                if (imageUrl != null && imageUrl.startsWith("http")) {
                    try {
                        storage.getReferenceFromUrl(imageUrl).delete();
                    } catch (Exception ignored) {}
                }
            }
        }
        if (thumbnailUrls != null) {
            for (String thumbnailUrl : thumbnailUrls) {
                if (thumbnailUrl != null && thumbnailUrl.startsWith("http")) {
                    try {
                        storage.getReferenceFromUrl(thumbnailUrl).delete();
                    } catch (Exception ignored) {}
                }
            }
        }
    }
}
