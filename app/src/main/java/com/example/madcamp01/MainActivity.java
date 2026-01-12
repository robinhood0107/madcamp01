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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        bottomNav = findViewById(R.id.bottom_navigation);
        // 프래그먼트 변경 시 하단 바 상태를 동기화하기 위한 리스너
        getSupportFragmentManager().addOnBackStackChangedListener(new FragmentManager.OnBackStackChangedListener() {
            @Override
            public void onBackStackChanged() {
                // null 체크 추가
                if (bottomNav == null) return;
                
                Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
                
                // 프래그먼트가 null이거나 아직 생성되지 않은 경우 무시
                if (currentFragment == null) return;

                // 네비게이션 바 상태와 타이틀 동기화
                // DetailFragment 등 다른 프래그먼트는 백스택에 있으므로 네비게이션 바 상태는 유지
                if (currentFragment instanceof WriteFragment) {
                    bottomNav.getMenu().findItem(R.id.nav_write).setChecked(true);
                    setTitle("글쓰기");
                } else if (currentFragment instanceof ListFragment) {
                    bottomNav.getMenu().findItem(R.id.nav_my_list).setChecked(true);
                    setTitle("내 여행 리스트");
                } else if (currentFragment instanceof GalleryFragment) {
                    bottomNav.getMenu().findItem(R.id.nav_sns_gallery).setChecked(true);
                    setTitle("갤러리");
                } else if (currentFragment instanceof MypageFragment) {
                    bottomNav.getMenu().findItem(R.id.nav_my_page).setChecked(true);
                    setTitle("마이페이지");
                }
                // DetailFragment 등 다른 프래그먼트는 백스택에 있으므로 네비게이션 바 상태는 유지
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
                Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);

                // 1. 현재 선택된 탭을 다시 누른 경우 처리
                if (itemId == bottomNav.getSelectedItemId()) {
                    // WriteFragment에서 같은 탭(글쓰기)을 다시 누른 경우 선택 다이얼로그 표시
                    if (currentFragment instanceof WriteFragment && itemId == R.id.nav_write) {
                        // 이전 탭은 현재 선택된 탭으로 유지 (이미 WriteFragment이므로)
                        showWriteChoiceDialog();
                        return true;
                    }
                    // 다른 경우는 무시
                    return false;
                }

                // [중요] 2. 현재 화면이 WriteFragment인지 확인하고 내용이 있는지 체크
                if (currentFragment instanceof WriteFragment) {
                    WriteFragment writeFrag = (WriteFragment) currentFragment;

                    // WriteFragment에 "내용이 수정되었나?" 물어보는 함수가 있다고 가정 (아래에서 만들 예정)
                    if (writeFrag.hasChanges()) {
                        // 내용이 있다면 팝업 띄우기
                        int previousSelectedId = bottomNav.getSelectedItemId(); // 이전 선택 상태 저장
                        new androidx.appcompat.app.AlertDialog.Builder(MainActivity.this)
                                .setTitle("작성 취소")
                                .setMessage("이 페이지를 나가면 작성 중인 내용이 사라집니다. 계속하시겠습니까?")
                                .setPositiveButton("나가기", (dialog, which) -> {
                                    // 사용자가 '나가기'를 선택하면 그제서야 탭 이동 수행
                                    switchFragment(itemId);
                                })
                                .setNegativeButton("취소", (dialog, which) -> {
                                    // 취소 시 네비게이션 바 상태를 이전 상태로 복원
                                    bottomNav.getMenu().findItem(previousSelectedId).setChecked(true);
                                })
                                .setOnCancelListener(dialog -> {
                                    // 다이얼로그가 외부에서 취소된 경우에도 상태 복원
                                    bottomNav.getMenu().findItem(previousSelectedId).setChecked(true);
                                })
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
                String title = "";
                
                if (itemId == R.id.nav_write) {
                    // WriteFragment로 이동할 때는 현재 탭을 이전 탭으로 저장
                    previousTabId = bottomNav.getSelectedItemId();
                    // WriteFragment로 이동할 때는 선택 다이얼로그 먼저 표시
                    showWriteChoiceDialog();
                    return; // 다이얼로그에서 처리하므로 여기서는 리턴
                } else if (itemId == R.id.nav_my_list) {
                    selectedFragment = new ListFragment();
                    title = "내 여행 리스트";
                } else if (itemId == R.id.nav_sns_gallery) {
                    selectedFragment = new GalleryFragment();
                    title = "갤러리";
                } else if (itemId == R.id.nav_my_page) {
                    selectedFragment = new MypageFragment();
                    title = "마이페이지";
                }

                if (selectedFragment != null) {
                    // 백스택 정리
                    getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                    
                    // 프래그먼트 교체
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, selectedFragment)
                            .commit();
                    
                    // 타이틀 설정
                    setTitle(title);
                    
                    // 네비게이션 바 상태 업데이트 (명시적으로 설정)
                    bottomNav.getMenu().findItem(itemId).setChecked(true);
                }
            }
            
            // 게시물 작성 선택 다이얼로그 (새로 작성하기 / 이전 탭으로 돌아가기)
            private void showWriteChoiceDialog() {
                // 다이얼로그 표시 전 현재 선택된 탭 저장 (다이얼로그 취소 시 복원용)
                final int currentSelectedTabId = bottomNav.getSelectedItemId();
                final boolean[] dialogActionTaken = {false}; // 다이얼로그에서 액션을 취했는지 여부
                
                android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(MainActivity.this);
                View dialogView = getLayoutInflater().inflate(R.layout.dialog_write_choice, null);
                builder.setView(dialogView);
                
                android.widget.Button btnNewWrite = dialogView.findViewById(R.id.btn_new_write);
                android.widget.Button btnBackToList = dialogView.findViewById(R.id.btn_back_to_list);
                
                android.app.AlertDialog dialog = builder.create();
                
                // 다이얼로그가 닫힐 때 처리 (외부 클릭, 뒤로가기 등)
                dialog.setOnDismissListener(d -> {
                    // 버튼을 클릭하지 않고 다이얼로그가 닫힌 경우에만 네비게이션 바 상태 복원
                    if (!dialogActionTaken[0]) {
                        bottomNav.getMenu().findItem(currentSelectedTabId).setChecked(true);
                    }
                });
                
                dialog.setOnCancelListener(d -> {
                    // 다이얼로그가 취소된 경우 네비게이션 바 상태 복원
                    if (!dialogActionTaken[0]) {
                        bottomNav.getMenu().findItem(currentSelectedTabId).setChecked(true);
                    }
                });
                
                // 새로 작성하기 버튼 클릭 시
                btnNewWrite.setOnClickListener(v -> {
                    dialogActionTaken[0] = true; // 액션 취함 표시
                    dialog.dismiss();
                    // 현재 WriteFragment에 변경사항이 있는지 확인
                    Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
                    if (currentFragment instanceof WriteFragment) {
                        WriteFragment writeFrag = (WriteFragment) currentFragment;
                        if (writeFrag.hasChanges()) {
                            // 변경사항이 있으면 경고 다이얼로그 표시
                            new androidx.appcompat.app.AlertDialog.Builder(MainActivity.this)
                                    .setTitle("작성 취소")
                                    .setMessage("새로 작성하면 현재 작성 중인 내용이 사라집니다. 계속하시겠습니까?")
                                    .setPositiveButton("새로 작성", (d, which) -> {
                                        showTravelInfoDialog();
                                    })
                                    .setNegativeButton("취소", (d, which) -> {
                                        // 취소 시 네비게이션 바 상태 복원
                                        bottomNav.getMenu().findItem(currentSelectedTabId).setChecked(true);
                                    })
                                    .setOnCancelListener(d -> {
                                        // 다이얼로그가 외부에서 취소된 경우에도 상태 복원
                                        bottomNav.getMenu().findItem(currentSelectedTabId).setChecked(true);
                                    })
                                    .show();
                            return;
                        }
                    }
                    // 변경사항이 없으면 바로 새로 작성
                    showTravelInfoDialog();
                });
                
                // 이전 탭으로 돌아가기 버튼 클릭 시
                btnBackToList.setOnClickListener(v -> {
                    dialogActionTaken[0] = true; // 액션 취함 표시
                    dialog.dismiss();
                    // 현재 WriteFragment에 변경사항이 있는지 확인
                    Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
                    if (currentFragment instanceof WriteFragment) {
                        WriteFragment writeFrag = (WriteFragment) currentFragment;
                        if (writeFrag.hasChanges()) {
                            // 변경사항이 있으면 경고 다이얼로그 표시
                            new androidx.appcompat.app.AlertDialog.Builder(MainActivity.this)
                                    .setTitle("작성 취소")
                                    .setMessage("이 페이지를 나가면 작성 중인 내용이 사라집니다. 계속하시겠습니까?")
                                    .setPositiveButton("나가기", (d, which) -> {
                                        navigateToPreviousTab();
                                    })
                                    .setNegativeButton("취소", (d, which) -> {
                                        // 취소 시 네비게이션 바 상태 복원
                                        bottomNav.getMenu().findItem(currentSelectedTabId).setChecked(true);
                                    })
                                    .setOnCancelListener(d -> {
                                        // 다이얼로그가 외부에서 취소된 경우에도 상태 복원
                                        bottomNav.getMenu().findItem(currentSelectedTabId).setChecked(true);
                                    })
                                    .show();
                            return;
                        }
                    }
                    // 변경사항이 없으면 바로 이동
                    navigateToPreviousTab();
                });
                
                dialog.show();
            }
            
            // 이전 탭으로 이동하는 함수
            private void navigateToPreviousTab() {
                // 이전 탭이 글쓰기 탭인 경우 기본값(내 여행 리스트)으로 이동
                int targetTabId = previousTabId;
                if (targetTabId == R.id.nav_write) {
                    targetTabId = R.id.nav_my_list;
                }
                
                // 이전 탭으로 직접 이동 (다이얼로그 없이)
                Fragment selectedFragment = null;
                if (targetTabId == R.id.nav_my_list) {
                    selectedFragment = new ListFragment();
                    setTitle("내 여행 리스트");
                } else if (targetTabId == R.id.nav_sns_gallery) {
                    selectedFragment = new GalleryFragment();
                    setTitle("갤러리");
                } else if (targetTabId == R.id.nav_my_page) {
                    selectedFragment = new MypageFragment();
                    setTitle("마이페이지");
                }
                
                if (selectedFragment != null) {
                    getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, selectedFragment)
                            .commit();
                    bottomNav.getMenu().findItem(targetTabId).setChecked(true);
                }
            }
            
            // 여행 정보 입력 다이얼로그 (신규 작성 시에만 표시)
            private void showTravelInfoDialog() {
                // 게시물 등록 진행 중이므로 네비게이션 바를 게시물 등록 탭으로 설정
                bottomNav.getMenu().findItem(R.id.nav_write).setChecked(true);
                
                // 다이얼로그 표시 전 현재 선택된 탭 저장 (다이얼로그 취소 시 복원용)
                final int currentSelectedTabId = previousTabId;
                
                // WriteFragment를 먼저 생성하여 뒤 배경으로 설정
                WriteFragment writeFragment = new WriteFragment();
                // 여행 정보는 아직 없지만 WriteFragment를 먼저 생성
                Bundle args = new Bundle();
                writeFragment.setArguments(args);
                
                getSupportFragmentManager().popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
                getSupportFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, writeFragment)
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
                        
                        // 확인 다이얼로그 표시
                        showTravelInfoConfirmationDialog(startDate, travelDays, writeFragment, dialog);
                    });
                    
                    android.widget.Button negativeButton = dialog.getButton(android.app.AlertDialog.BUTTON_NEGATIVE);
                    negativeButton.setOnClickListener(v -> {
                        // 취소 시 이전 탭으로 복원
                        navigateToPreviousTab();
                        dialog.dismiss();
                    });
                });
                
                // 다이얼로그가 외부에서 취소되거나 뒤로가기로 닫힐 때 처리
                dialog.setOnCancelListener(d -> {
                    // 취소 시 이전 탭으로 복원
                    navigateToPreviousTab();
                });
                
                dialog.setOnDismissListener(d -> {
                    // 다이얼로그가 닫힐 때 WriteFragment로 이동하지 않은 경우에만 상태 복원
                    Fragment currentFragment = getSupportFragmentManager().findFragmentById(R.id.fragment_container);
                    if (!(currentFragment instanceof WriteFragment)) {
                        navigateToPreviousTab();
                    }
                });
                
                dialog.show();
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
    
    // 여행 정보 확인 다이얼로그 표시
    private void showTravelInfoConfirmationDialog(java.util.Date startDate, int travelDays, WriteFragment writeFragment, android.app.AlertDialog previousDialog) {
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
                    // WriteFragment에 여행 정보 전달
                    Bundle newArgs = new Bundle();
                    newArgs.putInt("travelDays", travelDays);
                    newArgs.putLong("startDate", startDate.getTime());
                    writeFragment.setArguments(newArgs);
                    
                    // WriteFragment의 여행 정보 업데이트를 위해 재생성
                    WriteFragment updatedWriteFragment = new WriteFragment();
                    updatedWriteFragment.setArguments(newArgs);
                    
                    getSupportFragmentManager().beginTransaction()
                            .replace(R.id.fragment_container, updatedWriteFragment)
                            .commit();
                    setTitle("글쓰기");
                    bottomNav.getMenu().findItem(R.id.nav_write).setChecked(true);
                    
                    previousDialog.dismiss();
                })
                .setNegativeButton("수정하기", (d, which) -> {
                    // 수정하기를 누르면 이전 다이얼로그는 그대로 유지 (아무것도 안 함)
                })
                .setCancelable(false)
                .show();
    }

}
