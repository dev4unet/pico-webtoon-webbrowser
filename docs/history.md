# 진행내역

## 2026-02-14

### PC모드 페이지 줌 기능

- **PC모드 페이지 줌**: 50%~100% CSS zoom 적용 (메인 메뉴에서 조절)
- CSS `document.documentElement.style.zoom`으로 리로드 없이 즉시 반영
- 현재 탭에만 적용, 설정 내보내기/가져오기 포함

### 다이얼로그 UI 개선

- **다이얼로그 최대 너비 400dp 제한**: PC모드(가로)에서 다이얼로그가 너무 넓어지는 문제 해결
- `adjustDialogWidth()` 헬퍼 메서드 추가, 모든 AlertDialog에 적용
- **서브 다이얼로그 뒤로가기 네비게이션**: "취소" → "뒤로" 변경
  - 설정/모드선택/줌 → 메인 메뉴로 복귀
  - 최대기록/표시방식 → 설정 메뉴로 복귀

### 문서 현행화 및 v0.2.1 릴리스

- architecture.md, README.md, history.md 현행화
- v0.2.1 태그 생성 및 릴리스

### README.md 개편 및 스크린샷 추가

- README.md 전면 개편 (앱 소개, 개발 배경, 기능 설명 보강)
- 앱 스크린샷 이미지 추가 (`docs/sshot1-500.jpeg`, `docs/sshot2-500.jpeg`)
- `resizeableActivity` 설정 현행화 (`false` → `true`, `maxAspectRatio` 제거)

---

## 2026-02-13

### 브라우저 모드 시스템 개편

- **3모드 → 2모드 변경**: 웹툰 모드 (세로/모바일) + PC 모드 (가로/데스크톱)
- 일반 모드(세로/데스크톱) 제거 (PICO VR에서 가독성 불량)
- **동적 viewport 시스템** 도입: `injectViewportForMode()` + `OnLayoutChangeListener`
- `useWideViewPort(false)` + `loadWithOverviewMode(false)` 전략

### 다이얼로그 너비 MATCH_PARENT 수정

- 메인 메뉴, 설정, 스케일링 모드 다이얼로그에 MATCH_PARENT 적용
- SwipeRefreshLayout PC모드에서 비활성화

### 아키텍처 문서 생성

- `docs/architecture.md` 최초 생성 (앱 전체 구조, 설계 결정 기록)

---

## 2026-02-12

### APK 빌드 이슈 해결 (v0.1.0 → v0.1.1)

- Debug/Release APK 버전 불일치 해결 (빌드 step 통합)
- Release APK 설치 불가 해결 (signingConfig 추가)
- **공유 Keystore 시스템** 구축 (debug + release)
  - 모든 개발 PC + CI에서 동일한 서명
  - GitHub Secrets에 Base64 인코딩 등록
- **Git 태그 기반 버전 관리** 도입 (version.properties 단순화)

### 기능 구현 (v0.2.0)

- **메인 메뉴 구조**: ≡ 버튼 → 6항목 메인 메뉴 → 설정 메뉴
- **브라우저 모드**: 웹툰 모드 + PC 모드 (UA, viewport, 화면 방향 조합)
- **탭 상태 영속성**: 앱 종료 시 저장, 시작 시 복원 (ON/OFF)
- **방문 기록**: 자동 기록, 최대 보관 개수 설정, 표시 방식 선택
- **즐겨찾기 내보내기/불러오기**: JSON 형식
- **Pull-to-refresh**: SwipeRefreshLayout
- **환경 설정 내보내기/불러오기**: 즐겨찾기/방문 기록 포함 선택
- **탭 바 표시/숨김 설정**
- 뒤로가기 앱 종료 방지 버그 수정

---

## 2026-02-11

### GitHub Actions 자동 배포 시스템 구축

- **GitHub Actions 자동 배포 시스템 구축**:
  - `.github/workflows/release.yml` 생성
  - Git 태그 푸시 시 자동으로 Debug/Release APK 빌드
  - GitHub Release에 APK 파일 자동 첨부
  - 릴리스 페이지에 버전 정보 및 설치 가이드 자동 생성
