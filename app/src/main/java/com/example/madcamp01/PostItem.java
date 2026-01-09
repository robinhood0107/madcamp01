package com.example.madcamp01;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.firebase.firestore.DocumentId;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;
import java.util.List;

//데이터 모델(PostItem)
//게시물 하나에 들어갈 이미지 주소와 제목 그리고 게시글에 대한 생성자
public class PostItem implements Parcelable {
    @DocumentId
    private String documentId;


    private List<String> images;
    private String title;

    @ServerTimestamp // Firestore 타임스탬프 매핑용 어노테이션
    private Date createdAt;      // 타임스탬프 대응

    //firebase에서는 빈 생성자가 없을 경우 에러가 발생하기 때문에 꼭 생성필요
    public PostItem() {
    }

    public PostItem(String title, List<String> images) {
        this.title = title;
        this.images = images;
    }


    // Getter 와 Setter 메서드들
    public String getDocumentId() { return documentId; }
    public void setDocumentId(String documentId) { this.documentId = documentId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public List<String> getImages() { return images; }
    public void setImages(List<String> images) { this.images = images; }

    public Date getCreatedAt() { return createdAt; }
    public void setCreatedAt(Date createdAt) { this.createdAt = createdAt; }

    //Parcelable 구현
    protected PostItem(Parcel in) {
        documentId = in.readString();
        title = in.readString();
        images = in.createStringArrayList();
        // Date는 long 타입(타임스탬프 숫자)으로 변환해서 주고받음
        long tmpCreatedAt = in.readLong();
        createdAt = tmpCreatedAt == -1 ? null : new Date(tmpCreatedAt);
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(documentId);
        dest.writeString(title);
        dest.writeStringList(images);
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
