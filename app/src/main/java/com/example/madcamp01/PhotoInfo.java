package com.example.madcamp01;

import android.net.Uri;
import java.util.Date;

/**
 * 사진 정보를 담는 클래스
 * EXIF 데이터와 일차 정보를 포함
 */
public class PhotoInfo {
    private Uri uri;
    private Date photoDate;  // EXIF에서 추출한 촬영 날짜
    private Double latitude;  // 위도
    private Double longitude; // 경도
    private String location;  // 위치
    private String dayNumber; // 일차 ("1", "2", "3"...)
    private String imageUrl;   // 업로드 후 URL
    private String thumbnailUrl; // 썸네일 URL

    public PhotoInfo(Uri uri) {
        this.uri = uri;
    }

    public Uri getUri() {
        return uri;
    }

    public void setUri(Uri uri) {
        this.uri = uri;
    }

    public Date getPhotoDate() {
        return photoDate;
    }

    public void setPhotoDate(Date photoDate) {
        this.photoDate = photoDate;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getDayNumber() {
        return dayNumber;
    }

    public void setDayNumber(String dayNumber) {
        this.dayNumber = dayNumber;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getThumbnailUrl() {
        return thumbnailUrl;
    }

    public void setThumbnailUrl(String thumbnailUrl) {
        this.thumbnailUrl = thumbnailUrl;
    }
}
