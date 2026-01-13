package com.example.madcamp01;

import android.content.ContentResolver;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Address;
import android.location.Geocoder;
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
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;

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

    private EditText editTripTitle;
    private RecyclerView recyclerView;
    private PhotoAdapter photoAdapter;
    private PostItem currentPostItem = new PostItem();
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private FirebaseStorage storage = FirebaseStorage.getInstance();
    private ActivityResultLauncher<String> getMultipleContents;
    private FirebaseAuth auth;
    private androidx.appcompat.app.AlertDialog progressDialog;
    private com.google.android.material.materialswitch.MaterialSwitch switchIsPublic;
    private android.widget.TextView textTravelPeriod;
    private String editPostId = null;
    private boolean isSaving = false;
    
    private Date startDate;
    private int travelDays;
    private ExecutorService executorService = Executors.newFixedThreadPool(4);
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            editPostId = getArguments().getString("postId");
            if (editPostId == null) {
                travelDays = getArguments().getInt("travelDays", 0);
                long startDateLong = getArguments().getLong("startDate", 0);
                if (startDateLong > 0) {
                    startDate = new Date(startDateLong);
                }
            }
        }

        getMultipleContents = registerForActivityResult(
                new ActivityResultContracts.GetMultipleContents(),
                uris -> {
                    if (uris != null && !uris.isEmpty()) {
                        processNewPhotos(uris);
                    }
                }
        );
        auth = FirebaseAuth.getInstance();
    }
    
    /**
     * Fragment 생명주기: Fragment가 소멸될 때 호출되는 메서드.
     */
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
        View view = inflater.inflate(R.layout.fragment_write, container, false);

        editTripTitle = view.findViewById(R.id.edit_trip_title);
        recyclerView = view.findViewById(R.id.recycler_photos);
        Button btnAddPhoto = view.findViewById(R.id.btn_add_photo);
        Button btnSave = view.findViewById(R.id.btn_save);
        Button btnChangeTravelInfo = view.findViewById(R.id.btn_change_travel_info);
        switchIsPublic = view.findViewById(R.id.switch_is_public);
        textTravelPeriod = view.findViewById(R.id.text_travel_period);

        androidx.recyclerview.widget.LinearLayoutManager layoutManager = 
            new androidx.recyclerview.widget.LinearLayoutManager(getContext());
        recyclerView.setLayoutManager(layoutManager);
        photoAdapter = new PhotoAdapter(currentPostItem, getContext());
        recyclerView.setAdapter(photoAdapter);
        
        if (editPostId != null) {
            editTripTitle.setText(getArguments().getString("title"));
            switchIsPublic.setChecked(getArguments().getBoolean("isPublic"));
            btnSave.setText("수정 완료");
            btnChangeTravelInfo.setVisibility(View.VISIBLE);
            
            // 타이틀 설정
            if (getActivity() instanceof androidx.appcompat.app.AppCompatActivity) {
                ((androidx.appcompat.app.AppCompatActivity) getActivity()).setTitle("게시물 수정");
            }
            
            PostItem postItem = getArguments().getParcelable("postItem");
            if (postItem != null) {
                currentPostItem = postItem;
                photoAdapter.setPostItem(currentPostItem);
                
                List<String> images = postItem.getImages();
                if (images != null) {
                    for (int i = 0; i < images.size(); i++) {
                        String imageUrl = images.get(i);
                        if (imageUrl != null && imageUrl.startsWith("http")) {
                            currentPostItem.setImageUri(i, null);
                        } else if (imageUrl != null && !imageUrl.isEmpty()) {
                            try {
                                currentPostItem.setImageUri(i, Uri.parse(imageUrl));
                            } catch (Exception e) {
                                currentPostItem.setImageUri(i, null);
                            }
                        }
                    }
                }
                
                if (postItem.getStartDate() != null) {
                    startDate = postItem.getStartDate();
                }
                if (postItem.getTravelDays() > 0) {
                    travelDays = postItem.getTravelDays();
                }
            } else {
                ArrayList<String> imageUrls = getArguments().getStringArrayList("images");
                if (imageUrls != null) {
                    for (String url : imageUrls) {
                        currentPostItem.addPhoto(
                            Uri.parse(url),
                            new Date(),
                            null,
                            null,
                            "1",
                            url,
                            url,
                            null
                        );
                    }
                }
            }
            
            sortPhotosByDay();
            photoAdapter.updateAdapterItems();
            updateTravelPeriodText();
        } else {
            if (startDate != null && travelDays > 0) {
                btnChangeTravelInfo.setVisibility(View.VISIBLE);
            }
        }
        
        updateTravelPeriodText();
        
        btnAddPhoto.setOnClickListener(v -> {
            getMultipleContents.launch("image/*");
        });

        btnChangeTravelInfo.setOnClickListener(v -> {
            showTravelInfoDialog();
        });

        btnSave.setOnClickListener(v -> {
            uploadToFirebase();
        });

        photoAdapter.setOnPhotoDeleteListener(position -> {
            currentPostItem.removePhoto(position);
            photoAdapter.updateAdapterItems();
        });

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(
                ItemTouchHelper.UP | ItemTouchHelper.DOWN | ItemTouchHelper.START | ItemTouchHelper.END, 0) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView,
                                  @NonNull RecyclerView.ViewHolder viewHolder,
                                  @NonNull RecyclerView.ViewHolder target) {
                int fromPos = viewHolder.getAdapterPosition();
                int toPos = target.getAdapterPosition();
                swapPhotosInPostItem(fromPos, toPos);
                sortPhotosByDay();
                photoAdapter.updateAdapterItems();
                return true;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
            }
        });

        itemTouchHelper.attachToRecyclerView(recyclerView);
        return view;
    }
    /**
     * 새로 선택된 사진들에 대해 EXIF 데이터를 추출하고 여행 일차를 계산하여 PostItem에 추가.
     */
    private void processNewPhotos(List<Uri> uris) {
        if (startDate == null) {
            Toast.makeText(getContext(), "여행 시작일이 설정되지 않았습니다.", Toast.LENGTH_SHORT).show();
            return;
        }
        
        showProgressDialog();
        
        executorService.execute(() -> {
            List<Integer> newPhotoIndices = new ArrayList<>();
            List<Uri> rejectedPhotos = new ArrayList<>();
            
            for (Uri uri : uris) {
                int index = currentPostItem.addPhoto(
                    uri,
                    new Date(),
                    null,
                    null,
                    "1",
                    null,
                    null,
                    null
                );
                
                extractExifData(uri, index);
                
                Date photoDate = currentPostItem.getImageDate(index);
                if (startDate != null && travelDays > 0 && photoDate != null) {
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
                    
                    Calendar photoCal = Calendar.getInstance();
                    photoCal.setTime(photoDate);
                    photoCal.set(Calendar.HOUR_OF_DAY, 0);
                    photoCal.set(Calendar.MINUTE, 0);
                    photoCal.set(Calendar.SECOND, 0);
                    photoCal.set(Calendar.MILLISECOND, 0);
                    
                    if (photoCal.getTimeInMillis() < startCal.getTimeInMillis() || 
                        photoCal.getTimeInMillis() > endCal.getTimeInMillis()) {
                        rejectedPhotos.add(uri);
                        currentPostItem.removePhoto(index);
                        continue;
                    }
                }
                
                calculateDayNumber(index);
                newPhotoIndices.add(index);
            }
            
            if (!newPhotoIndices.isEmpty()) {
                sortNewPhotosByDate(newPhotoIndices);
            }
            
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (!newPhotoIndices.isEmpty()) {
                        sortPhotosByDay();
                        photoAdapter.updateAdapterItems();
                    }
                    
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
    
    private String formatDate(Date date) {
        if (date == null) return "";
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy년 MM월 dd일", Locale.getDefault());
        return sdf.format(date);
    }
    
    /**
     * 사진 파일의 EXIF 데이터에서 촬영 날짜와 위치 정보를 추출하여 PostItem에 저장.
     */
    private void extractExifData(Uri uri, int index) {
        try {
            InputStream inputStream = getContext().getContentResolver().openInputStream(uri);
            if (inputStream == null) {
                currentPostItem.updatePhoto(index, new Date(), null, null, "1", null, null, null);
                return;
            }
            
            ExifInterface exif = new ExifInterface(inputStream);
            inputStream.close();
            
            Date photoDate = new Date();
            Double latitude = null;
            Double longitude = null;
            String location = null;
            
            String dateTime = exif.getAttribute(ExifInterface.TAG_DATETIME);
            if (dateTime != null) {
                try {
                    SimpleDateFormat sdf = new SimpleDateFormat("yyyy:MM:dd HH:mm:ss", Locale.getDefault());
                    photoDate = sdf.parse(dateTime);
                } catch (ParseException e) {
                    photoDate = new Date();
                }
            }
            
            float[] latLong = new float[2];
            if (exif.getLatLong(latLong)) {
                latitude = (double) latLong[0];
                longitude = (double) latLong[1];
                location = getAddressFromLocation(latitude, longitude);
            }
            
            currentPostItem.updatePhoto(index, photoDate, latitude, longitude, 
                                       currentPostItem.getImageDay(index), null, null, location);
            
        } catch (IOException e) {
            e.printStackTrace();
            currentPostItem.updatePhoto(index, new Date(), null, null, "1", null, null, null);
        }
    }
    
    /**
     * 특정 인덱스의 사진에 대해 여행 시작일 기준으로 일차를 계산하여 PostItem에 저장.
     */
    private void calculateDayNumber(int index) {
        Date photoDate = currentPostItem.getImageDate(index);
        
        if (startDate == null || photoDate == null) {
            currentPostItem.updatePhoto(index, photoDate, 
                                       currentPostItem.getImageLatitude(index),
                                       currentPostItem.getImageLongitude(index),
                                       "1", null, null, currentPostItem.getImageLocation(index));
            return;
        }
        
        Calendar startCal = Calendar.getInstance();
        startCal.setTime(startDate);
        startCal.set(Calendar.HOUR_OF_DAY, 0);
        startCal.set(Calendar.MINUTE, 0);
        startCal.set(Calendar.SECOND, 0);
        startCal.set(Calendar.MILLISECOND, 0);
        
        Calendar photoCal = Calendar.getInstance();
        photoCal.setTime(photoDate);
        photoCal.set(Calendar.HOUR_OF_DAY, 0);
        photoCal.set(Calendar.MINUTE, 0);
        photoCal.set(Calendar.SECOND, 0);
        photoCal.set(Calendar.MILLISECOND, 0);
        
        long diffInMillis = photoCal.getTimeInMillis() - startCal.getTimeInMillis();
        long diffInDays = diffInMillis / (24 * 60 * 60 * 1000);
        
        int dayNumber = (int) diffInDays + 1;
        if (dayNumber < 1) dayNumber = 1;
        if (dayNumber > travelDays) dayNumber = travelDays;
        
        currentPostItem.updatePhoto(index, photoDate,
                                   currentPostItem.getImageLatitude(index),
                                   currentPostItem.getImageLongitude(index),
                                   String.valueOf(dayNumber), null, null, currentPostItem.getImageLocation(index));
    }
    
    /**
     * PostItem의 모든 병렬 리스트를 일차와 시간 순서로 정렬.
     */
    private void sortPhotosByDay() {
        int count = currentPostItem.getPhotoCount();
        if (count <= 1) return;
        
        List<Integer> indices = new ArrayList<>();
        for (int i = 0; i < count; i++) {
            indices.add(i);
        }
        
        Collections.sort(indices, new Comparator<Integer>() {
            @Override
            public int compare(Integer i1, Integer i2) {
                String day1 = currentPostItem.getImageDay(i1);
                String day2 = currentPostItem.getImageDay(i2);
                int dayCompare = (day1 != null ? day1 : "1").compareTo(day2 != null ? day2 : "1");
                if (dayCompare != 0) return dayCompare;
                
                Date d1 = currentPostItem.getImageDate(i1);
                Date d2 = currentPostItem.getImageDate(i2);
                if (d1 == null && d2 == null) return 0;
                if (d1 == null) return 1;
                if (d2 == null) return -1;
                return d1.compareTo(d2);
            }
        });
        
        reorderPostItemLists(indices);
    }
    
    private void sortNewPhotosByDate(List<Integer> indices) {
        Collections.sort(indices, new Comparator<Integer>() {
            @Override
            public int compare(Integer i1, Integer i2) {
                Date d1 = currentPostItem.getImageDate(i1);
                Date d2 = currentPostItem.getImageDate(i2);
                if (d1 == null && d2 == null) return 0;
                if (d1 == null) return 1;
                if (d2 == null) return -1;
                return d1.compareTo(d2);
            }
        });
    }
    
    private void reorderPostItemLists(List<Integer> newOrder) {
        int count = currentPostItem.getPhotoCount();
        if (newOrder.size() != count) return;
        
        List<Uri> tempUris = new ArrayList<>();
        List<String> tempImages = new ArrayList<>();
        List<String> tempDays = new ArrayList<>();
        List<String> tempThumbnails = new ArrayList<>();
        List<Date> tempDates = new ArrayList<>();
        List<Double> tempLatitudes = new ArrayList<>();
        List<Double> tempLongitudes = new ArrayList<>();
        List<String> tempLocations = new ArrayList<>();
        
        for (int i = 0; i < count; i++) {
            tempUris.add(currentPostItem.getImageUri(i));
            tempImages.add(currentPostItem.getImageUrl(i));
            tempDays.add(currentPostItem.getImageDay(i));
            tempThumbnails.add(currentPostItem.getImageThumbnailUrl(i));
            tempDates.add(currentPostItem.getImageDate(i));
            tempLatitudes.add(currentPostItem.getImageLatitude(i));
            tempLongitudes.add(currentPostItem.getImageLongitude(i));
            tempLocations.add(currentPostItem.getImageLocation(i));
        }
        
        for (int i = 0; i < count; i++) {
            int oldIndex = newOrder.get(i);
            currentPostItem.setImageUri(i, tempUris.get(oldIndex));
            if (currentPostItem.getImages() != null && i < currentPostItem.getImages().size()) {
                currentPostItem.getImages().set(i, tempImages.get(oldIndex));
            }
            if (currentPostItem.getImageDays() != null && i < currentPostItem.getImageDays().size()) {
                currentPostItem.getImageDays().set(i, tempDays.get(oldIndex));
            }
            if (currentPostItem.getImageThumbnails() != null && i < currentPostItem.getImageThumbnails().size()) {
                currentPostItem.getImageThumbnails().set(i, tempThumbnails.get(oldIndex));
            }
            if (currentPostItem.getImageDates() != null && i < currentPostItem.getImageDates().size()) {
                currentPostItem.getImageDates().set(i, tempDates.get(oldIndex));
            }
            if (currentPostItem.getImageLatitudes() != null && i < currentPostItem.getImageLatitudes().size()) {
                currentPostItem.getImageLatitudes().set(i, tempLatitudes.get(oldIndex));
            }
            if (currentPostItem.getImageLongitudes() != null && i < currentPostItem.getImageLongitudes().size()) {
                currentPostItem.getImageLongitudes().set(i, tempLongitudes.get(oldIndex));
            }
            if (currentPostItem.getImageLocations() != null && i < currentPostItem.getImageLocations().size()) {
                currentPostItem.getImageLocations().set(i, tempLocations.get(oldIndex));
            }
        }
    }
    
    private void swapPhotosInPostItem(int fromPos, int toPos) {
        int count = currentPostItem.getPhotoCount();
        if (fromPos < 0 || fromPos >= count || toPos < 0 || toPos >= count) {
            return;
        }
        
        swapInList(currentPostItem.getLocalOnlyImageUris(), fromPos, toPos);
        swapInList(currentPostItem.getImages(), fromPos, toPos);
        swapInList(currentPostItem.getImageDays(), fromPos, toPos);
        swapInList(currentPostItem.getImageThumbnails(), fromPos, toPos);
        swapInList(currentPostItem.getImageDates(), fromPos, toPos);
        swapInList(currentPostItem.getImageLatitudes(), fromPos, toPos);
        swapInList(currentPostItem.getImageLongitudes(), fromPos, toPos);
        swapInList(currentPostItem.getImageLocations(), fromPos, toPos);
    }
    
    private <T> void swapInList(List<T> list, int fromPos, int toPos) {
        if (list == null || fromPos < 0 || fromPos >= list.size() || 
            toPos < 0 || toPos >= list.size()) {
            return;
        }
        Collections.swap(list, fromPos, toPos);
    }

    /**
     * PostItem의 모든 사진을 Firebase Storage에 업로드하고 Firestore에 저장.
     */
    private void uploadToFirebase() {
        if (!isAdded() || getContext() == null) {
            return;
        }
        
        if (isSaving) {
            return; // 이미 저장 중이면 중복 실행 방지
        }
        
        String title = editTripTitle.getText().toString().trim();
        boolean isPublic = switchIsPublic.isChecked();
        
        if (title.isEmpty() || currentPostItem.getPhotoCount() == 0) {
            Toast.makeText(getContext(), "제목과 사진을 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 수정 모드에서는 startDate가 없어도 괜찮음 (기존 값 사용)
        if (editPostId == null && startDate == null) {
            Toast.makeText(getContext(), "여행 정보가 없습니다. 다시 시도해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        
        isSaving = true;
        showProgressDialog();
        
        sortPhotosByDay();
        
        List<Integer> photosToUpload = new ArrayList<>();
        List<Integer> photosAlreadyUploaded = new ArrayList<>();

        int count = currentPostItem.getPhotoCount();
        for (int i = 0; i < count; i++) {
            String imageUrl = currentPostItem.getImageUrl(i);
            if (imageUrl != null && imageUrl.startsWith("http")) {
                photosAlreadyUploaded.add(i);
            } else {
                photosToUpload.add(i);
            }
        }

        if (photosToUpload.isEmpty()) {
            savePostInfoToFirestore(title, isPublic);
            return;
        }

        final int[] uploadCount = {0};
        final int totalCount = photosToUpload.size();
        
        for (int index : photosToUpload) {
            uploadPhotoWithThumbnail(index, new UploadCallback() {
                @Override
                public void onComplete() {
                    uploadCount[0]++;
                    if (uploadCount[0] == totalCount) {
                        savePostInfoToFirestore(title, isPublic);
                    }
                }
                
                @Override
                public void onError(String error) {
                    isSaving = false;
                    if (isAdded() && getContext() != null) {
                        hideProgressDialog();
                        Toast.makeText(getContext(), "이미지 업로드 실패: " + error, Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }
    
    /**
     * 특정 인덱스의 사진을 Firebase Storage에 업로드하고 썸네일을 생성하여 업로드.
     */
    private void uploadPhotoWithThumbnail(int index, UploadCallback callback) {
        try {
            Uri uri = currentPostItem.getImageUri(index);
            if (uri == null) {
                callback.onError("Uri가 null입니다.");
                return;
            }
            
            String fileName = "images/" + System.currentTimeMillis() + "_" + uri.getLastPathSegment();
            StorageReference ref = storage.getReference().child(fileName);
            
            ref.putFile(uri)
                    .addOnSuccessListener(taskSnapshot -> {
                        ref.getDownloadUrl().addOnSuccessListener(originalUrl -> {
                            currentPostItem.updatePhoto(index,
                                currentPostItem.getImageDate(index),
                                currentPostItem.getImageLatitude(index),
                                currentPostItem.getImageLongitude(index),
                                currentPostItem.getImageDay(index),
                                originalUrl.toString(),
                                null,
                                currentPostItem.getImageLocation(index)
                            );
                            
                            createAndUploadThumbnail(index, callback);
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
     * 특정 인덱스의 사진으로부터 썸네일을 생성하고 Firebase Storage에 업로드.
     */
    private void createAndUploadThumbnail(int index, UploadCallback callback) {
        try {
            Uri uri = currentPostItem.getImageUri(index);
            if (uri == null) {
                String imageUrl = currentPostItem.getImageUrl(index);
                currentPostItem.updatePhoto(index,
                    currentPostItem.getImageDate(index),
                    currentPostItem.getImageLatitude(index),
                    currentPostItem.getImageLongitude(index),
                    currentPostItem.getImageDay(index),
                    imageUrl,
                    imageUrl,
                    currentPostItem.getImageLocation(index)
                );
                callback.onComplete();
                return;
            }
            
            InputStream inputStream = getContext().getContentResolver().openInputStream(uri);
            if (inputStream == null) {
                String imageUrl = currentPostItem.getImageUrl(index);
                currentPostItem.updatePhoto(index,
                    currentPostItem.getImageDate(index),
                    currentPostItem.getImageLatitude(index),
                    currentPostItem.getImageLongitude(index),
                    currentPostItem.getImageDay(index),
                    imageUrl,
                    imageUrl,
                    currentPostItem.getImageLocation(index)
                );
                callback.onComplete();
                return;
            }
            
            Bitmap originalBitmap = BitmapFactory.decodeStream(inputStream);
            inputStream.close();
            
            if (originalBitmap == null) {
                String imageUrl = currentPostItem.getImageUrl(index);
                currentPostItem.updatePhoto(index,
                    currentPostItem.getImageDate(index),
                    currentPostItem.getImageLatitude(index),
                    currentPostItem.getImageLongitude(index),
                    currentPostItem.getImageDay(index),
                    imageUrl,
                    imageUrl,
                    currentPostItem.getImageLocation(index)
                );
                callback.onComplete();
                return;
            }
            
            int thumbnailSize = 300;
            Bitmap thumbnail = Bitmap.createScaledBitmap(originalBitmap, thumbnailSize, thumbnailSize, true);
            
            String thumbnailFileName = "thumbnails/" + System.currentTimeMillis() + "_thumb_" + uri.getLastPathSegment();
            StorageReference thumbnailRef = storage.getReference().child(thumbnailFileName);
            
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            thumbnail.compress(Bitmap.CompressFormat.JPEG, 80, baos);
            byte[] thumbnailData = baos.toByteArray();
            
            thumbnailRef.putBytes(thumbnailData)
                    .addOnSuccessListener(taskSnapshot -> {
                        thumbnailRef.getDownloadUrl().addOnSuccessListener(thumbnailUrl -> {
                            currentPostItem.updatePhoto(index,
                                currentPostItem.getImageDate(index),
                                currentPostItem.getImageLatitude(index),
                                currentPostItem.getImageLongitude(index),
                                currentPostItem.getImageDay(index),
                                currentPostItem.getImageUrl(index),
                                thumbnailUrl.toString(),
                                currentPostItem.getImageLocation(index)
                            );
                            callback.onComplete();
                        }).addOnFailureListener(e -> {
                            String imageUrl = currentPostItem.getImageUrl(index);
                            currentPostItem.updatePhoto(index,
                                currentPostItem.getImageDate(index),
                                currentPostItem.getImageLatitude(index),
                                currentPostItem.getImageLongitude(index),
                                currentPostItem.getImageDay(index),
                                imageUrl,
                                imageUrl,
                                currentPostItem.getImageLocation(index)
                            );
                            callback.onComplete();
                        });
                    })
                    .addOnFailureListener(e -> {
                        String imageUrl = currentPostItem.getImageUrl(index);
                        currentPostItem.updatePhoto(index,
                            currentPostItem.getImageDate(index),
                            currentPostItem.getImageLatitude(index),
                            currentPostItem.getImageLongitude(index),
                            currentPostItem.getImageDay(index),
                            imageUrl,
                            imageUrl,
                            currentPostItem.getImageLocation(index)
                        );
                        callback.onComplete();
                    });
        } catch (Exception e) {
            String imageUrl = currentPostItem.getImageUrl(index);
            currentPostItem.updatePhoto(index,
                currentPostItem.getImageDate(index),
                currentPostItem.getImageLatitude(index),
                currentPostItem.getImageLongitude(index),
                currentPostItem.getImageDay(index),
                imageUrl,
                imageUrl,
                currentPostItem.getImageLocation(index)
            );
            callback.onComplete();
        }
    }
    
    interface UploadCallback {
        void onComplete();
        void onError(String error);
    }

    /**
     * PostItem의 모든 데이터를 Firestore에 저장하거나 업데이트.
     */
    private void savePostInfoToFirestore(String title, boolean isPublic) {
        if (!isAdded() || getContext() == null) {
            isSaving = false;
            return;
        }
        
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            isSaving = false;
            hideProgressDialog();
            Toast.makeText(getContext(), "로그인이 필요합니다.", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String uid = user.getUid();
        String email = user.getEmail();
        if (email == null) {
            email = "익명";
        }

        // 수정 모드에서 startDate와 travelDays가 없으면 기존 값 사용
        if (editPostId != null) {
            if (startDate == null && currentPostItem.getStartDate() != null) {
                startDate = currentPostItem.getStartDate();
            }
            if (travelDays <= 0 && currentPostItem.getTravelDays() > 0) {
                travelDays = currentPostItem.getTravelDays();
            }
        }
        
        // 최종적으로도 없으면 기본값 설정
        if (startDate == null) {
            startDate = new Date();
        }
        if (travelDays <= 0) {
            travelDays = 1;
        }

        currentPostItem.setTitle(title);
        currentPostItem.setStartDate(startDate);
        currentPostItem.setTravelDays(travelDays);
        currentPostItem.setIsPublic(isPublic);
        currentPostItem.setUserId(uid);
        currentPostItem.setUserEmail(email);
        
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
        
        List<Integer> indicesToRemove = new ArrayList<>();
        int count = currentPostItem.getPhotoCount();
        for (int i = 0; i < count; i++) {
            Date photoDate = currentPostItem.getImageDate(i);
            if (startCal != null && endCal != null && photoDate != null) {
                Calendar photoCal = Calendar.getInstance();
                photoCal.setTime(photoDate);
                photoCal.set(Calendar.HOUR_OF_DAY, 0);
                photoCal.set(Calendar.MINUTE, 0);
                photoCal.set(Calendar.SECOND, 0);
                photoCal.set(Calendar.MILLISECOND, 0);
                
                if (photoCal.getTimeInMillis() < startCal.getTimeInMillis() || 
                    photoCal.getTimeInMillis() > endCal.getTimeInMillis()) {
                    indicesToRemove.add(i);
                }
            }
        }
        
        for (int i = indicesToRemove.size() - 1; i >= 0; i--) {
            currentPostItem.removePhoto(indicesToRemove.get(i));
        }
        
        if (!indicesToRemove.isEmpty()) {
            Toast.makeText(getContext(), indicesToRemove.size() + "장의 사진이 여행 기간 밖에 촬영되어 저장되지 않았습니다.", Toast.LENGTH_LONG).show();
        }
        List<String> countries = new ArrayList<>();
        List<String> cities = new ArrayList<>();

        int photoCount = currentPostItem.getPhotoCount();
        for (int i = 0; i < photoCount; i++) {
            Double latObj = currentPostItem.getImageLatitude(i);
            Double lonObj = currentPostItem.getImageLongitude(i);
            
            if (latObj == null || lonObj == null) {
                continue;
            }
            
            double lat = latObj;
            double lon = lonObj;

            // 미리 만들어두신 getCountryAndCity 함수 활용
            try {
                String[] locationInfo = getCountryAndCity(lat, lon);
                // null이나 "어딘가에서"가 아닌 경우만 추가
                if (locationInfo != null && locationInfo.length >= 2) {
                    if (locationInfo[0] != null && !locationInfo[0].isEmpty() && !locationInfo[0].equals("어딘가에서")) {
                        if (!countries.contains(locationInfo[0])) {
                            countries.add(locationInfo[0]); // 국가명
                        }
                    }
                    if (locationInfo[1] != null && !locationInfo[1].isEmpty() && !locationInfo[1].equals("어딘가에서")) {
                        if (!cities.contains(locationInfo[1])) {
                            cities.add(locationInfo[1]);    // 도시명
                        }
                    }
                }
            } catch (Exception e) {
                android.util.Log.e("WriteFragment", "Error getting location info", e);
                // 계속 진행
            }
        }

        // Firestore에 리스트 형태로 저장
        Map<String, Object> post = new HashMap<>();
        post.put("title", currentPostItem.getTitle());
        post.put("images", currentPostItem.getImages());
        post.put("imageDays", currentPostItem.getImageDays());
        post.put("imageThumbnails", currentPostItem.getImageThumbnails());
        post.put("imageDates", currentPostItem.getImageDates());
        post.put("imageLatitudes", currentPostItem.getImageLatitudes());
        post.put("imageLongitudes", currentPostItem.getImageLongitudes());
        post.put("imageLocations", currentPostItem.getImageLocations());
        post.put("countries", countries);
        post.put("cities", cities);
        post.put("startDate", currentPostItem.getStartDate());
        post.put("travelDays", currentPostItem.getTravelDays());
        post.put("isPublic", currentPostItem.getIsPublic());
        post.put("userId", currentPostItem.getUserId());
        post.put("userEmail", currentPostItem.getUserEmail());

        if (editPostId != null) {
            db.collection("TravelPosts").document(editPostId)
                    .set(post, SetOptions.merge())
                    .addOnSuccessListener(aVoid -> {
                        isSaving = false;
                        if (isAdded() && getContext() != null) {
                            hideProgressDialog();
                            Toast.makeText(getContext(), "여행 기록이 수정되었습니다!", Toast.LENGTH_SHORT).show();
                            goToMainList();
                        }
                    })
                    .addOnFailureListener(e -> {
                        isSaving = false;
                        android.util.Log.e("WriteFragment", "Error updating post", e);
                        if (isAdded() && getContext() != null) {
                            hideProgressDialog();
                            String errorMsg = e.getMessage();
                            if (errorMsg == null || errorMsg.isEmpty()) {
                                errorMsg = "알 수 없는 오류가 발생했습니다.";
                            }
                            Toast.makeText(getContext(), "수정 실패: " + errorMsg, Toast.LENGTH_SHORT).show();
                        }
                    });
        } else {
            post.put("createdAt", FieldValue.serverTimestamp());
            db.collection("TravelPosts")
                    .add(post)
                    .addOnSuccessListener(documentReference -> {
                        isSaving = false;
                        if (isAdded() && getContext() != null) {
                            hideProgressDialog();
                            Toast.makeText(getContext(), "여행 기록이 성공적으로 저장되었습니다!", Toast.LENGTH_SHORT).show();
                            goToMainList();
                        }
                    })
                    .addOnFailureListener(e -> {
                        isSaving = false;
                        android.util.Log.e("WriteFragment", "Error saving post", e);
                        if (isAdded() && getContext() != null) {
                            hideProgressDialog();
                            String errorMsg = e.getMessage();
                            if (errorMsg == null || errorMsg.isEmpty()) {
                                errorMsg = "알 수 없는 오류가 발생했습니다.";
                            }
                            Toast.makeText(getContext(), "저장 실패: " + errorMsg, Toast.LENGTH_SHORT).show();
                        }
                    });
        }
    }
    private String getAddressFromLocation(double lat, double lon) {
        if (lat == 0.0 && lon == 0.0) return "어딘가에서";

        Geocoder geocoder = new Geocoder(getContext(), Locale.KOREA);
        try {
            List<Address> addresses = geocoder.getFromLocation(lat, lon, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                return address.toString();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "주소 변환 실패";
    }
    private String[] getCountryAndCity(double lat, double lon) {
        String country = null;
        String city = null;

        if (getContext() == null || (!Geocoder.isPresent())) {
            return new String[]{country, city};
        }

        if (lat != 0.0 || lon != 0.0) {
            try {
                Geocoder geocoder = new Geocoder(getContext(), Locale.getDefault());
                List<Address> addresses = geocoder.getFromLocation(lat, lon, 1);
                if (addresses != null && !addresses.isEmpty()) {
                    Address address = addresses.get(0);

                    country = address.getCountryName();
                    if (country != null && country.isEmpty()) {
                        country = null;
                    }

                    city = address.getLocality();
                    if (city == null || city.isEmpty()) {
                        city = address.getAdminArea();
                    }
                    if (city == null || city.isEmpty()) {
                        city = address.getSubAdminArea();
                    }
                    if (city != null && city.isEmpty()) {
                        city = null;
                    }
                }
            } catch (IOException e) {
                android.util.Log.e("WriteFragment", "Geocoder error", e);
            } catch (Exception e) {
                android.util.Log.e("WriteFragment", "Unexpected error in getCountryAndCity", e);
            }
        }
        return new String[]{country, city};
    }

    private void goToMainList() {
        if (getActivity() == null || !isAdded()) return;
        
        try {
            isSaving = false;
            hideProgressDialog();
            
            ListFragment listFragment = new ListFragment();
            androidx.fragment.app.FragmentManager fm = getActivity().getSupportFragmentManager();
            
            // 백스택 정리
            int backStackCount = fm.getBackStackEntryCount();
            if (backStackCount > 0) {
                fm.popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE);
            }
            
            // Fragment 교체
            fm.beginTransaction()
                    .replace(R.id.fragment_container, listFragment)
                    .commitNowAllowingStateLoss();
            
            // 네비게이션 바 상태 업데이트
            BottomNavigationView bottomNav = getActivity().findViewById(R.id.bottom_navigation);
            if (bottomNav != null) {
                bottomNav.getMenu().findItem(R.id.nav_my_list).setChecked(true);
            }
            
            // 타이틀 설정
            if (getActivity() instanceof androidx.appcompat.app.AppCompatActivity) {
                ((androidx.appcompat.app.AppCompatActivity) getActivity()).setTitle("My Feed");
            }
        } catch (Exception e) {
            android.util.Log.e("WriteFragment", "Error in goToMainList", e);
            if (getActivity() != null) {
                Toast.makeText(getContext(), "화면 전환 중 오류가 발생했습니다.", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void showProgressDialog() {
        if (progressDialog == null) {
            androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(getContext());
            builder.setCancelable(false);
            builder.setView(R.layout.loading);
            progressDialog = builder.create();
        }
        progressDialog.show();
    }

    private void hideProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }
    public boolean hasChanges() {
        if (isSaving) {
            return false;
        }

        String title = (editTripTitle != null) ? editTripTitle.getText().toString().trim() : "";
        return !title.isEmpty() || (currentPostItem != null && currentPostItem.getPhotoCount() > 0);
    }
    
    public boolean isSaving() {
        return isSaving;
    }
    
    private void showTravelInfoDialog() {
        if (getContext() == null) return;
        
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(getContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_travel_info, null);
        builder.setView(dialogView);
        
        android.widget.EditText editTravelDays = dialogView.findViewById(R.id.edit_travel_days);
        android.widget.DatePicker datePicker = dialogView.findViewById(R.id.date_picker_start);
        
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
                .setPositiveButton("확인", null)
                .setNegativeButton("취소", null)
                .create();
        
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
                
                Calendar calendar = Calendar.getInstance();
                calendar.set(datePicker.getYear(), datePicker.getMonth(), datePicker.getDayOfMonth());
                Date newStartDate = calendar.getTime();
                
                showTravelInfoConfirmationDialog(newStartDate, newTravelDays, dialog);
            });
            
            android.widget.Button negativeButton = dialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE);
            negativeButton.setOnClickListener(v -> {
                dialog.dismiss();
            });
        });
        
        dialog.show();
    }
    
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
        
        List<Integer> indicesToRemove = new ArrayList<>();
        int count = currentPostItem.getPhotoCount();
        for (int i = 0; i < count; i++) {
            Date photoDate = currentPostItem.getImageDate(i);
            if (photoDate != null) {
                Calendar photoCal = Calendar.getInstance();
                photoCal.setTime(photoDate);
                photoCal.set(Calendar.HOUR_OF_DAY, 0);
                photoCal.set(Calendar.MINUTE, 0);
                photoCal.set(Calendar.SECOND, 0);
                photoCal.set(Calendar.MILLISECOND, 0);
                
                if (photoCal.getTimeInMillis() < startCal.getTimeInMillis() || 
                    photoCal.getTimeInMillis() > endCal.getTimeInMillis()) {
                    indicesToRemove.add(i);
                }
            }
        }
        
        // 역순으로 제거 (인덱스 변경 방지)
        for (int i = indicesToRemove.size() - 1; i >= 0; i--) {
            currentPostItem.removePhoto(indicesToRemove.get(i));
        }
        
        if (!indicesToRemove.isEmpty()) {
            photoAdapter.updateAdapterItems();
            Toast.makeText(getContext(), indicesToRemove.size() + "장의 사진이 여행 기간 밖에 촬영되어 제거되었습니다.", Toast.LENGTH_LONG).show();
        }
    }
    
    private void showTravelInfoConfirmationDialog(Date newStartDate, int newTravelDays, android.app.AlertDialog previousDialog) {
        if (getContext() == null) return;
        
        Calendar endCal = Calendar.getInstance();
        endCal.setTime(newStartDate);
        endCal.add(Calendar.DAY_OF_MONTH, newTravelDays - 1);
        
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy년 MM월 dd일", Locale.getDefault());
        String startDateStr = sdf.format(newStartDate);
        String endDateStr = sdf.format(endCal.getTime());
        
        int nights = newTravelDays - 1;
        String message = startDateStr + "부터 " + endDateStr + "까지(" + nights + "박" + newTravelDays + "일, " + newTravelDays + "일차) 여행이 맞습니까?";
        
        new android.app.AlertDialog.Builder(getContext())
                .setTitle("여행 일정 확인")
                .setMessage(message)
                .setPositiveButton("맞습니다", (d, which) -> {
                    startDate = newStartDate;
                    travelDays = newTravelDays;
                    
                    int count = currentPostItem.getPhotoCount();
                    for (int i = 0; i < count; i++) {
                        calculateDayNumber(i);
                    }
                    
                    sortPhotosByDay();
                    photoAdapter.updateAdapterItems();
                    removePhotosOutsideTravelPeriod();
                    updateTravelPeriodText();
                    
                    Toast.makeText(getContext(), "여행 일정이 변경되었습니다.", Toast.LENGTH_SHORT).show();
                    previousDialog.dismiss();
                })
                .setNegativeButton("수정하기", (d, which) -> {
                })
                .setCancelable(false)
                .show();
    }
    
    private void updateTravelPeriodText() {
        if (textTravelPeriod == null) return;
        
        if (startDate != null && travelDays > 0) {
            Calendar endCal = Calendar.getInstance();
            endCal.setTime(startDate);
            endCal.add(Calendar.DAY_OF_MONTH, travelDays - 1);
            
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy년 MM월 dd일", Locale.getDefault());
            String startDateStr = sdf.format(startDate);
            String endDateStr = sdf.format(endCal.getTime());
            
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



