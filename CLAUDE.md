# PICO Webtoon Browser - Claude AI 작업 가이드

이 프로젝트는 PICO 4 Ultra VR 헤드셋용 세로형 웹툰 브라우저 Android 앱입니다.

## ⚙️ 모든 대화 시작 시 필수 작업

1. **최신 코드 가져오기**
   ```bash
   git pull origin main
   git pull origin develop
   ```

2. **프로젝트 컨텍스트 파악**
   - **반드시 읽기**: `!개인전용\.ai_context.md`
   - 이 파일에는 프로젝트의 모든 기술적 결정, 구현 완료 기능, 현재 상태, 진행 중인 작업이 상세히 기록되어 있습니다
   - 집과 회사를 오가며 작업하므로, 이전 세션의 작업 내용을 반드시 확인해야 합니다

3. **작업 내역 확인** (필요 시)
   - `!개인전용\작업내역\` 폴더의 최신 파일 확인
   - `!개인전용\요청.md`에서 미완료 요청사항 확인

## 📋 작업 완료 시 필수 작업

1. **문서 업데이트**
   - `.ai_context.md`: 현재 버전, 구현 완료 기능, 기술적 변경사항 반영
   - `작업내역/YYYYMMDD 작업내역.md`: 당일 작업 내용 상세 기록

2. **Git 커밋 및 Push**
   - 변경사항 커밋 (Co-Authored-By: Claude Sonnet 4.5 <noreply@anthropic.com>)
   - main과 develop 브랜치 동기화

3. **릴리스 생성 시**
   - Git 태그 생성: `git tag -a v0.X.X -m "Release v0.X.X"`
   - 태그 push: `git push origin v0.X.X`
   - GitHub Actions가 자동으로 APK 빌드 및 릴리스 생성

## 🔧 기술 스택 요약

- **언어**: Java (Android)
- **빌드**: Gradle 8.4 + AGP 8.2.0
- **JDK**: **17 LTS** (JDK 21은 로컬 빌드 이슈 있음)
- **버전 관리**: Git 태그 기반 자동 버전 관리
- **CI/CD**: GitHub Actions (자동 빌드 및 릴리스)

## ⚠️ 주의사항

- **항상 JDK 17 사용** (로컬 빌드 시 JDK 21 호환성 문제 있음)
- **릴리스 전 version.properties 확인** (VERSION_BUILD만 존재해야 함)
- **커밋 전 서브모듈 업데이트** (!개인전용 폴더)
- **APK 서명**: GitHub Actions에서 자동 처리 (로컬은 debug keystore)

## 📚 상세 문서

모든 상세 정보는 `!개인전용\.ai_context.md`를 참조하세요.
