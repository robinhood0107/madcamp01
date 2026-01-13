package com.example.madcamp01;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.request.RequestOptions;
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
    private static final int MARKER_SIZE = 120;
    private static final float BADGE_SIZE_RATIO = 0.35f;
    private static final int MAX_BADGE_COUNT = 9;
    
    private final Context context;
    
    protected boolean shouldRenderAsCluster(@NonNull Cluster<PhotoItem> cluster) {
        return cluster.getSize() > 1;
    }
    public PhotoRenderer(Context context, GoogleMap map, ClusterManager<PhotoItem> clusterManager) {
        super(context, map, clusterManager);
        this.context = context;
    }

    // 개별 마커(사진 하나)를 사진으로 표시
    @Override

    protected void onBeforeClusterRendered(@NonNull Cluster<PhotoItem> cluster, @NonNull MarkerOptions markerOptions) {
        Bitmap transparentBitmap = Bitmap.createBitmap(MARKER_SIZE, MARKER_SIZE, Bitmap.Config.ARGB_8888);
        transparentBitmap.eraseColor(0x00000000);
        markerOptions.icon(BitmapDescriptorFactory.fromBitmap(transparentBitmap));
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
        int clusterSize = cluster.getSize();
        loadMarkerImageWithBadge(topItem.getPhotoUrl(), marker, clusterSize);
    }

    private void loadMarkerImage(String url, Marker marker) {
        loadMarkerImageWithBadge(url, marker, 1);
    }

    private void loadMarkerImageWithBadge(String url, Marker marker, int count) {
        RequestOptions options = new RequestOptions()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .override(MARKER_SIZE, MARKER_SIZE)
                .circleCrop();

        Glide.with(context)
                .asBitmap()
                .load(url)
                .apply(options)
                .thumbnail(0.25f)   // 저해상도 썸네일을 먼저 표시
                .dontAnimate()
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                        Bitmap finalBitmap = resource;
                        // 2개 이상일 때만 배지 추가
                        if (count > 1) {
                            finalBitmap = addBadgeToBitmap(resource, count);
                        }
                        marker.setIcon(BitmapDescriptorFactory.fromBitmap(finalBitmap));
                    }
                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {}
                });
    }

    /**
     * 썸네일 이미지 우상단에 개수 배지 추가
     */
    private Bitmap addBadgeToBitmap(Bitmap original, int count) {
        int size = original.getWidth();
        Bitmap result = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(result);
        
        canvas.drawBitmap(original, 0, 0, null);
        
        int badgeSize = (int) (size * BADGE_SIZE_RATIO);
        int badgeX = size - badgeSize;
        int badgeY = 0;
        int centerX = badgeX + badgeSize / 2;
        int centerY = badgeY + badgeSize / 2;
        int radius = badgeSize / 2;
        
        // 배지 원 그리기
        Paint badgePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        badgePaint.setColor(0xFF3B82F6);
        canvas.drawCircle(centerX, centerY, radius, badgePaint);
        
        // 흰색 테두리
        Paint borderPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        borderPaint.setColor(0xFFFFFFFF);
        borderPaint.setStyle(Paint.Style.STROKE);
        borderPaint.setStrokeWidth(size * 0.02f);
        canvas.drawCircle(centerX, centerY, radius - size * 0.01f, borderPaint);
        
        // 숫자 텍스트 그리기
        Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(0xFFFFFFFF);
        textPaint.setTextSize(badgeSize * 0.5f);
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);
        
        String countText = count > MAX_BADGE_COUNT ? MAX_BADGE_COUNT + "+" : String.valueOf(count);
        Rect textBounds = new Rect();
        textPaint.getTextBounds(countText, 0, countText.length(), textBounds);
        float textY = centerY + textBounds.height() / 2;
        canvas.drawText(countText, centerX, textY, textPaint);
        
        return result;
    }
}
