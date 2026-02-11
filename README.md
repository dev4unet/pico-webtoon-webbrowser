# PICO 4 Ultra VR - Webtoon Browser

PICO 4 Ultra VR 헤드셋에서 웹툰을 편하게 읽기 위한 **세로형(Portrait) 웹 브라우저** 앱.

개인적으로 Pico4 Ultra VR환경에서는 게임보다는 주로 방에 누워서 자체 제공되는 VR용 기본 웹 브라우저를 이용해서 편하게 제 블로그 글을 확인하거나 가끔씩 네이버 웹 툰 등의 웹 기반 서핑을 하는데, `제가 시력이 안 좋아서 안경 없이 화면을 크게 확대해서 사용`하는 데 자체 제공되는 VR용 기본 웹브라우저는 가로나 세로로 편하게 사이즈 조절이 가능해서 큰 불편 없이 사용하고 있었는데 `세로 길이가 조금 더 길어졌으면 하는 아쉬움`이 생겼습니다.

Pico4 Ultra의 경우 안드로이드용 APK 설치가 쉽기에 안드로이드용 Chrome 웹 브라우저 앱을 설치해 봤는데 아쉽게도 기본 웹 브라우저와 달리 `Chrome의 경우 대각선으로만 크기를 늘릴 수 있어서 가로 길이가 너무 길어지는 문제`가 있었습니다.

평소 네이버 인증이 귀찮아서 네이버 웹툰은 네이버 웹툰 앱을 설치해서 편하게 누워서 보고 있었기에, `제가 사용하는 웹 서핑 용도에 맞도록` 평소처럼 누워서 편하게 사용하기 위해 네이버 `웹툰 앱처럼 세로가 긴 브라우저를 대충 간단하게 구현`했습니다.
원래는 기본 작업은 기본 웹 브라우저를 이용하다 특정 화면을 누워서 볼 때에만 사용하면 되니 주소창만 추가하려고 했었는데 살짝 욕심이 나서 조금은 더 사용하기 위해 버그가 생기더라도 `탭과 즐겨찾기를 대충 구현`했기에 아쉽지만 해당 영역으로 인한 세로 화면의 손실이 조금 생겼습니다.^^;;   
(필요하면 나중에 불 필요한 기능은 없애 버리겠지만 굳이 개발에 시간을 투자하고 싶지는 않아서...)

앱 이름을 브라우저로 하려다 세로형 브라우저보다는 보통 웹툰이 세로 형태로 많이 사용되기에 앱 이름을 직관적으로 PICO 웹툰 웹 브라우저라고 지었습니다.
기본 웹 브라우저처럼 가로/세로 자유롭게 사이즈 변경이 가능한 것은 아니고 `크롬처럼 대각선으로만 사이즈가 늘어 나지만 세로형 브라우저라서 다행히 제가 원하는 형태로 동작`하고 `기본 웹 브라우저 보다는 세로 길이가 조금 더 길게 늘어 나서 개인적으로는 만족`스럽네요.

지금 생각해 보니 웹 툰전용 웹 브라우저를 찾아서 설치하는 것이 더 좋았을 것 같기도 합니다.^^


이제 막 만들었기에 가끔씩 사용하면서 불편한 부분이 생길 경우 간단하게 수정되면 모르겠으나, 기본적으로 학습겸 심심풀이로 대충 간단하게 만들었고 관련 기본 지식이 없는 관계로 `추가 업데이트는 없을 예정이니` `만약, 필요한 기능이나 수정이 필요한 경우 편하게 Fork해서 사용`하시기 바랍니다.

---

## 아키텍처

### 기술 스택
| 항목 | 내용 |
|------|------|
| 플랫폼 | Android (일반 2D 앱) |
| 최소 SDK | API 29 (Android 10) |
| 타겟 SDK | API 34 (Android 14) |
| 빌드 도구 | Gradle 8.4 + AGP 8.2.0 |
| 언어 | Java 17 |
| 주요 라이브러리 | AndroidX AppCompat, WebKit, Material, SwipeRefreshLayout |
| VR SDK | 불필요 (PICO에서 2D 플로팅 윈도우로 실행) |

### 핵심 설계 결정

#### 세로형 윈도우 구현
```xml
android:screenOrientation="portrait"
android:resizeableActivity="false"
android:maxAspectRatio="5.0"
```
- `resizeableActivity="false"` — 이 설정이 핵심. `true`로 설정하면 `maxAspectRatio`가 무시됨
- `maxAspectRatio="5.0"` — 5:1 비율까지 세로로 긴 윈도우 허용
- `screenOrientation="portrait"` — 세로 모드 고정
- PICO VR에서 이 조합은 세로로 긴 2D 플로팅 윈도우를 생성

