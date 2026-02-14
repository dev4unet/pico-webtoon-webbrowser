# PICO Webtoon Browser - 앱 아키텍처

> 마지막 업데이트: 2026-02-14
> 이 문서는 앱의 전체 구조와 핵심 설계 결정을 기록합니다.
> 기능 추가/변경 시 본 문서도 함께 수정하여 현행화합니다.

---

## 앱 개요

PICO 4 Ultra VR 헤드셋에서 실행되는 **Android 2D 웹툰 브라우저** 앱입니다.
VR 공간의 플로팅 윈도우로 동작하며, 세로형(Portrait) 레이아웃에 최적화되어 있습니다.

- **패키지**: `net.dev4u.webtoonbrowser`
- **구조**: Single Activity (`MainActivity.java`)
- **최소 SDK**: Android (PICO 4 Ultra 호환)

---

## 전체 구조

```
MainActivity (AppCompatActivity)
├── UI Layer
│   ├── URL Bar (EditText) - URL 입력 및 검색
│   ├── Navigation Buttons - 뒤로/앞으로/새로고침/홈
│   ├── Tab Bar (LinearLayout, 동적 생성) - 탭 전환/닫기
│   ├── WebView Container (FrameLayout) - 탭별 WebView
│   ├── SwipeRefreshLayout - Pull-to-refresh
│   └── Dialog System - 메뉴, 설정, 북마크 등
│
├── Tab Management
│   ├── List<TabInfo> tabs - 탭 목록
│   ├── activeTabIndex - 활성 탭 인덱스
│   └── Tab 생명주기 (생성/전환/닫기/저장/복원)
│
├── Browser Mode System (2모드)
│   ├── 모드 0: 웹툰 모드 (세로/모바일 UA)
│   └── 모드 1: 일반 모드 (가로/데스크톱 UA/동적 viewport)
│
├── Data Layer (SharedPreferences + JSON)
│   ├── 북마크 (bookmarks)
│   ├── 방문 기록 (history)
│   ├── 탭 상태 (saved_tabs)
│   └── 앱 설정 (각종 preference 키)
│
└── File I/O
    ├── 북마크 내보내기/가져오기 (JSON)
    └── 설정 내보내기/가져오기 (JSON)
```

---

## 핵심 설계 결정

### 1. Single Activity 구조

**결정**: 전체 앱을 `MainActivity.java` 하나로 구현

**이유**:
- 단순한 브라우저 앱에 Fragment/Navigation 불필요
- PICO VR 환경에서 Activity 전환 시 윈도우 깜빡임 방지
- 모든 상태를 한 곳에서 관리하여 일관성 유지

### 2. 탭 관리: List + FrameLayout

**결정**: `List<TabInfo>`로 탭을 관리하고, 모든 WebView를 `FrameLayout`에 겹쳐 배치

**이유**:
- WebView를 매번 생성/파괴하면 로딩 지연 발생
- visibility 전환으로 즉시 탭 전환 가능
- 각 탭의 스크롤 위치, 로그인 상태 등이 유지됨

**구현**:
```java
// 탭 전환 시
for (TabInfo tab : tabs) {
    tab.webView.setVisibility(View.GONE);
}
tabs.get(index).webView.setVisibility(View.VISIBLE);
```

### 3. 브라우저 모드 시스템 (2모드)

**결정**: UA(User-Agent) + viewport + 화면 방향 조합으로 2가지 모드 구현

**이유**:
- WebSettings만으로는 PICO VR에서 시각적 차이가 없음 (실기 테스트 확인)
- 모바일 UA → 웹툰 사이트가 모바일 레이아웃 제공
- 데스크톱 UA + 동적 viewport → 데스크톱 레이아웃 + 창 크기 반응
- 일반 세로 모드는 가독성 불량으로 제거됨 (v0.3.0)

**모드별 설정**:

| 설정 | 웹툰모드 (0) | PC모드 (1) |
|------|-------------|-------------|
| User-Agent | 모바일 | 데스크톱 |
| UseWideViewPort | false | false |
| LoadWithOverviewMode | false | false |
| LayoutAlgorithm | TEXT_AUTOSIZING | NORMAL |
| 화면 방향 | Portrait | Landscape |
| Viewport 주입 | 없음 | width=device-width (고정 viewport 오버라이드) |

**viewport 전략**: `useWideViewPort(false)` + `loadWithOverviewMode(false)`로 viewport가 실제 View dp 너비를 따르게 함. 창 크기 변경 시 콘텐츠가 자연스럽게 리플로우됨. `injectViewportForMode()`는 페이지가 가진 고정 viewport를 `width=device-width`로 오버라이드. `OnLayoutChangeListener`가 창 크기 변경 감지 시 viewport를 재주입.

**PC모드 페이지 줌**: CSS `document.documentElement.style.zoom`으로 50%~100% 줌 적용. `injectViewportForMode()`에서 viewport 오버라이드 후 줌 주입. 줌 변경 시 현재 탭에만 적용 (리로드 없이 즉시 반영).

**주의**: `applyScalingMode()`에서 모든 설정이 한 번에 적용됨. 일부만 변경하면 불일치 발생.

### 4. 데이터 저장: SharedPreferences + JSON

**결정**: 모든 데이터를 SharedPreferences에 JSON 문자열로 저장

**이유**:
- SQLite나 Room은 단순 목록 데이터에 오버스펙
- JSON으로 직렬화하면 내보내기/가져오기가 간편
- SharedPreferences는 앱 제거 전까지 영속

**SharedPreferences 키 목록** (`PREFS_NAME = "webtoon_browser_prefs"`):

