package com.example.madcamp01;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.fragment.app.Fragment;

public class WriteFragment extends Fragment { // 클래스 이름

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // 이 프래그먼트와 연결된 화면(XML)을 불러오는 코드

        return inflater.inflate(R.layout.fragment_write, container, false);
    }
}