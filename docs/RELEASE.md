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

## 릴리스 APK 서명 설정

이 프로젝트는 Debug와 Release 각각 별도의 keystore로 서명하며, GitHub Actions에서 자동으로 처리됩니다.

### 서명 구조

| 빌드 타입 | Keystore | 용도 |
|-----------|----------|------|
| Debug | `~/.android/debug.keystore` (공유) | 개발/테스트용 |
| Release | `release.keystore` (프로젝트 루트) | 배포용 |

### GitHub Secrets 설정

포크(Fork) 후 GitHub Actions 빌드를 사용하려면 저장소의 **Settings → Secrets → Actions**에서 다음 Secret을 설정하세요:

1. `DEBUG_KEYSTORE_BASE64`: debug.keystore를 Base64로 인코딩한 값
2. `RELEASE_KEYSTORE_BASE64`: release.keystore를 Base64로 인코딩한 값
   ```bash
   # Windows (PowerShell)
   [Convert]::ToBase64String([IO.File]::ReadAllBytes("release.keystore"))

   # Linux/Mac
   base64 release.keystore | tr -d '\n'
   ```
3. `RELEASE_STORE_PASSWORD`: release keystore 비밀번호
4. `RELEASE_KEY_ALIAS`: release key alias
5. `RELEASE_KEY_PASSWORD`: release key 비밀번호

### Keystore 신규 생성 (포크 시)

포크한 프로젝트에서 자체 서명을 사용하려면:

```bash
keytool -genkey -v -keystore release.keystore -alias mykey -keyalg RSA -keysize 2048 -validity 10000
```

생성한 keystore를 프로젝트 루트에 두면 로컬 Release 빌드가 가능합니다.
`app/build.gradle`의 `signingConfigs.release`에서 keyAlias와 비밀번호를 환경변수 또는 기본값으로 수정하세요.

## 로컬 빌드

### Debug 빌드
```bash
./gradlew assembleDebug
```

### Release 빌드
```bash
./gradlew assembleRelease
```

**참고**: Release 빌드는 프로젝트 루트에 `release.keystore` 파일이 필요합니다. 파일이 없으면 빌드가 실패합니다. Keystore 설정은 위의 "릴리스 APK 서명 설정" 섹션을 참고하세요.

빌드된 APK 위치:
- Debug: `app/build/outputs/apk/debug/pico-webtoon-webbrowser-debug-v*.apk`
- Release: `app/build/outputs/apk/release/pico-webtoon-webbrowser-release-v*.apk`

## 버전 관리

버전은 **Git 태그** 기반으로 관리됩니다:

```bash
# 현재 버전 확인 (가장 최근 태그)
git describe --tags --abbrev=0

# 새 버전 태그 생성
git tag -a v0.3.0 -m "PICO Webtoon Browser v0.3.0"
```

- **릴리스 빌드** (태그가 HEAD를 가리킬 때): `0.3.0`
- **개발 빌드** (태그 이후 커밋): `0.3.1-dev` (PATCH+1 + `-dev` 접미사 자동 부여)

`app/version.properties`에는 `VERSION_BUILD`만 관리되며, 빌드할 때마다 자동으로 1씩 증가합니다 (`versionCode`용).

## 주의사항

1. **Keystore 파일 보안**: keystore 파일과 비밀번호는 절대 Git에 커밋하지 마세요!
2. **Git 태그**: 한 번 푸시한 태그는 수정하기 어려우니 신중하게 생성하세요
3. **버전 번호**: 시맨틱 버저닝(Semantic Versioning) 권장
4. **APK 크기**: 현재 Release APK는 ProGuard(minifyEnabled)가 비활성화 상태이므로 Debug와 크기가 유사합니다

## 문제 해결

### GitHub Actions 빌드 실패 시
1. https://github.com/dev4unet/pico-webtoon-webbrowser/actions 에서 로그 확인
2. JDK 버전, Gradle 버전 확인
3. Secret 값이 올바르게 설정되었는지 확인

### 로컬 빌드 실패 시
1. `JAVA_HOME`이 JDK 17로 설정되었는지 확인
2. `./gradlew clean` 후 다시 빌드
3. `./gradlew --stop` 후 Gradle daemon 재시작
