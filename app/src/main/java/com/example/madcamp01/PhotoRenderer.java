package com.example.madcamp01;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.view.ViewGroup;
import android.widget.ImageView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.google.maps.android.clustering.Cluster;
import com.google.maps.android.clustering.ClusterManager;
import com.google.maps.android.clustering.view.DefaultClusterRenderer;

public class PhotoRenderer extends DefaultClusterRenderer<PhotoItem> {
    private final Context context;
    protected boolean shouldRenderAsCluster(@NonNull Cluster<PhotoItem> cluster) {
        // 마커가 2개 이상 겹치면 무조건 클러스터링(그룹화)을 하되,
        // 나중에 이미지로 덮어씌울 것이므로 true를 반환합니다.
        return cluster.getSize() > 1;
    }
    public PhotoRenderer(Context context, GoogleMap map, ClusterManager<PhotoItem> clusterManager) {
        super(context, map, clusterManager);
        this.context = context;
    }

    // 개별 마커(사진 하나)를 사진으로 표시
    @Override

    protected void onBeforeClusterRendered(@NonNull Cluster<PhotoItem> cluster, @NonNull MarkerOptions markerOptions) {
        // 중요: 여기서 기본 숫자가 그려진 아이콘이 세팅됩니다.
        // 숫자가 보이는 게 싫다면 여기서 투명한 아이콘이나 임시 아이콘을 넣어버릴 수 있습니다.
        // 하지만 Glide가 금방 로드할 것이므로, 여기서는 그냥 두거나 투명 아이콘을 세팅합니다.
    }

    @Override
    protected void onClusterItemRendered(@NonNull PhotoItem item, @NonNull Marker marker) {
        // Glide로 이미지 로드 후 마커 아이콘 설정
        loadMarkerImage(item.getPhotoUrl(), marker);
    }

    // 겹친 그룹(클러스터)을 대표 사진 하나로 표시
    @Override
    protected void onClusterRendered(@NonNull Cluster<PhotoItem> cluster, @NonNull Marker marker) {
        PhotoItem topItem = cluster.getItems().iterator().next();
        loadMarkerImage(topItem.getPhotoUrl(), marker);
    }

    private void loadMarkerImage(String url, Marker marker) {
        Glide.with(context).asBitmap().load(url).circleCrop().override(150, 150)
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                        marker.setIcon(BitmapDescriptorFactory.fromBitmap(resource));
                    }
                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {}
                });
    }
}
