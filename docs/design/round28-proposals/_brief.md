# 라운드 28 시안 — 화면마다 새 컨셉으로 다시 짓기 (2026-10-08)

라운드 27 에서 사용자가 새 디자인 컨셉을 골랐다. 27a 구현(#244)은 **색만 바뀌고 화면 구성은 예전 그대로**라 사용자가 아쉬워했다:
"전반적으로 리뉴얼한 디자인안대로 구현되기를 원했지만 색깔만 입혀진 느낌이 강함 — 홈 화면만 좀 비슷하게 구현해준 느낌인데 거기서 선택해서 팝업되는 메뉴가 없는 거부터 바로 기존으로 돌아갔어",
"후면 직각 주차 1회 화면 등 기존에 메인 일러 기준으로 구성된 느낌도 모두 전체 컨셉에 맞게 각 페이지, 구성마다 최소 5개~10개 정도의 대안을 줘서 내가 원하는 바와 일치할 수 있게 조율 과정 필요".
이번 라운드 = **앱의 모든 화면을 새 컨셉으로 처음부터 다시 그린 대안**. 색 바꾸기가 아니라 **배치·구성 요소·정보 위계·움직임을 새로**.

## 형식 (라운드 27 과 같다 — `docs/design/round27-proposals/_brief.md` 의 "형식" 절을 그대로 따른다)

- 정적 HTML 한 장(인라인 CSS/SVG, `<script>` 금지, CSS @keyframes 허용 + 프레임 띠), 폭 1600px, 목업은 2560×1268 dp 를 **1/3(853×423)**.
- **화면마다 6~8안**(사용자: 최소 5~10). 칸마다 이름 · 한 줄 설명 · 장점 · 주의점(공수 · 운전 중 가독성 · 정직성). 화면마다 끝에 추천.
- 시안끼리 같은 데이터·문구.
- 파일 `docs/design/round28-proposals/<번호>-<주제>.html` + 같은 이름 `.png`(headless Edge, 폭 1600 — 렌더 명령은 27 브리프). 셸은 Git Bash, 한국어는 Write 도구로. **커밋·브랜치 변경 금지.** 레퍼런스 사진 복사·브랜드 이름 금지.

## 확정된 기준 (반드시 이 말투로 — 대안은 이 안에서 다양하게)

**먼저 이 셋을 열어 본다** — 사용자가 "딱 저렇게" 라고 한 화면들이다:
1. `docs/design/round27-proposals/4-dense.html` 의 **2 문장 시트** — 큰 문장 "오늘은 [후면 직각 주차]를 ⦿[힌트]로 연습해요." · 누를 수 있는 낱말 = **흰 알약 칩 + 아래 그림자**(선택된 낱말은 검은 알약 칩) · 문장 속 **노란 원 아이콘 알약**(막대 기호) · 끝말 "연습해요" 는 연한 회색 · 아래 작은 정보 칩(`주차 › 후면 직각 주차` · `틀린 순간에만 말할게요`) · 하단 `← 돌아가기`(흰 알약 + 왼쪽 원 화살표) · `제휴 시험장 ↗`(흰 알약) · `시작 →`(**검은 알약, 위가 살짝 밝은 그라데이션 + 깊고 부드러운 그림자**) · 낱말을 누르면 문장이 위로 줄어들고 **흰 카드가 아래에서 올라와** 분류 트랙(검은 고른 칩) + 과제 카드 넷(고른 것 = 검은 카드 + 흰 체크) + `↓ 닫기` · `이 과제로 →`. 바탕 = 연회색 그라데이션(#E6E6E6 → #D9D9D9 계열), 그 위 흰 카드.
2. `docs/design/round27-proposals/3-results.html` 의 **6 전 → 후 쌍 알약** — 어두운 판(Onyx), 지난 값 = 흐린 테두리 큰 알약, 지금 값 = Platinum 채운 큰 알약, 사이 가는 선 위 변화량 칩(+40), 오른쪽 촘촘한 표(항목 · 1회차 → 2회차), 모의시험은 0–100 자 + 합격선 80 세로 선 위 두 알약, 정직성 줄(● 실신호 · ◐ 시뮬레이션 · ◌ 미측정), 작은 대문자 라벨(SKILL · SAFETY), 흰 테두리 알약 `← 돌아가기`.
3. `docs/design/round27-proposals/1-home.html` 의 **H8** — 흰 벽 · 왼쪽 위 작은 워드마크 · 오른쪽 위 정보 블록 · 거대한 제목(제목 속 과제·모드 밑줄) · 가는 가로선 · 오른쪽 아래 큰 ↗ 원 = 시작. (지금 앱 홈 = `docs/screenshots/lesson/lesson-setup.png` 가 이미 이 모습)

공통 디자인 언어(위 셋에서 뽑은 것):
- 색: White · 연회색 그라데이션 바탕 · Onyx `#222526` · Jet `#1A1A1A` · Graphite `#353A3E` · Platinum `#E0E0E0` · Ash `#BFBFBF` · **노란 포인트 하나 `#F2C230`(작은 원 아이콘 알약 — 화면마다 한 곳)** · Signal `#F52D48` 은 **경고에만**.
- 형태: 큰 둥근 흰 카드(반경 24–32 dp) · 알약 · 검은 고른 칩/카드 · 흰 칩 + 그림자 · 테두리 알약 · 왼쪽 원 화살표 버튼. 결과·통계 판은 어두운 Onyx + 테두리/채운 큰 숫자 알약.
- 글자: 큰 문장(132 dp 안팎) Medium, 회색 약한 낱말, 작은 대문자 라벨. 한국어 Noto Sans KR, 숫자 그로테스크(Segoe UI Variable Display/Arial 로 흉내).
- 움직임: 누름 = 원이 번짐(A1 360 ms) · 선택 = 미끄러지는 돌(B1 280 ms) · 카드 = 아래에서 올라와 겹침(C1 420 ms, 160 ms 간격) · 기호 = 어두운 둥근 타일 속 흰 기호(E1). 주행 중(> 5 km/h) 장식 움직임 0.
- 차 그림 = **V4 기하**(둥근 사각 + 원 바퀴, 같은 굵기 선 — `docs/design/round27-proposals/6-vehicle.html` V4).

## 바꾸지 않는 것

- 기능·흐름·버튼 이름·문구·접근성 이름(`과제·모드 바꾸기` 등). 화면에 있는 정보는 다 있어야 한다(빼려면 "어디로 옮겼는지" 를 적는다).
- **주행 중(> 5 km/h) 화면에 터치 타깃·점수 0**, 잠금은 한 면 + "운전에 집중해 주세요". 운전자 문장 숫자 0(결과서·자세히 보기·진단서의 숫자는 예외). 정직성 배지 어디서든 읽힘.
- 차 화면 2560×1268 dp, 운전석 거리 가독성(본문 40 dp↑, 누르는 높이 112 dp↑).

## 지금 앱(27a 반영) 캡처 — `docs/screenshots/lesson/` (Read 로 본다)

홈 `lesson-setup.png` · 첫 실행 `lesson-setup-first.png` · 모드 팝업 `lesson-setup-mode-popup.png` · 과제 시트 `lesson-setup-sheet.png` · 프로필 `lesson-profile-sheet.png`·`lesson-profile-onboarding.png` · 코치 대화 `lesson-setup-coach-cards-follow.png` · 예약 카드 `lesson-setup-reserved.png` · 브리핑 `lesson-briefing-captions.png` · 주차 중 `lesson-maneuver-guides.png`·`lesson-maneuver-hint.png`·`lesson-maneuver-b.png` · 잠금 `lesson-locked.png`·`lesson-done-locked.png` · 점검 `lesson-checklist-modes.png`·`lesson-maneuver-checklist.png` · 코스 주행 `lesson-drive-exam.png`·`lesson-drive-road-red.png`·`lesson-drive-emergency.png` · 회차 판정 `lesson-done.png`·`lesson-done-checklist.png`·`lesson-done-exam-good.png` · 오늘의 기록 `lesson-report-session.png` · 자세히 보기 `lesson-details-session.png` · 진단서 `lesson-certificate.png` · 퀴즈 `lesson-quiz.png`·`lesson-quiz-done.png` · 시험장 `lesson-venues.png`·`lesson-venue-slots.png` · 준비실 `lesson-admin-home.png` · 띠 `lesson-admin-band.png`.

## 페이지 배정

1. `1-home-popups.html` — 홈에서 열리는 것: **모드 팝업** · **프로필 시트**(+ 첫 실행 프로필 질문) · **코치와 대화 시트**(말 카드) · **예약 카드가 있는 홈** — 각 6~8안.
2. `2-session.html` — **브리핑** · **주차 중**(가이드 도식 · 힌트 · 평가 — "후면 직각 주차 1회차") · **잠금** — 각 6~8안.
3. `3-checklist-drive.html` — **출발 전 점검**(가이드/힌트/평가) · **코스·도로 주행**(지도 · 신호 · 돌발) — 각 6~8안.
4. `4-results.html` — **회차 판정**(Done — 주차/점검/모의시험) · **오늘의 기록**(Report) · **진단서** — 각 6~8안. 자세히 보기는 6 쌍 알약으로 확정 — 이 셋이 그것과 한 가족으로 보이게.
5. `5-quiz-venue-admin.html` — **지식 퀴즈**(문항 · 해설 · 결과) · **제휴 시험장**(목록 · 시간·코스 · 예약 — 문장 시트와 같은 문법) · **준비실 + 관리자 띠** — 각 6~8안.