#### 왜 일반 Android 앱인가?
- PICO 4 Ultra는 Android 기반 OS 사용
- 일반 Android 앱은 VR 공간에서 2D 플로팅 윈도우로 실행됨
- 웹툰 읽기는 2D 컨텐츠이므로 VR 네이티브 렌더링이 불필요
- PICO XR SDK 없이도 충분하며, 개발 복잡도가 낮음

### 앱 구조
```
net.dev4u.webtoonbrowser
└── MainActivity.java      # 앱 전체 로직 (단일 Activity 구조)
    ├── Tab 관리            # 다중 탭 (WebView 인스턴스 관리)
    ├── WebView 설정        # JS, 쿠키, 혼합 컨텐츠 등
    ├── 네비게이션          # 뒤로/앞으로/새로고침/홈
    ├── URL바              # URL 입력, 자동 완성, 검색
    ├── 즐겨찾기            # SharedPreferences + JSON 저장
    ├── 다운로드            # DownloadManager 연동
    └── 파일 업로드         # WebChromeClient 파일 선택기
```

### UI 레이아웃 구조
```
┌───────────────────────────────────────┐
│ [Tab1] [Tab2] [Tab3]            [+]   │  ← 탭바 (36dp)
├───────────────────────────────────────┤
│ [◀][▶] [____URL바____] [★][↻][⌂][≡] │  ← 툴바 (44dp)
├───────────────────────────────────────┤
│ ████████████ Progress ████████████    │  ← 프로그레스바 (3dp)
├───────────────────────────────────────┤
│                                       │
│             WebView                   │  ← 웹 컨텐츠 영역
│       (SwipeRefreshLayout)            │  ← (나머지 전체)
│                                       │
└───────────────────────────────────────┘
```
- 툴바를 최소화(44dp)하여 웹툰 표시 영역 최대화
- 다크 테마로 VR 환경에서 눈 피로도 감소

### 데이터 저장
- **즐겨찾기**: `SharedPreferences`에 JSON 배열로 저장
  - 키: `bookmarks`, 값: `[{"title":"...", "url":"..."}]`
- **쿠키**: Android `CookieManager` 자동 관리
- **웹 캐시**: WebView 기본 캐시 (`LOAD_DEFAULT`)

---

## 개발 환경

### 필수 소프트웨어 요약

| 소프트웨어 | 버전 | 비고 |
|-----------|------|------|
| JDK | OpenJDK 17 LTS | Microsoft/Oracle/Adoptium 등 |
| Android SDK | API 34 | Android Studio 또는 command-line tools |
| Android Build Tools | 34.0.0 | (SDK 내 포함) |
| ADB | 36.0.2+ | (SDK platform-tools 내 포함) |
| Gradle | 8.4 (Wrapper) | 프로젝트 내 `gradlew.bat` |

**상세 개발 환경 구축 방법**:
- **Windows**: [docs/development-setup-windows.md](docs/development-setup-windows.md)
  - JDK 17 설치 및 환경변수 설정
  - Android SDK 설치 (Android Studio / Command-line Tools)
  - 프로젝트 Clone 및 빌드
  - 디바이스 설치 및 테스트
  - 문제 해결 가이드

---

## 프로젝트 구조

```
pico-webtoon-browser/
├── app/
│   ├── build.gradle                          # 모듈 빌드 설정
│   ├── version.properties                    # 버전 관리 (빌드 번호 자동 증가)
│   └── src/main/
│       ├── AndroidManifest.xml               # 앱 매니페스트 (세로형 설정 핵심)
│       ├── java/net/dev4u/webtoonbrowser/
│       │   └── MainActivity.java             # 앱 메인 액티비티
│       └── res/
│           ├── layout/
│           │   ├── activity_main.xml         # 메인 레이아웃
│           │   ├── tab_item.xml              # 탭 아이템
│           │   └── bookmark_item.xml         # 즐겨찾기 아이템
│           ├── drawable/                     # 드로어블 리소스
│           ├── mipmap-anydpi-v26/            # 앱 아이콘
│           ├── values/                       # 문자열, 색상, 스타일
│           └── xml/                          # FileProvider 경로 설정
├── .github/workflows/
│   └── release.yml                           # GitHub Actions 자동 배포
├── docs/
│   ├── README.md                             # 문서 폴더 안내
│   ├── RELEASE.md                            # 릴리스 배포 가이드
│   └── development-setup-windows.md          # Windows 개발 환경 구축 가이드
├── build.gradle                              # 프로젝트 빌드 설정
├── settings.gradle                           # 프로젝트 설정
├── gradle.properties                         # Gradle 속성
├── local.properties                          # 로컬 SDK 경로 (git 제외)
├── gradle/wrapper/                           # Gradle Wrapper
├── gradlew.bat                               # Gradle Wrapper 스크립트 (Windows)
├── gradlew                                   # Gradle Wrapper 스크립트 (Unix)
├── .gitignore                                # Git 제외 파일 목록
├── .gitmodules                               # Git Submodule 설정
├── README.md                                 # 이 파일
└── history.md                                # 개발 진행 기록
```

