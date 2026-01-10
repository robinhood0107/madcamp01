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
import com.google.firebase.firestore.SetOptions;
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
    private String editPostId = null;// 수정 기능

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // 수정 모드 체크
        if (getArguments() != null) {
            editPostId = getArguments().getString("postId");
        }

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
        // 수정 모드 시 기존 게시물 정보 가져오기
        if (editPostId != null) {
            editTripTitle.setText(getArguments().getString("title"));
            switchIsPublic.setChecked(getArguments().getBoolean("isPublic"));
            btnSave.setText("수정 완료"); //저장하기 버튼을 수정하기로 바꿈
            //기존 이미지 가져오기
            ArrayList<String> imageUrls = getArguments().getStringArrayList("images");
            if (imageUrls != null) {
                for (String url : imageUrls) {
                    selectedImageUris.add(Uri.parse(url)); // 기존 이미지 URL 추가
                }
                photoAdapter.notifyDataSetChanged();
            }
        }
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
        List<String> finalUrls = new ArrayList<>();
        List<Uri> newImageUris = new ArrayList<>();

        // 3. 사진 구분: 기존 URL(http)은 그대로 두고, 새 로컬 사진만 업로드 리스트에 담음
        for (Uri uri : selectedImageUris) {
            if (uri.toString().startsWith("http")) {
                finalUrls.add(uri.toString());
            } else {
                newImageUris.add(uri);
            }
        }

        // 새로 추가된 사진이 없으면 바로 DB 업데이트
        if (newImageUris.isEmpty()) {
            savePostInfo(title, finalUrls, isPublic);
            return;
        }

        // 새 사진들만 업로드
        for (Uri fileUri : newImageUris) {
            String fileName = "images/" + System.currentTimeMillis() + "_" + fileUri.getLastPathSegment();
            StorageReference ref = storage.getReference().child(fileName);
            ref.putFile(fileUri)
                    .addOnSuccessListener(taskSnapshot -> {
                        ref.getDownloadUrl().addOnSuccessListener(uri -> {
                            finalUrls.add(uri.toString());
                            // 모든 사진(기존+새거) 처리가 완료되면 정보 저장
                            if (finalUrls.size() == selectedImageUris.size()) {
                                savePostInfo(title, finalUrls, isPublic);
                            }
                        });
                    })
                    .addOnFailureListener(e -> {
                        hideProgressDialog();
                        Toast.makeText(getContext(), "이미지 업로드 실패", Toast.LENGTH_SHORT).show();
                    });
        }
    }

    // Firestore에 제목과 이미지 URL들 저장
    private void savePostInfo(String title, List<String> imageUrls, boolean isPublic) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        String uid = (user != null) ? user.getUid() : "anonymous";
        String email = (user != null) ? user.getEmail() : "익명";

        Map<String, Object> post = new HashMap<>();
        post.put("title", title);
        post.put("images", imageUrls); // 사진들의 주소 리스트
        post.put("isPublic", isPublic);
        post.put("userId", uid);      // 사용자의 고유 ID (나중에 본인 글만 필터링할 때 사용)
        post.put("userEmail", email);  // 화면에 표시할 작성자 이메일

        // [수정 모드 확인]
        if (editPostId != null) {
            // 수정 시에는 기존 문서를 찾아 덮어씌웁니다.
            db.collection("TravelPosts").document(editPostId)
                    .set(post, SetOptions.merge()) // 기존 필드를 유지하면서 수정된 내용만 덮어씀
                    .addOnSuccessListener(aVoid -> {
                        hideProgressDialog();
                        Toast.makeText(getContext(), "여행 기록이 수정되었습니다!", Toast.LENGTH_SHORT).show();
                        goToMainList();
                    })
                    .addOnFailureListener(e -> {
                        hideProgressDialog();
                        Toast.makeText(getContext(), "수정 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        } else {
            // 신규 작성 시에는 새로운 문서를 추가합니다.
            post.put("createdAt", FieldValue.serverTimestamp()); // 신규일 때만 시간 저장
            db.collection("TravelPosts")
                    .add(post)
                    .addOnSuccessListener(documentReference -> {
                        hideProgressDialog();
                        Toast.makeText(getContext(), "여행 기록이 성공적으로 저장되었습니다!", Toast.LENGTH_SHORT).show();
                        goToMainList();
                    })
                    .addOnFailureListener(e -> {
                        hideProgressDialog();
                        Toast.makeText(getContext(), "저장 실패: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    });
        }
    }

    // 메인 리스트 탭으로 이동하는 공통 함수
    private void goToMainList() {
        if (getActivity() != null) {
            BottomNavigationView bottomNav = getActivity().findViewById(R.id.bottom_navigation);
            bottomNav.setSelectedItemId(R.id.nav_my_list);
        }
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
