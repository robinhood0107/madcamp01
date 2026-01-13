package com.example.madcamp01;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.SignInButton;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

public class LoginActivity extends AppCompatActivity {

    private static final int RC_SIGN_IN = 9001;

    private EditText editEmail, editPassword;
    private EditText editPasswordConfirm;
    private Button btnMainAction;
    private TextView textToggleMode;
    private SignInButton btnGoogleLogin;

    private FirebaseAuth auth;
    private GoogleSignInClient googleSignInClient;
    private boolean isSignUpMode = false;

    @Override
    public void onStart() {
        super.onStart();
        FirebaseUser currentUser = auth.getCurrentUser();
        if (currentUser != null) {
            startActivity(new Intent(LoginActivity.this, MainActivity.class));
            finish();
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);
        auth = FirebaseAuth.getInstance();

        // Google Sign-In 설정
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken("58745205594-g3j1m3jbfpo1mmnanq68jrg7ui26im5s.apps.googleusercontent.com") // google-services.json이 있으면 자동 생성됨
                .requestEmail()
                .build();

        googleSignInClient = GoogleSignIn.getClient(this, gso);

        editEmail = findViewById(R.id.edit_login_email);
        editPassword = findViewById(R.id.edit_login_password);
        editPasswordConfirm = findViewById(R.id.edit_login_password_confirm);
        btnMainAction = findViewById(R.id.btn_main_action);
        textToggleMode = findViewById(R.id.text_toggle_mode);
        btnGoogleLogin = findViewById(R.id.btn_google_login);

        textToggleMode.setOnClickListener(v -> {
            isSignUpMode = !isSignUpMode;
            if (isSignUpMode) {
                editPasswordConfirm.setVisibility(View.VISIBLE);
                btnMainAction.setText("회원가입 하기");
                textToggleMode.setText("이미 계정이 있나요? 로그인");
            } else {
                editPasswordConfirm.setVisibility(View.GONE);
                btnMainAction.setText("로그인");
                textToggleMode.setText("계정이 없으신가요? 회원가입");
            }
        });

        btnMainAction.setOnClickListener(v -> {
            String email = editEmail.getText().toString().trim();
            String password = editPassword.getText().toString().trim();

            if (isSignUpMode) {
                String passwordConfirm = editPasswordConfirm.getText().toString().trim();
                if (!password.equals(passwordConfirm)) {
                    Toast.makeText(this, "비밀번호가 일치하지 않습니다.", Toast.LENGTH_SHORT).show();
                    return;
                }
                signUp(email, password);
            } else {
                signIn(email, password);
            }
        });

        // 구글 로그인 버튼 클릭
        btnGoogleLogin.setOnClickListener(v -> {
            Intent signInIntent = googleSignInClient.getSignInIntent();
            startActivityForResult(signInIntent, RC_SIGN_IN);
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                GoogleSignInAccount account = task.getResult(ApiException.class);
                firebaseAuthWithGoogle(account.getIdToken());
            } catch (ApiException e) {
                Toast.makeText(this, "구글 로그인 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        auth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        startActivity(new Intent(LoginActivity.this, MainActivity.class));
                        finish();
                    } else {
                        Toast.makeText(this, "Firebase 인증 실패", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void signIn(String email, String password) {
        if (email.isEmpty() || password.isEmpty()) return;
        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        startActivity(new Intent(LoginActivity.this, MainActivity.class));
                        finish();
                    } else {
                        Toast.makeText(this, "로그인 실패: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void signUp(String email, String password) {
        if (email.isEmpty() || password.isEmpty()) return;
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "회원가입 성공! 로그인 해주세요.", Toast.LENGTH_SHORT).show();
                        isSignUpMode = false;
                        editPasswordConfirm.setVisibility(View.GONE);
                        btnMainAction.setText("로그인");
                        textToggleMode.setText("계정이 없으신가요? 회원가입");
                    } else {
                        Toast.makeText(this, "가입 실패: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
    }
}