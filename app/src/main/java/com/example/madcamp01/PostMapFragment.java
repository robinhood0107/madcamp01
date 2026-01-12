package com.example.madcamp01;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QuerySnapshot;
import com.google.maps.android.clustering.ClusterManager;

import java.util.List;

public class PostMapFragment extends Fragment implements OnMapReadyCallback {

    private GoogleMap mMap;
    private ClusterManager<PhotoItem> clusterManager;
    private FirebaseFirestore db;
    private FirebaseAuth mAuth;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_post_map, container, false);

        db = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance(); // 내 이메일 정보를 가져오기 위함

        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager()
                .findFragmentById(R.id.map);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }

        return view;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;
        clusterManager = new ClusterManager<>(getContext(), mMap);

        PhotoRenderer renderer = new PhotoRenderer(getContext(), mMap, clusterManager);
        clusterManager.setRenderer(renderer);

        // --- 마커(아이템) 클릭 리스너 설정 ---
        clusterManager.setOnClusterItemClickListener(item -> {
            // 게시물 상세 화면으로 이동하는 로직
            navigateToPostDetail(item.getPostId());
            return true;
        });

        // 클러스터(겹친 뭉치) 클릭 시 줌인 로직 (선택 사항)
        clusterManager.setOnClusterClickListener(cluster -> {
            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(cluster.getPosition(), mMap.getCameraPosition().zoom + 2));
            return true;
        });

        mMap.setOnCameraIdleListener(clusterManager);
        mMap.setOnMarkerClickListener(clusterManager);

        loadAllMyPhotosFromFirestore();
    }

    private void loadAllMyPhotosFromFirestore() {
        if (mAuth.getCurrentUser() == null) return;

        String myEmail = mAuth.getCurrentUser().getEmail();

        // 1. 컬렉션 이름을 "TravelPosts"로 변경 (ListFragment와 동일하게)
        db.collection("TravelPosts")
                // 2. 필드명이 "userEmail"인지 "userId"인지 WriteFragment 저장 시점과 맞춰야 합니다.
                .whereEqualTo("userEmail", myEmail)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        QuerySnapshot querySnapshot = task.getResult();

                        // 데이터가 정말 오는지 로그로 확인 (Logcat에서 확인 가능)
                        Log.d("MapDebug", "가져온 문서 개수: " + querySnapshot.size());

                        clusterManager.clearItems(); // 기존 아이템 제거

                        for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                            String postId = document.getId();
                            // 필드명 확인: imageLatitudes, imageLongitudes, imageUrls
                            List<Double> lats = (List<Double>) document.get("imageLatitudes");
                            List<Double> lngs = (List<Double>) document.get("imageLongitudes");
                            List<String> urls = (List<String>) document.get("images");

                            if (lats != null && lngs != null && urls != null) {
                                for (int i = 0; i < lats.size(); i++) {
                                    if (lats.get(i) != 0.0 && lngs.get(i) != 0.0) {
                                        PhotoItem photoItem = new PhotoItem(lats.get(i), lngs.get(i), urls.get(i), postId);
                                        clusterManager.addItem(photoItem);
                                    }
                                }
                            }
                        }
                        clusterManager.cluster(); // 다시 그리기

                        // 마커가 추가되었을 때 카메라를 첫 번째 위치로 이동
                        if (!clusterManager.getAlgorithm().getItems().isEmpty()) {
                            PhotoItem first = clusterManager.getAlgorithm().getItems().iterator().next();
                            mMap.animateCamera(CameraUpdateFactory.newLatLngZoom(first.getPosition(), 10f));
                        }
                    } else {
                        Log.e("MapDebug", "데이터 가져오기 실패", task.getException());
                    }
                });
    }
    private void navigateToPostDetail(String postId) {
        // 1. Firestore에서 해당 게시물의 전체 데이터를 다시 가져옵니다.
        db.collection("TravelPosts").document(postId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        // 2. 가져온 데이터를 PostItem 객체로 변환합니다.
                        // (PostItem이 Parcelable을 구현하고 있다고 가정합니다 - ListFragment에서 잘 됐으므로)
                        PostItem postItem = documentSnapshot.toObject(PostItem.class);
                        if (postItem != null) {
                            postItem.setDocumentId(documentSnapshot.getId());

                            // 3. DetailFragment 생성 및 데이터 전달
                            DetailFragment detailFragment = new DetailFragment();
                            Bundle bundle = new Bundle();

                            // ListFragment와 동일한 키 "postData"를 사용하여 객체 자체를 넘겨줍니다.
                            bundle.putParcelable("postData", postItem);
                            detailFragment.setArguments(bundle);

                            // 4. 화면 전환
                            getParentFragmentManager().beginTransaction()
                                    .replace(R.id.fragment_container, detailFragment)
                                    .addToBackStack(null)
                                    .commit();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e("MapDebug", "게시물 상세 정보 로드 실패", e);
                });
    }

}
