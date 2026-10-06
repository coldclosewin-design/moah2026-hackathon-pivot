# 한 장 자료 — 관점별 변주 20안 공통 브리프 (2026-10-07)

확정 한 장(AC-2, `docs/presentation/onepager-proposals/AC2-story-top-journey.html` 이 PR #222 브랜치에 있음 — 아래 "내용 원장" 이 같은 내용)을 **다섯 관점 × 4안 = 20안**으로 다시 해석한다.
관점: ① 사내 보고용 ② 앱 홍보용 ③ 기관 제출용 ④ 발표용 ⑤ 디자인 포스터. 같은 사실을 쓰되 **그림·디자인·폰트·구성·색·판형**을 관점에 맞게 과감히 바꾼다. 서로 비슷해 보이면 실패다.

## 형식

- 한 안 = 정적 HTML 한 페이지(외부 파일·웹폰트·스크립트 없이, 인라인 CSS/SVG만, `<script>` 금지) + 같은 이름 PNG.
- 판형(안마다 하나 골라 페이지 맨 위 주석에 적는다): 16:9 슬라이드 **1920×1080** · 세로 A 판형 포스터/보고서 **1240×1754**(A4·A3 비율) · SNS 정사각 **1080×1080** · 세로 배너 **1080×1920**. 렌더 크기 = 판형 크기 그대로.
- 캡처: `<img src="../../screenshots/lesson/<파일>.png">`(2560×1440, 16:9). 잘라 써도 되지만 캡처 위에 가짜 UI·글자를 덧그리지 않는다.
- 파일: `docs/presentation/onepager-variations/<번호>-<영문-슬러그>.html` / `.png`. 번호는 아래 배정.
- 각 HTML 맨 위 `<!-- meta: 관점 | 대상 | 판형 | 폰트 | 색 | 구성 한 줄 -->` 주석 한 줄(목차가 읽는다).

## 폰트 (이 PC에 설치된 것만 — 외부 폰트 금지)

한글: `Noto Sans KR`(Thin·Light·DemiLight·Regular·Medium·Black) · `Noto Serif KR`(ExtraLight~Black) · `NanumGothic` · `NanumMyeongjo` · `NanumMyeongjoExtraBold` · `Malgun Gothic` · `Gulim` · `Dotum` · `Batang` · `Gungsuh` · `HeummNemogothic152` · `WooriDotum` · `WooriSinmoon` · `KB_Bold` · `Ownglyph 99 Regular`·`Ownglyph 512 Regular`(손글씨풍).
라틴(숫자·영문 제목용): `Bahnschrift`(Condensed·SemiBold 등) · `Segoe UI Variable Display` · `Georgia` · `Sitka Banner/Display` · `Impact` · `Arial Black` · `Franklin Gothic Medium` · `Cascadia Code` · `Consolas` · `Palatino Linotype` · `Cambria`.
글자를 렌더해 보고 한글이 대체 폰트로 깨지면 바꾼다. 굵기는 자유(이 변주는 앱 화면 규칙이 아니다).

## 색

관점에 맞게 자유. 단 앱 정체성을 아예 버리지 말고 한 곳 이상에 앱 색(Ink `#070827` · Periwinkle `#5B60A1` · Lavender `#E5E6F0` · Signal `#F52D48` · Paper `#FCFCFA`)이 남게. 공공·보고서 톤이면 차분하게, 홍보·포스터면 과감하게.

## 내용 원장 (사실만 — 과장 금지)

- 제품: **드라이브 코치 — 화내지 않는 조수석**. 초보·장롱면허 운전자를 위한 **AAOS(차량용 안드로이드) 운전 연수 어시스턴트**. 팀 **사고치조** · 발표자 **전준영** · MOAH 2026(모비스 SW 해커톤).
- 만든 이유(강조): **아이 때문에 다시 운전을 시작한 장롱면허 10년차 아내를 위해** 만들었다. 옆에서 가르치면 둘 다 힘들다.
- 왜 필요한가(확인한 공개 통계 — 숫자를 쓰면 출처를 같은 장에 작게):
  1. 자동차보험 차량 물적 사고 10건 중 3건(**30.2%**)이 주차 중, 그 주차 사고의 **53.8%가 후진** — 보험개발원·현대해상·동부화재·KB손해보험 공동 조사(2012~2014 보험금 지급 분석, 2017.1 발표)
  2. 초보운전자 첫해 사고의 **41%가 처음 100일 안에** — 현대해상 교통기후환경연구소 「초보운전자 사고감소를 위한 정책방안 연구」(2009~2015 사고 317만 건, 2016.12)
  3. 학원 도로연수 10시간 **약 50~60만 원** — 2025년 언론 보도 기준
  메시지: 가장 사고가 많은 순간(주차·첫 100일)을 화내지 않고 반복해 줄 조수석이 없다.
- 해법: 두려운 상황을 **과제**로 쪼개고 **차량 신호로 과정을 측정**해 **가이드 → 힌트 → 평가**로 도움을 줄여 간다. 정차하면 회차 판정, 쌓이면 리포트·진단서.
- 과제 **18**: 주차 4(후면·전면 직각·평행·사선) · 도로 주행 6(직선 정지·좌회전·차선 변경·회전교차로·일반 도로·장내기능 모의시험) · 점검 2(출발 전 점검·도로 표시 읽기) · 지식 6.
- 연습 여정 6단계: 01 대화로 과제 고르기(말 카드 · AI 코치) → 02 브리핑 → 03 연습(가이드/힌트/평가 — 틀린 순간엔 규칙이 바로 말함, 주행 중 5 km/h 넘으면 화면 잠금) → 04 회차 판정(점수 대신 판정 네 줄 + 신호로 추정한 궤적) → 05 리포트·진단서(무엇이 실신호인지 배지, 공유 범위는 운전자가 — 예시) → 06 제휴 시험장 모의시험(구간 감점·합격선 — 시험장 신호는 시뮬레이션).
- 차량 신호(VSS): 속도·조향·기어·안전벨트·시동·도어·지시등·비상등. 신호마다 실신호/시뮬레이션/미측정을 정직하게 표시.
- AI: 회차 멘트·총평·홈 대화는 사내 Copilot(실패하면 규칙 문장). 감점 순간 힌트는 규칙이 지연 0.
- 검증: 사내 차량 환경(Real VSS)에서 **코드 수정 없이** 빌드·실행 — 후면 주차 **실신호 7 · 시뮬레이션 1 · 미측정 0** · 사내 Copilot 응답 1.2~1.5 s · 단위 테스트 350(사외).
- 한계(숫자·검증을 쓰는 안은 한 줄로 반드시): 카메라가 없어 칸 안 최종 위치는 판정하지 않는다(과정을 본다) · 예약·진단서 공유·시험장 위치 신호는 시뮬레이션/예시 · 캡처는 외부 에뮬레이터 화면.
- 쓰지 말 것: "실제 도로에서 검증", "사고를 줄였다", "장롱면허 64%"(원보고서 미확인), 실존 기관·보험사 로고나 공문서 서식 흉내(기관 제출용도 "제안서 형식" 이지 실제 기관 양식을 사칭하지 않는다).

## 캡처 (docs/screenshots/lesson/, Read 로 볼 수 있다)

홈 `lesson-setup.png` · 말 카드 대화 `lesson-setup-coach-cards-follow.png` · 브리핑 `lesson-briefing.png` · 주차 중 힌트 `lesson-maneuver-hint.png` · 가이드 조향 도식 `lesson-maneuver-guides.png` · 회차 판정 `lesson-done-verdict-fix.png` · 리포트 `lesson-report-session.png` · 자세히 보기 `lesson-details-session.png` · 진단서 `lesson-certificate.png` · 장내 모의시험 지도 `lesson-drive-exam.png` · S자 연습 `lesson-drive-exam-practice-s-live-turn.png` · 도로 표시 퀴즈 `lesson-quiz-road-sign.png` · 잠금 `lesson-locked.png` · 준비실 `lesson-admin-home.png`.

## 렌더

```
"/c/Program Files (x86)/Microsoft/Edge/Application/msedge.exe" --headless=new --disable-gpu --hide-scrollbars --user-data-dir="<스크래치 아래 새 폴더>" --window-size=<W>,<H> --screenshot="<절대 경로>.png" "file:///<절대 경로>.html" 2>&1 | tail -1
```
명령 하나씩 실행하고(루프 안에서 `>/dev/null` 로 돌리면 파일이 안 생긴다), 1~10초 기다려 파일이 생겼는지 확인한 뒤 Read 로 보고 넘침·겹침·깨진 글자를 고친다. 셸은 Git Bash — 한국어를 heredoc 에 넣지 말고 Write 도구로 파일을 쓴다. 커밋·브랜치 변경 금지.
