# PICO Webtoon Browser 문서

이 폴더는 PICO Webtoon Browser 개발 시 참고할 문서들을 포함합니다.
## 아키텍처 문서

- **[architecture.md](architecture.md)** - 앱 전체 아키텍처
  - 앱 구조, 핵심 설계 결정, 데이터 저장 방식
  - 코드 수정 전 반드시 참고
  - 아키텍처 변경 시 자동 업데이트

## 일반 참고 자료

- **[RELEASE.md](RELEASE.md)** - 릴리스 배포 가이드
  - GitHub Actions를 통한 자동 배포 방법
  - 로컬 빌드 및 수동 배포 방법
  - Keystore 서명 설정 가이드

- **[development-setup-windows.md](development-setup-windows.md)** - Windows 개발 환경 구축 가이드
  - JDK 17 설치 및 환경변수 설정
  - Android SDK 설치 방법
  - 프로젝트 Clone 및 빌드 전체 과정

## 개인 전용 자료

`!개인전용` 폴더는 일반적으로 필요 없는 잡다한 내용들이라 중요하지 않으며, Private submodule로서 별도의 private git 저장소와 연결되어 있어서 접근 권한이 있는 사용자만 작업이 가능합니다.   
`!개인전용` 폴더에서의 작업이 필요 없는 분들은 아래 내용이 필요 없습니다.

### 처음 Clone 하는 방법 (다른 컴퓨터에서)

**Submodule 포함하여 Clone** (권장):
```bash
# 한 줄로 Clone (submodule 포함)
git clone --recursive https://github.com/dev4unet/pico-webtoon-webbrowser.git
cd pico-webtoon-webbrowser
```

**또는** Clone 후 Submodule 초기화:
```bash
git clone https://github.com/dev4unet/pico-webtoon-webbrowser.git
cd pico-webtoon-webbrowser
git submodule init
git submodule update
```

### !개인전용 폴더 작업 방법

**파일 수정 후**:
```bash
# 1. !개인전용 폴더에서 커밋 및 푸시
cd "!개인전용"
git add .
git commit -m "작업 내역 업데이트"
git push
cd ..

# 2. 메인 저장소에서 참조 업데이트
git add "!개인전용"
git commit -m "Update !개인전용"
git push
```

**편리한 설정** (선택사항):
```bash
# 자동 업데이트 설정 (pull 시 submodule도 자동 업데이트)
git config --global submodule.recurse true

# Git Alias 설정 (한 번에 푸시/풀)
git config alias.spush '!git push && cd "!개인전용" && git push && cd ..'
git config alias.spull '!git pull && cd "!개인전용" && git pull && cd ..'

# 사용법
git spush  # 메인 + !개인전용 모두 푸시
git spull  # 메인 + !개인전용 모두 풀
```

상세 가이드: [`!개인전용/GIT_SUBMODULE_가이드.md`](../!개인전용/GIT_SUBMODULE_가이드.md)


