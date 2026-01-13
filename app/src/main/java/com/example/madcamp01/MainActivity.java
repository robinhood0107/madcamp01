package com.example.madcamp01;

import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {
    //자바에서는 onCreate() 함수가 시작점
    private int previousTabId = R.id.nav_my_list; // 이전 탭 ID 저장 (기본값: 내 여행 리스트)
    private BottomNavigationView bottomNav; // 네비게이션 바 참조

    private boolean isTravelInfoFlowActive = false; // 여행 정보 입력 다이얼로그 진행 여부
    private boolean isFragmentTransitioning = false; // 프래그먼트 전환 중 플래그

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNav = findViewById(R.id.bottom_navigation);

        getSupportFragmentManager().addOnBackStackChangedListener(() -> {
            if (bottomNav == null) return;

            Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
            if (currentFragment == null) return;

            if (currentFragment instanceof PostMapFragment) {
                bottomNav.getMenu().findItem(R.id.nav_map).setChecked(true);
                setTitle("Map");
            } else if (currentFragment instanceof ListFragment) {
                bottomNav.getMenu().findItem(R.id.nav_my_list).setChecked(true);
                setTitle("My Feed");
            } else if (currentFragment instanceof GalleryFragment) {
                bottomNav.getMenu().findItem(R.id.nav_sns_gallery).setChecked(true);
                setTitle("Community");
            } else if (currentFragment instanceof MypageFragment) {
                bottomNav.getMenu().findItem(R.id.nav_my_page).setChecked(true);
                setTitle("Profile");
            }
        });

        // 처음 앱 켰을 때 보여줄 화면 및 제목, 네비게이션 상태 설정
        if (savedInstanceState == null) {
            getSupportFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new ListFragment())
                    .commit();
            setTitle("My Feed");
            bottomNav.setSelectedItemId(R.id.nav_my_list);
            previousTabId = R.id.nav_my_list;
        }

        // 탭 클릭 이벤트 리스너
        bottomNav.setOnItemSelectedListener(new BottomNavigationView.OnItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(@NonNull MenuItem item) {
                int itemId = item.getItemId();
                
                // 전환 중이면 무시
                if (isFragmentTransitioning) {
                    bottomNav.getMenu().findItem(previousTabId).setChecked(true);
                    return false;
                }

                Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);

                // 현재 선택된 탭을 다시 누른 경우 처리
                if (itemId == bottomNav.getSelectedItemId()) {
                    return false;
                }

                // 현재 화면이 WriteFragment인지 확인하고 내용이 있는지 체크
                if (currentFragment instanceof WriteFragment) {
                    WriteFragment writeFrag = (WriteFragment) currentFragment;
                    if (writeFrag.hasChanges()) {
                        int previousSelectedId = bottomNav.getSelectedItemId();
                        new androidx.appcompat.app.AlertDialog.Builder(MainActivity.this)
                                .setTitle("작성 취소")
                                .setMessage("이 페이지를 나가면 작성 중인 내용이 사라집니다. 계속하시겠습니까?")
                                .setPositiveButton("나가기", (dialog, which) -> switchFragment(itemId))
                                .setNegativeButton("취소", (dialog, which) ->
                                        bottomNav.getMenu().findItem(previousSelectedId).setChecked(true))
                                .setOnCancelListener(dialog ->
                                        bottomNav.getMenu().findItem(previousSelectedId).setChecked(true))
                                .show();
                        return false;
                    }
                }

                switchFragment(itemId);
                return true;
            }

            private void switchFragment(int itemId) {
                // 이미 전환 중이면 무시
                if (isFragmentTransitioning) return;
                
                FragmentInfo fragmentInfo = getFragmentInfo(itemId);
                if (fragmentInfo == null) return;

                isFragmentTransitioning = true;

                FragmentManager fm = getSupportFragmentManager();
                
                // 백스택 정리 (비동기로 처리하여 블로킹 방지)
                if (fm.getBackStackEntryCount() > 0) {
                    fm.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                }

                // 애니메이션 없이 즉시 교체
                try {
                    fm.beginTransaction()
                            .replace(R.id.fragment_container, fragmentInfo.fragment)
                            .commitAllowingStateLoss();
                } catch (Exception e) {
                    // 전환 실패 시 플래그 리셋
                    isFragmentTransitioning = false;
                    return;
                }

                setTitle(fragmentInfo.title);
                bottomNav.getMenu().findItem(itemId).setChecked(true);
                previousTabId = itemId;
                
                // 전환 완료 후 플래그 리셋 (약간의 딜레이를 두어 연속 클릭 방지)
                bottomNav.postDelayed(() -> isFragmentTransitioning = false, 100);
            }
        });
        
        // 뒤로 가기 버튼 클릭 시 확인 팝업 띄우기
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                if (getSupportFragmentManager().getBackStackEntryCount() > 0) {
                    getSupportFragmentManager().popBackStack();
                } else {
                    new androidx.appcompat.app.AlertDialog.Builder(MainActivity.this)
                            .setTitle("앱 종료")
                            .setMessage("정말 종료하시겠습니까?")
                            .setPositiveButton("종료", (dialog, which) -> finish())
                            .setNegativeButton("취소", null)
                            .show();
                }
            }
        };
        getOnBackPressedDispatcher().addCallback(this, callback);
    }
    
    private void showTravelInfoConfirmationDialog(java.util.Date startDate, int travelDays, android.app.AlertDialog previousDialog) {
        // 종료일 계산
        java.util.Calendar endCal = java.util.Calendar.getInstance();
        endCal.setTime(startDate);
        endCal.add(java.util.Calendar.DAY_OF_MONTH, travelDays - 1);
        
        // 날짜 포맷팅
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy년 MM월 dd일", java.util.Locale.getDefault());
        String startDateStr = sdf.format(startDate);
        String endDateStr = sdf.format(endCal.getTime());
        
        // X박X일 계산
        int nights = travelDays - 1;
        String message = startDateStr + "부터 " + endDateStr + "까지(" + nights + "박" + travelDays + "일, " + travelDays + "일차) 여행이 맞습니까?";
        
        new android.app.AlertDialog.Builder(MainActivity.this)
                .setTitle("여행 일정 확인")
                .setMessage(message)
                .setPositiveButton("맞습니다", (d, which) -> {
                    // 여행 정보 번들을 만들어 WriteFragment로 이동
                    Bundle newArgs = new Bundle();
                    newArgs.putInt("travelDays", travelDays);
                    newArgs.putLong("startDate", startDate.getTime());

                    WriteFragment writeFragment = new WriteFragment();
                    writeFragment.setArguments(newArgs);

                    // 여행 정보 입력 플로우 완료 표시 (취소시 복귀 방지)
                    isTravelInfoFlowActive = false;

                    FragmentManager fm = getSupportFragmentManager();
                    fm.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                    fm.beginTransaction()
                            .replace(R.id.fragment_container, writeFragment)
                            .commit();
                    setTitle("글쓰기");
                    previousDialog.dismiss();
                })
                .setNegativeButton("수정하기", null)
                .setCancelable(false)
                .show();
    }
    
    public void showTravelInfoDialog() {
        previousTabId = bottomNav != null ? bottomNav.getSelectedItemId() : R.id.nav_my_list;
        isTravelInfoFlowActive = true;

        // TravelInfo 다이얼로그가 떠 있는 동안 배경에 WriteFragment를 미리 표시
        WriteFragment draftWriteFragment = new WriteFragment();
        Bundle draftArgs = new Bundle();
        draftWriteFragment.setArguments(draftArgs);

        getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        getSupportFragmentManager().beginTransaction()
                .replace(R.id.fragment_container, draftWriteFragment)
                .commit();
        setTitle("글쓰기");

        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(MainActivity.this);
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_travel_info, null);
        builder.setView(dialogView);

        android.widget.EditText editTravelDays = dialogView.findViewById(R.id.edit_travel_days);
        android.widget.DatePicker datePicker = dialogView.findViewById(R.id.date_picker_start);

        android.app.AlertDialog dialog = builder.setTitle("여행 정보 입력")
                .setPositiveButton("확인", null) // 나중에 처리하기 위해 null로 설정
                .setNegativeButton("취소", null)
                .create();

        // 확인 버튼 클릭 시 처리
        dialog.setOnShowListener(d -> {
            android.widget.Button positiveButton = dialog.getButton(android.app.AlertDialog.BUTTON_POSITIVE);
            positiveButton.setOnClickListener(v -> {
                String daysStr = editTravelDays.getText().toString();
                if (daysStr.isEmpty()) {
                    android.widget.Toast.makeText(MainActivity.this, "여행 일수를 입력해주세요.", android.widget.Toast.LENGTH_SHORT).show();
                    return;
                }

                int travelDays = Integer.parseInt(daysStr);
                if (travelDays <= 0) {
                    android.widget.Toast.makeText(MainActivity.this, "여행 일수는 1일 이상이어야 합니다.", android.widget.Toast.LENGTH_SHORT).show();
                    return;
                }

                // 날짜 선택
                java.util.Calendar calendar = java.util.Calendar.getInstance();
                calendar.set(datePicker.getYear(), datePicker.getMonth(), datePicker.getDayOfMonth());
                java.util.Date startDate = calendar.getTime();

                showTravelInfoConfirmationDialog(startDate, travelDays, dialog);
            });

            android.widget.Button negativeButton = dialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE);
            negativeButton.setOnClickListener(v -> {
                restorePreviousTab();
                dialog.dismiss();
            });
        });

        dialog.setOnCancelListener(d -> restorePreviousTab());
        dialog.setOnDismissListener(d -> restorePreviousTab());

        dialog.show();
    }
    private void restorePreviousTab() {
        if (!isTravelInfoFlowActive) {
            return;
        }
        isTravelInfoFlowActive = false;

        FragmentInfo fragmentInfo = getFragmentInfo(previousTabId);
        if (fragmentInfo == null) return;

        FragmentManager fm = getSupportFragmentManager();
        fm.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        fm.beginTransaction()
                .replace(R.id.fragment_container, fragmentInfo.fragment)
                .commit();
        setTitle(fragmentInfo.title);
        if (bottomNav != null) {
            bottomNav.getMenu().findItem(previousTabId).setChecked(true);
        }
    }

    private FragmentInfo getFragmentInfo(int itemId) {
        Fragment fragment = null;
        String title = "";

        if (itemId == R.id.nav_map) {
            fragment = new PostMapFragment();
            title = "Map";
        } else if (itemId == R.id.nav_my_list) {
            fragment = new ListFragment();
            title = "My Feed";
        } else if (itemId == R.id.nav_sns_gallery) {
            fragment = new GalleryFragment();
            title = "Community";
        } else if (itemId == R.id.nav_my_page) {
            fragment = new MypageFragment();
            title = "Profile";
        }

        return fragment != null ? new FragmentInfo(fragment, title) : null;
    }

    private static class FragmentInfo {
        final Fragment fragment;
        final String title;

        FragmentInfo(Fragment fragment, String title) {
            this.fragment = fragment;
            this.title = title;
        }
    }
}
