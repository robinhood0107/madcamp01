package com.example.madcamp01;

import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

public class DetailFragment extends Fragment {

    private RecyclerView detailRecyclerView;
    private DetailAdapter detailAdapter;
    private PostItem postItem;

    // UI 컨트롤 변수
    private TextView tvTitle;
    private ImageButton btnSetting;
    private CardView popupCard;
    private SeekBar seekBar;
    private TextView tvColCount;
    private GridLayoutManager layoutManager;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                postItem = getArguments().getParcelable("postData", PostItem.class);
            } else {
                postItem = getArguments().getParcelable("postData");
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_detail, container, false);

        tvTitle = view.findViewById(R.id.tvDetailTitle);
        detailRecyclerView = view.findViewById(R.id.detailRecyclerView);
        btnSetting = view.findViewById(R.id.btnDetailSetting);
        popupCard = view.findViewById(R.id.detailControlPopup);
        seekBar = view.findViewById(R.id.detailSeekBar);
        tvColCount = view.findViewById(R.id.tvDetailColCount);

        List<String> images = new ArrayList<>();
        if (postItem != null) {
            tvTitle.setText(postItem.getTitle());
            if (postItem.getImages() != null) {
                images = postItem.getImages();
            }
        }
        detailAdapter = new DetailAdapter(getContext(), images);
        detailAdapter.setPostItem(postItem);
        detailAdapter.setSpanCount(2); // 초기값 2열 설정

        // --- 클릭 리스너 설정 (전체 화면 보기) ---
        detailAdapter.setOnItemClickListener(position -> {
            String clickedImageUrl = postItem.getImages().get(position);
            FullScreenImageFragment fullScreenFragment = FullScreenImageFragment.newInstance(clickedImageUrl);

            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, fullScreenFragment)
                    .addToBackStack(null)
                    .commit();
        });

        // --- 레이아웃 매니저 및 SpanSizeLookup 복원 ---
        layoutManager = new GridLayoutManager(getContext(), 2); // 초기 2열 (2x2 배열)
        layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                // 어댑터의 아이템 타입에 따라 칸 수 조절
                int viewType = detailAdapter.getItemViewType(position);
                if (viewType == DetailAdapter.VIEW_TYPE_FOOTER) {
                    // 푸터는 현재 설정된 열 개수를 모두 차지
                    return layoutManager.getSpanCount();
                } else if (viewType == DetailAdapter.VIEW_TYPE_DAY_HEADER) {
                    // 일차 헤더는 현재 설정된 열 개수를 모두 차지
                    return layoutManager.getSpanCount();
                }
                // 이미지는 1칸만 차지
                return 1;
            }
        });

        detailRecyclerView.setLayoutManager(layoutManager);
        detailRecyclerView.setAdapter(detailAdapter);

        // --- UI 컨트롤 로직 복원 ---
        btnSetting.setOnClickListener(v -> {
            if (popupCard.getVisibility() == View.VISIBLE) {
                popupCard.setVisibility(View.GONE);
            } else {
                popupCard.setVisibility(View.VISIBLE);
            }
        });

        // 초기값을 2열(2x2)로 설정
        seekBar.setProgress(1);
        tvColCount.setText("2열");
        
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int spanCount = progress + 1;
                tvColCount.setText(spanCount + "열");
                layoutManager.setSpanCount(spanCount);
                // 어댑터에 열 개수 전달 (4열 이상일 때 날짜 숨김)
                detailAdapter.setSpanCount(spanCount);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        return view;
    }
}