# 릴리스 배포 가이드

## GitHub Actions 자동 배포 시스템

이 프로젝트는 Git 태그를 푸시하면 자동으로 APK를 빌드하고 GitHub Release에 배포하는 시스템이 구축되어 있습니다.

## 사용 방법

### 1. 새 릴리스 버전 생성

```bash
# 버전 태그 생성 (예: v1.0.1)
git tag -a v1.0.1 -m "PICO Webtoon Browser v1.0.1"

# 태그를 원격 저장소에 푸시
git push origin v1.0.1
```

### 2. 자동 빌드 및 배포

태그가 푸시되면 GitHub Actions가 자동으로:
1. JDK 17 환경 설정
2. Debug APK 빌드
3. Release APK 빌드
4. GitHub Release 생성
5. APK 파일을 Release에 첨부

### 3. 릴리스 확인

https://github.com/dev4unet/pico-webtoon-webbrowser/releases 에서 확인 가능

## 릴리스 APK 서명 설정 (선택사항)

현재는 디버그 서명으로 릴리스 APK가 빌드됩니다. 프로덕션 배포를 위해서는 별도의 서명 키가 필요합니다.

### 1. Keystore 생성

```bash
keytool -genkey -v -keystore pico-webtoon-webbrowser.keystore -alias pico-webtoon -keyalg RSA -keysize 2048 -validity 10000
```

입력 정보:
- Password: 안전한 비밀번호 입력 (기억 필수!)
- Name: dev4unet 또는 회사명
- Organization: dev4u.net
- City, State, Country: 적절히 입력

### 2. GitHub Secrets 설정

https://github.com/dev4unet/pico-webtoon-webbrowser/settings/secrets/actions 에서:

1. `KEYSTORE_FILE`: keystore 파일을 Base64로 인코딩한 값
   ```bash
   # Windows (PowerShell)
   [Convert]::ToBase64String([IO.File]::ReadAllBytes("pico-webtoon-webbrowser.keystore"))

   # Linux/Mac
   base64 pico-webtoon-webbrowser.keystore | tr -d '\n'
   ```

2. `KEYSTORE_PASSWORD`: keystore 비밀번호
3. `KEY_ALIAS`: pico-webtoon
4. `KEY_PASSWORD`: 키 비밀번호

### 3. build.gradle 업데이트

`app/build.gradle`의 `buildTypes` 섹션에 서명 설정 추가:

```gradle
android {
    signingConfigs {
        release {
            // GitHub Actions에서 사용할 서명 설정
            if (System.getenv("KEYSTORE_FILE")) {
                def keystoreFile = file("../keystore.jks")
                storeFile keystoreFile
                storePassword System.getenv("KEYSTORE_PASSWORD")
                keyAlias System.getenv("KEY_ALIAS")
                keyPassword System.getenv("KEY_PASSWORD")
            }
        }
    }

    buildTypes {
        release {
            signingConfig signingConfigs.release
            minifyEnabled false
            proguardFiles getDefaultProguardFile('proguard-android-optimize.txt'), 'proguard-rules.pro'
        }
    }
}
```

### 4. GitHub Actions workflow 업데이트

`.github/workflows/release.yml`에 keystore 디코딩 단계 추가:

```yaml
- name: Decode Keystore
  env:
    KEYSTORE_BASE64: ${{ secrets.KEYSTORE_FILE }}
  run: |
    echo $KEYSTORE_BASE64 | base64 -d > keystore.jks

- name: Build Release APK
  env:
    KEYSTORE_PASSWORD: ${{ secrets.KEYSTORE_PASSWORD }}
    KEY_ALIAS: ${{ secrets.KEY_ALIAS }}
    KEY_PASSWORD: ${{ secrets.KEY_PASSWORD }}
  run: ./gradlew assembleRelease --no-daemon
```

## 로컬 빌드

### Debug 빌드
```bash
./gradlew assembleDebug
```

### Release 빌드 (서명 없이)
```bash
./gradlew assembleRelease
```

빌드된 APK 위치:
- Debug: `app/build/outputs/apk/debug/pico-webtoon-webbrowser-debug-v*.apk`
- Release: `app/build/outputs/apk/release/pico-webtoon-webbrowser-release-v*.apk`

## 버전 관리

버전은 `app/version.properties` 파일에서 관리됩니다:

```properties
VERSION_MAJOR=1  # 주요 버전 (Breaking Changes)
VERSION_MINOR=0  # 기능 추가
VERSION_PATCH=0  # 버그 수정
VERSION_BUILD=7  # 자동 증가 (빌드할 때마다)
```

버전 형식: `MAJOR.MINOR.PATCH.BUILD` (예: 1.0.0.7)

## 주의사항

1. **Keystore 파일 보안**: keystore 파일과 비밀번호는 절대 Git에 커밋하지 마세요!
2. **Git 태그**: 한 번 푸시한 태그는 수정하기 어려우니 신중하게 생성하세요
3. **버전 번호**: 시맨틱 버저닝(Semantic Versioning) 권장
4. **APK 크기**: Release APK는 ProGuard로 코드 최적화되어 Debug보다 작습니다

## 문제 해결

### GitHub Actions 빌드 실패 시
1. https://github.com/dev4unet/pico-webtoon-webbrowser/actions 에서 로그 확인
2. JDK 버전, Gradle 버전 확인
3. Secret 값이 올바르게 설정되었는지 확인

### 로컬 빌드 실패 시
1. `JAVA_HOME`이 JDK 17로 설정되었는지 확인
2. `./gradlew clean` 후 다시 빌드
3. `./gradlew --stop` 후 Gradle daemon 재시작
