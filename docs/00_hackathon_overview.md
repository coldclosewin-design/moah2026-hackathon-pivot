# MOAH 2026 해커톤 개요

> 출처: 사내 Confluence HACKATHON 스페이스를 사내에서 열람하고 정리한 개인 메모. 공식 규칙·주제·평가·일정·제출 형식은 항상 주최측 최신 공지가 우선한다.

## 기본 정보

| 항목 | 내용 |
|---|---|
| 이름 | MOAH 2026 (MOBIS SW Hackathon) |
| 플랫폼 | Android Automotive OS (AAOS) 차량용 앱 |
| 도메인/주제 | 차량앱 자유주제. 차량 신호(VSS) 조회·구독·제어 활용, 선택적으로 앱 내 AI 기능 탑재 |
| 개발 언어 | Kotlin 또는 Java (동일 프로젝트 내 혼용 가능) |
| 최소 SDK | API 29 이상 권장 |
| 팀 구성 | 1인 팀 |
| 제출물 | ① Source code (Bitbucket) ② APK (MarketUploader) ③ 시연 영상 |

주의: 공식 "주제 목록 / 평가 기준 / 일정"은 근거 페이지에 단일 항목으로 명시되어 있지 않다. 주최측 공지 및 제출 가이드(pageId 1038298817), 최종자료 제출(1317526558)을 반드시 재확인할 것. 위 "도메인/주제"는 Getting Started 문서의 앱 성격에서 도출한 것이며 규정이 아니다.

## 해커톤이 요구하는 앱의 성격

AAOS 기반 플랫폼(infoLINK) 위에 APK를 얹어 **운전자에게 편의/안전 관점의 서비스**를 제공한다. 이번 MOAH 2026에서는 infoLINK 에뮬레이터와 SDK로 앱을 개발하고 infoLINK MARKET에 업로드하여 배포까지 진행되는 과정을 경험한다.

## infoLINK 플랫폼 요약 (사내 소개자료 기준)

- IVI 복수 제품 개발을 위한 안드로이드 기반 SW 플랫폼. 최신 AAOS 적용(Android 13/14 운영 중).
- SDK 제공: API / API Doc / PC Emulator / Remote Controller. SDK로 만든 앱은 infoLINK 적용 제품 전부에서 실행 가능.
- SDK 구조: AAOS 위에 CAR 프레임워크(차량신호, CarInfo, CarInput, CarPower, CarState, CarUser 등)와 OEM 프레임워크(Bluetooth, Camera, Audio, CarPower, SomeIP, DSP 등).
- InfoLINK Market 운영: 앱 업로드 → App Store → IVI 제품에 설치/업데이트.
- 개발 환경: Emulator, Remote Target(실제 제품 원격 연결).
- IDELINK: 브라우저 접속만으로 SW 개발 가능한 Web IDE (Android Studio / VSCode), 빌드 엔진, Digital Twin, Test Automation.

## 참가자 가이드 문서 맵 (사내에서만 열람 가능)

| # | 문서 | pageId |
|---|---|---|
| 1 | Web IDE 환경에서 해커톤 시작하기 | 1032646808 |
| 2 | opencode로 바이브 코딩하기 | 1320797944 |
| 3 | 차량 제어 앱 만들기 | 1328137961 |
| 4 | 3D 에뮬레이터에서 차량 제어 확인하기 | 1317528505 |
| 5 | Template App으로 앱 개발 시작하기 | 1324683678 |
| 6 | Cloud Copilot AI 레퍼런스 어플리케이션 | 1317526611 |
| 7 | M.ADI 사용가이드 | 1336517358 |
| — | VehicleAPI (VSS) Reference | 1037767644 |
| — | 앱 제출 가이드 | 1038298817 |
| — | 차량 제어 신호 알아보기 (1,251개) | 1323873443 |
| — | Getting Started | 1328361635 |
| — | 최종자료 제출 | 1317526558 |

근거 페이지 URL 형식: `https://confluence.mobis.co.kr/pages/viewpage.action?pageId=<pageId>`

## 제출 절차 (사내에서 진행)

1. **Source code** → 팀별 제공 Bitbucket 프로젝트에 업로드 (`bitbucket.mobis.co.kr/projects/MOBIS_SW_HACKATHON`)
2. **APK** → WebIDE의 `market_uploader` 실행 → APK 선택 후 제출
3. **시연 영상** → 3D 에뮬레이터 연동 동작 녹화 (세부 형식은 주최 공지 확인)

## 정책·보안 유의 (반드시 선확인)

- 사내 소스(Template App, copilot_reference_app, M.ADI SDK 등)의 개인 PC 반출/복제가 해커톤 규정·보안정책상 허용되는지 주최측에 먼저 확인한다. 되돌리기 어렵고 대외적 영향이 있는 사안이다.
- 본 프로젝트의 방향(공개 문서 API 계약을 근거로 자작 스텁/Fake를 외부에서 작성, 실제 사내 소스는 반출하지 않음)은 이 리스크를 최소화한다.
- **외부 → 사내 단방향(inbound only)** 원칙을 지키고, 사내 화면에서 확인한 정보의 외부 이전은 소속 정책 범위 내에서 판단한다.
- 이 저장소는 사내 문서에서 파생된 내용을 포함하므로 **GitHub private** 로만 운영한다. 사내 스크린샷(`tmp-info/`)과 에뮬레이터 이미지(`emulator/`)는 git에 올리지 않는다.
