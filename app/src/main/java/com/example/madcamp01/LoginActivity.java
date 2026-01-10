package com.example.madcamp01;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import android.view.View;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class LoginActivity extends AppCompatActivity {


    private EditText editEmail, editPassword;
    private EditText editPasswordConfirm;
    private Button btnMainAction;
    private TextView textToggleMode;
    private FirebaseAuth auth; // Firebase 인증 객체
    private boolean isSignUpMode = false; // 회원가입 체크

    @Override
    public void onStart() {
        super.onStart();
        FirebaseUser currentUser = auth.getCurrentUser();
        /*
        if (currentUser != null) {
            // 이미 로그인되어 있다면 바로 메인으로 이동
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish();
            return;
        }

         */
    }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // 2. Firebase Auth 초기화
        auth = FirebaseAuth.getInstance();

        // 3. XML 위젯 연결
        editEmail = findViewById(R.id.edit_login_email);
        editPassword = findViewById(R.id.edit_login_password);
        editPasswordConfirm = findViewById(R.id.edit_login_password_confirm);
        btnMainAction = findViewById(R.id.btn_main_action);
        textToggleMode = findViewById(R.id.text_toggle_mode);

        // 모드 전환 텍스트 클릭 시
        textToggleMode.setOnClickListener(v -> {
            isSignUpMode = !isSignUpMode; // 상태 반전

            if (isSignUpMode) {
                // 회원가입 모드로 전환
                editPasswordConfirm.setVisibility(View.VISIBLE);
                btnMainAction.setText("회원가입 하기");
                textToggleMode.setText("이미 계정이 있나요? 로그인");
            } else {
                // 로그인 모드로 전환
                editPasswordConfirm.setVisibility(View.GONE);
                btnMainAction.setText("로그인");
                textToggleMode.setText("계정이 없으신가요? 회원가입");
            }
        });

        // 메인 버튼 클릭 시 (로그인 혹은 회원가입 실행)
        btnMainAction.setOnClickListener(v -> {
            String email = editEmail.getText().toString().trim();
            String password = editPassword.getText().toString().trim();

            if (isSignUpMode) {
                // 회원가입 로직
                String passwordConfirm = editPasswordConfirm.getText().toString().trim();
                if (!password.equals(passwordConfirm)) {
                    Toast.makeText(this, "비밀번호가 일치하지 않습니다.", Toast.LENGTH_SHORT).show();
                    return;
                }
                signUp(email, password);
            } else {
                // 로그인 로직
                signIn(email, password);
            }
        });
    }

    // 로그인 로직
    private void signIn(String email, String password) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // 로그인 성공 -> 메인 화면으로 이동
                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(this, "로그인 실패: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    // 회원가입 로직
    private void signUp(String email, String password) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "회원가입 성공! 로그인 해주세요.", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(this, "가입 실패: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}