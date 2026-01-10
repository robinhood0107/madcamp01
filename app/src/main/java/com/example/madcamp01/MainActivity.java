package com.example.madcamp01;

import android.os.Bundle;
import android.view.MenuItem;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {
    //자바에서는 onCreate() 함수가 시작점

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        // 프래그먼트 변경 시 하단 바 상태를 동기화하기 위한 리스너
        getSupportFragmentManager().addOnBackStackChangedListener(new FragmentManager.OnBackStackChangedListener() {
            @Override
            public void onBackStackChanged() {
                Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);

                if (currentFragment instanceof WriteFragment) {
                    bottomNav.getMenu().findItem(R.id.nav_write).setChecked(true);
                    setTitle("글쓰기");
                } else if (currentFragment instanceof ListFragment) {
                    bottomNav.getMenu().findItem(R.id.nav_my_list).setChecked(true);
                    setTitle("내 여행 리스트");
                } else if (currentFragment instanceof GalleryFragment) {
                    // [추가] 현재 화면이 GalleryFragment(SNS 게시판)인 경우 처리
                    bottomNav.getMenu().findItem(R.id.nav_sns_gallery).setChecked(true);
                    setTitle("갤러리");
                } else if (currentFragment instanceof MypageFragment) {
                    // [추가] 현재 화면이 MypageFragment인 경우 처리
                    bottomNav.getMenu().findItem(R.id.nav_my_page).setChecked(true);
                    setTitle("마이페이지");
                }
            }
        });

        // 처음 앱 켰을 때 보여줄 화면 및 제목, 네비게이션 상태 설정
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new ListFragment())
                    .commit();
            setTitle("내 여행 리스트"); // 초기 타이틀 설정
            bottomNav.setSelectedItemId(R.id.nav_my_list); // 초기 탭 선택
        }

        // 탭 클릭 이벤트 리스너
        bottomNav.setOnItemSelectedListener(new BottomNavigationView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int itemId = item.getItemId();

                // 1. 현재 선택된 탭을 다시 누른 경우 무시
                if (itemId == bottomNav.getSelectedItemId()) {
                    return false;
                }

                // [중요] 2. 현재 화면이 WriteFragment인지 확인하고 내용이 있는지 체크
                Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
                if (currentFragment instanceof WriteFragment) {
                    WriteFragment writeFrag = (WriteFragment) currentFragment;

                    // WriteFragment에 "내용이 수정되었나?" 물어보는 함수가 있다고 가정 (아래에서 만들 예정)
                    if (writeFrag.hasChanges()) {
                        // 내용이 있다면 팝업 띄우기
                        new androidx.appcompat.app.AlertDialog.Builder(MainActivity.this)
                                .setTitle("작성 취소")
                                .setMessage("이 페이지를 나가면 작성 중인 내용이 사라집니다. 계속하시겠습니까?")
                                .setPositiveButton("나가기", (dialog, which) -> {
                                    // 사용자가 '나가기'를 선택하면 그제서야 탭 이동 수행
                                    switchFragment(itemId);
                                })
                                .setNegativeButton("취소", null)
                                .show();
                        return false; // 일단 리스너에서는 false를 반환하여 즉시 이동을 막음
                    }
                }

                // 수정 중이 아니라면 바로 이동
                switchFragment(itemId);
                return true;
            }

            // 탭 이동 로직을 별도 함수로 분리 (중복 방지)
            private void switchFragment(int itemId) {
                Fragment selectedFragment = null;
                if (itemId == R.id.nav_write) {
                    selectedFragment = new WriteFragment();
                    setTitle("글쓰기");
                } else if (itemId == R.id.nav_my_list) {
                    selectedFragment = new ListFragment();
                    setTitle("내 여행 리스트");
                } else if (itemId == R.id.nav_sns_gallery) {
                    selectedFragment = new GalleryFragment();
                    setTitle("갤러리");
                } else if (itemId == R.id.nav_my_page) {
                    selectedFragment = new MypageFragment();
                    setTitle("마이페이지");
                }

                if (selectedFragment != null) {
                    getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, selectedFragment)
                            .commit();
                    // 팝업을 통해 이동하는 경우 하단 바 아이콘 상태를 강제로 맞춰줘야 함
                    bottomNav.getMenu().findItem(itemId).setChecked(true);
                }
            }
        });
        // 뒤로 가기 버튼 클릭 시 확인 팝업 띄우기
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                // 1. 현재 백스택에 프래그먼트가 남아있는지 확인
                if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                    // 수정 화면 등에서 뒤로 가기를 누르면 이전 화면으로 이동
                    getSupportFragmentManager().popBackStack();
                } else {
                    // 2. 더 이상 뒤로 갈 화면이 없으면 종료 확인 팝업 띄우기
                    new androidx.appcompat.app.AlertDialog.Builder(MainActivity.this)
                            .setTitle("앱 종료")
                            .setMessage("정말 종료하시겠습니까?")
                            .setPositiveButton("종료", (dialog, which) -> {
                                // 실제 앱 종료
                                finish();
                            })
                            .setNegativeButton("취소", null)
                            .show();
                }
            }
        };
        // 콜백 등록
        getOnBackPressedDispatcher().addCallback(this, callback);

    }

}
