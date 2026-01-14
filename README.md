# PinLog (핀로그)

당신의 여행을 기록하고 공유하세요

<!-- readme: contributors -start -->
<!-- readme: contributors -end -->

### ✨ 소개
PinLog는 여행 기록을 지도에 핀으로 남기고 공유할 수 있는 위치 기반 여행 기록 앱입니다. 여행 중 찍은 사진과 함께 위치 정보를 저장하고, 지도에서 여행 경로를 시각적으로 확인할 수 있습니다. 다른 사람들의 여행 기록을 탐색하고 영감을 얻을 수도 있습니다.

### 🚀 기술 스택

![Android](https://img.shields.io/badge/Android-3DDC84?style=for-the-badge&logo=android&logoColor=white)
![Java](https://img.shields.io/badge/java-%23ED8B00.svg?style=for-the-badge&logo=openjdk&logoColor=white)
![Firebase](https://img.shields.io/badge/Firebase-039BE5?style=for-the-badge&logo=Firebase&logoColor=white)
![Google Maps](https://img.shields.io/badge/Google%20Maps-4285F4?style=for-the-badge&logo=googlemaps&logoColor=white)

#### 주요 라이브러리
- **Firebase**
  - Authentication: 사용자 인증
  - Firestore: 실시간 데이터베이스
  - Storage: 이미지 저장
- **Google Maps SDK**: 지도 표시 및 위치 서비스
- **Glide**: 이미지 로딩 및 캐싱
- **PhotoView**: 이미지 확대/축소 기능
- **Material Components**: Material Design 컴포넌트

### 📌 주요 기능

- **여행 기록 작성**: 여행 일정, 제목, 사진, 위치 정보를 포함한 여행 기록을 작성할 수 있습니다.
- **My Feed**: 본인이 작성한 여행 기록을 목록 형태로 확인할 수 있습니다.
- **Community**: 다른 유저들이 공개한 여행 기록을 Hero 이미지 형태로 탐색하고, 검색 기능을 통해 원하는 기록을 찾을 수 있습니다.
- **지도 탐색**: Google Maps를 통해 본인이 작성한 여행 기록의 위치를 지도에서 확인하고, 클러스터링을 통해 효율적으로 탐색할 수 있습니다.
- **상세 페이지**: 각 여행 기록의 상세 정보를 확인하고, 사진을 그리드 형태로 탐색할 수 있습니다.
- **프로필**: 사용자 정보 확인, 로그아웃, 회원 탈퇴를 진행할 수 있습니다.
- **이미지 전체 화면**: 사진을 탭하여 전체 화면으로 확대하여 볼 수 있습니다.

### 📍 자세한 기능

#### 인증 및 사용자 관리
- **Google 로그인**: Google 계정을 통한 간편 로그인
- **이메일 회원가입/로그인**: 이메일과 비밀번호를 통한 회원가입 및 로그인
- **자동 로그인**: Firebase Auth를 통한 세션 관리 (앱 재시작 시 자동 로그인)
- **회원 탈퇴**: 비밀번호 재인증 후 계정 삭제

#### 여행 기록 관리
- **글 작성**: 여행 제목, 일정(시작일, 여행 일수), 사진, 위치 정보를 포함한 기록 작성
- **글 수정**: 작성한 여행 기록을 수정할 수 있습니다
- **글 삭제**: 자신이 작성한 기록을 삭제할 수 있습니다
- **공개/비공개 설정**: 여행 기록을 SNS 게시판으로의 공개 또는 비공개로 설정할 수 있습니다

#### 위치 기반 기능
- **EXIF 위치 추출**: 사진의 EXIF 데이터에서 위치 정보 자동 추출
- **지도 마커**: 각 여행 기록의 위치를 지도에 해당 사진의 썸네일 마커로 표시
- **클러스터링**: 가까운 위치의 마커를 자동으로 클러스터링하여 효율적으로 표시
- **역지오코딩**: 좌표를 주소로 변환하여 표시

#### 이미지 관리
- **다중 이미지 업로드**: 여러 장의 사진을 한 번에 선택하여 업로드
- **이미지 압축**: 업로드 전 이미지를 자동으로 압축하여 저장 공간 절약
- **이미지 회전 처리**: 가로사진과 세로사진에 대해 EXIF 방향 정보를 읽어 올바른 방향으로 표시
- **썸네일 생성**: 지도 마커용 썸네일 자동 생성
- **Glide를 통한 이미지 로딩**: 효율적인 이미지 로딩 및 캐싱

#### UI/UX
- **무한 스크롤**: 글 목록을 무한 스크롤로 불러오기
- **그리드 레이아웃**: 상세 페이지에서 사진을 그리드 형태로 탐색 (열 개수 조절 가능)
- **Hero 이미지**: Community 탭에서 주요 이미지를 ViewPager로 크게 표시
- **검색 기능**: Community 탭에서 도시/국가명으로 여행 기록 검색
- **정렬 기능**: Community 탭에서 최신순/오래된순 정렬
- **다크 모드 지원**: 시스템 설정에 따른 다크 모드 지원
- **Material Design 3**: Material 3 테마 적용 (테마는 Material 3, 위젯은 Material Components 사용)

### 🛠️ 설치 및 실행 방법

#### 전제 조건
- Android Studio (Hedgehog | 2023.1.1 이상)
- JDK 11 이상
- Android SDK (API 24 이상)
- Google Maps API 키
- Firebase 프로젝트 설정
  - `google-services.json` 파일 필요
  - Firebase Authentication 활성화
  - Firestore Database 설정
  - Firebase Storage 설정

#### 프로젝트 설정

1. **프로젝트 클론**
```bash
git clone https://github.com/reproducepark/madcamp01.git
cd madcamp01
```

2. **Google Maps API 키 설정**
   - `local.properties` 파일에 Google Maps API 키 추가:
   ```
   MAPS_API_KEY=YOUR_API_KEY_HERE
   ```

3. **Firebase 설정**
   - Firebase Console에서 프로젝트 생성
   - `google-services.json` 파일을 `app/` 디렉토리에 배치
   - Firebase Authentication에서 이메일/비밀번호 및 Google 로그인 활성화
   - Firestore Database 생성
   - Firebase Storage 설정

4. **프로젝트 빌드**
```bash
# Windows
.\gradlew.bat assembleDebug

# macOS/Linux
./gradlew assembleDebug
```

5. **APK 설치**
   - 빌드된 APK 파일은 `app/build/outputs/apk/debug/app-debug.apk`에 생성됩니다
   - Android 기기에 설치하여 실행

#### 개발 환경 설정
- Android Studio에서 프로젝트 열기
- Gradle 동기화
- 에뮬레이터 또는 실제 기기에서 실행

### 📱 주요 화면

- **스플래시 화면**: 앱 시작 시 표시
- **로그인 화면**: Google 로그인 및 이메일 회원가입/로그인
- **메인 화면**: 하단 네비게이션 바를 통한 화면 전환
  - **My Feed 탭**: 본인이 작성한 여행 기록 목록 (무한 스크롤, 본인 글만 표시)
  - **Community 탭**: 공개된 여행 기록을 Hero 이미지 형태로 탐색 (검색 및 정렬 기능 포함)
  - **Map 탭**: 본인이 작성한 여행 기록의 위치를 지도에 썸네일 마커로 표시
  - **Profile 탭**: 사용자 정보 확인, 로그아웃, 회원 탈퇴
- **글 작성 화면**: 여행 기록 작성 및 수정 (공개/비공개 설정 가능)
- **상세 화면**: 여행 기록 상세 정보 및 사진 그리드
- **이미지 전체 화면**: 사진 확대 보기

### 🔐 권한 요청

앱은 다음 권한을 요청합니다:
- **인터넷**: Firebase 및 Google Maps API 사용
- **위치 정보** (ACCESS_FINE_LOCATION, ACCESS_COARSE_LOCATION): 현재 위치 기반 기능
- **저장소 접근** (READ_EXTERNAL_STORAGE, READ_MEDIA_IMAGES, READ_MEDIA_VIDEO): 사진 선택 및 업로드
- **미디어 위치 정보** (ACCESS_MEDIA_LOCATION): 사진의 EXIF 데이터에서 위치 정보 추출

---

**PinLog** - 당신의 여행을 기록하고 공유하세요 ✈️
