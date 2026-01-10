package com.example.madcamp01;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.github.chrisbanes.photoview.PhotoView;

public class FullScreenImageFragment extends Fragment {

    private static final String ARG_IMAGE_URL = "image_url";

    public static FullScreenImageFragment newInstance(String imageUrl) {
        FullScreenImageFragment fragment = new FullScreenImageFragment();
        Bundle args = new Bundle();
        args.putString(ARG_IMAGE_URL, imageUrl);
        fragment.setArguments(args);
        return fragment;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_full_screen_image, container, false);

        // 1. 뷰 연결 (ImageView -> PhotoView)
        PhotoView photoView = view.findViewById(R.id.photoView);
        ImageButton btnClose = view.findViewById(R.id.btnClose);

        // 2. 이미지 로드
        if (getArguments() != null) {
            String imageUrl = getArguments().getString(ARG_IMAGE_URL);
            if (imageUrl != null) {
                Glide.with(this).load(imageUrl).into(photoView);
            }
        }

        // 3. 닫기 버튼 클릭 리스너 설정
        btnClose.setOnClickListener(v -> {
            // 부모 FragmentManager를 호출하여 현재 프래그먼트를 스택에서 제거 (즉, 이전 화면으로 돌아가기)
            getParentFragmentManager().popBackStack();
        });

        return view;
    }
}