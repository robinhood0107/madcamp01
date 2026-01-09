package com.example.madcamp01;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.fragment.app.Fragment;

public class ListFragment extends Fragment { // 클래스 이름

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // fragment_list.xml을 보여줌
        return inflater.inflate(R.layout.fragment_list, container, false);
    }
}