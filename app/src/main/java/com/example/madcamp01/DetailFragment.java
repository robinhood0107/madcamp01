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

    // 프래그먼트 생성 시 데이터 받기
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            //getParcelable 사용 시 클래스 타입을 명시.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                postItem = getArguments().getParcelable("postData", PostItem.class);
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_detail, container, false);

        // 1. 뷰 연결
        tvTitle = view.findViewById(R.id.tvDetailTitle);
        detailRecyclerView = view.findViewById(R.id.detailRecyclerView);

        btnSetting = view.findViewById(R.id.btnDetailSetting);
        popupCard = view.findViewById(R.id.detailControlPopup);
        seekBar = view.findViewById(R.id.detailSeekBar);
        tvColCount = view.findViewById(R.id.tvDetailColCount);

        // 2. 리사이클러뷰 설정 (기본 1열: 큰 사진 보기)
        layoutManager = new GridLayoutManager(getContext(), 1);
        detailRecyclerView.setLayoutManager(layoutManager);

        // 데이터 설정
        //footer SpanSizeLookup으로 GridLayoutManager에게 푸터만 예외적으로 가로를 꽉 채우기
        List<String> images = new ArrayList<>();
        if (postItem != null) {
            tvTitle.setText(postItem.getTitle());
            if (postItem.getImages() != null) {
                images = postItem.getImages();
            }
        }
        // (중요) 어댑터를 먼저 만들어서 변수에 할당합니다.
        detailAdapter = new DetailAdapter(getContext(), images);


        // 2. 레이아웃 매니저 설정 및 SpanSizeLookup 적용
        layoutManager = new GridLayoutManager(getContext(), 1); // 처음엔 1열로 시작

        //푸터를 항상 가로로 꽉 채우게 setSpanSizeLookup 적용
        layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                // 현재 위치(position)의 아이템 타입을 확인합니다.
                int viewType = detailAdapter.getItemViewType(position);

                // 만약 아이템 타입이 '푸터(FOOTER)'라면?
                if (viewType == DetailAdapter.VIEW_TYPE_FOOTER) {
                    // 현재 설정된 전체 열 개수만큼 자리를 차지해라! (즉, 꽉 채워라)
                    // 예: 3열 모드면 3칸을 차지, 5열 모드면 5칸을 차지
                    return layoutManager.getSpanCount();
                }

                // 일반 이미지 아이템이라면?
                // 평소처럼 1칸만 차지해라.
                return 1;
            }
        });

        // 레이아웃 매니저와 어댑터를 리사이클러뷰에 연결
        detailRecyclerView.setLayoutManager(layoutManager);
        detailRecyclerView.setAdapter(detailAdapter);

        //---여기까지가 setSpanSizeLookup



        // 3. 설정 버튼 클릭 -> 팝업 토글 (보였다 안보였다)
        btnSetting.setOnClickListener(v -> {
            if (popupCard.getVisibility() == View.VISIBLE) {
                popupCard.setVisibility(View.GONE);
            } else {
                popupCard.setVisibility(View.VISIBLE);
                popupCard.setAlpha(0f);
                popupCard.animate().alpha(1f).setDuration(200).start();
            }
        });

        // 4. 슬라이더 움직임 감지
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                // progress: 0 ~ 4
                // spanCount: 1 ~ 5
                int spanCount = progress + 1;

                tvColCount.setText(spanCount + "열");

                // 열 개수 즉시 변경
                layoutManager.setSpanCount(spanCount);

                // 화면 갱신 (이미지 재배치)
                detailAdapter.notifyItemRangeChanged(0, detailAdapter.getItemCount());
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });





        return view;
    }
}