- **릴리스 배포 가이드 작성**: `RELEASE.md`
  - 자동 배포 사용법
  - 로컬 빌드 방법
  - 릴리스 APK 서명 설정 가이드 (선택사항)
  - 버전 관리 방법
- **.gitignore 업데이트**: keystore 파일, 임시 파일 제외 추가
- **README.md 업데이트**: GitHub Actions 자동 배포 섹션 추가

---

## 2026-02-10

### 프로젝트 분석 및 환경 조사

- 최소 기능 분석: PICO 4 Ultra VR용 세로형 웹툰 브라우저 개발
- 개발 환경 확인:
  - JDK 17 설치 확인
  - Android SDK (platform 34, build-tools 34.0.0) 설치 확인
  - ADB 36.0.2 설치 확인
  - Gradle Wrapper 부트스트랩 필요
- PICO 4 Ultra SDK 및 개발 방식 조사
  - 일반 Android 2D 앱으로 개발 가능 (PICO XR SDK 불필요)
  - 2D 앱은 VR 공간에서 플로팅 윈도우로 실행됨
  - maxAspectRatio + portrait orientation으로 세로형 윈도우 구현 가능

### 개발 계획 수립

- 기술 접근 방식 결정: Android WebView 앱
- 세로형 구현 방법: `resizeableActivity="false"` + `maxAspectRatio="5.0"` + `screenOrientation="portrait"`
- 기본 홈페이지 설정
- 프로젝트 구조 및 전체 구현 계획 설계 완료
- 필요시 추가할 기능: 탭 기능, 즐겨찾기 기능, 스마트폰 테스트

### Gradle Wrapper 부트스트랩 및 빌드 설정

