package com.example.madcamp01;

import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.recyclerview.widget.GridLayoutManager;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory;
import com.google.firebase.firestore.FieldValue; // 시간 저장용
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage; // 스토리지용
import com.google.firebase.storage.StorageReference; // 스토리지 참조용

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class WriteFragment extends Fragment {

    // 1. 필요한 변수들 선언
    private EditText editTripTitle;       // 여행 제목 입력창
    private RecyclerView recyclerView;    // 사진 리스트 뷰
    private PhotoAdapter photoAdapter;    // 사진 리스트 관리자(어댑터)
    private List<Uri> selectedImageUris = new ArrayList<>(); // 사진 주소들을 담을 보관함
    private FirebaseFirestore db = FirebaseFirestore.getInstance();
    private FirebaseStorage storage = FirebaseStorage.getInstance();
    // 갤러리 실행 도구
    private ActivityResultLauncher<String> getMultipleContents;
    private FirebaseAuth auth; // Firebase Authentication 인스턴스 추가
    private androidx.appcompat.app.AlertDialog progressDialog; // 로딩바용
    private com.google.android.material.materialswitch.MaterialSwitch switchIsPublic; // 공개 비공개 버튼
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 2. 사진 선택 결과 처리기 등록
        getMultipleContents = registerForActivityResult(
                new ActivityResultContracts.GetMultipleContents(),
                uris -> {
                    if (uris != null && !uris.isEmpty()) {
                        selectedImageUris.addAll(uris);
                        // 어댑터에게 데이터가 바뀌었으니 화면을 새로고침하라고 명령
                        photoAdapter.notifyDataSetChanged();
                    }
                }
        );
        // Firebase Authentication 인스턴스 초기화
        auth = FirebaseAuth.getInstance();

        // 익명으로 로그인 시도
        auth.signInAnonymously()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // 로그인 성공
                        FirebaseUser user = auth.getCurrentUser();
                        android.util.Log.d("FIREBASE_AUTH", "Anonymous login successful: " + user.getUid());
                    } else {
                        // 로그인 실패
                        android.util.Log.e("FIREBASE_AUTH", "Anonymous login failed: " + task.getException());
                        Toast.makeText(getContext(), "Authentication failed.", Toast.LENGTH_SHORT).show();
                    }
                });

    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // 3. XML 레이아웃 연결
        View view = inflater.inflate(R.layout.fragment_write, container, false);

        // 4. XML 위젯들을 자바 변수에 연결
        editTripTitle = view.findViewById(R.id.edit_trip_title);
        recyclerView = view.findViewById(R.id.recycler_photos);
        Button btnAddPhoto = view.findViewById(R.id.btn_add_photo);
        Button btnSave = view.findViewById(R.id.btn_save);
        switchIsPublic = view.findViewById(R.id.switch_is_public);

        // 5. 리사이클러뷰(사진 리스트) 설정
        // 사진을 가로(Horizontal)로 나열하도록 설정
        GridLayoutManager layoutManager = new GridLayoutManager(getContext(), 2);
        recyclerView.setLayoutManager(layoutManager);

        // 어댑터 연결 (보관함과 화면을 연결)
        photoAdapter = new PhotoAdapter(selectedImageUris, getContext());
        recyclerView.setAdapter(photoAdapter);

        // 6. 버튼 클릭 시 갤러리 실행
        btnAddPhoto.setOnClickListener(v -> {
            getMultipleContents.launch("image/*"); // 이미지 파일만 선택하도록 실행
        });

        // 7. 저장 버튼 클릭 시 파이어베이스 업로드 함수 실행
        btnSave.setOnClickListener(v -> {
            // 이전에 만들어둔 파이어베이스 업로드 함수 호출
            uploadToFirebase();
        });

        return view;
    }
    // [업로드 함수]
    private void uploadToFirebase() {
        String title = editTripTitle.getText().toString();
        boolean isPublic = switchIsPublic.isChecked();
        // 제목이 비었거나 사진을 선택 안했으면 중단
        if (title.isEmpty() || selectedImageUris.isEmpty()) {
            Toast.makeText(getContext(), "제목과 사진을 입력해주세요.", Toast.LENGTH_SHORT).show();
            return;
        }
        showProgressDialog(); //로딩바 띄우기
        // 사진들을 하나씩 Storage에 먼저 올리고, 그 주소(URL)를 리스트에 담습니다.
        List<String> uploadedUrls = new ArrayList<>();

        for (Uri fileUri : selectedImageUris) {
            String fileName = "images/" + System.currentTimeMillis() + "_" + fileUri.getLastPathSegment();
            StorageReference ref = storage.getReference().child(fileName);
            ref.putFile(fileUri)
                    .addOnSuccessListener(taskSnapshot -> {
                        ref.getDownloadUrl().addOnSuccessListener(uri -> {
                            uploadedUrls.add(uri.toString());
                            if (uploadedUrls.size() == selectedImageUris.size()) {
                                savePostInfo(title, uploadedUrls, isPublic);
                            }
                        });
                    })
                    .addOnFailureListener(e -> {
                        hideProgressDialog();
                        Toast.makeText(getContext(), "저장 실패", Toast.LENGTH_SHORT).show();
                        android.util.Log.e("FIREBASE_TEST", "업로드 실패 원인: " + e.getMessage());
                        e.printStackTrace();
                    });
        }
    }

    // Firestore에 제목과 이미지 URL들 저장
    private void savePostInfo(String title, List<String> imageUrls, boolean isPublic) {
        Map<String, Object> post = new HashMap<>();
        post.put("title", title);
        post.put("images", imageUrls); // 사진들의 주소 리스트
        post.put("isPublic", isPublic);
        post.put("createdAt", FieldValue.serverTimestamp());

        db.collection("TravelPosts")
                .add(post)
                .addOnSuccessListener(documentReference -> {
                    hideProgressDialog();
                    Toast.makeText(getContext(), "여행 기록이 성공적으로 저장되었습니다!", Toast.LENGTH_SHORT).show();
                    if (getActivity() != null) {
                        BottomNavigationView bottomNav = getActivity().findViewById(R.id.bottom_navigation);
                        // '내 여행 리스트' 아이템 ID로 선택 변경
                        // (MainActivity에서 사용하신 R.id.nav_my_list와 동일해야 합니다)
                        bottomNav.setSelectedItemId(R.id.nav_my_list);
                    }
                })
                .addOnFailureListener(e -> {
                    hideProgressDialog();
                    Toast.makeText(getContext(), "저장 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
    private void showProgressDialog() {
        if (progressDialog == null) {
            // AlertDialog 생성
            androidx.appcompat.app.AlertDialog.Builder builder = new androidx.appcompat.app.AlertDialog.Builder(getContext());
            builder.setCancelable(false); // 로딩 중 창 밖을 눌러도 꺼지지 않게 설정
            builder.setView(R.layout.loading); // 커스텀 레이아웃 사용 (아래에서 생성)
            progressDialog = builder.create();
        }
        progressDialog.show();
    }

    private void hideProgressDialog() {
        if (progressDialog != null && progressDialog.isShowing()) {
            progressDialog.dismiss();
        }
    }
}