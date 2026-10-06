# NEXT — 새 세션은 여기서 시작

새 Claude Code 세션이 이 파일 하나로 이어받을 수 있게 쓴 인계 문서. **작업을 마칠 때마다 이 파일을 갱신한다**(끝난 것은 지우고, 새로 생긴 것은 추가). 과거의 경위는 `docs/journal/`, 규칙은 `AGENTS.md`·`CLAUDE.md`, **제품 정의는 `docs/topics/01_driving_coach.md` v2**.

**함께 볼 것**: [놓치지 말고 확인해야 할 사항](놓치지%20말고%20확인해야%20할%20사항.md) — 사용자가 생각날 때마다 적어 두는 확인·고려 목록. 작업을 고를 때 같이 본다.

마지막 갱신: 2026-10-03 오후 · **마감 2026-10-09(금)**(10-07 아님 — 9/30 사내 확인) · 제품명 **"드라이브 코치"** 1.0.0 · 제출 4종(PPT 1장 주최 양식 · 팀 Bitbucket `submission` 브랜치 · MarketUploader APK(VEHICLE, 재업로드마다 versionCode +1) · 에뮬 시연 영상 → PPT·영상은 MOAH@mobis.com). **⚑ 10/5 밤 상태**: 검토 피드백 12건 반영 끝 — 라운드 20 #167 · 라운드 21 #173(시안 선택분 ①~⑧ + 플레이크 ⑨) · 버그 #163·#164 · 장내 칸 ×1.3 #166(테스트 292). **열린 발주 없음.** 동결 태그 `inhouse-20261006-1` + 번들 · 5분 영상 `build/demo-5min-20261006.mp4`(무음) · 22분 검토 영상 `build/review-all-features-20261005.mp4`. **남은 일**: ① **사용자 직접 조작 피드백 12건 받음(10/5 밤)** → 분류 `docs/design/11_round22_feedback.md`(A/B/D + 시험장 플로우·신호 출처 의견·프로필 계획과 질문 10) · 바로 고칠 버그 4건 **Codex 라운드 22 발주** `docs/handoffs/2026-10-05_codex_ui_round22.md`(시험장 선택 카드 바탕 · Done `SpeechFooter` 사라짐으로 알약 점프 · 자세히 보기 지표 2×2 · 보조/주 알약 간격 ≥ 64 dp 공통 부품) · 시안 7주제 `docs/design/round22-proposals/index.html`(진단서 5 · 제목 5 · 모드 간격 5 · 홈 AI 대화 8 · 관리자 모드 4 · 자세히 보기 배지 3 · 프로필 4) → **사용자 결정(밤 2차)**: 1 진단서 B · 3 모드 C · 5 관리자 D+C · 6 배지 B 확정, 2 는 대상 정정(분류 트랙) → 시안 `2b`, 4 는 A+D → 간격 시안 `4b`, 7 은 A+B+D → 조합 시안 `7b`(11 문서 "사용자 결정" 표) · 3-C·6-B 는 라운드 22 발주서 ⑤·⑥ 에 합침 → **3차: 2b B 먹 · 4b A1 · 7b P2 확정**, 프로필 질문은 기본값으로 → **라운드 23 발주서** `docs/handoffs/2026-10-06_codex_ui_round23.md` → ✅ #176·#177 머지 → ✅ **모델 선행 전부 머지**: #178 진단서 범위별 혜택 · #179 관리자 모드(프리셋 다섯 `data/AdminPresets.kt`, Real 은 null) · #180 홈 코치 대화·예약 카드 · #181 tools(emu_flow 가 시트에서 과제 명시 + `--es preset`) · #182 프로필 칩·저장(`filesDir/profile.properties`)·첫 실행·리포트 끝 질문(테스트 321) → ✅ **Codex 라운드 22 머지 #184 `ef62f6f`**(10/6 새벽 리뷰: main 병합 후 빌드·321 · `RESERVE=1 emu_flow` PASS · `course_flow` PASS · `lesson_shots` 은 #182 의 빈 첫 프로필 때문에 예약 계측이 실패 → 픽스처 한 줄(`admin.setProfile(PROFILE_RUSTY)`)로 PASS. 진단서 잘림·버튼 두 줄은 23a ① 조건으로) → ✅ **라운드 23a #186 `410cc6e`**(진단서 B · 분류 트랙 먹 · 홈 A1 코치 대화·예약 카드) → ✅ **라운드 23b #187 `726abbc`**(준비실 = 홈 워드마크 약 2초 길게 · 하단 띠가 옛 `시연` 알약 대체 · 프로필 P2 첫 실행·시트·리포트 끝 카드 · `--es preset` 적용) → ✅ 대본·컷·덱·`06` 을 띠·준비실로(10/6, 이 PR) → ✅ **동결 태그 `inhouse-20261006-2` = `0ab3c30`**(번들 `build/moah2026-20261006-2.bundle`, clone·`REPO_ONLY=1` PASS) → ✅ 5분 영상 `build/demo-5min-20261006-2.mp4`(4분 59초, 무음) · 대본 재측정(녹화 켬 t+140) · 덱 캡처·숫자(테스트 321 · PR 190 · 발주서 31) · 사내 재개 절차 `docs/handoffs/2026-10-06_inhouse_resume.md`(#190) → ✅ **사내 검증 #4(10/6, 태그 `-2`)**: 차량·준비실·띠·모의시험 동작, 사내 배지 주차 7/1/0 · 모의시험 live 11 — 반드시 고칠 것 둘(사내 단위 테스트 1 실패 = 전조등·와이퍼 키 #194 · 움직이기 전 문 열기로 화면 멈춤 #193) + 점검 스크립트 첫 실행 화면 #195 + 문서(`pm clear --user 10`·`출발` 의미) · 기능 요청 홈 코치 텍스트 대화(`docs/design/12_home_coach_dialog.md`) → ✅ #193·#194·#195·#196 머지 → ✅ **태그 `inhouse-20261006-3` = `624cf04`**(녹화 기준, 번들 `build/moah2026-20261006-3.bundle`, 테스트 324 · emu_flow PASS · 번들 clone·`REPO_ONLY=1` PASS) → ✅ 홈 코치 대화 모델 #198(`CoachPort.converse` · 의도 검증 · Fake 키워드 규칙 · `persona` · 테스트 340) → ✅ Codex 라운드 24 화면 #200 · 사내 점검 텍스트 두 줄 단계 #201 → ✅ **태그 `inhouse-20261006-4` = `64462c0`**(번들 `build/moah2026-20261006-4.bundle`, 테스트 340 · emu_flow PASS · inhouse_check 코치 두 줄 · 번들 clone·`REPO_ONLY=1` PASS) → ✅ 디자인 검토(10/6 저녁): 예약 홈 이유 한 문장 #203 — Codex 다듬기 셋(준비실 상태 줄 가림 · 띠 `더 보기` 재생 상태 위치 · 대화 위쪽 페이드)은 녹화 뒤로 미룸 → ✅ **태그 `inhouse-20261006-5` = `f2af874`**(녹화 기준, 번들 `build/moah2026-20261006-5.bundle`, 테스트 341 · emu_flow·course_flow PASS · 번들 clone·`REPO_ONLY=1` PASS) → 사내 시작점 파일 `docs/handoffs/INHOUSE_NOW.md`(한 줄 명령, 태그마다 갱신) → ✅ **사내 검증 #5(15:30)**: `-5` 녹화 OK — Real 341 · 문 열기 멈춤 해결 · 홈 코치 두 줄 `(ai)` 1.2/1.5 s(사내 Copilot 여러 턴 확인) · 한국어 입력 불가(에뮬 키보드 앱이 죽음) → "말 카드" 요청 · 사용자 직접 사용 피드백(지식 알약 한 칸 · 시험장 S자 · 마감 시간대 · 잠금 중 관리자 · 코치 버튼 이름·오른쪽 · 프로필 편집 안내 · 헷갈리는 도로 상식) → 시안 라운드 25(`docs/design/round25-proposals/`) → **지금**: 사내는 `-5` 로 녹화(대화 컷은 말 카드 태그 뒤) · 사외는 시안 선택 → 말 카드 모델 · Codex 라운드 25 → 새 태그 · 사람 몫(사내 재검증·A안 녹화·PPT·submission·MarketUploader·메일). 시연은 준비실 프리셋 `후면 주차 두 회차`(또는 `--es preset rear-two`)로 시작 — 첫 실행 질문이 보이면 프리셋이 안 들어간 것. 에뮬: Claude `CSTDe_API_34` = `ANDROID_SERIAL=emulator-5554`, Codex `Codex_Round22` = 5556. **주의**: #182 뒤 main 은 처음 켤 때 프로필이 비어 홈 제안이 "출발 전 점검" 이다(첫 실행 화면은 23b) — 시연·녹화는 23b 뒤 프리셋으로 ② ✅ 덱·대본·컷 목록·제출 원고의 캡처 설명을 라운드 21 화면으로(10/5 밤 PR — Done 판정 2×2·상세 나란히·점검 막대·시험장 지도 카드, 실제 `emu_flow` 두 회차 리포트 `lesson-report-session.png`(✓✓✓✓ · 0/8/0)·`lesson-details-session.png` 추가. 옛 `lesson-report-verdict-last.png` 는 라운드 21 부터 △△✓✗·미측정 1 fixture) ③ 사람 몫 — 사내 재검증(INTEGRATION A, 새 항목 "코스 과제")·A안 녹화(10/7~8) · PPT 1장 · `submission` · MarketUploader · 메일. 선택: 자세히 보기 잘린 흐린 줄 · lesson_shots 공통 대기 도우미. **지금 상태: 사내 전달 태그 `inhouse-20261006-1` = `ab63dab`**(10/5 저녁 동결 — 검토 피드백 반영 라운드 20 #167·21 #173(시안 선택분), 버그 #163·#164, 장내 칸 ×1.3 #166, 테스트 292, 번들 clone·`REPO_ONLY=1` PASS. 직전 `inhouse-20261005-2` = `c05c473`. 10/5 오전 `-1`(f4a4e31) 뒤 #160 하나 — 시험 모드 감점 방송이 화면에 안 남던 것(무음 시연 녹화에서 발견, 소리 없는 사내 에뮬에서 감점이 안 보였을 것)을 고침, 테스트 288, 5분 시연 영상 `build/demo-5min-20261005.mp4`(무음, 1부 후면 + 2부 모의시험). 10/5 아침 **동결** — 전 범위 구현 #140~#153 + 라운드 18·19 #152·#157, 테스트 287, 번들 `build/moah2026-20261006-1.bundle`, 번들 clone·`REPO_ONLY=1 inhouse_check` PASS. 사내 재검증·녹화는 이 태그로, 새 확인 항목 = INTEGRATION A "코스 과제"). 직전 태그 `inhouse-20261004-1` = `df92450`(번들 `build/moah2026-20261004-1.bundle`, 단위 테스트 242 — 전면 직각 주차 + 라운드 13~17. **동결 태그** — 사용자 U9 확인 뒤 남은 것은 버그·사내 관찰 반영·제출물뿐. 직전 `inhouse-20261002-3` 으로 사내 검증 중이면 노트 #4 는 그대로 받고, ⑦ 은 이 태그로) — **사내 재검증은 이것으로**. **10/2 밤 사용자 새 계획 → 결정 셋**: 질감 **B**(살짝 그림자 + 물방울) · 주차 과제는 **전면 직각부터 하나만** · 동결 **10/6 그대로**. 전면 직각 주차의 코드(사양 #115 → 연결 #116 → 시드·시나리오·READY #117, 테스트 235)는 끝났고 사외 에뮬에서 끝까지 돈다(배지 7, 60/55 → 100/100). **라운드 13 전부 머지(10/3 아침)**: ① 질감 B #121 → ② 전면 화면 #122 → ③ 시드(`aligned` 서두 제거·퀴즈 10) #123 — 각각 main 을 브랜치에 머지해 충돌(계측 플래그·INTEGRATION C·README) 해소 뒤 빌드·238·후면 `emu_flow`·전면 흐름·`lesson_shots` PASS 로 리뷰. **사용자 캡처 피드백(10/3)**: 질감 B 좋음(U7 ✅) · 전면 Done 이 "그냥 차 한 대" 로 읽힘 · 과제 카드 그림 쏠림·제목/난이도 간격 어색 → 카드 시안 셋(A 균등·B 왼쪽·C 그림 면 + 띠) 중 **C** → **라운드 14 머지 #126 `6d84761`**(10/3 낮 리뷰: 빌드·239·후면 `emu_flow` PASS 113 s·전면 흐름 PASS 배지 7·`lesson_shots` PASS·캡처 7장, 후면 Done 캡처 불변) → **태그 `inhouse-20261003-2`**. **사용자 캡처 피드백 2차(10/3)**: Done 궤적 영역 상하 여백 · 작은 차 마크 앞뒤 → **라운드 15 머지 #129 `8d4d960`**(10/3 오후 리뷰: 빌드·242·후면 `emu_flow` PASS 114 s·전면 PASS·`lesson_shots` PASS(1회차는 예약 시트 플레이크, 재실행 PASS)·캡처 6장) → **태그 `inhouse-20261003-3`**. **사용자 피드백 3차(10/3 오후)**: 카드의 작은 차 마크가 "너무 별로" → 자동차 실루엣으로 → 시안(Maneuver 큰 차 실루엣 축소) → **라운드 16 머지 #132 `8cde048`**(10/3 리뷰: 빌드·242·`lesson_shots` PASS·후면 `emu_flow` 119 s·전면 PASS·캡처 2장, Maneuver·Done 캡처 불변) → **태그 `inhouse-20261003-4`(동결)**. **⚑ 10/4 계획 변경(사용자)**: 일정 때문에 줄이지 말고 **전 범위 구현** — 신호가 없는 기능은 시뮬레이션으로(`docs/design/10_full_scope.md`). 주행 과제 5·장내기능 모의시험(트랙 지도 실시간)·평행/사선 주차·예약→시험·기록 저장. **동결 10/7 오전**으로 이동. 단계 A1~A4(Claude) → B1(Codex) → C1~C3 → D. **10/4 전 범위 A1~A4 완료**: 시뮬레이션 신호 8 #141 → 코스 채점 엔진(구간 규칙 13종·모의 감점표) #142 → 도면 6장·시나리오 12벌 #143 → 구간 순서 보정 #144 → 상태기계 `Drive` 단계·도로 5 + 장내기능 모의시험 READY #145 → 에뮬 회귀 `tools/course_flow.sh` #146(장내 못함 70 불합격 → 잘함 100 합격, 배지 시뮬 17). 테스트 278, 본편 `emu_flow` 불변 PASS. **열린 발주: Codex 라운드 18**(코스 화면 — 지금은 Claude 임시 `DriveInterimScreen`, `lesson_shots` 는 주행 카드 READY 로 시트 계약이 바뀌어 라운드 18 에서 갱신). 이어서 **C1 평행·사선 주차 READY #148**(카탈로그 준비 중 0, 테스트 282) · **C2 세 시험장에 모의시험 코스 #149**(본편 예약 흐름 PASS) · 라운드 18 발주서에 ⑤(평행·사선 Done 칸 방향·준비 중 0·코스 칩) 추가. **C3 기록 저장은 보류**(시연 재현성·사내 앱 폴더 — 사용자 결정 대기). **10/5: 라운드 18 머지 #152**(주행 지도 `DriveScreen`·코스 Done/Report·주행 카드 축소 도면·평행/사선 Done 칸·준비 중 0 계측. 리뷰: 빌드·286·`lesson_shots` PASS·`RESERVE=1 emu_flow` PASS t+130 s·캡처 눈으로) → `course_flow` 2회차 "돌발 정지 지연" 은 시나리오 여유 부족(1.95 s vs 2 s) → **#153** 10 km/h·2.6 m/s² 로 약 1.3 s, `course_flow` PASS, 테스트 287. 다음 라운드 후보(작은 것): 신호등 없는 구간의 "신호등 꺼짐" 줄 · 감점 없는 회차의 빈 표 머리 · 차선 변경 카드 그림. 대본 2부·컷 E1~E8·덱 6b #156, 라운드 19 발주 #155 → **라운드 19 머지 #157**(신호등 꺼짐 줄 숨김 · 감점 없음 한 줄 · 차선 변경 카드 S 자. 리뷰: 빌드·287·`emu_flow` PASS t+139 s·`course_flow` PASS·`lesson_shots` 완주 5회 중 3 PASS — 새 차선 카드 픽셀 검사 1회·옛 시험장 시트 플레이크 1회, 마지막 2회 연속 PASS). **화면 라운드 끝.** 다음: 동결 태그 + 번들(10/7 오전) → 사내 재검증·A안 녹화(대본 1부 + 2부 선택) → 제출. **사용자 10/4**: 카드 실루엣 좋음, 단 전면 카드는 위가 막힌 칸이라 "벽에 부딪친" 느낌 → **라운드 17 머지 #136 `df92450`**(① Done 차 = 카드 실루엣 ② 전면 카드 양옆 두 줄. 리뷰: 빌드·242·`lesson_shots`·후면 `emu_flow` 120 s·전면 PASS·캡처 3장, Maneuver·Report 캡처 불변) → **태그 `inhouse-20261004-1`(동결)** — **10/4 사용자 확인 "괜찮다" → 화면 동결 확정**. 이후 코드 변경은 사내 관찰에서 나온 버그만. 열린 PR·열린 발주 없음. **거쳐 온 길(상세는 Step 14 · 14G 행 · 일지)**: 사내 이관 1차(9/29~30) → 관찰 반영 A~F(#62~#65) → `inhouse-20261001-1` → 라운드 9·AI 패널 → `-20261001-2` → **사내 검증 #2(10/1)**: 매니페스트 `uses-library` 누락으로 Real 이 Fake 폴백 → 반영 5 PR(#68~#72, 한 PR = 한 scope) → `inhouse-20261001-3` = `4d73ec1` → 10/1 밤~10/2: 라운드 10·감사 08/제출물 03 → 감사 A 4건·문서 정정·**public 확정**(#77)·D1~D6 → **주차 판정 네 가지**(설계 09 → `ParkingVerdict` #88 → 필수 필터 #92 → 라운드 11·12 #82·#90·#91·#93·#94 판정 네 줄 + 조향 도식) → `inhouse-20261002-1` = `fb7e56e` → **사내 검증 #3(10/2, `inhouse-20261001-3` 기준) Real 전부 PASS**(INTEGRATION B 10/2 [inhouse]) — `.gitignore` 뒤 글자 주석 1건만 동작 안 함 → build #96 → `-20261002-2` = `a576fc2` → `inhouse_check.sh` 저장소 단계(tools #100) → **`inhouse-20261002-3`**(문서 #97~#102). **일정**: ~10/3 사내 재검증(`-3` 로, 합격이면 사내 `inhouse/real-vss` 폐기) · ~10/6 기능 동결(버그·프롬프트·PPT 원고·대본 재실측·재녹화) · 10/7~8 사내 녹화 → `submission` → MarketUploader → 메일 · 10/9 예비. **새 세션은 여기서: ① 사내 관찰 노트 #4(`inhouse-20261002-3` ~ `-20261004-1` 중 하나 기준 — 캡처 제목의 태그로 구분)가 오면 INTEGRATION B 절에 옮기고 Step 14 "사내에서 확인할 것" ③~⑥ 대조 → 고칠 것은 PR 하나 = 한 scope → 다음 태그 ② 노트 전이면 Step 8 제출물(덱 5·6장 캡처 교체·8·9·11장 숫자 · PPT 1장 확정 · A6 컷 재녹화 — 대본 시각은 10/2 밤 태그 `-20261002-3` 로 재실측 완료, 111 s) ③ 시드 `aligned` 단독 서두 제거·퀴즈 10 은 라운드 13 ③(Codex, 선택).**

