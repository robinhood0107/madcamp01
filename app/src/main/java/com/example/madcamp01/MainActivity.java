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

            // WriteFragment, DetailFragment, FullScreenImageFragment 등은 네비게이션 바 상태를 변경하지 않음
            if (currentFragment instanceof WriteFragment || 
                currentFragment instanceof DetailFragment ||
                currentFragment instanceof FullScreenImageFragment) {
                return;
            }

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

                FragmentManager fm = getSupportFragmentManager();
                Fragment currentFragment = fm.findFragmentById(R.id.fragment_container);

                // 현재 선택된 탭을 다시 누른 경우 처리
                if (itemId == bottomNav.getSelectedItemId()) {
                    return false;
                }

                // FullScreenImageFragment에서는 네비게이션 탭 전환을 막고, 닫기 버튼/뒤로가기로만 나가도록 처리
                if (currentFragment instanceof FullScreenImageFragment) {
                    // 기존 탭 상태 유지
                    bottomNav.getMenu().findItem(previousTabId).setChecked(true);
                    return false;
                }

                // Detail 화면에서는 전용 로직으로 처리 (백스택 정리 후 곧바로 탭 전환)
                if (currentFragment instanceof DetailFragment) {

                    FragmentInfo fragmentInfo = getFragmentInfo(itemId);
                    if (fragmentInfo == null) return false;

                    try {
                        // Detail → List/Gallery/PostMap 등으로 안전하게 돌아가도록 백스택 전체 제거
                        if (fm.getBackStackEntryCount() > 0) {
                            fm.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                        }

                        fm.beginTransaction()
                                .replace(R.id.fragment_container, fragmentInfo.fragment)
                                .commitAllowingStateLoss();

                        setTitle(fragmentInfo.title);
                        bottomNav.getMenu().findItem(itemId).setChecked(true);
                        previousTabId = itemId;
                    } catch (Exception e) {
                        android.util.Log.e("MainActivity", "Error switching from FullScreen/Detail", e);
                        bottomNav.getMenu().findItem(previousTabId).setChecked(true);
                        return false;
                    }

                    return true;
                }

                // 현재 화면이 WriteFragment인지 확인하고 내용이 있는지 체크
                if (currentFragment instanceof WriteFragment) {
                    WriteFragment writeFrag = (WriteFragment) currentFragment;
                    
                    // 저장 중이면 탭 전환 완전히 차단
                    if (writeFrag.isSaving()) {
                        bottomNav.getMenu().findItem(previousTabId).setChecked(true);
                        android.widget.Toast.makeText(MainActivity.this, 
                                "저장 중입니다. 잠시만 기다려주세요.", 
                                android.widget.Toast.LENGTH_SHORT).show();
                        return false;
                    }
                    
                    // 내용이 있으면 확인 다이얼로그 표시
                    if (writeFrag.hasChanges()) {
                        int previousSelectedId = bottomNav.getSelectedItemId();
                        new androidx.appcompat.app.AlertDialog.Builder(MainActivity.this)
                                .setTitle("작성 취소")
                                .setMessage("이 페이지를 나가면 작성 중인 내용이 사라집니다. 계속하시겠습니까?")
                                .setPositiveButton("나가기", (dialog, which) -> {
                                    // WriteFragment를 안전하게 제거하고 탭 전환
                                    switchFragmentFromWriteFragment(itemId);
                                })
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

            private void switchFragmentFromWriteFragment(int itemId) {
                // WriteFragment에서 나갈 때 안전하게 처리
                if (isFragmentTransitioning) return;
                
                FragmentInfo fragmentInfo = getFragmentInfo(itemId);
                if (fragmentInfo == null) return;

                isFragmentTransitioning = true;

                FragmentManager fm = getSupportFragmentManager();
                
                try {
                    // 백스택에 WriteFragment가 있으면 먼저 제거
                    if (fm.getBackStackEntryCount() > 0) {
                        fm.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                    }
                    
                    // 현재 프래그먼트가 WriteFragment면 제거
                    Fragment currentFragment = fm.findFragmentById(R.id.fragment_container);
                    if (currentFragment instanceof WriteFragment) {
                        fm.beginTransaction()
                                .remove(currentFragment)
                                .commitAllowingStateLoss();
                    }
                    
                    // 약간의 딜레이 후 탭 전환 (프래그먼트 제거 완료 대기)
                    bottomNav.postDelayed(() -> {
                        try {
                            fm.beginTransaction()
                                    .replace(R.id.fragment_container, fragmentInfo.fragment)
                                    .commitAllowingStateLoss();
                            
                            setTitle(fragmentInfo.title);
                            bottomNav.getMenu().findItem(itemId).setChecked(true);
                            previousTabId = itemId;
                        } catch (Exception e) {
                            android.util.Log.e("MainActivity", "Error switching fragment after WriteFragment", e);
                            bottomNav.getMenu().findItem(previousTabId).setChecked(true);
                        } finally {
                            bottomNav.postDelayed(() -> isFragmentTransitioning = false, 100);
                        }
                    }, 50);
                } catch (Exception e) {
                    android.util.Log.e("MainActivity", "Error removing WriteFragment", e);
                    isFragmentTransitioning = false;
                    bottomNav.getMenu().findItem(previousTabId).setChecked(true);
                }
            }
            
            private void switchFragment(int itemId) {
                // 이미 전환 중이면 무시
                if (isFragmentTransitioning) return;
                
                FragmentInfo fragmentInfo = getFragmentInfo(itemId);
                if (fragmentInfo == null) return;

                isFragmentTransitioning = true;

                FragmentManager fm = getSupportFragmentManager();
                
                // 현재 프래그먼트 확인
                Fragment currentFragment = fm.findFragmentById(R.id.fragment_container);
                
                // WriteFragment가 백스택에 있거나 현재 화면인 경우 안전하게 처리
                if (currentFragment instanceof WriteFragment) {
                    WriteFragment writeFrag = (WriteFragment) currentFragment;
                    // 저장 중이면 전환 차단 (이미 위에서 체크했지만 안전을 위해)
                    if (writeFrag.isSaving()) {
                        isFragmentTransitioning = false;
                        bottomNav.getMenu().findItem(previousTabId).setChecked(true);
                        return;
                    }
                }
                
                // 백스택 정리 (동기적으로 처리하여 안전성 확보)
                try {
                    if (fm.getBackStackEntryCount() > 0) {
                        fm.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                    }
                } catch (Exception e) {
                    android.util.Log.e("MainActivity", "Error clearing back stack", e);
                    // 백스택 정리 실패해도 계속 진행
                }

                // 프래그먼트 교체
                try {
                    // 현재 프래그먼트가 여전히 WriteFragment인지 다시 확인
                    Fragment checkFragment = fm.findFragmentById(R.id.fragment_container);
                    if (checkFragment instanceof WriteFragment && ((WriteFragment) checkFragment).isSaving()) {
                        isFragmentTransitioning = false;
                        bottomNav.getMenu().findItem(previousTabId).setChecked(true);
                        return;
                    }
                    
                    fm.beginTransaction()
                            .replace(R.id.fragment_container, fragmentInfo.fragment)
                            .commitAllowingStateLoss();
                } catch (Exception e) {
                    android.util.Log.e("MainActivity", "Error switching fragment", e);
                    isFragmentTransitioning = false;
                    bottomNav.getMenu().findItem(previousTabId).setChecked(true);
                    return;
                }

                setTitle(fragmentInfo.title);
                bottomNav.getMenu().findItem(itemId).setChecked(true);
                previousTabId = itemId;
                
                // 전환 완료 후 플래그 리셋 (약간의 딜레이를 두어 연속 클릭 방지)
                bottomNav.postDelayed(() -> isFragmentTransitioning = false, 150);
            }
        });
        
        // 뒤로 가기 버튼 클릭 시 확인 팝업 띄우기
        OnBackPressedCallback callback = new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                FragmentManager fm = getSupportFragmentManager();
                Fragment currentFragment = fm.findFragmentById(R.id.fragment_container);
                
                // FullScreenImageFragment나 DetailFragment, WriteFragment가 있으면 백스택으로 돌아가기
                if (currentFragment instanceof FullScreenImageFragment || 
                    currentFragment instanceof DetailFragment ||
                    currentFragment instanceof WriteFragment) {
                    if (fm.getBackStackEntryCount() > 0) {
                        fm.popBackStack();
                    }
                } else if (fm.getBackStackEntryCount() > 0) {
                    fm.popBackStack();
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
                    if (bottomNav != null) {
                        bottomNav.setEnabled(true);
                        bottomNav.getMenu().findItem(R.id.nav_my_list).setChecked(true);
                    }
                    previousDialog.dismiss();
                })
                .setNegativeButton("수정하기", null)
                .setCancelable(false)
                .show();
    }
    
    public void showTravelInfoDialog() {
        previousTabId = bottomNav != null ? bottomNav.getSelectedItemId() : R.id.nav_my_list;
        isTravelInfoFlowActive = true;
        if (bottomNav != null) {
            bottomNav.setEnabled(false); // 여행 정보 다이얼로그 동안 탭 전환 비활성화
        }

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
                if (bottomNav != null) {
                    bottomNav.setEnabled(true);
                }
                restorePreviousTab();
                dialog.dismiss();
            });
        });

        dialog.setOnCancelListener(d -> {
            if (bottomNav != null) {
                bottomNav.setEnabled(true);
            }
            restorePreviousTab();
        });
        dialog.setOnDismissListener(d -> {
            if (bottomNav != null) {
                bottomNav.setEnabled(true);
            }
            restorePreviousTab();
        });

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
        
        try {
            // 백스택 정리 (FullScreenImageFragment, DetailFragment, WriteFragment 등 모두 제거)
            if (fm.getBackStackEntryCount() > 0) {
                fm.popBackStackImmediate(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
            }
            
            // 현재 프래그먼트 확인 및 제거
            Fragment currentFragment = fm.findFragmentById(R.id.fragment_container);
            if (currentFragment instanceof WriteFragment || 
                currentFragment instanceof FullScreenImageFragment ||
                currentFragment instanceof DetailFragment) {
                fm.beginTransaction()
                        .remove(currentFragment)
                        .commitAllowingStateLoss();
            }
            
            // 탭 복원
            fm.beginTransaction()
                    .replace(R.id.fragment_container, fragmentInfo.fragment)
                    .commitAllowingStateLoss();
            
            setTitle(fragmentInfo.title);
            if (bottomNav != null) {
                bottomNav.getMenu().findItem(previousTabId).setChecked(true);
            }
        } catch (Exception e) {
            android.util.Log.e("MainActivity", "Error restoring previous tab", e);
            // 실패해도 계속 진행
            try {
                fm.beginTransaction()
                        .replace(R.id.fragment_container, fragmentInfo.fragment)
                        .commitAllowingStateLoss();
                setTitle(fragmentInfo.title);
                if (bottomNav != null) {
                    bottomNav.getMenu().findItem(previousTabId).setChecked(true);
                }
            } catch (Exception e2) {
                android.util.Log.e("MainActivity", "Error in fallback restore", e2);
            }
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
