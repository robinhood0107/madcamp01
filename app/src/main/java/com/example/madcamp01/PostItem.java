package com.example.madcamp01;

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

    // --- [1] 이미지 및 일차 정보 (병렬 리스트 구조) ---
    // 모든 리스트는 images의 인덱스와 1:1로 대응됩니다.
    // 예: images[0]의 썸네일은 imageThumbnails[0], 좌표는 Lat[0]/Lon[0]
    
    private List<String> images;          // 원본 이미지 URL 리스트
    private List<String> imageDays;       // 일차 정보 ("1", "2"...)
    private List<String> imageThumbnails; // [변경] 각 사진별 썸네일 URL 리스트
    private List<Date> imageDates;        // [신규] 각 사진별 정확한 촬영 시각 리스트
    
    private List<Double> imageLatitudes;  // [변경] 각 사진별 위도 리스트 (없으면 null)
    private List<Double> imageLongitudes; // [변경] 각 사진별 경도 리스트 (없으면 null)

    // --- [2] 여행 기간 정보 ---
    private Date startDate;
    private int travelDays;

    // --- [3] 메타 데이터 ---
    @PropertyName("isPublic")
    private boolean isPublic;

    @ServerTimestamp
    private Date createdAt;

    public PostItem() {
        // Firestore용 빈 생성자
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

    // [신규 리스트 Getter/Setter]
    public List<String> getImageThumbnails() { return imageThumbnails; }
    public void setImageThumbnails(List<String> imageThumbnails) { this.imageThumbnails = imageThumbnails; }

    public List<Date> getImageDates() { return imageDates; }
    public void setImageDates(List<Date> imageDates) { this.imageDates = imageDates; }

    public List<Double> getImageLatitudes() { return imageLatitudes; }
    public void setImageLatitudes(List<Double> imageLatitudes) { this.imageLatitudes = imageLatitudes; }

    public List<Double> getImageLongitudes() { return imageLongitudes; }
    public void setImageLongitudes(List<Double> imageLongitudes) { this.imageLongitudes = imageLongitudes; }

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


    // --- Parcelable 구현 ---
    
    // 1. 읽기 (Constructor) - 순서 중요!
    @SuppressWarnings("unchecked")
    protected PostItem(Parcel in) {
        documentId = in.readString();
        userId = in.readString();
        userEmail = in.readString();
        title = in.readString();
        
        // String List 읽기
        images = in.createStringArrayList();
        imageDays = in.createStringArrayList();
        imageThumbnails = in.createStringArrayList(); // [신규]

        // Date List 읽기 (Serializable로 처리)
        imageDates = (ArrayList<Date>) in.readSerializable();

        // Double List 읽기 (Serializable로 처리)
        // Firestore에서 가져온 List<Double>을 Parcelable로 넘길 때 가장 안전한 방법입니다.
        imageLatitudes = (ArrayList<Double>) in.readSerializable();
        imageLongitudes = (ArrayList<Double>) in.readSerializable();
        
        // Date & int
        long tmpStartDate = in.readLong();
        startDate = tmpStartDate == -1 ? null : new Date(tmpStartDate);
        travelDays = in.readInt();

        // Boolean & Date
        isPublic = in.readByte() != 0;
        long tmpCreatedAt = in.readLong();
        createdAt = tmpCreatedAt == -1 ? null : new Date(tmpCreatedAt);
    }

    // 2. 쓰기 (writeToParcel) - 순서 중요!
    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(documentId);
        dest.writeString(userId);
        dest.writeString(userEmail);
        dest.writeString(title);
        
        // String List 쓰기
        dest.writeStringList(images);
        dest.writeStringList(imageDays);
        dest.writeStringList(imageThumbnails); // [신규]
        
        // Date List 쓰기 (Serializable로 처리)
        dest.writeSerializable((ArrayList<Date>) imageDates);
        
        // Double List 쓰기 (Serializable로 처리)
        // ArrayList는 Serializable 인터페이스를 구현하고 있으므로 통째로 넘길 수 있습니다.
        dest.writeSerializable((ArrayList<Double>) imageLatitudes);
        dest.writeSerializable((ArrayList<Double>) imageLongitudes);
        
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
