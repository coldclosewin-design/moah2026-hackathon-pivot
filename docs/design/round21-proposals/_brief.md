# 라운드 21 시안 — 공통 브리프 (2026-10-05)

사용자가 전체 기능 검토 영상을 보고 준 피드백 중 "시안을 보고 고르고 싶다" 는 8건의 시안 페이지를 만든다. 각 페이지는 **정적 HTML 한 장**(외부 파일·폰트·스크립트 없이, 인라인 CSS/SVG 만), 브라우저 폭 1600px 에서 보기 좋게.

## 앱의 디자인 언어 (반드시 지킨다)

- 2560×1268 dp 가로 자동차 화면(실제 기기 2560×1440, 위아래 시스템 바). 시안은 화면을 **1/3 축소(853×423 px)** 하거나, 관련 부분만 같은 축척으로 잘라 보여 준다. 실제 크기 감각을 위해 "실제 화면은 약 3배" 를 페이지 위에 적는다.
- 색 토큰 다섯 개만: `--ink:#070827` `--paper:#FCFCFA` `--peri:#5B60A1`(Periwinkle) `--lav:#E5E6F0`(Lavender) `--signal:#F52D48`(빨강, 주 알약·선택 강조) + `--muted: rgba(7,8,39,.60)`. **새 색 금지.** 투명도 변형은 허용.
- 컨셉 = "기하학 포스터": 큰 면 분할(왼쪽 Ink 패널 + 오른쪽 Paper), 각진 면, 둥근 알약 버튼, 굵은 글씨 대신 크기 대비(Bold 금지 — font-weight 400/500 만).
- 질감 B(라운드 13 확정): 버튼·카드에 **살짝 그림자 + 위쪽 1px 하이라이트** — `box-shadow: 0 4px 10px rgba(7,8,39,.10), 0 1px 2px rgba(7,8,39,.06), inset 0 1px 0 rgba(255,255,255,.9)`. 모든 카드형 면에 이 그림자를 쓴다(사용자 요청). 잠금 화면만 평면.
- 주 알약 = Signal 빨강, 흰 글자, 오른쪽 `→`, 높이 약 140 dp(1/3 축소 시 약 46px), **폭은 Setup `시작` 알약과 비슷하게 넉넉히**(1/3 축소 시 약 190px 이상).
- 글자: 한국어. 눈썹(Eyebrow) 32 dp = 섹션 이름(예: "연습할 과제", Periwinkle). 본문 40 dp, 제목 72 dp. 1/3 축소 시 약 11/13/24px.
- 운전자 화면 문장에는 숫자를 넣지 않는다(점수·횟수·초). 단 **과제 시트 화면은 숫자 0**(계측이 검사한다). 리포트 "자세히 보기"·진단서·시간대 라벨(14:00–15:00)·거리(3 km)는 숫자 허용.
- 폰트: `"Noto Sans KR","Malgun Gothic","Apple SD Gothic Neo",system-ui,sans-serif`.

## 페이지 형식

- 맨 위: 제목(주제), 한 줄 설명, 사용자 피드백 원문 인용.
- **지금** 한 칸(현재 캡처 `../../screenshots/lesson/<파일>.png` 를 `<img>` 로 1/3 크기) + 시안 A~E 칸. 칸마다 **이름(한 단어 컨셉) · 한 줄 설명 · 장점 · 주의점(구현 난이도·운전 중 가독성)**.
- 시안끼리 같은 데이터(같은 문구)를 쓴다 — 차이가 배치·형태로만 보이게.
- 맨 아래: "추천" 하나와 이유 한 줄.
- 파일 이름: `docs/design/round21-proposals/<번호>-<주제>.html`.

## 참고 캡처 (docs/screenshots/lesson/)

setup-sheet: `lesson-setup-sheet.png`·`-driving.png`·`-checklist.png`·`-knowledge.png` · 시험장: `lesson-venues.png`·`lesson-venue-slots.png`·`lesson-venues-booked.png`·`lesson-setup-reserved.png` · 점검: `lesson-maneuver-checklist.png`·`-pending.png`·`-bad.png`, Done `lesson-done-checklist.png` · 자세히 보기: `lesson-details.png`·`lesson-details-front.png`·`lesson-report-exam-details.png` · Done 주차: `lesson-done.png`·`lesson-done-verdict-fix.png`·`lesson-done-parallel-good.png` · 코스 지도: `lesson-drive-exam.png`·`lesson-drive-exam-parking.png`·`lesson-done-exam-bad.png` · 지식 테스트: `lesson-quiz.png`·`lesson-quiz-answered.png`·`lesson-quiz-correct.png`·`lesson-quiz-done-results.png`. 캡처는 Read 도구로 직접 볼 수 있다.
