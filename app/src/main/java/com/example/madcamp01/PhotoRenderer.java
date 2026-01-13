package com.example.madcamp01;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Shader;
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
    private final boolean disableClustering;
    
    protected boolean shouldRenderAsCluster(@NonNull Cluster<PhotoItem> cluster) {
        // 클러스터링이 비활성화된 경우 항상 false 반환 (개별 마커만 표시)
        if (disableClustering) {
            return false;
        }
        return cluster.getSize() > 1;
    }
    
    public PhotoRenderer(Context context, GoogleMap map, ClusterManager<PhotoItem> clusterManager) {
        this(context, map, clusterManager, false);
    }
    
    public PhotoRenderer(Context context, GoogleMap map, ClusterManager<PhotoItem> clusterManager, boolean disableClustering) {
        super(context, map, clusterManager);
        this.context = context;
        this.disableClustering = disableClustering;
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
        if (url == null || url.isEmpty()) {
            return;
        }
        
        // Glide는 기본적으로 EXIF orientation을 자동으로 처리합니다
        // asBitmap()을 사용할 때도 EXIF orientation이 처리되도록 설정
        RequestOptions options = new RequestOptions()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .override(MARKER_SIZE, MARKER_SIZE)
                .centerCrop()
                .error(R.drawable.ic_placeholder); // 에러 시 플레이스홀더

        Glide.with(context)
                .asBitmap()
                .load(url)
                .apply(options)
                .thumbnail(0.25f)   // 저해상도 썸네일을 먼저 표시
                .dontAnimate()
                .into(new CustomTarget<Bitmap>() {
                    @Override
                    public void onResourceReady(@NonNull Bitmap resource, @Nullable Transition<? super Bitmap> transition) {
                        if (resource == null || resource.isRecycled()) {
                            return;
                        }
                        
                        try {
                            // 썸네일은 이미 EXIF orientation 보정이 되어 있으므로 바로 원형으로 변환
                            Bitmap circularBitmap = makeCircularBitmap(resource);
                            if (circularBitmap == null) {
                                return;
                            }
                            
                            Bitmap finalBitmap = circularBitmap;
                            // 2개 이상일 때만 배지 추가
                            if (count > 1) {
                                finalBitmap = addBadgeToBitmap(circularBitmap, count);
                                if (finalBitmap != circularBitmap && !circularBitmap.isRecycled()) {
                                    circularBitmap.recycle();
                                }
                            }
                            
                            if (finalBitmap != null && !finalBitmap.isRecycled()) {
                                marker.setIcon(BitmapDescriptorFactory.fromBitmap(finalBitmap));
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    
                    @Override
                    public void onLoadCleared(@Nullable Drawable placeholder) {}
                    
                    @Override
                    public void onLoadFailed(@Nullable Drawable errorDrawable) {
                        // 로드 실패 시 기본 마커 사용
                        super.onLoadFailed(errorDrawable);
                    }
                });
    }
    
    /**
     * 비트맵을 원형으로 변환
     */
    private Bitmap makeCircularBitmap(Bitmap bitmap) {
        if (bitmap == null || bitmap.isRecycled()) {
            return null;
        }
        
        try {
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            int size = Math.min(width, height);
            
            // 정사각형 비트맵 생성
            Bitmap output = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
            if (output == null) {
                return null;
            }
            
            Canvas canvas = new Canvas(output);
            
            // 원형 마스크를 위한 Paint
            Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
            paint.setShader(new BitmapShader(bitmap, 
                    Shader.TileMode.CLAMP, 
                    Shader.TileMode.CLAMP));
            
            // 중앙에서 원형으로 그리기
            float radius = size / 2f;
            float centerX = size / 2f;
            float centerY = size / 2f;
            
            // 비트맵이 정사각형이 아닌 경우 중앙에서 크롭
            if (width != height) {
                float scale = Math.max((float)size / width, (float)size / height);
                canvas.save();
                canvas.scale(scale, scale, centerX, centerY);
            }
            
            canvas.drawCircle(centerX, centerY, radius, paint);
            
            if (width != height) {
                canvas.restore();
            }
            
            return output;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
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
