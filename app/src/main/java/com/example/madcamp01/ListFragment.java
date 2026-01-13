package com.example.madcamp01;

import android.Manifest;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class ListFragment extends Fragment {

    private RecyclerView recyclerView;
    private PostAdapter adapter;
    private ProgressBar loadingProgressBar;
    private boolean isLoading = false;

    // 헤더 및 프로필 카드 뷰
    private TextView tvHeaderTitle;
    private TextView tvUsername;
    private TextView tvPinsCount;
    private TextView tvFollowers;
    private TextView tvFollowing;
    private TextView tvBio;
    private TextView tvCurrentLocation;
    private View locationBadge;
    private ImageView ivProfilePicture;
    private ImageButton btnSettings;
    private ImageButton btnEditProfile;

    private FirebaseFirestore db;
    private FirebaseStorage storage;
    private DocumentSnapshot lastVisible;
    private final int LIMIT = 10;
    
    // 위치 관련
    private FusedLocationProviderClient fusedLocationClient;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 1001;

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

        // LinearLayoutManager로 변경 (세로 리스트)
        recyclerView.setLayoutManager(new LinearLayoutManager(view.getContext()));

        // 헤더 및 프로필 카드 뷰 초기화
        tvHeaderTitle = view.findViewById(R.id.tvHeaderTitle);
        tvUsername = view.findViewById(R.id.tvUsername);
        tvPinsCount = view.findViewById(R.id.tvPinsCount);
        tvFollowers = view.findViewById(R.id.tvFollowers);
        tvFollowing = view.findViewById(R.id.tvFollowing);
        tvBio = view.findViewById(R.id.tvBio);
        tvCurrentLocation = view.findViewById(R.id.tvCurrentLocation);
        locationBadge = view.findViewById(R.id.locationBadge);
        ivProfilePicture = view.findViewById(R.id.ivProfilePicture);
        btnSettings = view.findViewById(R.id.btnSettings);
        btnEditProfile = view.findViewById(R.id.btnEditProfile);

        // 위치 서비스 초기화
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(requireActivity());

        // 사용자 정보 로드 및 헤더 설정
        setupHeader();
        
        // 현재 위치 가져오기
        getCurrentLocation();

        // [수정] 지도 버튼 설정
        View btnOpenMap = view.findViewById(R.id.btn_open_map);
        if (btnOpenMap != null) {
            btnOpenMap.setOnClickListener(v -> {
                PostMapFragment mapFragment = new PostMapFragment();
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, mapFragment)
                        .addToBackStack(null)
                        .commit();
            });
        }
        View fabAddPost = view.findViewById(R.id.btn_new_post);
        if (fabAddPost != null) {
            fabAddPost.setOnClickListener(v -> {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).showTravelInfoDialog();
                }
            });
        }

        // 설정 버튼 클릭 리스너
        if (btnSettings != null) {
            btnSettings.setOnClickListener(v -> {
                // 설정 화면으로 이동하거나 다이얼로그 표시
                Toast.makeText(getContext(), "설정 기능은 준비 중입니다.", Toast.LENGTH_SHORT).show();
            });
        }

        // 프로필 편집 버튼 클릭 리스너
        if (btnEditProfile != null) {
            btnEditProfile.setOnClickListener(v -> {
                Toast.makeText(getContext(), "프로필 편집 기능은 준비 중입니다.", Toast.LENGTH_SHORT).show();
            });
        }


        // [수정] 어댑터 생성 시 isMyList를 true로 전달
        adapter = new PostAdapter(view.getContext(), true);
        recyclerView.setAdapter(adapter);

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

                if (getActivity() != null) {
                    // 타이틀만 업데이트
                    if (getActivity() instanceof androidx.appcompat.app.AppCompatActivity) {
                        ((androidx.appcompat.app.AppCompatActivity) getActivity()).setTitle("게시물 수정");
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
                    updatePinsCount(); // 삭제 후 게시물 수 업데이트
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
        deleteImageUrls(imageUrls, "image");
        deleteImageUrls(thumbnailUrls, "thumbnail");
    }

    private void deleteImageUrls(List<String> urls, String type) {
        if (urls == null || urls.isEmpty()) return;
        
        for (String url : urls) {
            if (url != null && !url.isEmpty() && url.startsWith("http")) {
                try {
                    StorageReference ref = storage.getReferenceFromUrl(url);
                    ref.delete().addOnFailureListener(e ->
                            Log.e("FirebaseStorage", "Failed to delete " + type + ": " + url, e));
                } catch (Exception e) {
                    Log.e("FirebaseStorage", "Error getting reference for " + type + ": " + url, e);
                }
            }
        }
    }

    private void hideLocationBadge() {
        if (locationBadge != null) {
            locationBadge.setVisibility(View.GONE);
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

        // 1. 현재 로그인한 사용자의 정보(이메일) 가져오기
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String currentUserEmail = (user != null) ? user.getEmail() : "";

        // 2. 쿼리에 본인 이메일 필터 추가 (userEmail 필드가 Firestore에 저장되어 있어야 함)
        Query query = db.collection("TravelPosts")
                .whereEqualTo("userEmail", currentUserEmail) // 본인 글만 필터링
                .orderBy("createdAt", Query.Direction.DESCENDING);

        if (lastVisible != null) {
            query = query.startAfter(lastVisible);
        }

        query = query.limit(LIMIT);

        query.get().addOnSuccessListener(queryDocumentSnapshots -> {
            loadingProgressBar.setVisibility(View.GONE);
            if (!isAdded()) return;

            if (!queryDocumentSnapshots.isEmpty()) {
                List<PostItem> newPosts = queryDocumentSnapshots.toObjects(PostItem.class);
                List<DocumentSnapshot> snapshots = new ArrayList<>();
                for (DocumentSnapshot doc : queryDocumentSnapshots.getDocuments()) {
                    snapshots.add(doc);
                }
                adapter.addPostList(newPosts, snapshots);
                lastVisible = queryDocumentSnapshots.getDocuments().get(queryDocumentSnapshots.size() - 1);
                
                // 게시물 수 업데이트 (첫 로드일 때만)
                if (isRefresh) {
                    updatePinsCount();
                }
            } else if (lastVisible == null) {
                // 첫 로드인데 게시물이 없는 경우
                Toast.makeText(getContext(), "게시물이 아직 없습니다.", Toast.LENGTH_LONG).show();
                updatePinsCount(); // 0으로 업데이트
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

    private void setupHeader() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            // 사용자명 설정 (이메일에서 @ 앞부분 사용)
            String email = user.getEmail();
            if (email != null) {
                String username = email.split("@")[0];
                if (tvUsername != null) {
                    tvUsername.setText("@" + username);
                }
            }

            // 프로필 사진 설정 (기본값 사용)
            if (ivProfilePicture != null) {
                // Glide를 사용하여 프로필 사진 로드 가능
                // 현재는 기본 아이콘 사용
            }
        }

        // 게시물 수를 로드하여 게시물 수 업데이트
        updatePinsCount();
    }

    /**
     * 현재 위치를 가져와서 표시
     */
    private void getCurrentLocation() {
        // 위치 권한 확인
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.ACCESS_FINE_LOCATION) 
                != PackageManager.PERMISSION_GRANTED) {
            // 권한이 없으면 요청
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 
                    LOCATION_PERMISSION_REQUEST_CODE);
            return;
        }

        // 위치 가져오기
        fusedLocationClient.getLastLocation()
                .addOnSuccessListener(location -> {
                    if (location != null) {
                        getAddressFromLocation(location.getLatitude(), location.getLongitude());
                    } else {
                        hideLocationBadge();
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("ListFragment", "Failed to get location", e);
                    hideLocationBadge();
                });
    }

    /**
     * 위도, 경도를 주소로 변환하여 표시
     */
    private void getAddressFromLocation(double latitude, double longitude) {
        Geocoder geocoder = new Geocoder(requireContext(), Locale.getDefault());
        try {
            List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                
                // 도시명 추출 (우선순위: locality > adminArea)
                String city = address.getLocality();
                if (city == null || city.isEmpty()) {
                    city = address.getAdminArea();
                }
                
                // 국가명 추출
                String country = address.getCountryName();
                
                // 위치 정보 표시 (아이폰 스타일 배지)
                if (tvCurrentLocation != null && locationBadge != null) {
                    String locationText;
                    if (city != null && !city.isEmpty()) {
                        locationText = city;
                    } else if (country != null && !country.isEmpty()) {
                        locationText = country;
                    } else {
                        locationText = "위치";
                    }
                    
                    tvCurrentLocation.setText(locationText);
                    locationBadge.setVisibility(View.VISIBLE);
                }
            } else {
                hideLocationBadge();
            }
        } catch (IOException e) {
            Log.e("ListFragment", "Geocoder error", e);
            hideLocationBadge();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // 권한이 허용되면 위치 가져오기
                getCurrentLocation();
            } else {
                hideLocationBadge();
            }
        }
    }

    private void updatePinsCount() {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) return;

        String currentUserEmail = user.getEmail();
        if (currentUserEmail == null) return;

        db.collection("TravelPosts")
                .whereEqualTo("userEmail", currentUserEmail)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    int count = queryDocumentSnapshots.size();
                    if (tvPinsCount != null) {
                        tvPinsCount.setText(String.valueOf(count));
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("ListFragment", "Error loading pins count", e);
                });
    }
}
