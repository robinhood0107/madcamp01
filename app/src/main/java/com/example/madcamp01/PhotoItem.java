package com.example.madcamp01;

import androidx.annotation.Nullable;
import com.google.android.gms.maps.model.LatLng;
import com.google.maps.android.clustering.ClusterItem;

public class PhotoItem implements ClusterItem {
    private final LatLng position;
    private final String photoUrl;
    private final String postId;

    public PhotoItem(double lat, double lng, String photoUrl, String postId) {
        this.position = new LatLng(lat, lng);
        this.photoUrl = photoUrl;
        this.postId = postId;
    }

    @Override
    public LatLng getPosition() {
        return position;
    }

    @Nullable
    @Override
    public String getTitle() {
        return null;
    }

    @Nullable
    @Override
    public String getSnippet() {
        return null;
    }
    @Nullable
    @Override
    public Float getZIndex() {
        return null;
    }

    public String getPhotoUrl() {
        return photoUrl;
    }

    public String getPostId() {
        return postId;
    }

}
