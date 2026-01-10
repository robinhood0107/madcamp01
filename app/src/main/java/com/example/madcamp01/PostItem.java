package com.example.madcamp01;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;
import java.util.List;

public class PostItem implements Parcelable {
    @DocumentId
    private String documentId;
    private String userId;
    private String userEmail; // [추가] 사용자 이메일 필드
    private String title;
    private List<String> images;
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

    public String getUserEmail() { return userEmail; } // [추가]
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; } // [추가]

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public List<String> getImages() { return images; }
    public void setImages(List<String> images) { this.images = images; }

    public boolean isPublic() { return isPublic; }
    public void setPublic(boolean aPublic) { isPublic = aPublic; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }


    // --- Parcelable 구현 ---
    protected PostItem(Parcel in) {
        documentId = in.readString();
        userId = in.readString();
        userEmail = in.readString(); // [수정]
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
        dest.writeString(userEmail); // [수정]
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