## 1. 지금 되는 것 (한 문단)

`main`(`df92450`, 10/4 · 태그 `inhouse-20261004-1` 동결 — 전면 직각 주차 #115~#117 + 라운드 13 #121~#123 + 14 #126 + 15 #129 + 16 #132 + 17 #136)에서 앱을 켜면 **기하학 포스터 디자인의 연수 세션 화면**(Setup → 과제 시트 두 층(시안 05, 면 카드·모핑) → Briefing → Maneuver(탑뷰 B·애커만 기하 조향 도식(앞·뒤 바퀴 점선, 라운드 12)·풀이 / 점검 칩 7) → Done(추정 궤적 재생·도착 칸(잘한 주차는 직각으로 들어감) + **판정 네 줄** 한 번에·방향(추정)·마무리·안전 ✓/△/✗/— / 점검 7항목 패널) → Report(두 문장 총평·마지막 회차 판정 네 줄·신호 출처 배지·`자세히 보기` 수치+방향 편차) + Quiz 2장 + 예약 시트)이 뜬다. 회차 멘트는 "한 번에 들어갔어요.\n이 감각 그대로 한 번만 더 해 봐요." 처럼 **측정된 판정에 근거한 서두 + 조언 한 문장**. 모든 화면이 속도 > 5 km/h 면 잠긴다(Maneuver·Quiz·Done·Report·QuizDone). 시연 조작 패널은 우상단 알약(`시연`)으로 접혀 있고 맨 아래에 `AI 코치 · <상태>` 줄(+`AI 연결`)이 있다. **빌드·단위테스트 226 통과**, APK 라벨 "드라이브 코치"·`uses-library mobis.framework`(`aapt dump badging` 으로 확인), CSTDe_API_34 에서 `Lesson contract passed`(12묶음, 픽셀 검사 포함)·`emu_flow.sh` PASS(리포트까지 약 125 s). 사내(Real)는 `local.properties` 한 줄로 같은 커밋이 빌드된다. 캡처는 `docs/screenshots/lesson/`(README 에 목록). 운전자 문장에 숫자 없음, 시간 감점 유예 60 s, 급제동 임계 -3.0(시나리오 -4.4).

**제품 정의 v2 (9/26)**: 시연 본편은 **후면 직각 주차 과제**. `Setup(대화·제안) → Briefing → Maneuver(도식) → Done("다 됐어요") → Report(도어 열림)`. 채점은 과정만. 모드 가이드→힌트→평가→지식테스트. 상세는 `topics/01_driving_coach.md`.

**시작 가능한 과제(READY) 넷**: 출발 전 점검(Step 11, 가이드·힌트·평가, 차는 서 있음) · 후면 직각 주차(시연 본편) · **전면 직각 주차**(10/3 #115~#117 — 사양 `ParkingSpec.FRONT_PERPENDICULAR`, 화면은 라운드 13) · 지식 테스트(Step 10, 화면은 라운드 2 에서 완성). 나머지 7개는 "준비 중"(카탈로그 11). 단위 테스트 226(main `fb7e56e`, 10/2).

**9/27 결정 3건이 main 에 반영됨(PR #21 → 화면 PR #25)**: 운전자 문장에 숫자 없음("서두.\n조언." 두 문장 + 총평 "흐름.\n안전." 두 문장) · 회차 시작 발화 · `SHOW_DEMO_PANEL` 플래그 · `AttemptRecord.path`(추정 궤적 — Done 왼쪽에 그려짐). 시연 패널은 기본 접힘(`시연`). 단위 테스트 147.

## 2. 남은 일 (의존 순서. 날짜 배정이 아니다)

### Step 3 — 신호 계층 + 채점 (Claude) — ✅ PR #1 머지 `4e6c1f0`(9/26) · 3g Hybrid 는 PR #4
| # | 일 | 어디 |
|---|---|---|
| 3a ✅ | B층 상수 10개 | `vss-stub/.../VssConstants.java` (COVESA 추정, `SelectedGear` 로 P/D 표현), 파서 `vehicle/VssGear.kt`(`Gear.parse`, `toVssIgnitionOn`) |
| 3b ✅ | `SignalAvailability { LIVE, SIMULATED, MISSING }` + `SignalRegistry` + `AvailabilityBadge` | `vehicle/SignalAvailability.kt`. 세션 동안 값이 온 키만 기록. Fake 면 전부 `SIMULATED` |
| 3c ✅ | `Scenario`/`ScenarioBuilder`(`at`, `speedRamp`), `FakeVehiclePort.play(scenario, speedFactor)`·`stop`·`holdSpeed`·`playback` | `vehicle/Scenario.kt`, `vehicle/FakeVehiclePort.kt`. 주차 2벌 `data/ParkingScenarios.kt`(잘한 26 s / 못한 44 s). 기본 sin 속도 시뮬은 도로용으로 그대로 |
| 3d ✅ | A층 `MotionSegmenter`, `HarshEventDetector`(300 ms 시간창 차분·1 s 디바운스) | `scoring/`. 이동 평균은 진짜 급정지를 깎아서 뺐다(일지 9/26) |
| 3e ✅ | B층 `SteeringReversalCounter`, `GearShiftCounter`, `ProximityMonitor`(거리 → 없으면 boolean), `PreDriveChecklist` | `scoring/`. 입력이 비면 null = 미측정 |
| 3f ✅ | `ParkingMetrics`·`ParkingRubric`·`ParkingScorer`(숙련/안전)·`ParkingDelta`·**`ParkingRecorder`**(delta 를 받아 시계열 → metrics → score) | `scoring/ParkingScorer.kt`, `ParkingRecorder.kt`. 상태기계는 `recorder.onDelta(now, delta)` 만 부르면 된다. `ParkingRecorderScenarioTest` 가 시나리오 2벌의 숫자를 고정 |
| 3g ✅ | `HybridVehiclePort(real, fake)` — Real 이 한 번이라도 값을 낸 키는 live, 나머지는 Fake 시나리오. `SignalRegistry.forPort` 가 키별 출처를 갈라 배지가 "실신호 2 · 시뮬 6" 로 섞인다 | `vehicle/HybridVehiclePort.kt`. 팩토리가 Real 을 기본으로 감싼다(`-PfillMissing=false` 면 순수 Real). live 키엔 시연 조작이 먹지 않는다 |

단위 테스트 21 → 62 (실패 0).

### Step 4 — 상태기계 + 시드 (Claude) — ✅ PR #2 머지 `6a6afb3`(9/26)
| # | 일 | 어디 |
|---|---|---|
| 4a ✅ | `LessonPhase` — `Setup · Briefing · Maneuver · Done · Report` (+ `GuideStepView`) | `feature/lesson/LessonPhase.kt`. `Maneuver` 에는 점수 필드가 **타입상 없다** |
| 4b ✅ | `LessonStateMachine`(`begin·finishAttempt·nextAttempt·endSession·reset`), `LessonViewModel`(+ `DemoControls`: 시나리오 재생·정차·도어 — Fake 일 때만) | `feature/lesson/LessonStateMachine.kt`, `LessonViewModel.kt`. 회차 시작 때 `vehicle.get()` 으로 레지스트리를 먼저 심는다(안 그러면 가이드가 전 단계를 MISSING 으로 보고 한 번에 읽어 버린다 — 일지 9/26) |
| 4c ✅ | `GuideRunner` — 단계 = (대사, 신호, 확인 문장, 판정). 한 스냅샷에 여러 단계가 충족돼 있으면 연달아 확인 | `feature/lesson/GuideRunner.kt`. 주차 6단계는 `SeedCatalog.parkingGuide` |
| 4d ✅ | `HintRules` — 채점 지표 **증가분**으로 힌트(벨트·근접·급조작 URGENT / 조향·기어 NORMAL), 규칙별 5 s 쿨다운 | `feature/lesson/HintRules.kt` |
| 4e ✅ | `SeedCatalog` — 과제 9, 가이드 6단계, 프로필 질문 5 + 시연 프로필(장롱 10년차·주차 공포), 멘트 21(밴드 4 × 태그), 지식 3, 예약 카드, 혜택 예시 | `data/SeedCatalog.kt`. **문구는 Codex 가 다듬는다** |
| 4f ✅ | `ManeuverDisplayState` 매퍼 + 리플렉션 테스트(skill/safety/reversal/shift/score 필드 없음) | `feature/lesson/ManeuverDisplayState.kt` |
| 4g ✅ | `Profile`(진술+관측), `ProgressStore`(인메모리), `ModeAdvisor`(가이드 70점×2 → 힌트, 힌트 80점×2 → 평가; 과제는 공포 → 약점 → 첫 쉬운 것) | `feature/lesson/LessonModels.kt`, `ProgressStore.kt` |
| 4h ✅ | `CoachPort` + `FakeCoachPort`(멘트 풀 변주·총평) — Step 6 의 Fake 를 앞당김. AI 구현체만 남음 | `ports/CoachPort.kt`, `feature/lesson/RemarkPool.kt` |
| 4i ✅ | `App.kt` 배선: registry·store·coach·scenarios·lesson. **`MainActivity` 는 아직 Dashboard** — 화면은 Step 5 Codex | `App.kt` |

단위 테스트 62 → 89 (실패 0). 첫 Codex 발주서: `docs/handoffs/2026-09-26_codex_lesson_screens.md`.

### Step 5 — 화면 (Codex) — ✅ PR #5 머지 `73ded64` (Claude 리뷰: 빌드·emu_flow PASS·lesson_shots PASS·캡처 눈으로)
`ui/lesson/`의 Setup / Briefing / Maneuver / Done / Report + DemoPanel + LessonRoute. 화면은 단계별 데이터만 받고 Route만 LessonPhase를 분기한다. Maneuver는 원래 snapshot의 locked/stopped 판정을 전달받는다(표시 속도 반올림으로 5.1 km/h를 해제하지 않음). 잠금 시 시연 패널까지 제거, 정차 때만 완료 버튼, 힌트는 4초 후 소거, 완료 버튼은 두 번 펄스 후 정지한다. 전 화면 32 sp 이상·한국어 Phrase 줄바꿈·말줄임 없는 자막.

- `LessonPresentationTest`: 표시 매핑 10개 추가(전체 99개). 미측정 지표 생략, 비교 없음/개선/악화, 배지·도식 경계, 이전 회차 TTS의 점수 문장 차단 등. Maneuver의 자막·힌트·가이드에서 `점수/감점/N점` 문장을 걸러 접근성 트리로도 새지 않게 한다.
- `LessonScreenInstrumentation`: 추가 Gradle 의존성 없이 플랫폼 접근성 검사. 잠금 상태 클릭·스크롤 0, 점수 비노출, 화면 액션, 신호 누락, 4줄 자막, 힌트 만료, 진단서 라디오. AGP가 덮어쓰는 첫 instrumentation 항목은 기본 runner 자리표시자로 보존했다.
- Fake 기본 속도가 도로용 45~95 km/h이므로 Setup 진입 시 DemoControls.stopCar(), 시나리오 재생 시 stopScenario → resumeCar → play를 호출한다. Real은 demo=null이라 해당 호출·패널이 없다.
- 원본 `tools/emu_flow.sh`는 PASS/clashes 0이지만 두 번째 회차가 끝나기 전에 완료할 수 있는 대기 조건 오류를 발견했다. `INTEGRATION.md` C절의 도구 수정 요청을 먼저 확인할 것.
- 두 번째 `asked done`만 `(attempt 2)`로 한정한 로컬 검증본으로 전체 재생 PASS/clashes 0: 못한 주차 60/55·이동 4회, 잘한 주차 100/100·이동 2회, 도어 → Report. 시작부터 리포트 104초(덤프·탭 대기 포함), 회차 시간 53초/34초. 실시간 Fake 배지는 `실신호 0 · 시뮬레이션 8 · 미측정 0`으로 실제 수신 개수를 표시한다.

### Step 6 — AI (Claude) — ✅ 전송 계층 PR #64(Cloud Copilot, 9/30 사내 실측 폴백 0) · 패널 화면은 Codex 발주
`ports/CoachPort.kt`(계약) + `FakeCoachPort`(시드 풀, Step 4) + **`CloudCoachPort(fallback, transport)`**(프롬프트 조립 `CoachPrompts`·타임아웃 4 s·응답 검증 길이/줄수/금지어·실패 시 폴백, 예외 안 던짐 — 테스트로 강제). `App.kt` 는 `transport = null` 로 배선 → 지금은 항상 시드 풀. **사내 Copilot 인증 방식 확인 → `CoachTransport.complete(system, user)` 구현체 하나** 넣으면 끝.

### Step 7 — 시연 파이프라인 (Claude) — ✅ 실행 검증 완료 (PR #6 에서 대기 조건·고정값 검증 보강)
| # | 일 | 상태 |
|---|---|---|
| 7a | `tools/emu_flow.sh` 주차 흐름(Setup 힌트 → 못한 주차 → 다 됐어요 → 한 번 더 → 잘한 주차 → 문 열기 → Report). 힌트 3종·회차 2·배지를 로그(`hint:`·`asked done`·`attempt N: skill=`·`report:`)와 라벨로 판정 | ✅ PASS·clashes 0·103 s. PR #6: `asked done (attempt N` 한정 + 회차 채점 고정값(60/55·4 · 100/100·2) 검증 |
| 7b | `tools/lesson_shots.sh` — Codex 의 `LessonScreenInstrumentation` 을 돌려 `lesson-*.png` 를 꺼낸다. `concept_shots.sh`·`gps_flow.sh` 는 삭제(16번 `0c95d18` 에 있음) | ✅ 실행 PASS·8장 캡처 |
| 7c | `docs/05_demo_script.md` — 2분 10초 타임라인·말할 것·시간 조절·사고 대응 | ✅ 실측 시각 반영(PR #6) |
| 7d | `DEMO_SPEED_FACTOR` 기본 10 → **1.0**(주차 시나리오는 실시간이 맞다). 상태기계에 스크립트용 로그 `hint:`·`asked done` | ✅ |
| 7e | 화면 PR 리뷰 뒤: `emu_flow.sh` PASS → 대본 시각 실측 → 캡처를 눈으로. 계측 계약을 만들 때 `build.gradle.kts` 의 `testInstrumentationRunner` 주석을 먼저 읽는다 | ✅ 9/26 (Codex 는 자리표시자 첫 항목으로 함정 회피) |

### Step 8 — 발표·영상·제출 — 🟡 사내 Real 검증 PASS(10/2 #3). **남은 것 = 사람 몫 제출물**(화면 동결 `inhouse-20261004-1`, 제 몫 문서·소리 없는 녹화는 10/4 ✅): 대본 ✅(10/4 실측 `시작` → 리포트 125 s, 라운드 13~17 화면 설명) → 덱 5·6장 캡처 교체(질감 B 적용 뒤)·8·9·11장 숫자·12장 "구현된 것" 에 전면 직각 주차 → PPT 1장 확정 → B안 재녹화(소리 포함) → 사내 A안 녹화 → `submission` → MarketUploader → 메일. **전면 직각 주차는 영상 본편에 넣지 않는다**(본편 = 후면) — 12장·덱의 "구현된 것" 캡처 한 장으로만
| # | 일 | 상태 |
|---|---|---|
| 8a | `docs/presentation/01_deck_outline.md` — 12장·7분, 장마다 화면·말·근거·초, 길이 조절(3/5/7/10분), 예상 질문 8, 발표자가 채울 것 | ✅ 초안(9/26) → 감사 03(#85) → 검증 #3 숫자(#110) → ✅ **10/4 동결 기준**: 4장 시연 길이 125 s·영상 `build/demo-freeze.mp4`, 6장 캡처 `lesson-report.png`(질감 B), 9장 숫자(테스트 242·PR 138·발주서 25), 12장 "구현된 것" 에 전면 직각 주차(`lesson-setup-sheet-front.png`·`lesson-done-front-good.png`) → ✅ **10/5 밤 라운드 21 기준**: 5장 Done 판정 2×2·6장 실제 세션 리포트 `lesson-report-session.png`(02 · ✓✓✓✓ · 0/8/0) + `lesson-details-session.png`(두 회차 나란히)·6b 코스 상세 두 카드·12장 점검 막대·시험장 지도 카드. 남은 것: 발표 직전 숫자 재집계 · 사내 노트 #4 의 ⑥ 결과를 11장 ② 에 |
| 8b | `docs/presentation/02_video_shotlist.md` — 컷 13개(약 2분 05초), A 사내판(Hybrid·Signal Simulator)/B 외부판, 편집 원칙, 녹화 절차·함정 | ✅ 초안(9/26) |
| 8c | **시연 영상 B안 녹화**(외부 에뮬, 사람이 직접 누름, 소리 포함은 PC 녹화) | 🟡 소리 없는 판 ✅ **10/4 동결 판** `build/demo-freeze.mp4`(155 s, 1920×1080, `RECORD= RESERVE=1 emu_flow.sh` — 시작 → 리포트 125 s, 질감 B·카드 C·같은 차 실루엣 화면, 프레임 스트립 `build/demo-freeze-strip.png`). 옛 판(라운드 8b 9/29·라운드 3 9/27)은 옛 UI. **소리 포함 제출용은 ⬜ 사용자**(PC 녹화, 대본 타임라인 그대로 — 대본 ✅ 10/4 실측 시각: Done 58 s · 회차 2 71 s · 리포트 125 s) |
| 8d | 사내: 이관 → A안 녹화 → Bitbucket 소스 → MarketUploader APK | 🟡 **이관·검증은 끝**(9/29~30 1차 → 검증 #2 10/1 → **검증 #3 10/2 Real 전부 PASS**, Step 14). 현재 전달 태그 **`inhouse-20261004-1`** = `df92450`(번들 `build/moah2026-20261004-1.bundle`, 라운드 13~17 포함, **동결** — 직전 `-20261003-2`·`-20261003-1`·`-20261002-3`, 전달 방법은 `docs/06`·`07`: public clone + `git checkout <태그>`, 막히면 번들). ⬜ 사내 재검증(③~⑥ 은 `inhouse-20261002-3` 이든 `-20261003-1` 이든, ⑦ 질감·전면은 `inhouse-20261003-1` 로 + jar 복사 직후 `REPO_ONLY=1 bash tools/inhouse_check.sh`) → ⬜ 기능 동결 태그(~10/6) → ⬜ 10/7~8 사내: A안 녹화 · `submission` 브랜치(태그 + `inhouse:` 커밋 1개) · MarketUploader(VEHICLE, versionCode +1) · PPT·영상 메일 |
| 8e | 슬라이드를 사내 양식으로 옮기기, 2번 장 개인 계기, **PPT 1장**(`docs/presentation/03_submission_onepager.md` 3안 중 B1) | ⬜ 사용자 |

### Step 14 — 사내 이관 1차 결과 반영 A~F (9/30 사내 관찰 요지 문서 → 사외, 10/1) — 발주 원문은 사내 문서(반입 캡처 4장, 저장소 밖)
| # | 일 | 상태 |
|---|---|---|
| 14A | 사내 전환을 "설정"으로: `local.properties` `mobis.vss.jar` 한 줄 + jar(`automotive/libs/` gitignore) → `settings`/`build.gradle.kts` 스위치, `USE_FAKE_VSS` 자동, `uses-library mobis.framework`, versionName 1.0.0, `gradlew +x`, README·06 §2 | ✅ PR #62 — **단, 매니페스트 `uses-library`·`.gitignore` `automotive/libs/`·`app_name` 은 e541131 에서 빠졌다**(본문에만 적힘, 사내 검증 #2 가 Fake 폴백으로 잡음) → ✅ PR #68 보강 |
| 14B | VSS 상수 이름 = 경로 대문자·밑줄(사내 jar 규칙) 10개 치환(28 파일), `Rear.Distance` → `vehicle/SimOnlySignals.kt`(실물에 없음), AGENTS 규칙 4 | ✅ PR #62 |
| 14C | Real/Hybrid 보정: 빈 값 미수신, `FakeVehiclePort.writeThrough` → Hybrid 가 실물에 먼저 씀, `forced`(실물이 거부한 키), 진단 로그, 테스트 2 | ✅ PR #63 (#62 위) |
| 14D | Cloud Copilot 전송 계층 `ports/copilot/`(device code → OAuth(device-protected) → 세션 토큰 → chat/completions, 401 1회 재시도), `CloudCoachPort` 5 s·모든 Exception 폴백·`twoLines . ! ?`·SYSTEM 한 줄, `CLOUD_COACH` 플래그, `DemoControls.aiState/connectAi` | ✅ PR #64 (#63 위) → ✅ Codex 패널 `AI 코치` 줄 **PR #66 머지 `6463120`**(`docs/handoffs/2026-10-01_codex_ai_panel.md` — 리뷰: 빌드·197·`emu_flow` PASS·`lesson_shots` 3/5(2회는 라운드 8 `captureSetupMorph` 첫 `takeScreenshot()` null, 이 PR 무관)·캡처 2장 NeedsLogin/Code 40 sp) |
| 14E | `tools/inhouse_check.sh`(adb 만: root·Wi-Fi·16M·설치·글자 탭 흐름·한 화면 요약, 캡처 없음) | ✅ PR #65 — 사외 Fake 로 흐름·요약 점검(`RealVehiclePort ready` 만 FAIL 이 정상) |
| 14F | 문서: `docs/07_two_site_workflow.md`(사외/사내 역할·태그·세션 순서·submission·커밋 규칙), AGENTS "프로젝트가 무엇인가" 3줄 + 커밋 규칙 7, NEXT(마감 10/9·일정), INTEGRATION B 9/30 [inhouse]·[hybrid]·[ai] | ✅ PR #65 |
| 14G | 태그 → 사내: `local.properties` 한 줄로 Real 빌드 · `copilot_config.json` push · `tools/inhouse_check.sh` 합격 → 사내 `inhouse/real-vss` 폐기, 태그 기준으로 다시 | ✅ `inhouse-20261001-1` = `3f5f4ea` → ✅ `inhouse-20261001-2` = `4922cab` → ✅ **사내 검증 #2 완료**(10/1: Real 은 매니페스트 누락으로 Fake 폴백, 임시로 넣으니 실신호 7·시뮬 1 전부 PASS — INTEGRATION B 10/1 [inhouse]) → ✅ **`inhouse-20261001-3` = `4d73ec1`**(14H 전부 머지 뒤, 번들 `build/moah2026-20261001-3.bundle` clone 테스트 — 매니페스트·gitignore 포함 확인, clean 빌드 203·APK badging 확인) → ✅ **`inhouse-20261002-1` = `fb7e56e`**(10/2 저녁 — 라운드 10·11·12, 감사 A 4, 판정 네 가지, 조향 도식 포함. 번들 `build/moah2026-20261002-1.bundle`: 메인 체크아웃이 `main` 을 잡고 있어 브랜치 `inhouse/base-20261002-1` 로, clone 테스트 OK, 226) → ✅ **사내 검증 #3 완료**(10/2, 태그 `inhouse-20261001-3` 기준: Real 빌드·APK·`inhouse_check.sh` 로그인 전 PASS(실신호 7·시뮬 1·미측정 0·FATAL 0·폴백 3)·로그인 후 PASS(폴백 0·채점 2.8 s/1.4 s)·기기 인증 — INTEGRATION B 10/2 [inhouse]. `.gitignore` 뒤 글자 주석만 동작 안 함 → build #96) → ✅ **`inhouse-20261002-2` = `a576fc2`**(10/2 밤 — `-1` + docs #95 + build #96. 번들 `build/moah2026-20261002-2.bundle`: `HEAD` 를 `inhouse/base-20261002-2` 로 두고 만들어 clone 이 바로 체크아웃, 태그·`check-ignore`·매니페스트 확인) → ✅ **`inhouse-20261002-3` = `194a6d4`**(`-2` + tools #100 `inhouse_check.sh` 저장소 단계 + 문서 #97~#99. 번들 `build/moah2026-20261002-3.bundle` clone 테스트: 태그·`REPO_ONLY=1` PASS) → ⬜ 사내 재검증(사용자, **이 태그로** — ③~⑥ 관찰) → ⬜ 관찰 노트 #4 → INTEGRATION B |
| 14H | **사내 검증 #2 반영**(10/1 저녁, PR 하나 = 한 scope): **반드시** build #68(매니페스트 `uses-library`·`.gitignore` `automotive/libs/`·`app_name` "드라이브 코치" — e541131 에서 빠졌던 것 + 문서 정정) · **고치면 좋은 것** tools #69(우리 앱 FATAL 만·`wait-for-device`·시트에서 과제 명시 선택·`-- Copilot:` 상태·logcat 안내) · vehicle #70(write-through `CancellationException` 전파·forced 재쓰기 방지·`get real=` 값 로그·테스트 주석/assert) · ports #71(Code 상태 보존·세션 일시 오류는 폴백만·헤더 `vscode/1.95.0`·`copilot-chat/0.25.2025010801`·`Openai-Intent`·시작 상태 로그·낡은 주석) · docs #72(02 상수 잔재·06 이름 규칙·커밋 규칙 8·라운드 10 발주·일지). 패널 `https://` 그대로는 Codex 라운드 10 ⓓ | ✅ 전부 머지(10/1 저녁, #68 `2907e56` → #69 `60c9bd7` → #70 `89d7cab` → #71 `c4bb772` → #72 `4d73ec1` — INTEGRATION B 인접 줄이 #70·#71·#72 에서 세 번 충돌, 매번 `origin/main` 을 브랜치에 머지해 해소(force push 없음)) → ✅ 태그 `-3` + 번들 |

**다음 태그 합격 기준(사내 검증 #2 가 정한 것)**: 사내 `local.properties` 한 줄 → `bash tools/inhouse_check.sh` 가 **로그인 전에도** FATAL(우리 앱)·`RealVehiclePort ready`·배지 PASS(AI 폴백은 로그인 전이라 허용), **로그인 후 폴백 0**.

**사내에서 확인할 것(14G, 관찰 노트로 적어 오기 — 10/2 검증 #3 결과 반영)**: ① ✅ `inhouse_check.sh` 요약(검증 #3: 로그인 전 Real ready·실신호 7·시뮬 1·미측정 0·FATAL 0·폴백 3 / 로그인 후 폴백 0·채점 2.8 s·1.4 s. `missing=[…]`·`setVSS rejected` 줄은 노트에 없었다 — 다음엔 그 두 줄만) ② ✅ `USE_FAKE_VSS=false` 빌드 ③ 🟡 OAuth — 한 번 재로그인 뒤 `state=Ready`(예상대로). **`install -r` 뒤 유지**(패널 `AI 코치 · 연결됨` 그대로)는 다음 검증에서 ④ ⬜ 총평이 측정 안 하는 항목을 또 조언하는지 ⑤ ⬜ TTS 없는 사내 에뮬에서 자막만으로 시연이 읽히는지(대본 타이밍) ⑥ ⬜ **(10/2 추가) 판정 네 줄·`자세히 보기` 의 `방향 편차 N°` 가 실신호(실차 조향각·기어)로 말이 되는지** — 안 되면 `docs/design/09` V2 대로 방향 판정을 미측정으로 돌린다 ⑦ ⬜ **(10/3 추가, 라운드 13 머지 뒤 태그에서)** 질감 B 가 사내 디스플레이에서 "살짝" 으로 보이고 글자 대비가 유지되는지 · 전면 직각 주차가 Real 신호로 끝까지 가는지(배지 7, 뒤 거리 칸 없음).

### Step 9 — UI 재설계 (Codex 별도 세션 · 기준 = 기하학 포스터) — ✅ 라운드 1~12 머지(마지막 라운드 12 ② #94 `fb7e56e`, 10/2) · 🟡 **라운드 13 열림**(10/3 발주, 9s: ① 질감 B → ② 전면 직각 주차 화면 → ③ 시드 정리 선택). 이것이 동결(~10/6) 전 **마지막 화면 라운드** — 그 뒤 새 라운드는 사내 재검증 관찰(③~⑦)에서 나온 것만
| # | 일 | 상태 |
|---|---|---|
| 9a | `docs/design/02_design_brief.md` — 라운드 0~3, 채울 것 9, **불변 규칙 12** + 검사 수단. 레퍼런스 = `docs/design/geometric-poster-development/`(시안 4장·핸들 B·시각 규칙·색 토큰 5). 탐색 이력(종이 UI·미니멀·Pinterest)은 `docs/design/README.md`(Codex) | ✅ 채움(모션 포함 9개 모두 사용자 확인 9/26) |
| 9b | 라운드 1 발주서 `docs/handoffs/2026-09-26_codex_ui_round1.md` — 디자인 시스템(`CoachStyle`) + `Maneuver`·`Report`, 드래프트 PR, `lesson_shots`·`emu_flow` PASS, 불변 표 12줄. **0절: Codex 본 트리 정리**(시안 폴더 → `codex/design-refs` PR, NEXT·일지 수정 되돌리기) | ✅ 시안 PR #13 `b211107` · 라운드 1 PR #14 `508f03b` (리뷰: 빌드·129·`emu_flow` PASS·`lesson_shots` PASS·캡처 vs 시안·불변 12줄). 사용자 톤 확정 |
| 9c | 라운드 2 — `Setup`(제안 문장 + 과제·모드 시트, 준비 중 회색)·`Briefing`(핸들 B 에셋)·`Done`·`Quiz`/`QuizDone`·점검 과제 칩 3개(C절 3건), 글자 없는 일러스트 에셋(WebP ≤ 1 MB), `emu_flow.sh` PASS. **라운드 1 관찰 반영**: 접힌 상태 라벨에 대상 붙이기(전부 MISSING 이면 접지 않기), `LessonCanvas` Bold → `BrandMark` 통일. 발주서 `docs/handoffs/2026-09-26_codex_ui_round2.md`. 선행(PR #16): `ManeuverDisplayState` 점검용 필드 5개, `emu_flow.sh` 시트 대응 | ✅ 발주 → ⬜ Codex |
| 9e | 상태기계 후속(Claude): ① 도어 선열림 `doorArmed` ② 조사 헬퍼 → 브리핑 "뒤 거리를 봅니다" | ✅ PR #17 `22a433a` |
| 9f | **라운드 2 Codex PR #18** — 리뷰 완료(빌드·137·`emu_flow` PASS 98 s·`lesson_shots` PASS·캡처 21장 vs 시안). 시연 영상 녹화 중 **경합 발견 → PR #19**(`publishManeuver` CAS: delta 코루틴이 Done 을 묵은 Maneuver 로 덮어쓰던 것). 영상(2분 14초)은 #18 + #19 빌드 | ⬜ 사용자 승인 대기: **#18 → #19 순서로 머지** |
| 9g | **라운드 2 영상 피드백 22건 분류** → `docs/design/03_round2_feedback.md`. 결정 3건 ✅(9/27): 숫자는 운전자 문장에서 전부 순화 / 궤적 시뮬레이션 진행 / 패널은 자동 접힘. **A 묶음(Claude, 브랜치 `claude/driver-copy-path`)**: `CoachPort` 두 문장(`AdviceRules` 조언 + 서두 풀), 총평 숫자 제거, 회차 시작 발화, `SHOW_DEMO_PANEL` 플래그, `PathReconstructor` → `AttemptRecord.path`. **디자인 세션 발주** `docs/handoffs/2026-09-27_codex_design_topview.md`(차 도식 3~4안·후진 도형·Done 궤적 시안) | 🟡 A 묶음 PR 대기 · 디자인 세션 → 사용자 선택 → 라운드 3 발주서(Claude) |
| 9h | **A 묶음 PR #21 머지**(`955adfa`). 디자인 시안 PR #22 머지(`88c29fe`) → **사용자 선택 B**(고정 바퀴 + 방향 호 + 후진 셰브론), Done 궤적 시안 확정 | ✅ |
| 9d | **라운드 3 발주서** `docs/handoffs/2026-09-27_codex_ui_round3.md` — 세 규칙(패널 접힘·숫자 없음·Headline 자동 축소) + Setup 시트 30/70·Briefing 정리·Maneuver B 도식(방향 호·셰브론)·Done 추정 궤적·Report 신호 출처 + 불변 표 + 캡처 교체. `emu_flow.sh` 는 접힌 패널을 `문 열기` 전에 다시 연다 | ✅ 발주 → ✅ **Codex PR #25 머지 `c0c3dfa`**(리뷰: 빌드·147·`emu_flow` PASS 108 s·`lesson_shots` PASS·캡처 24장 vs 시안·불변 표 — 관찰 3: 총평 머리말 중복(Claude 후속 ✅ PR #26), 회차 1 유예 여유 2 s(대본 사고 대응에 기록), `시연` 토글 상단 여백(라운드 4 후보)) → ✅ 대본 시각 실측 반영(`05_demo_script.md`) → ✅ **재녹화(B안, 소리 없음)** → ⬜ 사용자 영상 피드백 → ⬜ 컷 목록 "길이" 열 실측 · 덱 5·6장 스크린샷 교체 |
| 9i | **라운드 3 영상 피드백 11건** → `docs/design/04_round3_feedback.md`(B 9 · D 2 · A 0 — "움직임과 손맛"). **라운드 4 발주서** `docs/handoffs/2026-09-28_codex_ui_round4.md`: 조향 애니메이션(바퀴 회전 + 호 굴곡 + 점선 보조선 2) · Done 궤적 3 초 재생 · 주 버튼 규칙(140 dp × ≥ 720 dp: 시트 `시작`·`한 번 더`) · 시트 카테고리 띠 · 시연 토글 두 안 캡처 + 레일 정리 · 조향각 풀이(`오른쪽으로 한 바퀴 반`) · `조수석` 눈썹. 선행: `emu_flow.sh` 가 `시연` 을 `text`/`content-desc` 로 찾음 · **`ParkingRubric.graceSeconds` 45 → 60**(55 s 경계에서 60/58 이 흔들려 — 일지 9/28) | ✅ 발주 → ✅ 드래프트 PR #29 → ✅ 결정(D1 알약 · D2 `코치`) → ✅ **리뷰·머지 `090ec40`**(빌드·154·`emu_flow` PASS 115 s·`lesson_shots` PASS 25장·클립 2개 프레임 스트립·불변 표. 관찰: 선택 카드 `Paper` 띠가 바탕과 같아 안 보임 → 라운드 5 후보 / Done 캡처 대기 4 s·발주서 §2 오기 → ✅ 이 PR) → ⬜ 재녹화 |
| 9j | **라운드 4 영상 피드백**: "주차 후기 시뮬레이션이 주차에서 나오는 느낌" → 시간 순서·물리는 맞고(`pathThroughTime` 시작→끝, +y = 앞) **관습이 Maneuver("뒤가 위")와 반대**라 후진이 전진으로 읽힘. **라운드 5 발주서** `docs/handoffs/2026-09-28_codex_ui_round5.md`: Done 캔버스 180° 회전(뒤가 위, 오른쪽 조향 후진이 화면 왼쪽으로) · 후진 구간 재생 중 빨간 셰브론 · 시작 차는 윤곽선만 · 선택 카드 띠 `Ink`(라운드 4 관찰). 데이터·채점 불변 | ✅ 발주 → ✅ Codex PR #32 → ✅ **리뷰·머지 `49f5254`**(빌드·154·`emu_flow` PASS 120 s·`lesson_shots` PASS 9묶음(띠·뒤가 위 픽셀 검사 추가)·Done 캡처·클립: 아래에서 위로 후진, 후진 구간 셰브론, 끝 차가 위·왼쪽) → ✅ 재녹화 `build/demo-round5.mp4` → ⬜ 사용자 검토 |
| 9k | **라운드 6 · 시안 05 구현**: 주차·주행·조작·지식 글자 메뉴 → U자 과제 칸. 주차 네 칸과 도식, 선택 Signal 메뉴·밑줄·화살표 + Ink 칸·빨간 체크. 제안 과제 카테고리로 열리고 모드·시작 고정, 주행 다섯 준비 중 과제는 가로 스크롤·시작 숨김, 지식은 QUIZ만. 준비 중 목록을 둘러본 뒤 돌아와도 직전 READY 선택을 유지한다. 예약은 접고 펼친 본문만 스크롤한다. 전면 직각·사선 주차 PLANNED 시드 추가. 빌드·157 테스트·계약·원본 흐름 PASS, 시안/캡처는 `docs/screenshots/lesson/README.md` | ✅ 발주(디자인 → 선택 05 → §1) → ✅ Codex PR #37 → ✅ **리뷰·머지 `4fa5ef7`**(빌드·157·`emu_flow` PASS 123 s·`lesson_shots` PASS 10묶음(Setup 첫 렌더·카테고리 경로 픽셀)·캡처 vs 시안 05. 관찰: 도식 없는 카테고리 칸 위쪽이 비어 보임(라운드 7 후보) / Codex 가 NEXT 를 직접 고침(다음부터 C절) / `emu_flow` 첫 캡처 스플래시(콜드 스타트, 판정 무관)) → ✅ 재녹화 `build/demo-round6.mp4` → ⬜ 사용자 검토 |
| 9l | **라운드 6·동승자 영상 피드백 6건** → `docs/design/05_round7_feedback.md`: 지식 테스트 내 답·정답 표시(B) · 중간 `그만하기` → Setup(B) · 조향 앞바퀴 좌우 각 분리 + 보조선을 회전 중심 공유 두 호로(B + 순수 함수) · 출발 전 점검 확장(**D1** 7단계 제안) · **동승자 제거**(결정됨 — 화면 라운드 7, 모델은 Claude 후속) · 시험장 예약 시스템(**D3** 범위 가/나/다, 제안 나). **라운드 7 발주서** `docs/handoffs/2026-09-28_codex_ui_round7.md` | ✅ 발주 → ✅ Codex PR #46 → ✅ **리뷰·머지**(빌드·169·`emu_flow` PASS 배지 8·`lesson_shots` PASS(1회 흔들림: 답 클릭 직후 클릭 가능 노드 수 — 재실행 PASS, 폴링 권고)·캡처 3장·스트립·불변 표) → ⬜ Claude 동승자 모델 제거 → ✅ D1·D3 결정 → ✅ 발주(PR #47·#48) |
| 9m | **라운드 8 영상 피드백 3건**(`build/demo-round8.mp4`) → `docs/design/06_round8_feedback.md`: 시트 과제 칸 테두리 → 면 카드(B) · Setup → 시트 일러스트 모핑(B) · Done 궤적 "빠져나가는 모습 → 역재생"(**D1**: (가) 역재생 / (나) 도착 칸 그리기, 추천 (나) — 두 번째 지적). **라운드 8 발주서** `docs/handoffs/2026-09-28_codex_ui_round8.md`(퀴즈 계측 폴링 포함) | ✅ 발주 → ✅ **D1 = (나)**(9/28) → ✅ Codex PR #55 → ✅ **리뷰·머지 `71a5753`**(9/29: 빌드·179·`emu_flow` PASS 141 s·`lesson_shots` 정착 폴링 변경 요청 뒤 5회(4 PASS, 1 예약 칩 탭 흔들림)·캡처 3 + 스트립 2) → ✅ 재녹화 `demo-round8b.mp4` 187 s → ⬜ 사용자 검토 |
| 9n | **라운드 9 자체 점검**(9/30, 사내 피드백 전) → `docs/design/07_round9_selfcheck.md`: 캡처 44장 + 코드 + 에뮬 워크스루(점검 가이드 100/100·힌트 30/40·퀴즈 그만하기·예약 → 시작) → 16건. **규칙 위반 1**(퀴즈 잠금 화면 `맞은 문제 N`). Claude 선행 4(이 PR: 점검 총평에 "문" · 지식 난이도 하 · 점검 칩 기록 필드 · `reset` 자막 비움). **라운드 9 발주서** `docs/handoffs/2026-09-30_codex_ui_round9.md`(§1 규칙 · §2 다듬기 9 · §3 계측 · §4 결정 2) | ✅ 점검 → ✅ Claude 선행 → ✅ D1·D2 = 추천(9/30: 자세히 보기 항목별 넣음 · 브리핑 제목 주차 셋 전부·점검 첫~마지막) → ✅ Codex PR #61 → ✅ **리뷰·머지 `8dc2392`**(10/1: main 위 리베이스 빌드·192·`emu_flow` PASS·`lesson_shots` 3/3·캡처 §1 잠금 퀴즈 깨끗 · 2.1~2.9 전부 · §4 `조향 왕복 3 · 기어 전환 2 · 근접 1 · 급정지 1`·브리핑 3항목) |
| 9o | **라운드 10 후보**(사내 관찰 노트와 묶어 발주): ⓐ `briefingHeadline` 3항목 "핸들 방향**과** 기어 전환**과** 뒤 거리를" → 마지막 앞만 `과`, 그 앞은 `, `(Codex `LessonPresentation.kt`) ⓑ 계측 `takeScreenshot()` null 재시도 헬퍼(50 ms × 3 — 10/1 `captureSetupMorph` 에서 5회 중 2회 NPE, Codex `LessonScreenInstrumentation.kt`) ⓒ `DemoPanel` `Code` 상태 `Text` 를 `LessonText` 스타일(ko-KR·Phrase 줄바꿈)로 ⓓ 패널 코드 주소 `https://` 포함 그대로(사내 검증 #2) | ✅ 발주 → ✅ Codex PR #76 → ✅ **리뷰·머지 `aad6a46`**(10/2: 빌드·203·`lesson_shots` 3/3·`emu_flow` PASS·캡처 2장) |
| 9p | **Codex 대기 시간 롤 — 품질 감사 A + 제출물 원고 B**(코드 변경 0): 사내 검증·Claude 리뷰 시간에 Codex 가 노는 것을 채우되 화면·로직 손상이 구조적으로 불가능한 일만. A 감사 → `docs/design/08_consistency_audit.md`(불변 전수·일관성·문구 톤·상태 커버리지·대비·영상 컷, 발견은 A/B/D 분류만 + **라운드 11 발주서 초안**) · B 제출물 원고 → `docs/presentation/03_submission_onepager.md`(PPT 1장 3안·덱 반영 제안·영상 자막·심사자 리허설). 발주서 `docs/handoffs/2026-10-01_codex_audit_r10.md`(PR #73). 검토 때 제외한 것: `ui/` 정리 리팩터(동작 불변이 늘 거짓이 됨), 대본 재실측(에뮬 경합), 시드 문구 확장(라운드 10 테스트와 겹침 → 라운드 11) | ✅ 검토·발주 → ✅ Codex PR #75 → ✅ **리뷰·머지 `ee884f6`**(변경 파일 셋뿐, 사내 숫자 조건 표기, **B4 가 `isPrivate=false` 발견** → public 확정 #77) → ✅ A 4건(#78·#79·#80·#84) · 문서 정정(#85) · D1~D6(#86) · 라운드 11 발주(#81) 전부 소진 |
| 9q | **라운드 11**(감사 08 B 9건 + 결과 화면 잠금): ① `data` 시드 톤(가이드 어미·Done 서두 한 문장·퀴즈 해설, 톤 가드 테스트) ② `ui` Done·Report·QuizDone 잠금 레이어·긴 문장 4줄·점검 설명 시제·출처 목록·카테고리 `Muted`·fixture 캡처 39장·재시작 140 dp(D5). 발주서 `docs/handoffs/2026-10-01_codex_ui_round11.md` | ✅ 발주 → ✅ ① #82 `308cc1e`(207) → ✅ ② #90 `9f990a3`(215) — 둘 다 빌드·3/3·PASS |
| 9r | **라운드 12 — 메모 1·2번**: ① `data` 서두 숙련 평가 + 시나리오 기하(잘한 주차 끝 헤딩 41° → **88.5°**, 못한 주차 73.4°, 고정값 불변) ①′ `data` 판정 서두 8 템플릿(`one_go`·`one_fix`·`many`·`aligned`) ② `ui` **판정 네 줄**(Done 왼쪽 아래·Report "마지막 회차의 판정", ✓/△/✗/—) · `자세히 보기` 방향 편차 · **조향 도식 애커만 기하**(안·바깥 점선 곡률, 바퀴 걸침, 뒷바퀴 점선). 발주서 `docs/handoffs/2026-10-02_codex_ui_round12.md`. Claude 선행: `ParkingVerdict`(#88) · 서두 필수 필터(#92) | ✅ 발주(#89) → ✅ ① #91 → ✅ ①′ #93(220) → ✅ ② #94 **`fb7e56e`**(226) — 메모 1·2번 모두 화면에. 남은 사소: `aligned` 단독 서두 제거(선택) |
| 9s | **라운드 13 — 질감 B · 전면 직각 주차 화면 · 시드 정리**(10/3 발주 `docs/handoffs/2026-10-03_codex_ui_round13.md`): ① `ui` 그림자·하이라이트 토큰(CoachStyle 한 곳, D7 수치) → 주 버튼·칩·과제 카드·판정/점검 패널. 도식·글자·잠금 화면은 평면 ② `ui` `entryGear == DRIVE` 면 Maneuver·Done 앞이 위, `rearDistanceApplies == false` 면 뒤 거리 칸 제거, 도착 칸 뒤쪽 열림, 계측 전면 묶음 ③ `data`(선택) `aligned` 단독 서두 제거 · 퀴즈 5 → 10. Claude 선행: #115 `ParkingSpec` · #116 연결 · #117 시드. 캡처 근거 `docs/design/round13-front/` | ✅ 선행 3 PR · ✅ 발주 → ✅ Codex ① #121 `4fbc58c` → ✅ ② #122 `c1650ae` → ✅ ③ #123 `80d07f2`(10/3 아침 리뷰: 각 브랜치 빌드·238·후면 `emu_flow` PASS 110~111 s·전면 흐름 PASS 배지 7·`lesson_shots` PASS·캡처 눈으로. 관찰: 질감 B 는 상한 안, 준비 중 카드는 평면이라 READY 와 더 잘 구분됨 · `SurfaceTexture` 가 크기마다 비트맵 마스크 — 지금 화면 수에선 문제 없음) → 태그 `inhouse-20261003-1` |
| 9t | **라운드 14 — 과제 카드 C 배치 · 전면 Done 방향 보강**(10/3 발주 `docs/handoffs/2026-10-03_codex_ui_round14.md`, 사용자 캡처 피드백): ① 시트 카드를 위 2/3 그림 면(정중앙) + 아래 1/3 띠(제목 한 줄 + 난이도 같은 기준선)로, 준비 중은 평면, 도식 없는 카테고리는 아이콘 하나 ② 전면 Done 끝 차 앞 셰브론 상시·도착 칸 입구 점선·시작 셰브론·캡션 "앞으로 들어간 주차예요." — 후면 Done 불변. 시안 `docs/design/round13-front/card-preview.html`(C 열) | ✅ 발주 → ✅ Codex PR #126 → ✅ **리뷰·머지 `6d84761`**(빌드·239·후면 `emu_flow` 113 s·전면 PASS·`lesson_shots`·캡처 7장. 관찰: 주행 준비 중 카드 두 장은 36 sp 에서도 제목 말줄임 — 준비 중이라 둠, 제목 줄이기는 시드 몫) → ✅ 태그 `inhouse-20261003-2` |
| 9u | **라운드 15 — Done 궤적 영역 상하 여백 균등 · 작은 차 마크 앞뒤 구분**(10/3 발주 `docs/handoffs/2026-10-03_codex_ui_round15.md`, 사용자 캡처 피드백 2차): ① 보이는 요소 전체 bbox 로 세로 가운데, 카드 그림 면도 점검 ② Done 차·시작 윤곽·카드 도식에 공통 규칙(앞 유리 넓게·보닛 밝게·앞 모서리 둥글게), 전면 카드는 차 앞이 칸 쪽. 후면 Done 캡처 변경 허용 | ✅ 발주 → ✅ Codex PR #129 → ✅ **리뷰·머지 `8d4d960`**(빌드·242·후면 `emu_flow` 114 s·전면 PASS·`lesson_shots` PASS — 1회차 예약 시트 `assertPlannedTask` 플레이크, 새 검사 `Path painted margins`·`Small car windows` 는 PASS·캡처 6장) → ✅ 태그 `inhouse-20261003-3`(동결) |
| 9v | **라운드 16 — 주차 카드 차를 실루엣으로**(10/3 발주 `docs/handoffs/2026-10-03_codex_ui_round16.md`, 사용자 "너무 별로"): `VehicleDiagram` 몸체·패널·유리·미러 경로를 함수로 뽑아 카드가 같이 씀, 후면 뒤가 위·전면 앞이 위, 색 Maneuver 조합. Done 마크는 범위 밖. 시안 `docs/design/round13-front/card-silhouette-preview.html` | ✅ 발주 → ✅ Codex PR #132 → ✅ **리뷰·머지 `8cde048`**(경로를 `VehicleSilhouette.kt` 로 옮김 — 숫자 동일, Maneuver·Done 캡처 0 변경) → ✅ 태그 `inhouse-20261003-4`(동결) |
| 9w | **라운드 17 — Done 차 실루엣 · 전면 카드 주차 라인**(10/4 발주 `docs/handoffs/2026-10-04_codex_ui_round17.md`): ① Done 끝 차·재생 차·시작 자세를 `VehicleSilhouette`(카드·Maneuver 와 같은 모양)로, 미러 폭까지 세로 가운데 재계산, `SmallCarMark` 제거 ② 전면 카드 위가 막힌 U자 → 양옆 두 줄 | ✅ 발주(10/4 범위 확장 #135) → ✅ Codex PR #136 → ✅ **리뷰·머지 `df92450`**(경로를 `VehicleSilhouetteGeometry` 로, `SmallCarMark` 제거) → ✅ 태그 `inhouse-20261004-1`(동결) |
| 9d-준비 ✅ | 리뷰 뒤 단계 **선반영**(9/27, 브랜치 `claude/round3-prep`): `docs/05_demo_script.md`(패널 `시연` 조작·두 문장 멘트·탑뷰 B·추정 궤적·`신호 출처`·사고 대응 2줄, **시각은 잠정**), `presentation/02_video_shotlist.md`(컷 7·9 궤적, 편집 원칙 "숫자 보이면 옛 빌드", 녹화 함정 ③④), `01_deck_outline.md`(5·6장 화면 파일명·한 줄), `topics/01` §4.1·§4.5 Done 문장. main `10e4d1a` 기준선: 빌드·141 통과 | ✅ |

리뷰 루틴(Claude): worktree 에서 head 빌드 → `emu_flow.sh`(원본, 유예 60·급제동 -3.0 기준) → `lesson_shots.sh` → 캡처를 그 라운드 시안(`docs/design/round3-topview/`·`round6-sheet/`)과 나란히 눈으로 → 애니메이션은 계측 클립(`/sdcard/lesson-round4-*.mp4`)을 `ffmpeg` 프레임 스트립으로 → 발주서 §2 불변 표 grep → PR 코멘트. 이미지만인 시안 PR 은 바로 머지. **리뷰 뒤**: 머지 → `screenrecord` + `emu_flow` 로 B안 재녹화(`build/demo-roundN.mp4`) → 사용자 검토 → 피드백은 `docs/design/0N_roundN_feedback.md` 로 분류(A Claude / B Codex / D 사용자) → 결정이 필요한 것은 **캡처로** 묻는다(두 안 비교·디자인 세션). 컷 목록 "길이" 열은 사람이 누른 녹화에서 잰다.

**보류 후보(라운드 7 때 적은 것, 이후 피드백·감사 08 전수 캡처에서 재지적 없음)**: 도식 없는 카테고리(주행·조작·지식)의 세부 칸 위쪽이 비어 보임 → 칸 높이 축소 또는 제목 세로 가운데. 사용자가 다시 짚으면 그때 발주.

**순서**: 디자인(9, 라운드 13 ①② 까지 — 질감이 모든 화면에 보이므로 **그 뒤에** 찍는다) → 녹화(8c) → 사내(8d). 디자인 뒤에 찍어야 두 번 찍지 않는다. **지금 트리 배치(9/28)**: 본 트리 = Codex 것(Claude 는 `git checkout` 하지 않음) · Codex = 본 트리 또는 `.worktrees/ui-roundN` · Claude = `.worktrees/hybrid`(항상 main 으로 되돌려 둔다) — 동시 작업 규칙(§3). 재녹화 mp4 는 `.worktrees/hybrid/build/`(저장소 밖).

### Step 10 — 지식 테스트 (Claude 상태기계 ✅ PR #9 · Codex 화면 ✅ 라운드 2 #18 → 라운드 7 답·정답 표시·`그만하기` → 라운드 9 잠금 화면 숫자 제거 #61)
| # | 일 | 상태 |
|---|---|---|
| 10a | `LessonPhase.Quiz`(index·item·locked·chosen·correctSoFar)·`QuizDone`(results·items·remark), `QuizItem`·`QuizResult`·`QuizRecord`. 상태기계 `answer(choice)`·`nextQuestion()`, 잠금(속도 > 5) 중 답 무시, `endSession` 은 푼 것까지로 결과. 문제·선택지·정답 이유를 음성으로 | ✅ |
| 10b | 시드: 지식 과제 READY(QUIZ 모드만), 문항 5(회전교차로·비상등·우천 제동·야간 상향등·안전거리). 문구는 Codex 가 다듬는다 | ✅ |
| 10c | 화면 `Quiz`·`QuizDone` — C절 요청. 그때까지 `LessonRoute` 에 `[cross]` 자리표시자(Briefing 화면 재사용, 자막으로 진행) | ✅ Codex 라운드 2 PR #18(Quiz 2장) → 라운드 7(내 답·정답 표시, 중간 `그만하기`) → 라운드 11 ② `QuizDone` 잠금 레이어(#90) |
| 10d | (선택) `CloudCoachPort` 로 퀴즈 총평 변주 · STT 로 음성 답변(사내 확인 뒤) | ⬜ |

### Step 11 — 출발 전 점검 과제 (Claude ✅ PR #10 머지 `08e966b` · Codex 화면 ✅ 라운드 2 칩 3 → 11f 7단계 #49)
| # | 일 | 상태 |
|---|---|---|
| 11a | 시드 `TASK_PREDRIVE` READY(`CHECKLIST`, 주행 불필요), 가이드 3단계(벨트 → P 확인 → 시동), 점검 멘트 12, `SeedCatalog.scenariosFor(task)` | ✅ |
| 11b | `PreDriveSummary` 에 `beltOnMillis`·`ignitionOnMillis`·`beltBeforeIgnition`(마지막 false→true 전이) · `ChecklistScorer`/`ChecklistRubric`(숙련 = 순서·완성·시간, 안전 = 움직임·P·벨트) · `ParkingRecorder.scoreChecklist` | ✅ |
| 11c | 상태기계: 과제 유형으로 채점기 선택, 힌트 규칙 분리(`HintRules(checklist = true)` — 시동 먼저·움직임), "다 되셨나요?" 는 벨트·시동·P 가 다 보이면. `CoachPort.remark(task, …)` 로 과제 전달, 머리말 `attemptHead(task, score)`("출발 준비 N초."), `RemarkTemplate.taskType` 으로 멘트 풀 분리 | ✅ |
| 11d | `ChecklistScenarios` 잘한 점검(8 s, 100/100) · 못한 점검(12 s, 60/70). `DemoControls.scenarios` 는 진행 중 과제의 것만 | ✅ |
| 11e | 화면: 점검 과제용 `Maneuver` 칩 3개·`Done/Report` 행 교체 — C절 요청 | ✅ Codex 라운드 2 PR #18 → 11f 에서 칩 7·리포트 ✓/✗ 7행(#49) |

단위 테스트 113 → 129. `emu_flow.sh`(주차) 는 영향 없음 — 시연 본편은 그대로 주차.

| 11f | **7단계 확장**(결정 D1, 9/28): 도어 → 벨트 → P → 브레이크+시동 → 좌 지시등 → 우 지시등 → 비상등. 새 VSS 경로 없음(스텁의 `Door`·`DirectionIndicator`·`Hazard`·`Brake.PedalPosition`). 발주서 `docs/handoffs/2026-09-28_codex_predrive_7steps.md` — Claude 선행 A1~A10(스냅샷 필드·점검 키 집합(주차 배지 불변)·`PreDriveSummary`·채점·가이드 7·힌트 2·조언 3·시나리오 2벌·표시 상태·B절) → Codex(칩 7·리포트 체크 표·문구) | ✅ 발주 → ✅ Claude 선행(`claude/predrive-7-model`, 이 PR — 단위 테스트 166 · `emu_flow` PASS 배지 8 불변 · 못한 점검 30/40) → ✅ Codex PR #49 → ✅ **리뷰·머지 `7959727`**(빌드·179·`emu_flow` PASS 배지 8·`lesson_shots` PASS 11묶음·캡처 4장: 칩 7 = 4+3·미측정 회색·혼합 출처·리포트 ✓/✗ 7행) |

### Step 13 — 제휴 시험장 예약 (결정 D3 = (나), 9/28) — 발주서 `docs/handoffs/2026-09-28_codex_reservation.md`
| # | 일 | 상태 |
|---|---|---|
| 13a | Claude 선행: `Venue`·`Course`·`Slot`·`Reservation`(`ReservationCard` 제거), 시드 시험장 3·코스 3·시간대 3, `ProgressStore.reservation`, 상태기계 `reserve`/`cancelReservation`(Setup 에서만), `Setup.venues/reservation`, `ModeAdvisor.suggestTask` 예약 코스 우선, 테스트 | ✅ PR #48 `97d81c8`(`ReservationCard` 는 Codex 화면 전환 때 함께 삭제, `Setup.booking`·`venues` 추가 · 테스트 168 · `emu_flow` PASS) |
| 13b | Codex: 시트 `제휴 시험장` 층(카드 3 → 시간·코스 칩 → `예약` → 확인 카드·`취소`), Setup 배지 `예약 · …`, 계측·캡처 4장 | ✅ Codex PR #50 → ✅ **리뷰·머지 `d66f31d`**(#49 뒤 리베이스 1곳 · 빌드·183·`emu_flow` PASS·`lesson_shots` PASS 12묶음(타임아웃 300 s 로) · 캡처 6장. 소유 예외로 `ReservationCard`·`Setup.reservation` 삭제됨) |
| 13c | 대본 0:10 배지·덱 12장 "있는 것" | ✅ 이 PR(대본 준비 절 한 줄 · 덱 12장 "있는 것" = 예약 + 점검 7단계) |

### Step 12 — 동승자 공유 — ⛔ **제거 완료**(9/28 사용자: "운전자 입장에서 불필요". 화면 PR #46, 모델·시드·`emu_flow`·대본·컷·덱 = 이 PR). 이력은 PR #40~#43 · 발주서 `docs/handoffs/2026-09-28_codex_companion_share.md`(기록용)
| # | 일 | 상태 |
|---|---|---|
| 12a | Claude 선행: `CompanionShareLevel`·`CompanionNote`(`CoachPort.companionNote`, Fake = `CompanionRules`(밴드 풀 + `AdviceRules.Advice` 운전자/동승자 쌍), Cloud 프롬프트·폴백)·`LessonReport.companion/companionShareLevels/cheers`·`Setup.cheer`·상태기계 `shareWithCompanion`/`cheer`(`ProgressStore`, `reset` 이 안 지움)·`SeedCatalog.cheers`·`emu_flow` 선택 단계(화면 없으면 건너뜀) | ✅ PR #41 머지 `98ec8f8`(단위 테스트 164) |
| 12b | Codex: Report `동승자` 탭(두 문장·공유 범위 3·응원 칩 3·`다시 시작`), Setup 응원 눈썹, 계측·캡처 2장 | ✅ Codex PR #42 머지(빌드·단위 테스트 164·`Lesson contract passed`·`emu_flow` PASS) → ⛔ PR #46 에서 제거 |
| 12b-후속 | Codex C절 요청: 재시작 Setup 추천 이유 "가이드를 0번 통과했어요"(숫자·어색) → `ModeAdvisor.suggest` 세 문장 숫자 없이(`ModeAdvisorReasonTest`) | ✅ 이 PR |
| 12c | 대본: 진단서 뒤 동승자 10 s(2:25 행), 컷 목록 11b, 덱 12장 동승자 공유를 "있는 것" 으로 | ✅ 이 PR |

### 사용자 결정·행동이 필요한 것
| # | 일 | 상태 |
|---|---|---|
| U1 | 제품명 | ✅ **"드라이브 코치"** 1.0.0(`app_name`, #68 — 검증 #3 APK 라벨 확인) |
| U2 | 사내 출근 일정 | ✅ 이관 1차 9/29~30 · 검증 #2 10/1 · 검증 #3 10/2 PASS. 남은 사내 일정: ~10/3 재검증 `-2` · 10/7~8 녹화·제출 · 10/9 예비 |
| U3 | 주차장·코스 시드 좌표 | ✅ Step 13 시드(시험장 3·코스 3·시간대 3)로 대체 — 시드 값이지 실측 아님 |
| U4 | 라운드 1 드래프트 PR 캡처를 보고 톤 확정 | ✅ 9/26(9b) · 이후 라운드 2~12 피드백 전부 소진 |
| U5 | **사내 재검증 `inhouse-20261002-3`** 관찰 노트 #4: ③ OAuth `install -r` 뒤 유지 ④ 총평 범위 밖 조언 ⑤ 자막만으로 시연 ⑥ 판정 네 줄·방향 편차가 실신호로 말이 되는지 + `inhouse_check.sh` 의 `missing=[…]`·`setVSS rejected` 두 줄. ⑦(질감 B·전면 과제)은 라운드 13 머지 뒤 **다음 태그**에서 | ⬜ 사용자(~10/3). ⑥ 이 안 되면 `docs/design/09` V2(방향 미측정) |
| U7 | **라운드 13 결과 확인** | ✅ 10/3: 질감 B 좋음(그대로) · 전면 Done 은 "그냥 차 한 대" → 라운드 14 ② · 카드 배치 C → 라운드 14 ① |
| U8 | **라운드 14 결과 확인** | ✅ 10/3: 카드 C 좋음. 남은 지적 둘(Done 궤적 영역 상하 여백 · 차 마크 앞뒤) → 라운드 15 |
| U9 | **라운드 15~17 결과 확인**(Done 여백·차 실루엣·카드 라인) | ✅ 10/4: 괜찮음 → `inhouse-20261004-1` 동결 확정 |
| U6 | 제출물 사람 몫: 소리 포함 B안 녹화(8c) · PPT 1장 B1 + 사내 양식·2번 장 개인 계기(8e) · 10/7~8 사내 A안 녹화·`submission`·MarketUploader·메일(8d) | ⬜ 사용자 |

### 뒤로 미루는 항목 (기술적 불확실성)
| 항목 | 이유 |
|---|---|
| STT | 설계에 자리만(정차 중 답변·Setup 대화). 구현은 3지선다 버튼부터. 사내 확인 후 |
| 주행 중 실시간 AI 발화 | 네트워크 왕복. 힌트는 규칙 |
| UI 컨셉 경쟁 | 필요해지면 16번 `ConceptScreens`·`ConceptContractInstrumentation` 되가져오기 |
| 의사 3D 지도 | 도로 과제 화면에 필요하면 16번에서 되가져오기(§5) |

### 알고 있지만 검증하지 못한 것
- B층 경로 — 사내 실측(검증 #2 10/1 · #3 10/2) **실신호 7 · 시뮬레이션 1 · 미측정 0**. 어느 키가 시뮬 1 인지는 노트에 없어(`missing=[…]` 줄) 다음 검증에서 확인. 조향각·기어가 실신호로 오는 것은 도식·가이드가 사내에서 돌아간 것으로 간접 확인, **판정 네 줄·방향 편차가 실차 값으로 말이 되는지(⑥)는 미확인**.
- 주차센서 VSS 경로는 추정조차 불확실 — 시뮬 1 이 이것일 가능성이 크다.
- 사내 환경: 빌드(설정 한 줄)·설치·Real·Hybrid·Cloud Copilot 전송·기기 인증은 확인됨(검증 #1~#3). **미확인**: TTS 유무와 자막만의 시연(⑤), OAuth 가 `install -r` 뒤 유지되는지(③), MarketUploader 업로드 절차(10/7~8 에 처음).
- 전면 직각 주차(10/3): 사외 Fake 로만 돌았다. 근접을 실물 `IsWarning` 하나로 보고 **앞 센서 경고라고 가정**(INTEGRATION B 10/3) — 사내에서 Signal Simulator 로 켜 보기 전엔 모른다(⑦). 화면(앞이 위·뒤 거리 칸 제거)은 라운드 13 ② 전이라 아직 후면 화면 그대로.

## 3. 일하는 방식 (16번에서 굳은 것, 그대로)

- **역할**: Claude = 인프라·포트·채점·상태기계·데이터 구조·문서·리뷰·머지. Codex = 화면·테스트·시드 문구. 화면은 `docs/handoffs/YYYY-MM-DD_codex_<topic>.md` 오더로(급한 한 줄은 `[cross]`).
- **흐름**: `claude/<topic>` 브랜치 → PR → 사용자가 "N 머지해" → `gh pr merge N --squash`. **머지 승인 없이 다음 작업을 쌓지 않는다.** (부트스트랩·기획 문서는 Day 0~1 이라 main 직접 커밋 — 이후는 PR)
- **Codex PR 리뷰 루틴**: 직접 빌드 → 에뮬 캡처 → **눈으로 본다** → `04_agent_workflow.md` 체크리스트 → PR 코멘트.
- **동시 작업 규칙 (9/26 사고 뒤)**: Codex 가 본 트리(`C:\Project\17_hackathon-pivot`)에서 작업 중이면 Claude 는 **거기서 `git checkout` 을 하지 않는다** — Codex 의 미커밋 파일이 Claude 브랜치로 넘어온다. Claude 는 `git worktree add .worktrees/<topic> <브랜치>` 로 별도 트리에서 빌드·커밋·PR 한다(`.worktrees/` 는 `.git/info/exclude`). 본 트리의 브랜치는 Codex 것이 유지돼야 한다. 반대로 Claude 만 일할 때는 본 트리를 쓴다.
- **에뮬**: `"$LOCALAPPDATA/Android/Sdk/emulator/emulator.exe" -avd CSTDe_API_34 -no-snapshot-load` 백그라운드. 먼저 `adb get-state`. 앱은 user 10. 함정은 `tools/README.md`. 스크린샷은 `screencap -d 4619827259835644672`, 탭은 그냥 `input tap`(`-d` 는 실패).
- **셸 함정**: 큰 heredoc + 한국어 → Bash 파싱 실패(9/25 재현). 긴 파일은 Write 도구, 커밋 메시지는 `-m` 여러 개 또는 `-F 파일`. **Gradle 은 PowerShell 로**(`.\gradlew.bat …`) — Git Bash 에서 `cmd //c gradlew.bat` 은 이 환경에서 실행되지 않는다(9/26). 작은 치환은 `sed -i`, Kotlin 백틱 테스트명은 heredoc 에 넣지 않는다.
- **사용자 선호**: 한국어. 선택지가 있으면 추천과 함께. 검증 못 한 것은 그렇다고. 남은 일수를 이유로 범위를 깎지 않는다 — 미루는 이유는 기술적 불확실성만. **코드에 이름이 박히기 전에 기획을 넓히는 타이밍을 중시한다**(9/26).

## 4. 어디에 무엇이 있나

| 찾는 것 | 위치 |
|---|---|
| 제품 정의·시연 시나리오·신호 표 | `docs/topics/01_driving_coach.md` (v2) |
| 차량 신호 포트 | `vehicle/VehiclePort.kt`, `FakeVehiclePort.kt`(시나리오 재생기 들어갈 곳), `RealVehiclePort.kt`, `VehiclePortFactory.kt` |
| 신호 상수 | `vss-stub/src/main/java/mobis/vss/VssConstants.java` — 지금 5개 |
| 음성 | `ports/TtsPort.kt` — `speak(text, priority)`, `lastSpoken`, `SpeechPriority.URGENT` |
| 위치·경로 | `ports/LocationPort.kt`, `GpsLocationPort.kt`, `Route.kt` |
| 화면 골격 | `ui/MainActivity.kt`(Dashboard 배선), `ui/CoachStyle.kt`, `ui/concepts/DesignScale.kt` |
| 빌드 플래그 6개 | `automotive/build.gradle.kts` — `USE_FAKE_VSS`, `FILL_MISSING_WITH_FAKE`(`-PfillMissing`, Real 일 때 Hybrid), `USE_FAKE_LOCATION`, `TTS_VOICE`, `DEMO_SPEED_FACTOR`(기본 1.0 = 실시간), `SHOW_DEMO_PANEL`(`-PdemoPanel`, false 면 시연 패널 없음) |
| 도구 | `tools/emu_flow.sh`(주차 세션 자동 재생 — 라운드 3 부터 `시연` 으로 패널을 열고 누른다), `lesson_shots.sh`(계측 캡처), `README.md`(함정 목록) |
| 시연 대본 | `docs/05_demo_script.md` — 라운드 3 선반영, **시각은 라운드 2 실측(약 100 s) 잠정** |
| 발표·영상 | `docs/presentation/01_deck_outline.md`(12장·대본·예상 질문), `02_video_shotlist.md`(컷 13·녹화 절차 A/B·함정 4) |
| 화면 캡처 | `docs/screenshots/lesson/` — 라운드 3 계측 24장 + 실제 세션 `lesson-done-path.png`·`lesson-maneuver-b.png`·`lesson-panel-open.png`, 검증 로그 `flow.txt`·`flow-round3-*.txt` |
| 디자인 | `docs/design/02_design_brief.md`(불변 규칙·채울 것), `03_round2_feedback.md`(피드백 22건·결정 3), `04_round3_feedback.md`(피드백 11건·결정 2), `round3-topview/`(탑뷰 B·Done 궤적 시안), `references/`, `01_ui_concept_candidates.md`(16번 후보 20) |
| 가정 원장 | `docs/INTEGRATION.md` B절 |

## 5. 16번에서 되가져올 수 있는 것 (읽기 전용 참조)

`C:\Project\16_hackathon` main `0c95d18`(2026-09-22).

```bash
git -C /c/Project/16_hackathon show 0c95d18:<16번 경로> > <대상 경로>
```

| 필요해질 수 있는 것 | 16번 경로 |
|---|---|
| 상태기계 구조 | `automotive/src/main/kotlin/com/moah/hackathon/feature/journey/{JourneyStateMachine,JourneyPhase,JourneyViewModel}.kt` (`DemoControls` 포함) |
| 화면 필터 패턴 + 테스트 | `.../feature/drive/DrivingDisplayState.kt`, `automotive/src/test/.../DrivingDisplayStateTest.kt` |
| 의사 3D 지도 | `.../ui/concepts/droad/{RoadProjection,RoadMap3D,RoadStyle}.kt` |
| UI 컨셉 계약·계측 | `.../ui/concepts/ConceptScreens.kt`, `automotive/src/androidTest/.../ConceptContractInstrumentation.kt` |
| 시연 대본·발표·영상 구조 | `docs/05_demo_script.md`, `docs/presentation/{01_deck_outline,02_video_shotlist}.md` |
| Codex 오더 형식 | `docs/handoffs/2026-09-18_codex_order_status.md` 등 13건 |
| 시드 데이터 형태 | `.../data/SeedCatalog.kt` |
