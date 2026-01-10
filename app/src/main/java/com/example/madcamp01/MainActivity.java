package com.example.madcamp01;

import android.os.Bundle;
import android.view.MenuItem;

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
                Fragment selectedFragment = null;
                int itemId = item.getItemId();
                // 현재 선택된 탭을 다시 누른 경우 아무것도 하지 않음
                if (itemId == bottomNav.getSelectedItemId()) {
                    return false;
                }
                if (itemId == R.id.nav_write) {
                    // 1번 탭: 글쓰기
                    selectedFragment = new WriteFragment();
                    setTitle("글쓰기"); // 제목 변경
                } else if (itemId == R.id.nav_my_list) {
                    // 2번 탭: 내 여행 리스트 (기존 ListFragment)
                    selectedFragment = new ListFragment();
                    setTitle("내 여행 리스트"); // 제목 변경
                } else if (itemId == R.id.nav_sns_gallery) {
                    // 3번 탭: 전체 갤러리 (SNS처럼 전체)
                    selectedFragment = new GalleryFragment();
                    setTitle("갤러리"); // 제목 변경
                } else if (itemId == R.id.nav_my_page) {
                    // 4번 탭: 마이페이지
                    selectedFragment = new MypageFragment();
                    setTitle("갤러리"); // 제목 변경
                }

                if (selectedFragment != null) {
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, selectedFragment)
                            .commit();
                }
                return true;
            }
        });
    }
}