- Gradle 8.4 바이너리 다운로드 (services.gradle.org)
- 임시 디렉토리에 압축 해제 후 `gradle wrapper --gradle-version 8.4` 실행
- gradlew.bat, gradlew, gradle/wrapper/* 생성 완료
- settings.gradle 생성 (pluginManagement, dependencyResolutionManagement 설정)
- build.gradle (프로젝트) 생성 - AGP 8.2.0
- app/build.gradle (모듈) 생성 - compileSdk 34, minSdk 29, targetSdk 34
- gradle.properties, local.properties 생성
- 의존성: appcompat 1.6.1, webkit 1.8.0, material 1.10.0, swiperefreshlayout 1.1.0

### Android 리소스 및 매니페스트 생성

- AndroidManifest.xml 생성
  - 핵심 설정: `resizeableActivity="false"`, `maxAspectRatio="5.0"`, `screenOrientation="portrait"`
  - 권한: INTERNET, ACCESS_NETWORK_STATE, DOWNLOAD_WITHOUT_NOTIFICATION
  - FileProvider 설정 (다운로드/업로드용)
  - Intent filter (http/https URL 처리)
- 레이아웃 파일 생성:
  - activity_main.xml: 탭바(36dp) + 툴바(44dp) + 프로그레스바(3dp) + WebView 컨테이너
  - tab_item.xml: 개별 탭 뷰 (제목 + 닫기 버튼)
  - bookmark_item.xml: 즐겨찾기 목록 아이템
- values 리소스: strings.xml (한국어), colors.xml (다크 테마), styles.xml
- drawable 리소스: url_bar_bg.xml, tab_background_active/inactive.xml, 앱 아이콘 (globe vector)
- xml/file_paths.xml: FileProvider 경로 설정

### MainActivity.java 구현

- 앱 핵심 로직 구현 (단일 Activity 구조)
- **탭 관리**: TabInfo 클래스, addNewTab(), closeTab(), switchToTab(), refreshTabBar()
  - 다중 WebView 인스턴스를 FrameLayout에 스택으로 관리
  - 탭 전환 시 visibility 토글
  - onCreateWindow 처리 (팝업 → 새 탭)
- **WebView 설정**: JS, DOM Storage, 쿠키, 혼합 컨텐츠, 줌, 커스텀 UA
- **네비게이션**: 뒤로/앞으로/새로고침/홈 버튼 + alpha 피드백
- **URL바**: URL 입력 + actionGo + 자동 https:// 추가 + Google 검색 폴백
- **즐겨찾기**: SharedPreferences + JSON 저장/로드, 별 아이콘 색상 변경, 목록 다이얼로그
- **다운로드**: DownloadManager 연동, 쿠키 전달
- **파일 업로드**: onShowFileChooser 처리
- **수명주기**: onResume/onPause/onDestroy에서 WebView 관리

### 빌드 및 스마트폰 테스트

- `gradlew.bat assembleDebug` 빌드 성공 (1분 47초)
  - 경고: deprecated API 사용 (onBackPressed) - 기능에는 영향 없음
- APK 경로: `app/build/outputs/apk/debug/app-debug.apk`
- 연결된 스마트폰 확인
- `adb install -r` 설치 성공
- `adb shell am start` 앱 실행 성공
- **테스트 결과**:
  - 홈페이지 정상 로딩 ✓
  - 다크 테마 적용 ✓
  - 세로형 레이아웃 정상 ✓
  - 새 탭 생성 (+버튼) ✓ → 탭바에 2개 탭 표시
  - 탭 전환 ✓
  - 즐겨찾기 추가 (★버튼) ✓ → "즐겨찾기에 추가됨" 토스트
  - 즐겨찾기 목록 (≡버튼) ✓ → 다이얼로그에 저장된 항목 표시
  - 홈 버튼 ✓ → 홈페이지로 복귀
  - Intent로 URL 전달 (am start -d URL) ✓ → Naver 로딩 성공


### 문서 작성

- README.md 작성 완료
  - 아키텍처, 기술 스택, 핵심 설계 결정
  - 개발 환경 상세
  - 프로젝트 구조
  - 빌드 방법 및 디바이스 설치 방법
  - 주요 기능 목록
  - 다른 컴퓨터에서 환경 구축 가이드
- 진행내역.md 업데이트 완료

### Git 저장소 연결 및 패키지명/APK명/버전 변경

- .gitignore 생성 (build/, .gradle/, local.properties, *.apk, screenshots/)
- **APK 파일명 변경**: `app-debug.apk` → `pico-webtoon-webbrowser-{buildType}-v{version}.apk`
  - applicationVariants.configureEach로 outputFileName 커스텀
- **버전 자동 증가**: `app/version.properties` + `incrementVersion` 태스크
  - 빌드마다 VERSION_BUILD 자동 증가
  - versionCode/versionName에 반영
  - 업데이트 설치 시 기존 데이터 보존
- clean 빌드 성공: `pico-webtoon-webbrowser-debug-v1.0.0.1.apk` 생성
- 새 패키지로 디바이스 설치 및 실행 확인

### 문서 작성 및 Git 커밋

- `README.md` 업데이트 (변경된 패키지명, APK명, 버전 정보 반영)
- `진행내역.md` 업데이트
- Git 초기 커밋 및 원격 저장소 푸시

### 기능 개선 및 PICO 설치

- **홈페이지 설정 기능 추가** (홈 버튼 길게 누르기):
  - 현재 페이지를 홈으로 설정
  - URL 직접 입력으로 홈 변경
  - 기본값으로 초기화
  - SharedPreferences에 `home_url` 키로 저장
- **탭 최대 너비 제한 버그 수정**:
  - `maxWidth` 속성은 LinearLayout에서 동작하지 않는 문제 발견
  - `refreshTabBar()`에서 프로그래밍적으로 160dp 최대 너비 제한 적용
  - 타이틀이 길거나 없는 경우에도 닫기 버튼(X) 항상 표시
- PICO 4 Ultra 디바이스에 v1.0.0.2 빌드 설치 및 실행 확인