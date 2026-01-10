package com.example.madcamp01;

import android.app.AlertDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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

public class ListFragment extends Fragment {

    private RecyclerView recyclerView;
    private PostAdapter adapter;
    private ProgressBar loadingProgressBar;
    private boolean isLoading = false;

    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private DocumentSnapshot lastVisible;
    private final int LIMIT = 10;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        db = FirebaseFirestore.getInstance();
        storage = FirebaseStorage.getInstance();
        recyclerView = view.findViewById(R.id.recyclerView);
        loadingProgressBar = view.findViewById(R.id.loadingProgressBar);
        recyclerView.setLayoutManager(new GridLayoutManager(getContext(), 2));
        adapter = new PostAdapter(getContext());
        recyclerView.setAdapter(adapter);

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

        adapter.setOnItemLongClickListener(this::showPopupMenu);

        recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (!isLoading && !recyclerView.canScrollVertically(1)) {
                    loadMoreData(false);
                }
            }
        });

        loadMoreData(true);
    }

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

    private void loadMoreData(boolean isRefresh) {
        if (isLoading) return;
        isLoading = true;

        if (isRefresh) {
            adapter.clearPosts();
            lastVisible = null;
            loadingProgressBar.setVisibility(View.VISIBLE);
        }

        Query query = (lastVisible == null)
                ? db.collection("TravelPosts").orderBy("createdAt", Query.Direction.DESCENDING).limit(LIMIT)
                : db.collection("TravelPosts").orderBy("createdAt", Query.Direction.DESCENDING).startAfter(lastVisible).limit(LIMIT);

        query.get().addOnSuccessListener(queryDocumentSnapshots -> {
            loadingProgressBar.setVisibility(View.GONE);
            if (!isAdded()) return;

            if (!queryDocumentSnapshots.isEmpty()) {
                List<PostItem> newPosts = queryDocumentSnapshots.toObjects(PostItem.class);
                adapter.addPostList(newPosts);
                lastVisible = queryDocumentSnapshots.getDocuments().get(queryDocumentSnapshots.size() - 1);
            } else if (lastVisible == null) {
                // 첫 로드 시 데이터가 없는 경우, 메시지를 띄우지 않고 그냥 비워둡니다.
            }
            isLoading = false;
        }).addOnFailureListener(e -> {
            loadingProgressBar.setVisibility(View.GONE);
            if (!isAdded()) return;
            Log.e("Firestore", "Error loading data", e);
            Toast.makeText(getContext(), "데이터 로딩 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
            isLoading = false;
        });
    }
}