---

## 빌드 방법

### Debug APK 빌드
```batch
cd C:\workspace\picovrWebbrowser
set JAVA_HOME=C:\Program Files\Microsoft\jdk-17.0.17.10-hotspot
set ANDROID_HOME=C:\Users\dev4unet\AppData\Local\Android\Sdk
gradlew.bat assembleDebug
```

APK 출력 경로: `app\build\outputs\apk\debug\pico-webtoon-webbrowser-debug-v{version}.apk`

※ 빌드마다 version.properties의 VERSION_BUILD가 자동 증가하여 업데이트 설치 시 기존 데이터 유지

### Release APK 빌드
```batch
gradlew.bat assembleRelease
```
※ Release 빌드에는 서명 키 설정이 필요합니다.

---

## 디바이스 설치

### 스마트폰/PICO 설치
스마트폰이나 PICO VR 기기에서 apk를 직접 다운로드 및 실행하거나 adb로 설치
```batch
# 디바이스 연결 확인
adb devices

# APK 설치
adb install -r app\build\outputs\apk\debug\pico-webtoon-webbrowser-debug-v*.apk

# 앱 실행
adb shell am start -n net.dev4u.webtoonbrowser/.MainActivity
```

### PICO 4 Ultra 설치 시 주의사항
1. PICO 헤드셋에서 **개발자 모드** 활성화
2. **USB 디버깅** 활성화
3. USB 케이블로 PC와 연결
4. `adb devices`로 연결 확인 후 설치

---

## 릴리스 배포 (GitHub Actions)

이 프로젝트는 Git 태그를 푸시하면 자동으로 APK를 빌드하고 GitHub Release에 배포하는 CI/CD가 구축되어 있습니다.

### 자동 배포 사용법

```bash
# 1. 버전 태그 생성
git tag -a v1.0.1 -m "버전 1.0.1 릴리스"

# 2. 태그 푸시
git push origin v1.0.1

# 3. GitHub Actions가 자동으로 빌드 및 배포
# https://github.com/dev4unet/pico-webtoon-browser/releases 에서 확인
```

**자동으로 처리되는 작업:**
- JDK 17 환경 설정
- Debug APK 빌드
- Release APK 빌드
- GitHub Release 생성
- APK 파일을 Release에 첨부

자세한 내용은 [docs/RELEASE.md](docs/RELEASE.md) 참고

---

## 주요 기능

| 기능 | 설명 |
|------|------|
| 세로형 윈도우 | maxAspectRatio 5.0으로 세로로 긴 윈도우 |
| 다중 탭 | 탭 추가/전환/닫기 지원 |
| 즐겨찾기 | 현재 페이지 즐겨찾기 추가/삭제/목록 |
| URL 입력 | 직접 URL 입력, 자동 https 추가, 검색어 입력 시 Google 검색 |
| 네비게이션 | 뒤로/앞으로/새로고침/홈 |
| 홈페이지 설정 | 홈 버튼 길게 눌러서 홈페이지 변경 (현재 페이지/URL 직접 입력/기본값 초기화) |
| Pull-to-Refresh | 아래로 당겨서 새로고침 |
| 다운로드 | 파일 다운로드 지원 (DownloadManager) |
| 파일 업로드 | 파일 선택기를 통한 업로드 |
| 다크 테마 | VR 환경 최적화 다크 UI |
| 쿠키 지원 | 서드파티 쿠키 포함 |
| 혼합 컨텐츠 | HTTP/HTTPS 혼합 컨텐츠 허용 |

---

## 컴퓨터에 개발 환경 구축

윈도우즈 환경에서 이 프로젝트를 빌드하려면 다음 문서를 참고하세요:

- **Windows**: [docs/development-setup-windows.md](docs/development-setup-windows.md)
  - JDK 17 설치 (MSI / ZIP)
  - Android SDK 설치 (Android Studio / Command-line Tools)
  - 환경변수 설정 (JAVA_HOME, ANDROID_HOME)
  - 프로젝트 Clone 및 빌드
  - 문제 해결 가이드

**간단 요약**:
1. JDK 17 설치 및 JAVA_HOME 설정
2. Android SDK (API 34) 설치 및 ANDROID_HOME 설정
3. 프로젝트 Clone: `git clone https://github.com/dev4unet/pico-webtoon-browser.git`
4. local.properties 설정: `sdk.dir=C\:\\경로\\Android\\Sdk`
5. 빌드: `gradlew.bat assembleDebug`
