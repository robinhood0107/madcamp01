package com.example.madcamp01;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

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
        mAuth = FirebaseAuth.getInstance();

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
        clusterManager = new ClusterManager<>(requireContext(), mMap);

        PhotoRenderer renderer = new PhotoRenderer(requireContext(), mMap, clusterManager);
        clusterManager.setRenderer(renderer);

        // --- 개별 마커 클릭 리스너 ---
        clusterManager.setOnClusterItemClickListener(item -> {
            if (item.getPostId() != null) {
                navigateToPostDetail(item.getPostId());
            }
            return true;
        });

        // --- 클러스터(뭉치) 클릭 리스너 ---
        // 확대 기능을 없애고, 뭉쳐있는 경우 가장 첫 번째 아이템의 게시물로 무조건 이동합니다.
        clusterManager.setOnClusterClickListener(cluster -> {
            if (!cluster.getItems().isEmpty()) {
                PhotoItem firstItem = cluster.getItems().iterator().next();
                if (firstItem.getPostId() != null) {
                    navigateToPostDetail(firstItem.getPostId());
                }
            }
            return true;
        });

        mMap.setOnCameraIdleListener(clusterManager);
        mMap.setOnMarkerClickListener(clusterManager);

        loadAllMyPhotosFromFirestore();
    }

    private void loadAllMyPhotosFromFirestore() {
        if (mAuth.getCurrentUser() == null) return;

        String myEmail = mAuth.getCurrentUser().getEmail();

        db.collection("TravelPosts")
                .whereEqualTo("userEmail", myEmail)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null) {
                        QuerySnapshot querySnapshot = task.getResult();
                        Log.d("MapDebug", "가져온 문서 개수: " + querySnapshot.size());

                        clusterManager.clearItems();

                        for (DocumentSnapshot document : querySnapshot.getDocuments()) {
                            String postId = document.getId();
                            List<Double> lats = (List<Double>) document.get("imageLatitudes");
                            List<Double> lngs = (List<Double>) document.get("imageLongitudes");
                            List<String> urls = (List<String>) document.get("images");

                            if (lats != null && lngs != null && urls != null) {
                                for (int i = 0; i < lats.size(); i++) {
                                    if (i < lngs.size() && i < urls.size()) {
                                        Double lat = lats.get(i);
                                        Double lng = lngs.get(i);
                                        if (lat != null && lng != null && lat != 0.0 && lng != 0.0) {
                                            PhotoItem photoItem = new PhotoItem(lat, lng, urls.get(i), postId);
                                            clusterManager.addItem(photoItem);
                                        }
                                    }
                                }
                            }
                        }
                        clusterManager.cluster();

                        if (!clusterManager.getAlgorithm().getItems().isEmpty()) {
                            PhotoItem first = clusterManager.getAlgorithm().getItems().iterator().next();
                            mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(first.getPosition(), 5f));
                        }
                    }
                });
    }

    private void navigateToPostDetail(String postId) {
        db.collection("TravelPosts").document(postId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (!isAdded()) return;
                    if (documentSnapshot.exists()) {
                        PostItem postItem = documentSnapshot.toObject(PostItem.class);
                        if (postItem != null) {
                            postItem.setDocumentId(documentSnapshot.getId());

                            DetailFragment detailFragment = new DetailFragment();
                            Bundle bundle = new Bundle();
                            bundle.putParcelable("postData", postItem);
                            detailFragment.setArguments(bundle);

                            requireActivity().getSupportFragmentManager().beginTransaction()
                                    .add(R.id.fragment_container, detailFragment)
                                    .hide(this)
                                    .addToBackStack(null)
                                    .commit();
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    if (isAdded()) {
                        Toast.makeText(getContext(), "게시물을 불러오지 못했습니다.", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    @Override
    public void onDestroyView() {
        if (mMap != null) {
            mMap.clear();
            mMap.setOnCameraIdleListener(null);
            mMap.setOnMarkerClickListener(null);
        }
        super.onDestroyView();
    }
}