| 키 | 타입 | 기본값 | 용도 |
|----|------|--------|------|
| `bookmarks` | String (JSON) | `[]` | 북마크 목록 |
| `history` | String (JSON) | `[]` | 방문 기록 |
| `saved_tabs` | String (JSON) | `[]` | 탭 상태 |
| `home_url` | String | `https://s.dev4u.net/code` | 홈페이지 |
| `scaling_mode` | int | `0` | 브라우저 모드 |
| `restore_tabs` | boolean | `false` | 탭 복원 여부 |
| `max_history` | int | `100` | 최대 기록 수 |
| `show_tabs` | boolean | `true` | 탭 바 표시 |
| `bookmark_display` | int | `0` | 북마크 표시 방식 |
| `history_display` | int | `0` | 기록 표시 방식 |
| `pc_zoom` | int | `100` | PC모드 페이지 줌 (%) |

### 5. 메뉴 구조: 2단계 다이얼로그

**결정**: AlertDialog 기반 2단계 메뉴 (메인 메뉴 → 설정)

**이유**:
- Android Menu/ActionBar는 PICO VR에서 접근성 낮음
- 다이얼로그는 VR 플로팅 윈도우에서도 정상 표시
- 자주 사용하는 기능을 메인 메뉴에 배치

**메인 메뉴 구조 (6~7항목)**:
1. 즐겨찾기 목록
2. 방문 기록
3. 브라우저 모드: {현재모드} (→ 2모드 선택 다이얼로그)
4. PC모드 페이지 줌: N% (PC모드일 때만 표시, → 줌 선택 다이얼로그)
5. 앱 시작 시 탭 복원: ON/OFF (즉시 토글)
6. 탭 바 표시: ON/OFF (즉시 토글)
7. 설정 (→ 설정 메뉴)

**설정 메뉴 구조 (8항목)**:
1. 홈페이지 설정
2. 최대 방문 기록 개수
3. 즐겨찾기 표시 방식
4. 방문 기록 표시 방식
5. 즐겨찾기 내보내기/가져오기
6. 설정 내보내기/가져오기
7. 방문 기록 삭제
8. 앱 정보

### 6. 다이얼로그 UI 패턴

**결정**: 모든 다이얼로그 최대 너비 400dp 제한 + 서브 다이얼로그에 "뒤로" 네비게이션

**이유**:
- PC모드(가로)에서 MATCH_PARENT 다이얼로그가 너무 넓어 사용성 저하
- 서브 다이얼로그에서 "취소"만 있으면 메인 메뉴로 돌아갈 수 없어 불편

**구현**:
```java
// 다이얼로그 너비 제한 (모든 AlertDialog에 적용)
private void adjustDialogWidth(AlertDialog dialog) {
    int maxWidthPx = (int) (400 * getResources().getDisplayMetrics().density);
    int width = Math.min(screenWidth, maxWidthPx);
    dialog.getWindow().setLayout(width, WRAP_CONTENT);
}

// 서브 다이얼로그 뒤로가기 패턴
.setNegativeButton(R.string.go_back, (d, w) -> showParentMenu())
```

**네비게이션 구조**:
- 메인 메뉴 → 설정: "뒤로" → `showMainMenu()`
- 메인 메뉴 → 브라우저 모드 선택: "뒤로" → `showMainMenu()`
- 메인 메뉴 → PC모드 줌: "뒤로" → `showMainMenu()`
- 설정 → 최대 기록 개수: "뒤로" → `showSettings()`
- 설정 → 즐겨찾기 표시: "뒤로" → `showSettings()`
- 설정 → 기록 표시: "뒤로" → `showSettings()`

---

## Inner Class 구조

```java
// 탭 정보 (WebView + 메타데이터)
static class TabInfo {
    WebView webView;
    String title;
    String url;
}

// 북마크 항목
static class BookmarkEntry {
    String title;
    String url;
}

// 방문 기록 항목
static class HistoryEntry {
    String title;
    String url;
    long timestamp;
}
```

---

## 파일 구조

```
app/src/main/
├── java/net/dev4u/webtoonbrowser/
│   └── MainActivity.java          # 전체 앱 로직
├── res/
│   ├── layout/
│   │   ├── activity_main.xml      # 메인 레이아웃
│   │   ├── tab_item.xml           # 탭 아이템 (동적 생성)
│   │   └── bookmark_item.xml      # 북마크 아이템
│   ├── drawable/
│   │   ├── tab_background_active.xml
│   │   ├── tab_background_inactive.xml
│   │   └── url_bar_bg.xml
│   ├── values/
│   │   ├── strings.xml            # 문자열 리소스
│   │   ├── colors.xml             # 색상 정의
│   │   └── styles.xml             # 다크 테마
│   └── xml/
│       └── file_paths.xml         # FileProvider 경로
└── AndroidManifest.xml            # 권한, Activity 설정
```

---

## 빌드 환경

| 항목 | 값 |
|------|-----|
| JDK | 17 LTS (JDK 21은 빌드 이슈) |
| Gradle | 8.4 |
| AGP | 8.2.0 |
| minSdk | PICO 4 Ultra 호환 |
| CI/CD | GitHub Actions (자동 빌드/릴리스) |
| 서명 | 공유 debug keystore (환경 간 일관성) |

---

## 변경 이력

| 날짜 | 변경 내용 |
|------|----------|
| 2026-02-13 | 초기 아키텍처 문서 생성 (23개 완료 기능 기준) |
| 2026-02-13 | 브라우저 모드 3모드→2모드 변경, 동적 viewport 도입 |
| 2026-02-14 | PC모드 페이지 줌 기능 추가 (50%~100%), SharedPreferences 키 추가 |
| 2026-02-14 | 다이얼로그 UI 패턴 추가 (400dp 최대 너비, 뒤로가기 네비게이션) |
