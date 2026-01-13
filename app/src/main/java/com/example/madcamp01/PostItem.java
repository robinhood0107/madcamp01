package com.example.madcamp01;

import android.net.Uri;
import android.os.Parcel;
import android.os.Parcelable;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.PropertyName;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class PostItem implements Parcelable {
    @DocumentId
    private String documentId;
    private String userId;
    private String userEmail;
    private String title;

    // 이미지 및 일차 정보 (병렬 리스트 구조 - 모든 리스트는 images의 인덱스와 1:1 대응)
    private List<String> images;
    private List<String> imageDays;
    private List<String> imageThumbnails;
    private List<Date> imageDates;
    private List<Double> imageLatitudes;
    private List<Double> imageLongitudes;
    private List<String> imageLocations;
    private List<String> imageCountries;
    private List<String> imageCities;
    private transient List<Uri> localOnlyImageUris; // 로컬 작업용 (Firebase 저장 안됨)

    // 여행 기간 정보
    private Date startDate;
    private int travelDays;

    // 메타 데이터
    @PropertyName("isPublic")
    private boolean isPublic;

    @ServerTimestamp
    private Date createdAt;

    public PostItem() {
        initializeLists();
    }
    
    private void initializeLists() {
        if (images == null) images = new ArrayList<>();
        if (imageDays == null) imageDays = new ArrayList<>();
        if (imageThumbnails == null) imageThumbnails = new ArrayList<>();
        if (imageDates == null) imageDates = new ArrayList<>();
        if (imageLatitudes == null) imageLatitudes = new ArrayList<>();
        if (imageLongitudes == null) imageLongitudes = new ArrayList<>();
        if (imageLocations == null) imageLocations = new ArrayList<>();
        if (localOnlyImageUris == null) localOnlyImageUris = new ArrayList<>();
    }

    // --- Getter & Setter ---
    public String getDocumentId() { return documentId; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public List<String> getImages() { return images; }
    public void setImages(List<String> images) { this.images = images; }
    public List<String> getImageDays() { return imageDays; }
    public void setImageDays(List<String> imageDays) { this.imageDays = imageDays; }
    public List<String> getImageThumbnails() { return imageThumbnails; }
    public void setImageThumbnails(List<String> imageThumbnails) { this.imageThumbnails = imageThumbnails; }
    public List<Date> getImageDates() { return imageDates; }
    public void setImageDates(List<Date> imageDates) { this.imageDates = imageDates; }
    public List<Double> getImageLatitudes() { return imageLatitudes; }
    public void setImageLatitudes(List<Double> imageLatitudes) { this.imageLatitudes = imageLatitudes; }
    public List<Double> getImageLongitudes() { return imageLongitudes; }
    public void setImageLongitudes(List<Double> imageLongitudes) { this.imageLongitudes = imageLongitudes; }
    public List<String> getImageLocations() { return imageLocations; }
    public void setImageLocations(List<String> imageLocations) { this.imageLocations = imageLocations; }

    public List<Uri> getLocalOnlyImageUris() { 
        if (localOnlyImageUris == null) localOnlyImageUris = new ArrayList<>();
        return localOnlyImageUris; 
    }
    public void setLocalOnlyImageUris(List<Uri> localOnlyImageUris) { this.localOnlyImageUris = localOnlyImageUris; }

    public Date getStartDate() { return startDate; }
    public void setStartDate(Date startDate) { this.startDate = startDate; }
    public int getTravelDays() { return travelDays; }
    public void setTravelDays(int travelDays) { this.travelDays = travelDays; }
    @PropertyName("isPublic")
    public boolean getIsPublic() { return isPublic; }
    @PropertyName("isPublic")
    public void setIsPublic(boolean isPublic) { this.isPublic = isPublic; }
    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }
    public void setImageCountries(List<String> imageCountries) { this.imageCountries = imageCountries; }
    public void setImageCities(List<String> imageCities) { this.imageCities = imageCities; }
    public List<String> getImageCountries() { return imageCountries; }
    public List<String> getImageCities() { return imageCities;}

    // --- 헬퍼 메서드: 인덱스 기반 접근 ---
    
    /**
     * 인덱스로 특정 사진의 로컬 Uri를 반환 (Firebase에 저장되지 않음).
     */
    public Uri getImageUri(int index) {
        if (localOnlyImageUris == null || index < 0 || index >= localOnlyImageUris.size()) {
            return null;
        }
        return localOnlyImageUris.get(index);
    }
    
    public Date getImageDate(int index) {
        if (imageDates == null || index < 0 || index >= imageDates.size()) {
            return null;
        }
        return imageDates.get(index);
    }
    
    public Double getImageLatitude(int index) {
        if (imageLatitudes == null || index < 0 || index >= imageLatitudes.size()) {
            return null;
        }
        return imageLatitudes.get(index);
    }
    
    public Double getImageLongitude(int index) {
        if (imageLongitudes == null || index < 0 || index >= imageLongitudes.size()) {
            return null;
        }
        return imageLongitudes.get(index);
    }
    
    public String getImageDay(int index) {
        if (imageDays == null || index < 0 || index >= imageDays.size()) {
            return "1";
        }
        return imageDays.get(index);
    }
    
    public String getImageUrl(int index) {
        if (images == null || index < 0 || index >= images.size()) {
            return null;
        }
        return images.get(index);
    }
    
    public String getImageThumbnailUrl(int index) {
        if (imageThumbnails == null || index < 0 || index >= imageThumbnails.size()) {
            return null;
        }
        return imageThumbnails.get(index);
    }
    
    public String getImageLocation(int index) {
        if (imageLocations == null || index < 0 || index >= imageLocations.size()) {
            return null;
        }
        return imageLocations.get(index);
    }
    
    /**
     * 모든 병렬 리스트에 사진 정보를 동시에 추가.
     * @return 추가된 사진의 인덱스
     */
    public int addPhoto(Uri uri, Date photoDate, Double latitude, Double longitude, 
                       String dayNumber, String imageUrl, String thumbnailUrl, String location) {
        initializeLists();
        
        int index = images.size();
        
        if (uri != null) {
            localOnlyImageUris.add(uri);
        } else {
            localOnlyImageUris.add(null);
        }
        images.add(imageUrl != null ? imageUrl : "");
        imageDays.add(dayNumber != null ? dayNumber : "1");
        imageThumbnails.add(thumbnailUrl != null ? thumbnailUrl : (imageUrl != null ? imageUrl : ""));
        imageDates.add(photoDate != null ? photoDate : new Date());
        imageLatitudes.add(latitude);
        imageLongitudes.add(longitude);
        imageLocations.add(location);
        
        return index;
    }
    
    /**
     * 모든 병렬 리스트에서 특정 인덱스의 사진을 동시에 삭제.
     */
    public void removePhoto(int index) {
        if (index < 0 || index >= getPhotoCount()) {
            return;
        }
        
        if (localOnlyImageUris != null && index < localOnlyImageUris.size()) {
            localOnlyImageUris.remove(index);
        }
        if (images != null && index < images.size()) {
            images.remove(index);
        }
        if (imageDays != null && index < imageDays.size()) {
            imageDays.remove(index);
        }
        if (imageThumbnails != null && index < imageThumbnails.size()) {
            imageThumbnails.remove(index);
        }
        if (imageDates != null && index < imageDates.size()) {
            imageDates.remove(index);
        }
        if (imageLatitudes != null && index < imageLatitudes.size()) {
            imageLatitudes.remove(index);
        }
        if (imageLongitudes != null && index < imageLongitudes.size()) {
            imageLongitudes.remove(index);
        }
        if (imageLocations != null && index < imageLocations.size()) {
            imageLocations.remove(index);
        }
    }
    
    /**
     * 특정 인덱스의 사진 정보를 업데이트.
     */
    public void updatePhoto(int index, Date photoDate, Double latitude, Double longitude,
                           String dayNumber, String imageUrl, String thumbnailUrl, String location) {
        if (index < 0 || index >= getPhotoCount()) {
            return;
        }
        
        initializeLists();
        
        if (imageDates != null && index < imageDates.size() && photoDate != null) {
            imageDates.set(index, photoDate);
        }
        if (imageLatitudes != null && index < imageLatitudes.size()) {
            imageLatitudes.set(index, latitude);
        }
        if (imageLongitudes != null && index < imageLongitudes.size()) {
            imageLongitudes.set(index, longitude);
        }
        if (imageDays != null && index < imageDays.size() && dayNumber != null) {
            imageDays.set(index, dayNumber);
        }
        if (images != null && index < images.size() && imageUrl != null) {
            images.set(index, imageUrl);
        }
        if (imageThumbnails != null && index < imageThumbnails.size() && thumbnailUrl != null) {
            imageThumbnails.set(index, thumbnailUrl);
        }
        if (imageLocations != null && index < imageLocations.size()) {
            imageLocations.set(index, location);
        }
    }
    
    /**
     * 특정 인덱스의 로컬 Uri를 업데이트 (Firebase에 저장되지 않음).
     */
    public void setImageUri(int index, Uri uri) {
        initializeLists();
        while (localOnlyImageUris.size() <= index) {
            localOnlyImageUris.add(null);
        }
        if (index >= 0) {
            localOnlyImageUris.set(index, uri);
        }
    }
    
    public int getPhotoCount() {
        if (images == null) return 0;
        return images.size();
    }
    
    /**
     * 모든 병렬 리스트의 크기가 동기화되어 있는지 확인.
     */
    public boolean isListsSynchronized() {
        int size = getPhotoCount();
        return (imageDays == null || imageDays.size() == size) &&
               (imageThumbnails == null || imageThumbnails.size() == size) &&
               (imageDates == null || imageDates.size() == size) &&
               (imageLatitudes == null || imageLatitudes.size() == size) &&
               (imageLongitudes == null || imageLongitudes.size() == size) &&
               (imageLocations == null || imageLocations.size() == size);
    }

    // --- Parcelable 구현 ---
    
    /**
     * Parcelable에서 PostItem 객체를 복원 (읽기 순서는 writeToParcel과 동일해야 함).
     */
    @SuppressWarnings("unchecked")
    protected PostItem(Parcel in) {
        documentId = in.readString();
        userId = in.readString();
        userEmail = in.readString();
        title = in.readString();
        
        images = in.createStringArrayList();
        imageDays = in.createStringArrayList();
        imageThumbnails = in.createStringArrayList();
        imageDates = (ArrayList<Date>) in.readSerializable();
        imageLatitudes = (ArrayList<Double>) in.readSerializable();
        imageLongitudes = (ArrayList<Double>) in.readSerializable();
        imageLocations = (ArrayList<String>) in.readSerializable();
        
        long tmpStartDate = in.readLong();
        startDate = tmpStartDate == -1 ? null : new Date(tmpStartDate);
        travelDays = in.readInt();
        isPublic = in.readByte() != 0;
        long tmpCreatedAt = in.readLong();
        createdAt = tmpCreatedAt == -1 ? null : new Date(tmpCreatedAt);
        
        localOnlyImageUris = new ArrayList<>();
    }

    /**
     * PostItem 객체를 Parcelable로 직렬화 (쓰기 순서는 생성자와 동일해야 함).
     */
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(documentId);
        dest.writeString(userId);
        dest.writeString(userEmail);
        dest.writeString(title);
        dest.writeStringList(images);
        dest.writeStringList(imageDays);
        dest.writeStringList(imageThumbnails);
        dest.writeSerializable((ArrayList<Date>) imageDates);
        dest.writeSerializable((ArrayList<Double>) imageLatitudes);
        dest.writeSerializable((ArrayList<Double>) imageLongitudes);
        dest.writeSerializable((ArrayList<String>) imageLocations);
        dest.writeLong(startDate != null ? startDate.getTime() : -1);
        dest.writeInt(travelDays);
        dest.writeByte((byte) (isPublic ? 1 : 0));
        dest.writeLong(createdAt != null ? createdAt.getTime() : -1);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<PostItem> CREATOR = new Creator<PostItem>() {
        @Override
        public PostItem createFromParcel(Parcel in) {
            return new PostItem(in);
        }

        @Override
        public PostItem[] newArray(int size) {
            return new PostItem[size];
        }
    };
}
