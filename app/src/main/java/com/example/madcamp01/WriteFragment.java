package com.example.madcamp01;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.ExifInterface;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory;
import com.google.firebase.firestore.FieldValue; // 시간 저장용
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage; // 스토리지용
import com.google.firebase.storage.StorageReference; // 스토리지 참조용

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WriteFragment extends Fragment {

    // 1. 필요한 변수들 선언
    private EditText editTripTitle;       // 여행 제목 입력창
    private RecyclerView recyclerView;    // 사진 리스트 뷰
    private PhotoAdapter photoAdapter;    // 사진 리스트 관리자(어댑터)
    private List<PhotoInfo> photoInfoList = new ArrayList<>(); // 사진 정보 리스트
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private FirebaseStorage storage = FirebaseStorage.getInstance();
    // 갤러리 실행 도구
    private ActivityResultLauncher<String> getMultipleContents;
    private FirebaseAuth auth; // Firebase Authentication 인스턴스 추가
    private androidx.appcompat.app.AlertDialog progressDialog; // 로딩바용
    private com.google.android.material.materialswitch.MaterialSwitch switchIsPublic; // 공개 비공개 버튼
    private android.widget.TextView textTravelPeriod; // 여행 기간 표시 텍스트
    private String editPostId = null;// 수정 중인지 체크
    private boolean isSaving = false; // 저장 프로세스 중인지 확인
    
    // 여행 정보
    private Date startDate;  // 여행 시작일
    private int travelDays;  // 여행 일수
    private ExecutorService executorService = Executors.newFixedThreadPool(4); // EXIF 추출용 스레드 풀
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 수정 모드 체크
        if (getArguments() != null) {
            editPostId = getArguments().getString("postId");
            // 신규 작성 시 여행 정보 가져오기
            if (editPostId == null) {
                travelDays = getArguments().getInt("travelDays", 0);
                long startDateLong = getArguments().getLong("startDate", 0);
                if (startDateLong > 0) {
                    startDate = new Date(startDateLong);
                }
            }
        }

        // 2. 사진 선택 결과 처리기 등록
        getMultipleContents = registerForActivityResult(
                new ActivityResultContracts.GetMultipleContents(),
                uris -> {
                    if (uris != null && !uris.isEmpty()) {
                        // 사진 추가 시 EXIF 추출 및 일차 분류
                        processNewPhotos(uris);
                    }
                }
        );
        // Firebase Authentication 인스턴스 초기화
        auth = FirebaseAuth.getInstance();
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        if (executorService != null) {
            executorService.shutdown();
        }
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // 3. XML 레이아웃 연결
        View view = inflater.inflate(R.layout.fragment_write, container, false);

        // 4. XML 위젯들을 자바 변수에 연결
        editTripTitle = view.findViewById(R.id.edit_trip_title);
        recyclerView = view.findViewById(R.id.recycler_photos);
        Button btnAddPhoto = view.findViewById(R.id.btn_add_photo);
        Button btnSave = view.findViewById(R.id.btn_save);
        Button btnChangeTravelInfo = view.findViewById(R.id.btn_change_travel_info);
        switchIsPublic = view.findViewById(R.id.switch_is_public);
        textTravelPeriod = view.findViewById(R.id.text_travel_period);

        // 5. 리사이클러뷰(사진 리스트) 설정
        // 일차별로 그룹화되어 있으므로 LinearLayoutManager 사용
        androidx.recyclerview.widget.LinearLayoutManager layoutManager = 
            new androidx.recyclerview.widget.LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);

        // 어댑터 연결 (보관함과 화면을 연결)
        photoAdapter = new PhotoAdapter(photoInfoList, getContext());
        recyclerView.setAdapter(photoAdapter);
        // 수정 모드 시 기존 게시물 정보 가져오기
        if (editPostId != null) {
            editTripTitle.setText(getArguments().getString("title"));
            switchIsPublic.setChecked(getArguments().getBoolean("isPublic"));
            btnSave.setText("수정 완료"); //저장하기 버튼을 수정하기로 바꿈
            btnChangeTravelInfo.setVisibility(View.VISIBLE); // 여행 일짜 변경 버튼 표시
            
            // PostItem 전체를 받았는지 확인
            PostItem postItem = getArguments().getParcelable("postItem");
            if (postItem != null) {
                // PostItem에서 모든 정보 가져오기
                List<String> images = postItem.getImages();
                List<String> imageDays = postItem.getImageDays();
                List<String> imageThumbnails = postItem.getImageThumbnails();
                List<Date> imageDates = postItem.getImageDates(); // 정확한 촬영 시각
                List<Double> imageLatitudes = postItem.getImageLatitudes();
                List<Double> imageLongitudes = postItem.getImageLongitudes();
                
                if (images != null) {
                    for (int i = 0; i < images.size(); i++) {
                        PhotoInfo photoInfo = new PhotoInfo(Uri.parse(images.get(i)));
                        photoInfo.setImageUrl(images.get(i));
                        if (imageDays != null && i < imageDays.size()) {
                            photoInfo.setDayNumber(imageDays.get(i));
                        }
                        if (imageThumbnails != null && i < imageThumbnails.size()) {
                            photoInfo.setThumbnailUrl(imageThumbnails.get(i));
                        }
                        // 정확한 촬영 시각 설정
                        if (imageDates != null && i < imageDates.size() && imageDates.get(i) != null) {
                            photoInfo.setPhotoDate(imageDates.get(i));
                        }
                        if (imageLatitudes != null && i < imageLatitudes.size() && imageLatitudes.get(i) != null) {
                            photoInfo.setLatitude(imageLatitudes.get(i));
                        }
                        if (imageLongitudes != null && i < imageLongitudes.size() && imageLongitudes.get(i) != null) {
                            photoInfo.setLongitude(imageLongitudes.get(i));
                        }
                        photoInfoList.add(photoInfo);
                    }
                }
                
                // 여행 정보 설정
                if (postItem.getStartDate() != null) {
                    startDate = postItem.getStartDate();
                }
                if (postItem.getTravelDays() > 0) {
                    travelDays = postItem.getTravelDays();
                }
            } else {
                // 기존 방식 호환성 (이미지 URL만 있는 경우)
                ArrayList<String> imageUrls = getArguments().getStringArrayList("images");
                if (imageUrls != null) {
                    for (String url : imageUrls) {
                        PhotoInfo photoInfo = new PhotoInfo(Uri.parse(url));
                        photoInfo.setImageUrl(url);
                        photoInfo.setDayNumber("1"); // 기본값
                        photoInfoList.add(photoInfo);
                    }
                }
            }
            
            sortPhotosByDay();
            photoAdapter.updateAdapterItems();
            
            // 여행 기간 텍스트 업데이트
            updateTravelPeriodText();
        } else {
            // 등록 모드: 여행 정보가 설정되어 있으면 버튼 표시
            if (startDate != null && travelDays > 0) {
                btnChangeTravelInfo.setVisibility(View.VISIBLE);
            }
        }
        
        // 여행 기간 텍스트 업데이트
        updateTravelPeriodText();
        
        // 6. 버튼 클릭 시 갤러리 실행
        btnAddPhoto.setOnClickListener(v -> {
            getMultipleContents.launch("image/*"); // 이미지 파일만 선택하도록 실행
        });

        // 여행 일짜 변경 버튼 클릭 시
        btnChangeTravelInfo.setOnClickListener(v -> {
            showTravelInfoDialog();
        });

        // 7. 저장 버튼 클릭 시 파이어베이스 업로드 함수 실행
        btnSave.setOnClickListener(v -> {
            // 이전에 만들어둔 파이어베이스 업로드 함수 호출
            uploadToFirebase();
        });

        //삭제 및 변경 기능 구현
        // 1. 삭제 리스너 연결
        photoAdapter.setOnPhotoDeleteListener(position -> {
            photoInfoList.remove(position);
            photoAdapter.updateAdapterItems();
        });

        // 2. 순서 변경(Drag & Drop) 기능 추가
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN | ItemTouchHelper.START | ItemTouchHelper.END, 0) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                // 데이터 리스트 내에서 위치 변경
                int fromPos = viewHolder.getAdapterPosition();
                int toPos = target.getAdapterPosition();

                java.util.Collections.swap(photoInfoList, fromPos, toPos);
                sortPhotosByDay();
                photoAdapter.updateAdapterItems();
                return true;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                // 스와이프 삭제는 사용하지 않으므로 비워둠
            }
        });

        // 3. 리사이클러뷰에 연결
        itemTouchHelper.attachToRecyclerView(recyclerView);
        return view;
    }
    /**
     * 새로 추가된 사진들을 처리 (EXIF 추출 및 일차 분류)
     */
    private void processNewPhotos(List<Uri> uris) {
        // 시작일 검증 (등록/수정 모두 필요)
        if (startDate == null) {
            Toast.makeText(getContext(), "여행 시작일이 설정되지 않았습니다.", Toast.LENGTH_SHORT).show();
            return;
        }
        
        showProgressDialog();
        
        executorService.execute(() -> {
            List<PhotoInfo> newPhotoInfos = new ArrayList<>();
            List<Uri> rejectedPhotos = new ArrayList<>(); // 여행 기간 밖의 사진들
            
            for (Uri uri : uris) {
                PhotoInfo photoInfo = new PhotoInfo(uri);
                // EXIF 데이터 추출
                extractExifData(photoInfo);
                
                // 여행 기간(시작일 ~ 종료일) 밖의 사진은 제외 (등록/수정 모두 적용)
                if (startDate != null && travelDays > 0 && photoInfo.getPhotoDate() != null) {
                    Calendar startCal = Calendar.getInstance();
                    startCal.setTime(startDate);
                    startCal.set(Calendar.HOUR_OF_DAY, 0);
                    startCal.set(Calendar.MINUTE, 0);
                    startCal.set(Calendar.SECOND, 0);
                    startCal.set(Calendar.MILLISECOND, 0);
                    
                    // 여행 종료일 계산 (시작일 + travelDays - 1일, 마지막 날 23:59:59까지)
                    Calendar endCal = Calendar.getInstance();
                    endCal.setTime(startDate);
                    endCal.add(Calendar.DAY_OF_MONTH, travelDays - 1); // travelDays가 3이면 0, 1, 2일차이므로 -1
                    endCal.set(Calendar.HOUR_OF_DAY, 23);
                    endCal.set(Calendar.MINUTE, 59);
                    endCal.set(Calendar.SECOND, 59);
                    endCal.set(Calendar.MILLISECOND, 999);
                    
                    Calendar photoCal = Calendar.getInstance();
                    photoCal.setTime(photoInfo.getPhotoDate());
                    photoCal.set(Calendar.HOUR_OF_DAY, 0);
                    photoCal.set(Calendar.MINUTE, 0);
                    photoCal.set(Calendar.SECOND, 0);
                    photoCal.set(Calendar.MILLISECOND, 0);
                    
                    // 시작일보다 이전이거나 종료일보다 이후면 제외
                    if (photoCal.getTimeInMillis() < startCal.getTimeInMillis() || 
                        photoCal.getTimeInMillis() > endCal.getTimeInMillis()) {
                        rejectedPhotos.add(uri);
                        continue; // 이 사진은 추가하지 않음
                    }
                }
                
                // 일차 계산
                calculateDayNumber(photoInfo);
                newPhotoInfos.add(photoInfo);
            }
            
            // 날짜순으로 정렬
            Collections.sort(newPhotoInfos, new Comparator<PhotoInfo>() {
                @Override
                public int compare(PhotoInfo p1, PhotoInfo p2) {
                    Date d1 = p1.getPhotoDate();
                    Date d2 = p2.getPhotoDate();
                    if (d1 == null && d2 == null) return 0;
                    if (d1 == null) return 1;
                    if (d2 == null) return -1;
                    return d1.compareTo(d2);
                }
            });
            
            // 메인 스레드에서 UI 업데이트
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (!newPhotoInfos.isEmpty()) {
                        photoInfoList.addAll(newPhotoInfos);
                        // 일차별로 정렬하여 어댑터 업데이트
                        sortPhotosByDay();
                        photoAdapter.updateAdapterItems();
                    }
                    
                    // 제외된 사진이 있으면 사용자에게 알림
                    if (!rejectedPhotos.isEmpty()) {
                        Calendar endCal = Calendar.getInstance();
                        endCal.setTime(startDate);
                        endCal.add(Calendar.DAY_OF_MONTH, travelDays - 1);
                        String message = rejectedPhotos.size() + "장의 사진이 여행 기간(" + 
                            formatDate(startDate) + " ~ " + formatDate(endCal.getTime()) + ") 밖에 촬영되어 추가되지 않았습니다.";
                        Toast.makeText(getContext(), message, Toast.LENGTH_LONG).show();
                    }
                    
                    hideProgressDialog();
                });
            }
        });
    }
    
    /**
     * 날짜를 읽기 쉬운 형식으로 포맷
     */
    private String formatDate(Date date) {
        if (date == null) return "";
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy년 MM월 dd일", Locale.getDefault());
        return sdf.format(date);
    }
    
    /**
     * EXIF 데이터 추출 (날짜, 위치 정보)
     */
    private void extractExifData(PhotoInfo photoInfo) {
        try {
            InputStream inputStream = getContext().getContentResolver().openInputStream(photoInfo.getUri());
            if (inputStream == null) return;
            
            ExifInterface exif = new ExifInterface(inputStream);
            inputStream.close();
            
            // 날짜 추출
            String dateTime = exif.getAttribute(ExifInterface.TAG_DATETIME);
            if (dateTime != null) {
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.getDefault());
                    Date photoDate = sdf.parse(dateTime);
                    photoInfo.setPhotoDate(photoDate);
                } catch (ParseException e) {
                    // 날짜 파싱 실패 시 파일 수정 시간 사용
                    photoInfo.setPhotoDate(new Date());
                }
            } else {
                // EXIF 날짜가 없으면 현재 시간 사용
                photoInfo.setPhotoDate(new Date());
            }
            
            // 위치 정보 추출
            float[] latLong = new float[2];
            if (exif.getLatLong(latLong)) {
                photoInfo.setLatitude((double)latLong[0]);
                photoInfo.setLongitude((double)latLong[1]);
            }
            
        } catch (IOException e) {
            e.printStackTrace();
            // 오류 발생 시 기본값 설정
            photoInfo.setPhotoDate(new Date());
        }
    }
    
    /**
     * 사진의 일차 계산 (정확한 촬영 시각은 그대로 유지하고, 일차만 계산)
     */
    private void calculateDayNumber(PhotoInfo photoInfo) {
        // 정확한 촬영 시각은 이미 EXIF에서 추출되어 photoInfo.getPhotoDate()에 저장되어 있음
        // 여기서는 일차만 계산합니다.
        
        if (startDate == null || photoInfo.getPhotoDate() == null) {
            photoInfo.setDayNumber("1"); // 기본값
            return;
        }
        
        // 시작일을 00:00:00으로 설정
        Calendar startCal = Calendar.getInstance();
        startCal.setTime(startDate);
        startCal.set(Calendar.HOUR_OF_DAY, 0);
        startCal.set(Calendar.MINUTE, 0);
        startCal.set(Calendar.SECOND, 0);
        startCal.set(Calendar.MILLISECOND, 0);
        
        // 사진 촬영일을 00:00:00으로 설정 (일차 계산용)
        Calendar photoCal = Calendar.getInstance();
        photoCal.setTime(photoInfo.getPhotoDate());
        photoCal.set(Calendar.HOUR_OF_DAY, 0);
        photoCal.set(Calendar.MINUTE, 0);
        photoCal.set(Calendar.SECOND, 0);
        photoCal.set(Calendar.MILLISECOND, 0);
        
        // 일수 차이 계산
        long diffInMillis = photoCal.getTimeInMillis() - startCal.getTimeInMillis();
        long diffInDays = diffInMillis / (24 * 60 * 60 * 1000);
        
        int dayNumber = (int) diffInDays + 1; // 1일차부터 시작
        
        // 여행 일수 범위 내로 제한
        if (dayNumber < 1) dayNumber = 1;
        if (dayNumber > travelDays) dayNumber = travelDays;
        
        photoInfo.setDayNumber(String.valueOf(dayNumber));
        
        // 참고: photoInfo.getPhotoDate()에는 정확한 촬영 시각(날짜+시간)이 그대로 저장되어 있음
    }
    
    /**
     * 사진을 일차별로 정렬
     */
    private void sortPhotosByDay() {
        Collections.sort(photoInfoList, new Comparator<PhotoInfo>() {
            @Override
            public int compare(PhotoInfo p1, PhotoInfo p2) {
                // 먼저 일차로 정렬
                int dayCompare = p1.getDayNumber().compareTo(p2.getDayNumber());
                if (dayCompare != 0) return dayCompare;
                
                // 같은 일차면 시간순으로 정렬
                Date d1 = p1.getPhotoDate();
                Date d2 = p2.getPhotoDate();
                if (d1 == null && d2 == null) return 0;
                if (d1 == null) return 1;
                if (d2 == null) return -1;
                return d1.compareTo(d2);
            }
        });
    }

    // [업로드 함수]
    private void uploadToFirebase() {
        String title = editTripTitle.getText().toString();
        boolean isPublic = switchIsPublic.isChecked();
        // 제목이 비었거나 사진을 선택 안했으면 중단
        if (title.isEmpty() || photoInfoList.isEmpty()) {
            Toast.makeText(getContext(), "제목과 사진을 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 시작일자 확인 (신규 작성 시)
        if (editPostId == null && startDate == null) {
            Toast.makeText(getContext(), "여행 정보가 없습니다. 다시 시도해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        isSaving = true;
        showProgressDialog(); //로딩바 띄우기
        
        // 일차별로 정렬
        sortPhotosByDay();
        
        // 사진들을 하나씩 Storage에 먼저 올리고, 그 주소(URL)를 리스트에 담습니다.
        List<PhotoInfo> photosToUpload = new ArrayList<>();
        List<PhotoInfo> photosAlreadyUploaded = new ArrayList<>();

        // 사진 구분: 기존 URL(http)은 그대로 두고, 새 로컬 사진만 업로드 리스트에 담음
        for (PhotoInfo photoInfo : photoInfoList) {
            if (photoInfo.getImageUrl() != null && photoInfo.getImageUrl().startsWith("http")) {
                photosAlreadyUploaded.add(photoInfo);
            } else {
                photosToUpload.add(photoInfo);
            }
        }

        // 새로 추가된 사진이 없으면 바로 DB 업데이트
        if (photosToUpload.isEmpty()) {
            savePostInfoToFirestore(title, isPublic);
            return;
        }

        // 새 사진들 업로드 (원본 + 썸네일)
        final int[] uploadCount = {0};
        final int totalCount = photosToUpload.size();
        
        for (PhotoInfo photoInfo : photosToUpload) {
            uploadPhotoWithThumbnail(photoInfo, new UploadCallback() {
                @Override
                public void onComplete() {
                    uploadCount[0]++;
                    if (uploadCount[0] == totalCount) {
                        // 모든 사진 업로드 완료
                        savePostInfoToFirestore(title, isPublic);
                    }
                }
                
                @Override
                public void onError(String error) {
                    hideProgressDialog();
                    Toast.makeText(getContext(), "이미지 업로드 실패: " + error, Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
    
    /**
     * 사진과 썸네일을 업로드
     */
    private void uploadPhotoWithThumbnail(PhotoInfo photoInfo, UploadCallback callback) {
        try {
            // 원본 이미지 업로드
            String fileName = "images/" + System.currentTimeMillis() + "_" + photoInfo.getUri().getLastPathSegment();
            StorageReference ref = storage.getReference().child(fileName);
            
            ref.putFile(photoInfo.getUri())
                    .addOnSuccessListener(taskSnapshot -> {
                        ref.getDownloadUrl().addOnSuccessListener(originalUrl -> {
                            photoInfo.setImageUrl(originalUrl.toString());
                            
                            // 썸네일 생성 및 업로드
                            createAndUploadThumbnail(photoInfo, callback);
                        }).addOnFailureListener(e -> {
                            callback.onError(e.getMessage());
                        });
                    })
                    .addOnFailureListener(e -> {
                        callback.onError(e.getMessage());
                    });
        } catch (Exception e) {
            callback.onError(e.getMessage());
        }
    }
    
    /**
     * 썸네일 생성 및 업로드
     */
    private void createAndUploadThumbnail(PhotoInfo photoInfo, UploadCallback callback) {
        try {
            InputStream inputStream = getContext().getContentResolver().openInputStream(photoInfo.getUri());
            if (inputStream == null) {
                // 썸네일 생성 실패 시 원본 URL 사용
                photoInfo.setThumbnailUrl(photoInfo.getImageUrl());
                callback.onComplete();
                return;
            }
            
            Bitmap originalBitmap = BitmapFactory.decodeStream(inputStream);
            inputStream.close();
            
            if (originalBitmap == null) {
                photoInfo.setThumbnailUrl(photoInfo.getImageUrl());
                callback.onComplete();
                return;
            }
            
            // 썸네일 크기 (300x300)
            int thumbnailSize = 300;
            Bitmap thumbnail = Bitmap.createScaledBitmap(originalBitmap, thumbnailSize, thumbnailSize, true);
            
            // 썸네일을 임시 파일로 저장 후 업로드
            String thumbnailFileName = "thumbnails/" + System.currentTimeMillis() + "_thumb_" + photoInfo.getUri().getLastPathSegment();
            StorageReference thumbnailRef = storage.getReference().child(thumbnailFileName);
            
            // Bitmap을 byte 배열로 변환
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            thumbnail.compress(Bitmap.CompressFormat.JPEG, 80, baos);
            byte[] thumbnailData = baos.toByteArray();
            
            thumbnailRef.putBytes(thumbnailData)
                    .addOnSuccessListener(taskSnapshot -> {
                        thumbnailRef.getDownloadUrl().addOnSuccessListener(thumbnailUrl -> {
                            photoInfo.setThumbnailUrl(thumbnailUrl.toString());
                            callback.onComplete();
                        }).addOnFailureListener(e -> {
                            // 썸네일 업로드 실패 시 원본 URL 사용
                            photoInfo.setThumbnailUrl(photoInfo.getImageUrl());
                            callback.onComplete();
                        });
                    })
                    .addOnFailureListener(e -> {
                        // 썸네일 업로드 실패 시 원본 URL 사용
                        photoInfo.setThumbnailUrl(photoInfo.getImageUrl());
                        callback.onComplete();
                    });
        } catch (Exception e) {
            // 썸네일 생성 실패 시 원본 URL 사용
            photoInfo.setThumbnailUrl(photoInfo.getImageUrl());
            callback.onComplete();
        }
    }
    
    interface UploadCallback {
        void onComplete();
        void onError(String error);
    }

    // Firestore에 PostItem의 모든 필드 저장
    private void savePostInfoToFirestore(String title, boolean isPublic) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String uid = (user != null) ? user.getUid() : "anonymous";
        String email = (user != null) ? user.getEmail() : "익명";

        // PostItem 구조에 맞게 데이터 구성
        List<String> images = new ArrayList<>();
        List<String> imageDays = new ArrayList<>();
        List<String> imageThumbnails = new ArrayList<>();
        List<Date> imageDates = new ArrayList<>(); // 각 사진의 정확한 촬영 시각
        List<Double> imageLatitudes = new ArrayList<>();
        List<Double> imageLongitudes = new ArrayList<>();
        
        // 여행 종료일 계산 (등록/수정 모두 검증)
        Calendar endCal = null;
        Calendar startCal = null;
        if (startDate != null && travelDays > 0) {
            startCal = Calendar.getInstance();
            startCal.setTime(startDate);
            startCal.set(Calendar.HOUR_OF_DAY, 0);
            startCal.set(Calendar.MINUTE, 0);
            startCal.set(Calendar.SECOND, 0);
            startCal.set(Calendar.MILLISECOND, 0);
            
            endCal = Calendar.getInstance();
            endCal.setTime(startDate);
            endCal.add(Calendar.DAY_OF_MONTH, travelDays - 1);
            endCal.set(Calendar.HOUR_OF_DAY, 23);
            endCal.set(Calendar.MINUTE, 59);
            endCal.set(Calendar.SECOND, 59);
            endCal.set(Calendar.MILLISECOND, 999);
        }
        
        int filteredCount = 0;
        for (PhotoInfo photoInfo : photoInfoList) {
            // 여행 기간 밖의 사진은 저장하지 않음 (등록/수정 모두 적용)
            if (startCal != null && endCal != null && photoInfo.getPhotoDate() != null) {
                Calendar photoCal = Calendar.getInstance();
                photoCal.setTime(photoInfo.getPhotoDate());
                photoCal.set(Calendar.HOUR_OF_DAY, 0);
                photoCal.set(Calendar.MINUTE, 0);
                photoCal.set(Calendar.SECOND, 0);
                photoCal.set(Calendar.MILLISECOND, 0);
                
                // 여행 기간 밖이면 건너뜀
                if (photoCal.getTimeInMillis() < startCal.getTimeInMillis() || 
                    photoCal.getTimeInMillis() > endCal.getTimeInMillis()) {
                    filteredCount++;
                    continue;
                }
            }
            
            images.add(photoInfo.getImageUrl() != null ? photoInfo.getImageUrl() : "");
            imageDays.add(photoInfo.getDayNumber() != null ? photoInfo.getDayNumber() : "1");
            imageThumbnails.add(photoInfo.getThumbnailUrl() != null ? photoInfo.getThumbnailUrl() : photoInfo.getImageUrl());
            // 정확한 촬영 시각 저장 (EXIF에서 추출한 날짜+시간)
            imageDates.add(photoInfo.getPhotoDate() != null ? photoInfo.getPhotoDate() : new Date());
            imageLatitudes.add(photoInfo.getLatitude());
            imageLongitudes.add(photoInfo.getLongitude());
        }
        
        // 필터링된 사진이 있으면 알림
        if (filteredCount > 0) {
            Toast.makeText(getContext(), filteredCount + "장의 사진이 여행 기간 밖에 촬영되어 저장되지 않았습니다.", Toast.LENGTH_LONG).show();
        }

        Map<String, Object> post = new HashMap<>();
        post.put("title", title);
        post.put("images", images);
        post.put("imageDays", imageDays);
        post.put("imageThumbnails", imageThumbnails);
        post.put("imageDates", imageDates); // 정확한 촬영 시각 저장
        post.put("imageLatitudes", imageLatitudes);
        post.put("imageLongitudes", imageLongitudes);
        post.put("startDate", startDate != null ? startDate : new Date());
        post.put("travelDays", travelDays > 0 ? travelDays : 1);
        post.put("isPublic", isPublic);
        post.put("userId", uid);
        post.put("userEmail", email);

        // [수정 모드 확인]
        if (editPostId != null) {
            // 수정 시에는 기존 문서를 찾아 덮어씌웁니다.
            db.collection("TravelPosts").document(editPostId)
                    .set(post, SetOptions.merge()) // 기존 필드를 유지하면서 수정된 내용만 덮어씀
                    .addOnSuccessListener(aVoid -> {
                        hideProgressDialog();
                        Toast.makeText(getContext(), "여행 기록이 수정되었습니다!", Toast.LENGTH_SHORT).show();
                        goToMainList();
                    })
                    .addOnFailureListener(e -> {
                        hideProgressDialog();
                        Toast.makeText(getContext(), "수정 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } else {
            // 신규 작성 시에는 새로운 문서를 추가합니다.
            post.put("createdAt", FieldValue.serverTimestamp()); // 신규일 때만 시간 저장
            db.collection("TravelPosts")
                    .add(post)
                    .addOnSuccessListener(documentReference -> {
                        hideProgressDialog();
                        Toast.makeText(getContext(), "여행 기록이 성공적으로 저장되었습니다!", Toast.LENGTH_SHORT).show();
                        goToMainList();
                    })
                    .addOnFailureListener(e -> {
                        hideProgressDialog();
                        Toast.makeText(getContext(), "저장 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }

    // 메인 리스트 탭으로 이동하는 공통 함수
    private void goToMainList() {
        if (getActivity() != null) {
            // 실제로 ListFragment로 교체
            ListFragment listFragment = new ListFragment();
            getActivity().getSupportFragmentManager().popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE);
            getActivity().getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, listFragment)
                    .commit();
            
            // 네비게이션 바 상태 업데이트
            BottomNavigationView bottomNav = getActivity().findViewById(R.id.bottom_navigation);
            if (bottomNav != null) {
                bottomNav.getMenu().findItem(R.id.nav_my_list).setChecked(true);
            }
            
            // 타이틀 업데이트
            if (getActivity() instanceof androidx.appcompat.app.AppCompatActivity) {
                ((androidx.appcompat.app.AppCompatActivity) getActivity()).setTitle("내 여행 리스트");
            }
        }
    }

    private void showProgressDialog() {
        if (progressDialog == null) {
            // AlertDialog 생성
            androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(getContext());
            builder.setCancelable(false); // 로딩 중 창 밖을 눌러도 꺼지지 않게 설정
            builder.setView(R.layout.loading); // 커스텀 레이아웃 사용 (아래에서 생성)
            progressDialog = builder.create();
        }
        progressDialog.show();
    }

    private void hideProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }
    public boolean hasChanges() { //사용자가 제목을 입력했거나 사진을 한 장이라도 올렸는지 확인하는 함수
        // 만약 저장 버튼을 눌러서 나가는 중이라면 무조건 false 반환 (팝업 안 띄움)
        if (isSaving) {
            return false;
        }

        String title = (editTripTitle != null) ? editTripTitle.getText().toString().trim() : "";
        // 1. 제목이 비어있지 않거나
        // 2. 선택된 사진이 하나라도 있다면 '내용이 있음'으로 판단
        return !title.isEmpty() || (photoInfoList != null && !photoInfoList.isEmpty());
    }
    
    /**
     * 여행 일정 변경 다이얼로그 표시 (등록/수정 모드 모두 사용)
     */
    private void showTravelInfoDialog() {
        if (getContext() == null) return;
        
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(getContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_travel_info, null);
        builder.setView(dialogView);
        
        android.widget.EditText editTravelDays = dialogView.findViewById(R.id.edit_travel_days);
        android.widget.DatePicker datePicker = dialogView.findViewById(R.id.date_picker_start);
        
        // 기존 여행 정보로 초기화
        if (travelDays > 0) {
            editTravelDays.setText(String.valueOf(travelDays));
        }
        if (startDate != null) {
            Calendar cal = Calendar.getInstance();
            cal.setTime(startDate);
            datePicker.updateDate(cal.get(Calendar.YEAR), cal.get(Calendar.MONTH), cal.get(Calendar.DAY_OF_MONTH));
        }
        
        String dialogTitle = (editPostId != null) ? "여행 일정 변경" : "여행 일정 설정";
        android.app.AlertDialog dialog = builder.setTitle(dialogTitle)
                .setPositiveButton("확인", null) // 나중에 처리하기 위해 null로 설정
                .setNegativeButton("취소", null)
                .create();
        
        // 확인 버튼 클릭 시 처리
        dialog.setOnShowListener(d -> {
            android.widget.Button positiveButton = dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(v -> {
                String daysStr = editTravelDays.getText().toString();
                if (daysStr.isEmpty()) {
                    Toast.makeText(getContext(), "여행 일수를 입력해주세요.", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                int newTravelDays = Integer.parseInt(daysStr);
                if (newTravelDays <= 0) {
                    Toast.makeText(getContext(), "여행 일수는 1일 이상이어야 합니다.", Toast.LENGTH_SHORT).show();
                    return;
                }
                
                // 날짜 선택
                Calendar calendar = Calendar.getInstance();
                calendar.set(datePicker.getYear(), datePicker.getMonth(), datePicker.getDayOfMonth());
                Date newStartDate = calendar.getTime();
                
                // 확인 다이얼로그 표시
                showTravelInfoConfirmationDialog(newStartDate, newTravelDays, dialog);
            });
            
            android.widget.Button negativeButton = dialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE);
            negativeButton.setOnClickListener(v -> {
                dialog.dismiss();
            });
        });
        
        dialog.show();
    }
    
    /**
     * 여행 기간 밖의 사진 제거
     */
    private void removePhotosOutsideTravelPeriod() {
        if (startDate == null || travelDays <= 0) return;
        
        Calendar startCal = Calendar.getInstance();
        startCal.setTime(startDate);
        startCal.set(Calendar.HOUR_OF_DAY, 0);
        startCal.set(Calendar.MINUTE, 0);
        startCal.set(Calendar.SECOND, 0);
        startCal.set(Calendar.MILLISECOND, 0);
        
        Calendar endCal = Calendar.getInstance();
        endCal.setTime(startDate);
        endCal.add(Calendar.DAY_OF_MONTH, travelDays - 1);
        endCal.set(Calendar.HOUR_OF_DAY, 23);
        endCal.set(Calendar.MINUTE, 59);
        endCal.set(Calendar.SECOND, 59);
        endCal.set(Calendar.MILLISECOND, 999);
        
        List<PhotoInfo> photosToRemove = new ArrayList<>();
        for (PhotoInfo photoInfo : photoInfoList) {
            if (photoInfo.getPhotoDate() != null) {
                Calendar photoCal = Calendar.getInstance();
                photoCal.setTime(photoInfo.getPhotoDate());
                photoCal.set(Calendar.HOUR_OF_DAY, 0);
                photoCal.set(Calendar.MINUTE, 0);
                photoCal.set(Calendar.SECOND, 0);
                photoCal.set(Calendar.MILLISECOND, 0);
                
                if (photoCal.getTimeInMillis() < startCal.getTimeInMillis() || 
                    photoCal.getTimeInMillis() > endCal.getTimeInMillis()) {
                    photosToRemove.add(photoInfo);
                }
            }
        }
        
        if (!photosToRemove.isEmpty()) {
            photoInfoList.removeAll(photosToRemove);
            photoAdapter.updateAdapterItems();
            Toast.makeText(getContext(), photosToRemove.size() + "장의 사진이 여행 기간 밖에 촬영되어 제거되었습니다.", Toast.LENGTH_LONG).show();
        }
    }
    
    /**
     * 여행 정보 확인 다이얼로그 표시
     */
    private void showTravelInfoConfirmationDialog(Date newStartDate, int newTravelDays, android.app.AlertDialog previousDialog) {
        if (getContext() == null) return;
        
        // 종료일 계산
        Calendar endCal = Calendar.getInstance();
        endCal.setTime(newStartDate);
        endCal.add(Calendar.DAY_OF_MONTH, newTravelDays - 1);
        
        // 날짜 포맷팅
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy년 MM월 dd일", Locale.getDefault());
        String startDateStr = sdf.format(newStartDate);
        String endDateStr = sdf.format(endCal.getTime());
        
        // X박X일 계산
        int nights = newTravelDays - 1;
        String message = startDateStr + "부터 " + endDateStr + "까지(" + nights + "박" + newTravelDays + "일, " + newTravelDays + "일차) 여행이 맞습니까?";
        
        new android.app.AlertDialog.Builder(getContext())
                .setTitle("여행 일정 확인")
                .setMessage(message)
                .setPositiveButton("맞습니다", (d, which) -> {
                    // 여행 정보 업데이트
                    startDate = newStartDate;
                    travelDays = newTravelDays;
                    
                    // 기존 사진들의 일차 재계산
                    for (PhotoInfo photoInfo : photoInfoList) {
                        calculateDayNumber(photoInfo);
                    }
                    
                    // 일차별로 정렬하여 어댑터 업데이트
                    sortPhotosByDay();
                    photoAdapter.updateAdapterItems();
                    
                    // 여행 기간 밖의 사진이 있는지 확인하고 제거
                    removePhotosOutsideTravelPeriod();
                    
                    // 여행 기간 텍스트 업데이트
                    updateTravelPeriodText();
                    
                    Toast.makeText(getContext(), "여행 일정이 변경되었습니다.", Toast.LENGTH_SHORT).show();
                    previousDialog.dismiss();
                })
                .setNegativeButton("수정하기", (d, which) -> {
                    // 수정하기를 누르면 이전 다이얼로그는 그대로 유지 (아무것도 안 함)
                })
                .setCancelable(false)
                .show();
    }
    
    /**
     * 여행 기간 텍스트 업데이트
     */
    private void updateTravelPeriodText() {
        if (textTravelPeriod == null) return;
        
        if (startDate != null && travelDays > 0) {
            // 종료일 계산
            Calendar endCal = Calendar.getInstance();
            endCal.setTime(startDate);
            endCal.add(Calendar.DAY_OF_MONTH, travelDays - 1);
            
            // 날짜 포맷팅
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy년 MM월 dd일", Locale.getDefault());
            String startDateStr = sdf.format(startDate);
            String endDateStr = sdf.format(endCal.getTime());
            
            // X박X일 계산
            int nights = travelDays - 1;
            String periodText = startDateStr + " ~ " + endDateStr + " (" + nights + "박" + travelDays + "일, " + travelDays + "일차)";
            
            textTravelPeriod.setText("여행 기간: " + periodText);
            textTravelPeriod.setVisibility(View.VISIBLE);
        } else {
            textTravelPeriod.setText("여행 기간: 설정되지 않음");
            textTravelPeriod.setVisibility(View.VISIBLE);
        }
    }
}



