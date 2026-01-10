package com.example.madcamp01;

import android.content.Context;
import android.util.AttributeSet;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

// 스스로 정사각형 비율을 유지하는 커스텀 ImageView
public class SquareImageView extends androidx.appcompat.widget.AppCompatImageView {

    public SquareImageView(@NonNull Context context) {
        super(context);
    }

    public SquareImageView(@NonNull Context context, @Nullable AttributeSet attrs) {
        super(context, attrs);
    }

    public SquareImageView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        // 부모로부터 받은 가로 크기(widthMeasureSpec)를 세로 크기에도 똑같이 적용하여 정사각형을 만듭니다.
        super.onMeasure(widthMeasureSpec, widthMeasureSpec);
    }
}
