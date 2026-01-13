package com.example.madcamp01;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;
import com.google.maps.android.clustering.ClusterManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class DetailAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    // 뷰 타입 상수 정의 (일반 이미지 vs 일차 헤더 vs 마지막 푸터)
    public static final int VIEW_TYPE_IMAGE = 0;
    public static final int VIEW_TYPE_DAY_HEADER = 1;
    public static final int VIEW_TYPE_FOOTER = 2;

    private final Context context;
    private final List<String> imageUrls;
    private PostItem postItem;
    private OnItemClickListener listener;
    private int currentSpanCount = 2; // 기본값 2열

    public DetailAdapter(Context context, List<String> imageUrls) {
        this.context = context;
        this.imageUrls = imageUrls;
    }

    public void setPostItem(PostItem postItem) {
        this.postItem = postItem;
    }

    public void setSpanCount(int spanCount) {
        this.currentSpanCount = spanCount;
        notifyDataSetChanged(); // 데이터 변경 알림으로 날짜 표시 업데이트
    }

    @Override
    public int getItemViewType(int position) {
        if (postItem == null || imageUrls.isEmpty()) {
            boolean hasFooter = hasAnyLocationData();
            if (hasFooter && position == imageUrls.size()) {
                return VIEW_TYPE_FOOTER;
            }
            return VIEW_TYPE_IMAGE;
        }
        
        int currentPosition = 0;
        String previousDay = null;
        boolean hasFooter = hasAnyLocationData();
        
        for (int i = 0; i <= imageUrls.size(); i++) {
            if (i == imageUrls.size()) {
                // 마지막 푸터 (좌표가 있을 때만)
                if (hasFooter && position == currentPosition) {
                    return VIEW_TYPE_FOOTER;
                }
                break;
            }
            
            String currentDay = postItem.getImageDay(i);
            
            // 첫 번째 이미지이거나 일차가 바뀌면 헤더 추가
            if (i == 0 || (previousDay != null && !previousDay.equals(currentDay))) {
                if (position == currentPosition) {
                    return VIEW_TYPE_DAY_HEADER;
                }
                currentPosition++;
            }
            
            if (position == currentPosition) {
                return VIEW_TYPE_IMAGE;
            }
            
            previousDay = currentDay;
            currentPosition++;
        }
        
        return VIEW_TYPE_IMAGE;
    }

    @Override
    public int getItemCount() {
        if (postItem == null || imageUrls.isEmpty()) {
            return imageUrls.size() + (hasAnyLocationData() ? 1 : 0); // 이미지 + 푸터(좌표가 있을 때만)
        }
        
        // 일차별 헤더 개수 계산 (첫 번째 일차 포함)
        int headerCount = 1; // 첫 번째 일차 헤더
        String previousDay = null;
        for (int i = 0; i < imageUrls.size(); i++) {
            String currentDay = postItem.getImageDay(i);
            if (previousDay != null && !previousDay.equals(currentDay)) {
                headerCount++;
            }
            previousDay = currentDay;
        }
        
        // 좌표가 있는 일차가 있으면 footer 추가
        int footerCount = hasAnyLocationData() ? 1 : 0;
        return imageUrls.size() + headerCount + footerCount; // 이미지 + 헤더 + 푸터(조건부)
    }
    
    // 좌표가 있는 사진이 하나라도 있는지 확인
    private boolean hasAnyLocationData() {
        if (postItem == null || imageUrls.isEmpty()) {
            return false;
        }
        
        for (int i = 0; i < imageUrls.size(); i++) {
            Double latitude = postItem.getImageLatitude(i);
            Double longitude = postItem.getImageLongitude(i);
            if (latitude != null && longitude != null && 
                latitude != 0.0 && longitude != 0.0) {
                return true;
            }
        }
        return false;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == VIEW_TYPE_IMAGE) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_detail_image, parent, false);
            return new ImageViewHolder(view);
        } else if (viewType == VIEW_TYPE_DAY_HEADER) {
            View view = LayoutInflater.from(context).inflate(R.layout.item_detail_day_header, parent, false);
            return new DayHeaderViewHolder(view);
        } else {
            View view = LayoutInflater.from(context).inflate(R.layout.item_detail_footer, parent, false);
            return new FooterViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        int viewType = holder.getItemViewType();
        
        if (viewType == VIEW_TYPE_FOOTER) {
            // Footer에 지도 표시
            FooterViewHolder footerHolder = (FooterViewHolder) holder;
            if (postItem != null) {
                setupMapInFooter(footerHolder);
            }
        } else if (viewType == VIEW_TYPE_IMAGE) {
            // 실제 이미지 인덱스 계산
            int imageIndex = getImageIndexForPosition(position);
            if (imageIndex < 0 || imageIndex >= imageUrls.size()) {
                return;
            }
            
            String imageUrl = imageUrls.get(imageIndex);
            ImageViewHolder imageHolder = (ImageViewHolder) holder;

            Glide.with(context)
                    .load(imageUrl)
                    .placeholder(R.drawable.ic_launcher_background)
                    .centerCrop()
                    .into(imageHolder.ivDetailImage);

            // 날짜 정보만 표시
            if (postItem != null && imageIndex < postItem.getPhotoCount() && currentSpanCount < 4) {
                Date imageDate = postItem.getImageDate(imageIndex);
                if (imageDate != null) {
                    SimpleDateFormat sdf = new SimpleDateFormat("yy.MM.dd", Locale.getDefault());
                    imageHolder.tvDate.setText(sdf.format(imageDate));
                    imageHolder.tvDate.setVisibility(View.VISIBLE);
                } else {
                    imageHolder.tvDate.setVisibility(View.GONE);
                }
            } else {
                imageHolder.tvDate.setVisibility(View.GONE);
            }

            // 클릭 리스너 설정
            final int finalImageIndex = imageIndex;
            imageHolder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(finalImageIndex);
                }
            });
        } else if (viewType == VIEW_TYPE_DAY_HEADER) {
            String dayNumber = getDayNumberForHeaderPosition(position);
            DayHeaderViewHolder headerHolder = (DayHeaderViewHolder) holder;
            if (dayNumber != null && !dayNumber.isEmpty()) {
                headerHolder.tvDayHeader.setText(dayNumber + "일차");
            } else {
                headerHolder.tvDayHeader.setText("1일차");
            }
        }
    }
    
    private int getImageIndexForPosition(int position) {
        if (postItem == null || imageUrls.isEmpty()) {
            return position;
        }
        int imageIndex = 0;
        int currentPosition = 0;
        String previousDay = null;
        for (int i = 0; i < imageUrls.size(); i++) {
            String currentDay = postItem.getImageDay(i);
            if (i == 0 || (previousDay != null && !previousDay.equals(currentDay))) {
                if (position == currentPosition) return -1;
                currentPosition++;
            }
            if (position == currentPosition) return imageIndex;
            previousDay = currentDay;
            imageIndex++;
            currentPosition++;
        }
        return -1;
    }
    
    private String getDayNumberForHeaderPosition(int position) {
        if (postItem == null || imageUrls.isEmpty()) return "1";
        int currentPosition = 0;
        String previousDay = null;
        for (int i = 0; i < imageUrls.size(); i++) {
            String currentDay = postItem.getImageDay(i);
            if (i == 0 || (previousDay != null && !previousDay.equals(currentDay))) {
                if (position == currentPosition) return currentDay;
                currentPosition++;
            }
            if (position == currentPosition) return null;
            previousDay = currentDay;
            currentPosition++;
        }
        return null;
    }

    static class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView ivDetailImage;
        TextView tvDate;
        public ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            ivDetailImage = itemView.findViewById(R.id.ivDetailImage);
            tvDate = itemView.findViewById(R.id.tvDate);
        }
    }

    static class DayHeaderViewHolder extends RecyclerView.ViewHolder {
        TextView tvDayHeader;
        public DayHeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvDayHeader = itemView.findViewById(R.id.tvDayHeader);
        }
    }

    static class FooterViewHolder extends RecyclerView.ViewHolder {
        MapView mapView;
        View mapOverlay;
        boolean mapInitialized = false;
        ClusterManager<PhotoItem> clusterManager;
        GoogleMap googleMap;
        public FooterViewHolder(@NonNull View itemView) {
            super(itemView);
            mapView = itemView.findViewById(R.id.map_footer);
            mapOverlay = itemView.findViewById(R.id.map_overlay);
        }
    }
    
    @SuppressLint("ClickableViewAccessibility")
    private void setupMapInFooter(FooterViewHolder holder) {
        if (postItem == null || holder.mapView == null) return;
        if (!hasAnyLocationData()) {
            holder.itemView.setVisibility(View.GONE);
            return;
        }
        holder.itemView.setVisibility(View.VISIBLE);

        // 지도 내부 터치 시 RecyclerView 스크롤 방지
        if (holder.mapOverlay != null) {
            holder.mapOverlay.setOnTouchListener((v, event) -> {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                    case MotionEvent.ACTION_MOVE:
                        v.getParent().requestDisallowInterceptTouchEvent(true);
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        v.getParent().requestDisallowInterceptTouchEvent(false);
                        break;
                }
                return false; // 이벤트를 소비하지 않아 지도가 받을 수 있게 함
            });
        }

        if (!holder.mapInitialized) {
            holder.mapView.onCreate(null);
            holder.mapView.onResume();
            holder.mapInitialized = true;
        }
        
        holder.mapView.getMapAsync(googleMap -> {
            holder.googleMap = googleMap;
            googleMap.getUiSettings().setZoomControlsEnabled(true);
            googleMap.getUiSettings().setScrollGesturesEnabled(true);
            googleMap.getUiSettings().setZoomGesturesEnabled(true);
            googleMap.getUiSettings().setMapToolbarEnabled(false);
            
            holder.clusterManager = new ClusterManager<>(context, googleMap);
            PhotoRenderer renderer = new PhotoRenderer(context, googleMap, holder.clusterManager);
            holder.clusterManager.setRenderer(renderer);
            
            googleMap.setOnCameraIdleListener(holder.clusterManager);
            googleMap.setOnMarkerClickListener(holder.clusterManager);
            
            List<LatLng> allLocations = new ArrayList<>();
            List<LatLng> firstDayLocations = new ArrayList<>();
            int photoCount = postItem.getPhotoCount();
            String postId = postItem.getDocumentId() != null ? postItem.getDocumentId() : "unknown";
            
            for (int i = 0; i < photoCount; i++) {
                Double latitude = postItem.getImageLatitude(i);
                Double longitude = postItem.getImageLongitude(i);
                if (latitude != null && longitude != null && latitude != 0.0 && longitude != 0.0) {
                    LatLng location = new LatLng(latitude, longitude);
                    allLocations.add(location);
                    String day = postItem.getImageDay(i) != null ? postItem.getImageDay(i) : "1";
                    if ("1".equals(day)) firstDayLocations.add(location);
                    String imageUrl = postItem.getImageThumbnailUrl(i) != null ? postItem.getImageThumbnailUrl(i) : postItem.getImageUrl(i);
                    if (imageUrl != null && !imageUrl.isEmpty()) {
                        PhotoItem photoItem = new PhotoItem(latitude, longitude, imageUrl, postId);
                        holder.clusterManager.addItem(photoItem);
                    }
                }
            }
            holder.clusterManager.cluster();
            
            if (!allLocations.isEmpty()) {
                LatLngBounds.Builder allBoundsBuilder = new LatLngBounds.Builder();
                for (LatLng location : allLocations) allBoundsBuilder.include(location);
                LatLngBounds allBounds = allBoundsBuilder.build();
                LatLng center;
                if (!firstDayLocations.isEmpty()) {
                    LatLngBounds.Builder firstDayBuilder = new LatLngBounds.Builder();
                    for (LatLng location : firstDayLocations) firstDayBuilder.include(location);
                    center = firstDayBuilder.build().getCenter();
                } else {
                    center = allBounds.getCenter();
                }
                double maxSpan = Math.max(allBounds.northeast.latitude - allBounds.southwest.latitude, allBounds.northeast.longitude - allBounds.southwest.longitude);
                float zoomLevel = maxSpan < 0.01 ? 13.0f : (maxSpan < 0.1 ? 11.5f : 10.0f);
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(center, zoomLevel));
            } else {
                googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(37.5665, 126.9780), 10f));
            }
        });
    }

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }
}