package com.example.madcamp01;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.PropertyName;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;
import java.util.List;

public class PostItem implements Parcelable {
    @DocumentId
    private String documentId;
    private String userId;
    private String userEmail;
    private String title;
    private List<String> images;

    // [핵심 수정 1] Firestore가 필드 이름을 명확히 인식하도록 @PropertyName 사용
    @PropertyName("isPublic")
    private boolean isPublic;

    @ServerTimestamp
    private Date createdAt;

    public PostItem() {
        // Firestore 매핑을 위한 빈 생성자
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

    // [핵심 수정 2] Getter와 Setter 이름을 표준 형식으로 통일합니다.
    @PropertyName("isPublic")
    public boolean getIsPublic() { return isPublic; }

    @PropertyName("isPublic")
    public void setIsPublic(boolean isPublic) { this.isPublic = isPublic; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }


    // --- Parcelable 구현 (isPublic 관련 수정 없음) ---
    protected PostItem(Parcel in) {
        documentId = in.readString();
        userId = in.readString();
        userEmail = in.readString();
        title = in.readString();
        images = in.createStringArrayList();
        isPublic = in.readByte() != 0;
        long tmpCreatedAt = in.readLong();
        createdAt = tmpCreatedAt == -1 ? null : new Date(tmpCreatedAt);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(documentId);
        dest.writeString(userId);
        dest.writeString(userEmail);
        dest.writeString(title);
        dest.writeStringList(images);
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