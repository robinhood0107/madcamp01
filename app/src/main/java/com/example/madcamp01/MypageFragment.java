package com.example.madcamp01;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MypageFragment extends Fragment {

    private FirebaseAuth mAuth;
    private TextView txtUserEmail, txtJoinDate;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_mypage, container, false);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();

        txtUserEmail = view.findViewById(R.id.txt_user_email);
        txtJoinDate = view.findViewById(R.id.txt_join_date);
        Button btnLogout = view.findViewById(R.id.btn_logout);
        Button btnDeleteAccount = view.findViewById(R.id.btn_delete_account);

        // 1. 사용자 정보 표시
        if (user != null) {
            txtUserEmail.setText("이메일: " + user.getEmail());

            // 가입 날짜 변환 (long -> 날짜 형식)
            long creationTimestamp = user.getMetadata().getCreationTimestamp();
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy년 MM월 dd일", Locale.KOREA);
            txtJoinDate.setText("가입 날짜: " + sdf.format(new Date(creationTimestamp)));
        }

        // 2. 로그아웃 기능
        btnLogout.setOnClickListener(v -> {
            mAuth.signOut();
            Toast.makeText(getContext(), "로그아웃 되었습니다.", Toast.LENGTH_SHORT).show();
            // 로그인 화면으로 이동
            Intent intent = new Intent(getActivity(), LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
        });

        // 3. 회원 탈퇴 기능 (확인 팝업 띄우기)
        btnDeleteAccount.setOnClickListener(v -> {
            new AlertDialog.Builder(getContext())
                    .setTitle("회원 탈퇴")
                    .setMessage("정말로 탈퇴하시겠습니까? 모든 기록이 삭제됩니다.")
                    .setPositiveButton("탈퇴", (dialog, which) -> {
                        if (user != null) {
                            user.delete().addOnCompleteListener(task -> {
                                if (task.isSuccessful()) {
                                    Toast.makeText(getContext(), "탈퇴되었습니다.", Toast.LENGTH_SHORT).show();
                                    Intent intent = new Intent(getActivity(), LoginActivity.class);
                                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                    startActivity(intent);
                                } else {
                                    Toast.makeText(getContext(), "탈퇴 실패: 다시 로그인 후 시도해주세요.", Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    })
                    .setNegativeButton("취소", null)
                    .show();
        });

        return view;
    }
}