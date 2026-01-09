package com.example.madcamp01;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.fragment.app.Fragment;

public class GalleryFragment extends Fragment { // 클래스 이름
    //전체 갤러리

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // fragment_gallery.xml을 보여줌
        return inflater.inflate(R.layout.fragment_gallery, container, false);
    }
}
