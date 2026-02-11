# Windows 개발 환경 구축 가이드

이 문서는 Windows 환경에서 PICO Webtoon Browser 프로젝트를 빌드하기 위한 개발 환경 구축 방법을 안내합니다.

---

## 필수 소프트웨어

| 소프트웨어 | 버전 | 필수 여부 | 비고 |
|-----------|------|----------|------|
| JDK | OpenJDK 17 LTS | ✅ 필수 | Microsoft/Oracle/Adoptium 등 |
| Android SDK | API 34 | ✅ 필수 | Android Studio 또는 command-line tools |
| Android Build Tools | 34.0.0 | ✅ 필수 | (SDK 내 포함) |
| ADB | 36.0.2+ | 권장 | (SDK platform-tools 내 포함) |
| Gradle | 8.4 (Wrapper) | 자동 | 프로젝트 내 `gradlew.bat` 포함 |
| Git | 최신 버전 | 권장 | 소스 코드 Clone용 |

---

## 1. JDK 17 설치

### 배포판 선택

이 프로젝트는 **Java 17 LTS**를 사용합니다. 여러 OpenJDK 배포판이 있으며, 모두 **무료**이고 **상업적 사용이 가능**합니다.

**주요 OpenJDK 배포판 비교**

| 배포판 | 라이센스 | Windows 최적화 | 장기 지원 | 추천 환경 | 다운로드 |
|--------|----------|----------------|-----------|-----------|----------|
| **Microsoft OpenJDK** | GPL v2 | ⭐⭐⭐ | ✅ Microsoft 지원 | **Windows (권장)** | [다운로드](https://learn.microsoft.com/ko-kr/java/openjdk/download) |
| Oracle OpenJDK | GPL v2 | ⭐⭐ | ✅ Oracle 지원 | 모든 플랫폼 | [다운로드](https://jdk.java.net/17/) |
| Eclipse Temurin (Adoptium) | GPL v2 | ⭐⭐ | ✅ Eclipse 재단 | 모든 플랫폼 | [다운로드](https://adoptium.net/) |
| Amazon Corretto | GPL v2 | ⭐⭐ | ✅ Amazon 지원 | AWS 환경 | [다운로드](https://aws.amazon.com/ko/corretto/) |

**Windows 사용자 권장**: **Microsoft Build of OpenJDK**
- Windows 플랫폼 최적화
- Microsoft의 장기 지원 및 보안 패치
- Azure, Visual Studio 등 Microsoft 제품과 통합

### 설치 방법 (Microsoft OpenJDK 기준)

#### 방법 1: MSI 인스톨러 (권장)

1. [Microsoft OpenJDK 다운로드 페이지](https://learn.microsoft.com/ko-kr/java/openjdk/download) 접속
2. **OpenJDK 17 LTS** → **Windows** → **x64** → **MSI 다운로드**
3. 다운로드한 MSI 파일 실행
4. 설치 마법사에 따라 설치 (기본 경로: `C:\Program Files\Microsoft\jdk-17.x.x`)
5. **"Add to PATH" 옵션 선택** (자동으로 환경변수 설정)

#### 방법 2: ZIP 파일 (수동 설치)

1. ZIP 파일 다운로드
2. 원하는 경로에 압축 해제 (예: `C:\sdk\open-jdk\jdk-17.0.18+8`)
3. 환경변수 수동 설정 (아래 참고)

### 환경변수 설정

#### JAVA_HOME 설정

**방법 1: 시스템 환경변수 GUI**

1. `Win + R` → `sysdm.cpl` 입력 → 엔터
2. **고급** 탭 → **환경 변수** 클릭
3. **시스템 변수**에서 **새로 만들기**:
   - 변수 이름: `JAVA_HOME`
   - 변수 값: `C:\Program Files\Microsoft\jdk-17.0.17.10-hotspot` (실제 설치 경로)
4. **Path** 변수 편집 → **새로 만들기** → `%JAVA_HOME%\bin` 추가
5. **확인** 클릭

**방법 2: 명령 프롬프트 (관리자 권한)**

```batch
# JAVA_HOME 설정
setx JAVA_HOME "C:\Program Files\Microsoft\jdk-17.0.17.10-hotspot" /M

# Path에 추가
setx Path "%Path%;%JAVA_HOME%\bin" /M
```

**방법 3: PowerShell (관리자 권한)**

```powershell
# JAVA_HOME 설정
[System.Environment]::SetEnvironmentVariable("JAVA_HOME", "C:\Program Files\Microsoft\jdk-17.0.17.10-hotspot", "Machine")

# Path에 추가
$oldPath = [System.Environment]::GetEnvironmentVariable("Path", "Machine")
[System.Environment]::SetEnvironmentVariable("Path", "$oldPath;%JAVA_HOME%\bin", "Machine")
```

### 설치 확인

새 명령 프롬프트를 열고 확인:

```batch
java -version
javac -version
echo %JAVA_HOME%
```

**예상 출력:**
```
openjdk version "17.0.18" 2024-01-16 LTS
OpenJDK Runtime Environment Microsoft-8035246 (build 17.0.18+8-LTS)
OpenJDK 64-Bit Server VM Microsoft-8035246 (build 17.0.18+8-LTS, mixed mode, sharing)
```

---

## 2. Android SDK 설치

### 방법 1: Android Studio 설치 (권장)

#### 설치 과정

1. [Android Studio 다운로드](https://developer.android.com/studio)
2. 설치 파일 실행 및 기본 설정으로 설치
3. Android Studio 첫 실행 시 **SDK Setup Wizard** 진행:
   - SDK 설치 경로 확인 (기본: `C:\Users\사용자명\AppData\Local\Android\Sdk`)
   - **Android 14 (API 34)** 선택
   - **Android SDK Build-Tools 34.0.0** 선택
   - **Android SDK Platform-Tools** 선택

#### SDK Manager에서 추가 구성요소 설치

1. Android Studio 실행
2. **Tools** → **SDK Manager**
3. **SDK Platforms** 탭:
   - ✅ **Android 14.0 (API 34)** 체크
4. **SDK Tools** 탭:
   - ✅ **Android SDK Build-Tools 34.0.0** 체크
   - ✅ **Android SDK Platform-Tools** 체크
   - ✅ **Android Emulator** (선택사항)
5. **Apply** → 설치 진행

### 방법 2: Command-line Tools만 설치 (경량)

#### 설치 과정

1. [Android Command-line Tools 다운로드](https://developer.android.com/studio#command-line-tools-only)
2. ZIP 파일을 원하는 경로에 압축 해제 (예: `C:\Android\cmdline-tools`)
3. 폴더 구조 조정:
   ```
   C:\Android\
   └── cmdline-tools\
       └── latest\
           ├── bin\
           ├── lib\
           └── ...
   ```

#### SDK 구성요소 설치

명령 프롬프트 (관리자 권한):

```batch
# SDK 경로 설정
set ANDROID_HOME=C:\Android

# sdkmanager 실행 (경로는 실제 설치 위치에 맞게 조정)
cd C:\Android\cmdline-tools\latest\bin

# 필수 구성요소 설치
sdkmanager "platforms;android-34"
sdkmanager "build-tools;34.0.0"
sdkmanager "platform-tools"

# 라이센스 동의
sdkmanager --licenses
```

### 환경변수 설정

#### ANDROID_HOME (또는 ANDROID_SDK_ROOT) 설정

**시스템 환경변수**에 추가:

1. `Win + R` → `sysdm.cpl` → **환경 변수**
2. **시스템 변수** → **새로 만들기**:
   - 변수 이름: `ANDROID_HOME`
   - 변수 값: `C:\Users\사용자명\AppData\Local\Android\Sdk` (실제 SDK 경로)
3. **Path** 변수에 추가:
   - `%ANDROID_HOME%\platform-tools`
   - `%ANDROID_HOME%\tools`
   - `%ANDROID_HOME%\tools\bin`

**명령 프롬프트 (관리자 권한)**:

```batch
setx ANDROID_HOME "C:\Users\사용자명\AppData\Local\Android\Sdk" /M
setx Path "%Path%;%ANDROID_HOME%\platform-tools;%ANDROID_HOME%\tools;%ANDROID_HOME%\tools\bin" /M
```

### 설치 확인

새 명령 프롬프트에서:

```batch
adb version
echo %ANDROID_HOME%
```

**예상 출력:**
```
Android Debug Bridge version 1.0.41
Version 36.0.2-12147458
```

---

## 3. 프로젝트 Clone

### Git 설치 (필요 시)

1. [Git for Windows 다운로드](https://git-scm.com/download/win)
2. 설치 파일 실행 (기본 설정으로 설치)

### 프로젝트 Clone

```batch
# 원하는 작업 폴더로 이동
cd D:\WorkspaceAndroid

# 프로젝트 Clone
git clone https://github.com/dev4unet/pico-webtoon-browser.git

# 프로젝트 폴더로 이동
cd pico-webtoon-browser
```

---

## 4. local.properties 설정

프로젝트 루트에 `local.properties` 파일 생성 (또는 수정):

```properties
# Android SDK 경로 (백슬래시를 이스케이프 처리)
sdk.dir=C\:\\Users\\사용자명\\AppData\\Local\\Android\\Sdk
```

**경로 확인 방법**:
```batch
echo %ANDROID_HOME%
```

출력된 경로를 복사하여 `\`를 `\\`로 변경합니다.

---

## 5. 빌드

### Debug APK 빌드

```batch
# 프로젝트 루트에서
gradlew.bat assembleDebug
```

**빌드 진행 상황:**
```
> Task :app:compileDebugJavaWithJavac
> Task :app:dexBuilderDebug
> Task :app:mergeDebugNativeLibs
> Task :app:packageDebug
> Task :app:assembleDebug

BUILD SUCCESSFUL in 1m 23s
```

**APK 출력 위치:**
```
app\build\outputs\apk\debug\pico-webtoon-webbrowser-debug-v1.0.0.x.apk
```

### Release APK 빌드

```batch
gradlew.bat assembleRelease
```

**참고**: Release 빌드는 서명 설정이 필요합니다. 자세한 내용은 [RELEASE.md](RELEASE.md) 참고.

### 빌드 오류 해결

#### 오류: "SDK location not found"

**원인**: `local.properties`가 없거나 경로가 잘못됨

**해결**:
```batch
# local.properties 생성
echo sdk.dir=C\:\\Users\\사용자명\\AppData\\Local\\Android\\Sdk > local.properties
```

#### 오류: "Unsupported Java version"

**원인**: JDK 버전이 맞지 않음 (JDK 21 등)

**해결**:
```batch
# JDK 17로 JAVA_HOME 변경
setx JAVA_HOME "C:\Program Files\Microsoft\jdk-17.0.17.10-hotspot"

# 새 명령 프롬프트에서 확인
java -version
```

#### 오류: "Execution failed for task ':app:compileDebugJavaWithJavac'"

**원인**: Gradle 캐시 손상

**해결**:
```batch
# Gradle 캐시 정리 후 재빌드
gradlew.bat clean
gradlew.bat assembleDebug
```

---

## 6. 디바이스 설치 및 테스트

### PICO 4 Ultra 연결

1. PICO 헤드셋에서 **개발자 모드** 활성화
2. **USB 디버깅** 활성화
3. USB 케이블로 PC와 연결

### ADB 연결 확인

```batch
adb devices
```

**예상 출력:**
```
List of devices attached
A1B2C3D4E5F6    device
```

### APK 설치

```batch
# APK 설치 (-r 옵션: 기존 앱 덮어쓰기)
adb install -r app\build\outputs\apk\debug\pico-webtoon-webbrowser-debug-v*.apk

# 앱 실행
adb shell am start -n net.dev4u.webtoonbrowser/.MainActivity
```

### 로그 확인

```batch
# 실시간 로그 확인
adb logcat | findstr "WebtoonBrowser"
```

---

## 7. IDE 설정 (선택사항)

### Android Studio에서 프로젝트 열기

1. Android Studio 실행
2. **File** → **Open**
3. `pico-webtoon-browser` 폴더 선택
4. Gradle Sync 자동 실행 대기

### IntelliJ IDEA 설정

1. IntelliJ IDEA 실행
2. **File** → **Open**
3. `pico-webtoon-browser` 폴더 선택
4. **Import project from Gradle** 선택
5. Gradle JVM을 **JDK 17**로 설정

---

## 8. 개발 워크플로우

### 일반적인 작업 흐름

```batch
# 1. 최신 코드 가져오기
git pull origin main

# 2. 코드 수정

# 3. 빌드
gradlew.bat assembleDebug

# 4. 디바이스 설치 및 테스트
adb install -r app\build\outputs\apk\debug\pico-webtoon-webbrowser-debug-v*.apk

# 5. 커밋 및 푸시
git add .
git commit -m "작업 내역"
git push origin main
```

### 빌드 번호 자동 증가

이 프로젝트는 `app/version.properties` 파일에서 빌드 번호를 관리합니다:

```properties
VERSION_MAJOR=1
VERSION_MINOR=0
VERSION_PATCH=0
VERSION_BUILD=8
```

빌드할 때마다 `VERSION_BUILD`가 자동으로 1씩 증가합니다.

---

## 체크리스트

### 초기 환경 구축

- [ ] JDK 17 설치 완료
- [ ] `java -version` 확인
- [ ] JAVA_HOME 환경변수 설정
- [ ] Android SDK 설치 (API 34)
- [ ] ANDROID_HOME 환경변수 설정
- [ ] `adb version` 확인
- [ ] Git 설치
- [ ] 프로젝트 Clone
- [ ] local.properties 설정
- [ ] 빌드 성공 확인

### 디바이스 테스트

- [ ] PICO 개발자 모드 활성화
- [ ] USB 디버깅 활성화
- [ ] `adb devices` 연결 확인
- [ ] APK 설치 성공
- [ ] 앱 실행 확인

---

## 참고 문서

- [메인 README.md](../README.md) - 프로젝트 전체 개요
- [RELEASE.md](RELEASE.md) - 릴리스 배포 가이드
- [Android 개발자 문서](https://developer.android.com/docs)
- [Gradle 빌드 가이드](https://docs.gradle.org/current/userguide/userguide.html)

---

작성일: 2026-02-11
최종 업데이트: 2026-02-11
