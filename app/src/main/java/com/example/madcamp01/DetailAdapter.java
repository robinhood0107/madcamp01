package com.example.madcamp01;

import android.content.Context;
import android.view.LayoutInflater;
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
    
    // 첫 번째로 좌표가 있는 일차를 찾고, 해당 일차의 사진들만 반환
    private String findFirstDayWithLocation() {
        if (postItem == null || imageUrls.isEmpty()) {
            return null;
        }
        
        // 일차별로 그룹화하여 첫 번째로 좌표가 있는 일차 찾기
        String firstDayWithLocation = null;
        String currentDay = null;
        
        for (int i = 0; i < imageUrls.size(); i++) {
            String day = postItem.getImageDay(i);
            Double latitude = postItem.getImageLatitude(i);
            Double longitude = postItem.getImageLongitude(i);
            
            // 일차가 바뀌거나 첫 번째 사진일 때
            if (currentDay == null || !currentDay.equals(day)) {
                currentDay = day;
                
                // 이 일차에 좌표가 있는 사진이 있는지 확인
                boolean hasLocationInThisDay = false;
                for (int j = i; j < imageUrls.size(); j++) {
                    String checkDay = postItem.getImageDay(j);
                    if (!checkDay.equals(day)) break;
                    
                    Double lat = postItem.getImageLatitude(j);
                    Double lng = postItem.getImageLongitude(j);
                    if (lat != null && lng != null && lat != 0.0 && lng != 0.0) {
                        hasLocationInThisDay = true;
                        break;
                    }
                }
                
                if (hasLocationInThisDay && firstDayWithLocation == null) {
                    firstDayWithLocation = day;
                    break; // 첫 번째로 좌표가 있는 일차를 찾았으므로 종료
                }
            }
        }
        
        return firstDayWithLocation;
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

            // 날짜 정보만 표시 (일차는 제거)
            // 4열 이상일 때는 날짜 표시하지 않음
            if (postItem != null && imageIndex < postItem.getPhotoCount() && currentSpanCount < 4) {
                Date imageDate = postItem.getImageDate(imageIndex);
                
                // 날짜 표시 (년도 포함)
                if (imageDate != null) {
                    SimpleDateFormat sdf = new SimpleDateFormat("yy년 MM월 dd일", Locale.getDefault());
                    imageHolder.tvDate.setText(sdf.format(imageDate));
                    imageHolder.tvDate.setVisibility(View.VISIBLE);
                } else {
                    imageHolder.tvDate.setVisibility(View.GONE);
                }
            } else {
                imageHolder.tvDate.setVisibility(View.GONE);
            }

            // 클릭 리스너 설정 (실제 이미지 인덱스 사용)
            final int finalImageIndex = imageIndex;
            imageHolder.itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(finalImageIndex);
                }
            });
        } else if (viewType == VIEW_TYPE_DAY_HEADER) {
            // 일차 헤더 표시
            String dayNumber = getDayNumberForHeaderPosition(position);
            DayHeaderViewHolder headerHolder = (DayHeaderViewHolder) holder;
            if (dayNumber != null && !dayNumber.isEmpty()) {
                headerHolder.tvDayHeader.setText(dayNumber + "일차");
            } else {
                headerHolder.tvDayHeader.setText("1일차");
            }
        }
        // 푸터는 데이터 바인딩이 필요 없습니다.
    }
    
    // position에 해당하는 실제 이미지 인덱스 반환
    private int getImageIndexForPosition(int position) {
        if (postItem == null || imageUrls.isEmpty()) {
            return position;
        }
        
        int imageIndex = 0;
        int currentPosition = 0;
        String previousDay = null;
        
        for (int i = 0; i < imageUrls.size(); i++) {
            String currentDay = postItem.getImageDay(i);
            
            // 첫 번째 이미지이거나 일차가 바뀌면 헤더 추가
            if (i == 0 || (previousDay != null && !previousDay.equals(currentDay))) {
                if (position == currentPosition) {
                    return -1; // 헤더 위치
                }
                currentPosition++;
            }
            
            if (position == currentPosition) {
                return imageIndex;
            }
            
            previousDay = currentDay;
            imageIndex++;
            currentPosition++;
        }
        
        return -1;
    }
    
    // 헤더 position에 해당하는 일차 반환
    private String getDayNumberForHeaderPosition(int position) {
        if (postItem == null || imageUrls.isEmpty()) {
            return "1";
        }
        
        int currentPosition = 0;
        String previousDay = null;
        
        for (int i = 0; i < imageUrls.size(); i++) {
            String currentDay = postItem.getImageDay(i);
            
            // 첫 번째 이미지이거나 일차가 바뀌면 헤더
            if (i == 0 || (previousDay != null && !previousDay.equals(currentDay))) {
                if (position == currentPosition) {
                    return currentDay;
                }
                currentPosition++;
            }
            
            if (position == currentPosition) {
                return null; // 이미지 위치
            }
            
            previousDay = currentDay;
            currentPosition++;
        }
        
        return null;
    }

    // --- 뷰홀더 --- //
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
        boolean mapInitialized = false;
        ClusterManager<PhotoItem> clusterManager;
        GoogleMap googleMap;
        
        public FooterViewHolder(@NonNull View itemView) {
            super(itemView);
            mapView = itemView.findViewById(R.id.map_footer);
        }
    }
    
    private void setupMapInFooter(FooterViewHolder holder) {
        if (postItem == null || holder.mapView == null) return;
        
        // 좌표가 있는 사진이 하나라도 있는지 확인
        if (!hasAnyLocationData()) {
            // 좌표가 있는 사진이 없으면 footer 숨김
            holder.itemView.setVisibility(View.GONE);
            return;
        }
        
        holder.itemView.setVisibility(View.VISIBLE);
        
        // MapView 초기화 (한 번만)
        if (!holder.mapInitialized) {
            holder.mapView.onCreate(null);
            holder.mapView.onResume();
            holder.mapInitialized = true;
        }
        
        holder.mapView.getMapAsync(new OnMapReadyCallback() {
            @Override
            public void onMapReady(GoogleMap googleMap) {
                holder.googleMap = googleMap;
                googleMap.getUiSettings().setZoomControlsEnabled(true); // + - 버튼 활성화
                googleMap.getUiSettings().setScrollGesturesEnabled(true);
                googleMap.getUiSettings().setZoomGesturesEnabled(true);
                googleMap.getUiSettings().setMapToolbarEnabled(false);
                
                // ClusterManager 초기화
                holder.clusterManager = new ClusterManager<>(context, googleMap);
                PhotoRenderer renderer = new PhotoRenderer(context, googleMap, holder.clusterManager);
                holder.clusterManager.setRenderer(renderer);
                
                googleMap.setOnCameraIdleListener(holder.clusterManager);
                googleMap.setOnMarkerClickListener(holder.clusterManager);
                
                // 해당 게시물의 모든 사진 위치를 썸네일 마커로 표시
                List<LatLng> allLocations = new ArrayList<>();
                List<LatLng> firstDayLocations = new ArrayList<>(); // 1일차 위치만
                int photoCount = postItem.getPhotoCount();
                String postId = postItem.getDocumentId();
                if (postId == null || postId.isEmpty()) {
                    postId = "unknown";
                }
                
                for (int i = 0; i < photoCount; i++) {
                    Double latitude = postItem.getImageLatitude(i);
                    Double longitude = postItem.getImageLongitude(i);
                    
                    if (latitude != null && longitude != null && 
                        latitude != 0.0 && longitude != 0.0) {
                        LatLng location = new LatLng(latitude, longitude);
                        allLocations.add(location);
                        
                        // 일차 정보 가져오기
                        String day = postItem.getImageDay(i);
                        if (day == null || day.isEmpty()) {
                            day = "1";
                        }
                        
                        // 1일차 위치 저장
                        if ("1".equals(day)) {
                            firstDayLocations.add(location);
                        }
                        
                        // 썸네일 URL 가져오기 (썸네일이 없으면 원본 이미지 사용)
                        String imageUrl = postItem.getImageThumbnailUrl(i);
                        if (imageUrl == null || imageUrl.isEmpty()) {
                            imageUrl = postItem.getImageUrl(i);
                        }
                        if (imageUrl == null || imageUrl.isEmpty()) {
                            continue; // 이미지 URL이 없으면 스킵
                        }
                        
                        // PhotoItem 생성하여 ClusterManager에 추가
                        PhotoItem photoItem = new PhotoItem(latitude, longitude, imageUrl, postId);
                        holder.clusterManager.addItem(photoItem);
                    }
                }
                
                // 클러스터링 적용
                holder.clusterManager.cluster();
                
                // 카메라 조정: 1일차 위치를 중심으로, 모든 마커가 보이도록
                if (!allLocations.isEmpty()) {
                    LatLngBounds.Builder allBoundsBuilder = new LatLngBounds.Builder();
                    for (LatLng location : allLocations) {
                        allBoundsBuilder.include(location);
                    }
                    LatLngBounds allBounds = allBoundsBuilder.build();
                    
                    // 1일차 위치가 있으면 1일차 중심으로, 없으면 전체 중심으로
                    LatLng center;
                    if (!firstDayLocations.isEmpty()) {
                        // 1일차 위치의 중심점 계산
                        LatLngBounds.Builder firstDayBuilder = new LatLngBounds.Builder();
                        for (LatLng location : firstDayLocations) {
                            firstDayBuilder.include(location);
                        }
                        LatLngBounds firstDayBounds = firstDayBuilder.build();
                        center = firstDayBounds.getCenter();
                    } else {
                        // 1일차 위치가 없으면 전체 중심
                        center = allBounds.getCenter();
                    }
                    
                    // bounds의 너비와 높이를 계산하여 적당한 줌 레벨 결정
                    double latSpan = allBounds.northeast.latitude - allBounds.southwest.latitude;
                    double lngSpan = allBounds.northeast.longitude - allBounds.southwest.longitude;
                    double maxSpan = Math.max(latSpan, lngSpan);
                    
                    // 줌 레벨 계산 (너무 확대되지 않도록 최소 줌 레벨 제한)
                    float zoomLevel;
                    if (maxSpan < 0.01) {
                        // 매우 가까운 위치들 - 최대 13 줌
                        zoomLevel = 13.0f;
                    } else if (maxSpan < 0.1) {
                        // 가까운 위치들 - 11-12 줌
                        zoomLevel = 11.5f;
                    } else {
                        // 멀리 떨어진 위치들 - 10 줌
                        zoomLevel = 10.0f;
                    }
                    
                    // 1일차 중심으로 카메라 이동 (모든 마커가 보이도록 줌 레벨 조정)
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(center, zoomLevel));
                } else {
                    // 위치 정보가 없으면 기본 위치로 설정
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(new LatLng(37.5665, 126.9780), 10f));
                }
            }
        });
    }

    // --- 클릭 리스너 인터페이스 --- //
    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }
}
