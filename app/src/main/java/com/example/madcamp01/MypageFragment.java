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
        // 3. 회원 탈퇴 기능 (비밀번호 입력 팝업 + 자동 재인증)
        btnDeleteAccount.setOnClickListener(v -> {
            // 비밀번호 입력을 위한 EditText 생성
            final android.widget.EditText passwordInput = new android.widget.EditText(getContext());
            passwordInput.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
            passwordInput.setHint("비밀번호를 입력하세요");

            new AlertDialog.Builder(getContext())
                    .setTitle("회원 탈퇴")
                    .setMessage("보안을 위해 비밀번호를 다시 입력해주세요. 확인 즉시 모든 기록이 삭제됩니다.")
                    .setView(passwordInput)
                    .setPositiveButton("탈퇴", (dialog, which) -> {
                        String password = passwordInput.getText().toString();
                        if (password.isEmpty()) {
                            Toast.makeText(getContext(), "비밀번호를 입력해야 합니다.", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        if (user != null && user.getEmail() != null) {
                            // 1. 사용자의 이메일과 입력한 비밀번호로 '인증 정보' 생성
                            com.google.firebase.auth.AuthCredential credential =
                                    com.google.firebase.auth.EmailAuthProvider.getCredential(user.getEmail(), password);

                            // 2. 재인증 시도 (사용자 몰래 백그라운드에서 다시 로그인하는 것과 같음)
                            user.reauthenticate(credential).addOnCompleteListener(reAuthTask -> {
                                if (reAuthTask.isSuccessful()) {
                                    // 3. 재인증 성공 시 바로 계정 삭제 진행
                                    user.delete().addOnCompleteListener(deleteTask -> {
                                        if (deleteTask.isSuccessful()) {
                                            Toast.makeText(getContext(), "회원 탈퇴가 완료되었습니다.", Toast.LENGTH_SHORT).show();
                                            // 로그인 화면으로 이동
                                            Intent intent = new Intent(getActivity(), LoginActivity.class);
                                            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                                            startActivity(intent);
                                        } else {
                                            Toast.makeText(getContext(), "탈퇴 실패: " + deleteTask.getException().getMessage(), Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                } else {
                                    // 재인증 실패 (비밀번호가 틀린 경우 등)
                                    Toast.makeText(getContext(), "인증 실패: 비밀번호를 확인해주세요.", Toast.LENGTH_SHORT).show();
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