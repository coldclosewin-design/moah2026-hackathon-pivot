# 디자인·품질 감사 08 — 제출 전 기준선

2026-10-01 · 기준 `1a0b5352f57ba0a79dd55e356711b96ca6b1d2b0` · `codex/audit-r10`. [발주서](../handoffs/2026-10-01_codex_audit_r10.md)의 A1~A6 결과다. **코드·리소스·테스트 수정, 에뮬 기동, adb, 새 캡처, 녹화는 하지 않았다.** 독립 진행 중인 `codex/ui-round10`의 결과를 합격 처리하거나 이 문서에 포함하지 않는다.

## 범위와 판정 방법

발주서의 “캡처는 기존 95장”을 파일 목록과 대조한 결과, `docs/screenshots/lesson/`는 **95개 파일 = PNG 53장 + MP4 2개 + TXT 39개 + README 1개**다. PNG 53장 전체를 펼쳐 보고 코드와 대조했다. 네 스트립 PNG도 각각 한 파일로 세었다. 95장 검사 완료라고 쓰지 않는다. 아래 부록이 빠짐없는 PNG 대장이다. 사내 캡처·사내 소스는 사용하지 않았다.

근거는 **관찰**(저장된 캡처), **정적 확인**(기준 커밋의 코드), **검증 공백**(가능한 상태지만 캡처 없음)으로 나눈다. 기존 계측 로그의 PASS는 당시 기록이며 이번 재실행 결과가 아니다. 코드 위치의 `ui/`, `data/`, `feature/`, `ports/`, `scoring/`는 `automotive/src/main/kotlin/com/moah/hackathon/` 아래다. 위치는 파일명과 함수명으로도 찾을 수 있다.

분류: **A** Claude(상태기계·채점·포트·시드 구조·문서), **B** Codex(화면·시드 내용·계측), **D** 사용자 결정. 심각도: **상** 규칙 위반 또는 시연에서 드러나는 의미 오류, **중** 일관성·검증 공백, **하** 취향. 후속 발주가 명시한 예외는 되돌리라고 단정하지 않고 D로 기록한다. 이 감사의 결론은 **위반 0이 아님**이다.

## A1. 불변 전수

| 번호 | 화면 | 근거(캡처 파일명·코드 위치) | 발견 | 분류 | 심각도 | 제안 |
|---|---|---|---|---|---|---|
| A1-01 | Done → 재출발, Report·QuizDone | 캡처 없음; `feature/lesson/LessonStateMachine.kt:378`의 Done 분기, `toReport()`·`finishQuiz()`, `ui/lesson/LessonRoute.kt` | 정적 확인: Done에서 속도 상승은 화면을 갱신하지 않고, Report·QuizDone은 차량 구독을 종료한다. 이 화면들의 버튼·스크롤·결과를 주행 잠금으로 바꾸는 전역 처리가 보이지 않는다. Maneuver·Quiz 잠금 PASS만으로 “모든 주행 화면 터치 0”을 증명할 수 없다 | A | 상 | Done에서 문을 닫고 속도를 올리는 재현부터 요청. 정차 전용 결과 화면도 재출발 시 잠기게 하는 공통 상태 계약을 Claude가 먼저 정하고, 이후 UI 발주 |
| A1-02 | 두 번째 힌트·평가 회차 | 캡처 없음(시작 발화 순간); `LessonStateMachine.kt:320`, `ui/lesson/LessonPresentation.kt`의 `maneuverText` | 정적 확인: `${attempt}회차예요`를 TTS로 발화한다. UI 필터는 점수·감점·초 등만 막아 이 문장도 통과한다. 운전자 문장 횟수 금지와 충돌 | A | 상 | 두 번째도 “필요할 때만 말할게요.” / “조용히 볼게요.”로 발화. 회차 식별은 별도 메타데이터 정책 D1로 결정 |
| A1-03 | Maneuver·Done·Setup·Quiz | `lesson-locked.png`, `lesson-quiz-locked.png`, `lesson-setup.png`; `ManeuverScreen.kt:113`, `DoneScreen.kt:53`, `LessonPresentation.kt:31`, `QuizScreen.kt`의 `QuizNumber` | 관찰: 잠금에도 회차·속도·가이드 단계, 퀴즈 번호가 남는다. Setup은 장롱 연차, Done은 회차를 표시한다. 계기 값·가이드 단계는 라운드 4, 퀴즈 번호는 라운드 9에서 유지하도록 명시했다. 모든 숫자를 점수 위반으로 셀 수 없으나 AGENTS의 넓은 숫자 금지와 경계가 불명확 | D | 중 | D1에서 “운전자 문장”과 계기·문항·회차 메타데이터를 구분한 허용표 확정. 선택 화면 거리·시간, 상세·진단서 수치는 허용 유지 |
| A1-04 | Quiz·QuizDone·점검 Done/Report·궤적 | `lesson-quiz-answered.png`, `lesson-quiz-done-results.png`, `lesson-done-checklist-bad.png`, `lesson-report-checklist-bad.png`, `lesson-done.png`; `QuizScreen.kt:64`, `QuizDoneScreen.kt:44`, `DoneScreen.kt:36`, `ReportScreen.kt:157`, `EstimatedPath.kt:63` | 관찰·정적 확인: 오답, 실패 ✗, 급조작 점이 Signal이다. “나빠진 것은 회색”과 충돌하지만 라운드 7의 오답·라운드 9의 ✗는 명시 발주 사항이다 | D | 상 | D2에서 실패는 회색으로 통일할지, 정차 결과에 빨강 예외를 둘지 비교. 기존 승인 내용을 무단으로 되돌리지 않기 |
| A1-05 | AI 패널 | `lesson-panel-open.png`, `lesson-panel-ai-code.png`; `DemoPanel.kt:78`, `:80`; AI 패널 발주 §2 | 정적 확인: 32sp 미만 활성 사용처는 **28sp 두 곳**이다. Code 주소의 직접 Text 1곳, 그 외 네 상태의 공통 상세 LessonText 1곳. AI 패널 발주 자체가 28sp를 지정했다 | D | 상 | D3: 32sp로 올리기 / 시연 패널만 28sp 예외를 문서화하기. 라운드 10의 Code 스타일·주소 수정과 중복 작업 금지 |
| A1-06 | 긴 Done·Report 문장 | 캡처 없음(최장 입력); `LessonComponents.kt:80`, `DoneScreen.kt:55`, `ReportScreen.kt:45` | 검증 공백·정적 확인: 공통 Headline은 72→64→56sp로 줄여도 최대 3줄이다. 4줄 이상의 명시 줄바꿈 또는 좁은 영역의 긴 멘트는 마지막 크기에서도 전부 보인다는 보장이 없다. Maneuver 4줄 통과를 결과 화면에 일반화할 수 없다 | B | 중 | 긴 메인 문장용 레이아웃을 분리해 4줄을 모두 보장하고, 실제 Fake 최장 멘트·Cloud 길이 상한의 결과 화면을 캡처 |
| A1-07 | Cloud 멘트·총평 / Report 예외 폴백 | 캡처 없음; `ports/CloudCoachPort.kt`의 `CoachPrompts.validate`·`twoLines`, `LessonStateMachine.kt`의 `toReport` 예외 분기 | 정적 확인: validate는 길이·줄 수·금지어만 검사하며 숫자·정확히 두 문장을 강제하지 않는다. toReport 예외 문장에도 `${attempts.size}회`가 있다. 실제 Cloud가 위반했다는 실측은 없음 | A | 상 | “두 문장·숫자 없음”의 프롬프트와 검증 계약을 맞추고 예외 폴백도 같은 규칙 적용. 실패 시 시드에도 같은 품질 기준 적용(A3) |

불변별 통과 근거와 한계:

| 검사 | 결과·근거 |
|---|---|
| Maneuver 점수·감점·N점 | **기존 PNG에서 위반 0**. `ManeuverDisplayState`에 점수 필드가 없고 `maneuverText`가 점수 문자열을 배제한다. 회차·계기 숫자는 A1-03으로 별도 기록. 접근성 트리를 이번에 새로 읽지는 않았으며 기존 `contract-round9-1/2/3.txt`의 계약 통과만 참고 |
| 잠금 클릭·스크롤 / 눈에 보이는 터치 유도 | `lesson-locked.png`, `lesson-quiz-locked.png`에서 앱 영역의 버튼·밑줄·화살표·시연 알약 없음. `ManeuverScreen`의 `!locked`, `QuizScreen`의 분기로 제거. OS 상하단 바는 앱 밖이다. 다른 단계의 재출발은 A1-01 |
| 정차 중에만 다 됐어요 | `ManeuverScreen`의 `!locked && stopped`, `LessonModels.kt`의 `speedKmh < STOP_SPEED_KMH(1)` 확인. 이동 캡처에 완료 버튼 없음. 1.0 경계의 새 실측은 하지 않음 |
| 자막 4줄 | `lesson-missing.png`에 네 줄 모두 보임. Maneuver는 줄바꿈 셋 이상인 spoken을 무제한 LessonText로 분리한다. Done·Report 긴 메인은 A1-06 |
| 배지·다시 시작·예시 고지 | `lesson-report.png`, `lesson-certificate.png`와 `ReportScreen`에서 확인. 요약·진단서에 다시 시작, 상세에는 돌아가기를 통해 접근. 상세를 포함한 모든 탭에 다시 시작이 있어야 한다는 뜻인지는 기준 문서 정리 시 명시 |
| 신호 세 상태 | `lesson-mixed.png`: 조향 실신호 / 기어 시뮬레이션 / 뒤 거리 미측정. `signalLabel`과 `StateLabel`로 구분. `lesson-maneuver-checklist-missing.png`의 브레이크·시동 혼합 출처도 분리 |
| 팔레트 | `ui/`의 `Color(0x` **5곳 모두 CoachStyle.kt:8~12**, 토큰 밖 색 리터럴 0. Muted는 Ink 60%, 다른 알파·합성·Color.Transparent는 새 토큰이 아님. PNG의 앱 영역도 같은 색면 계열; 안티앨리어싱·일러스트 가장자리·OS 바의 색을 새 토큰 위반으로 세지 않음. 대비는 A5 |
| Bold·기본 폰트 | `FontWeight.Bold` 0, 활성 Lesson 화면은 Normal/Medium만 사용. 커스텀 Font/serif·TTF/OTF 파일 없음. `MainActivity`는 MaterialTheme → DesignScale → LessonRoute. 미사용 `DashboardScreen.kt:76`의 24sp, `:89`의 SemiBold는 현재 Lesson 경로에 연결되지 않은 잔재이며 현재 캡처 위반으로 세지 않음 |
| 32sp 하한 | 활성 경로는 A1-05의 두 28sp 위치만 예외 후보. BrandMark·Eyebrow 32sp, Report 출처 32sp, 궤적 캡션 32sp. **눈썹·출처 칸에 별도 28sp 사용 없음** |
| DesignScale | `ui/concepts/DesignScale.kt`의 2560×1268 기준·fontScale 1 확인. 정확히 이 종횡비인 창에서 설계 dp와 일치하며 다른 비율은 남는 축이 늘어난다. 기기 물리 크기·운전석 시거리 적합성은 미확인 |
| 단계 데이터 계약 | 화면 인자에 LessonPhase 직접 전달 없음. `LessonRoute`만 상태를 분기하고 Maneuver는 `toDisplayState()`를 받음 |
| PLANNED·미지원 모드 | `lesson-setup-sheet-driving.png`, `-driving-end.png`, `lesson-setup-knowledge.png`; `TaskBay`의 disabled·회색·준비 중, `TaskSheet`의 `filter(task::supports)` 확인. 선택된 준비 중 카테고리 메뉴는 Signal(승인 예외), 개별 과제 칸은 회색 |

라벨 13개를 `res/values/strings.xml`과 호출부·시나리오 제목에 대조했다. **문자열 변경 0**이며 시연은 눈에 보이는 글자 대신 같은 contentDescription이다.

| 리소스 | 정확한 라벨 | 리소스 | 정확한 라벨 |
|---|---|---|---|
| lesson_start | 시작 | lesson_finish | 다 됐어요 |
| lesson_again | 한 번 더 | lesson_end | 오늘은 여기까지 |
| lesson_restart | 다시 시작 | demo_toggle | 시연 |
| demo_good | 잘한 주차 | demo_bad | 못한 주차 |
| demo_stop_car | 정차 | demo_resume_car | 출발 |
| demo_open_door | 문 열기 | demo_close_door | 문 닫기 |
| demo_stop_scenario | 시나리오 정지 | — | — |

모드 `가이드 / 힌트 / 평가 / 지식 테스트`도 `LessonModels.kt:49`와 동일하다. 점검 시나리오는 별도 제목 `잘한 점검 / 못한 점검`을 사용하며 주차 라벨을 바꾸지 않는다.

## A2. 화면 간 일관성

| 번호 | 화면 | 근거(캡처 파일명·코드 위치) | 발견 | 분류 | 심각도 | 제안 |
|---|---|---|---|---|---|---|
| A2-01 | Setup·Briefing·Maneuver·Done·Report·Quiz | 각 대표 PNG; `BrandMark`, 각 Screen의 padding | 브랜드는 대체로 좌 180·상 64dp인데 Done은 오른쪽 본문 좌 120·상 96dp. Quiz 본문은 상 64, Maneuver·Report 본문은 상 96. 동일 로고를 기준점으로 읽으면 위치가 달라짐 | D | 하 | D4 비교용 규칙안: “브랜드는 포스터 패널 좌상단 180/64dp, 본문 단계 눈썹은 본문 좌단/상단 96dp”; 현재 내용 중심 배치와 비교 |
| A2-02 | 준비·주차·결과 패널 | `lesson-briefing.png`, `lesson-locked.png`, Done 세 유형, Report; 각 Screen의 weight | Setup/Maneuver 53:47, 시트 30:70, Briefing 57:43, Done 궤적·점검/Report/Quiz 38:62, 빈 Done 12:88. 역할에 따른 폭 차이는 설명 가능하며 무조건 50:50 통일할 근거 없음 | D | 하 | “정보가 있는 결과 패널은 38%, 장식만 남으면 12%, 연습 중 도식은 53%”를 유지할지 D4에서 비교 |
| A2-03 | 과제·점검·예약·퀴즈 카드 | `lesson-setup-sheet.png`, `lesson-maneuver-checklist.png`, `lesson-venues.png`, `lesson-quiz.png`; TaskBay/ChecklistValue/VenueCard/SelectionChip | 카드 높이 432(면은 하단 32 제외)/240/220/min120, 칩 96dp. 모두 각진 면이고 주 행동만 pill. 텍스트 양·역할 차이가 있어 현재 크기 차이 자체는 오류 아님 | D | 하 | 규칙안: “선택·상태 카드는 직각, 실행 버튼만 pill; 높이는 정보량에 따라 고정하고 같은 역할끼리만 통일” |
| A2-04 | Briefing·Setup·Done 자막 | `lesson-briefing.png`, `lesson-setup.png`, `lesson-done.png`; BriefingScreen·SpeechFooter·Headline | Briefing만 좌하단 음성 안내 중, Setup은 하단 구분선 자막, Maneuver는 메인 문장, Done은 중복 자막 생략. 다만 Briefing 인자 subtitle은 사용하지 않고 원문은 접근성 설명에만 있음 | D | 중 | “준비는 음성 진행 표시, 조작 중은 메인 문장, 결과는 동일 문장 중복 금지”로 문서화하되, 무음 시연에서 Briefing 원문 자막을 보여 줄지 D4 비교 |
| A2-05 | Report·QuizDone의 다시 시작 | `lesson-report.png`, `lesson-quiz-done.png`; `PrimaryPill`·각 호출 | 주요 운전자 액션 네 곳은 140dp/최소720dp, Report·QuizDone 재시작은 기본 min120dp. 라운드 4는 네 액션만 140dp로 지정했으므로 일괄 위반 아님 | D | 중 | D5에서 정차 결과의 재시작도 140dp로 통일할지 결정. 보조 이동은 밑줄 TextAction 유지 |
| A2-06 | 점검 상세 | `lesson-report-checklist-bad.png`, `-missing.png`; `ChecklistPresentation.kt:57`~`:65`의 checklistResults | 관찰: ✗ 옆 설명이 “시동 전 닫힘”, “시동 2초 · 브레이크 밟고 시동”처럼 성공 사실로 읽힌다. 미측정 행에도 같은 성공형 설명이 남음. 기호와 문장이 충돌 | B | 상 | “결과 설명은 결과와 같은 시제·상태를 쓴다”: 실패는 “시동 때 문 열림 / 브레이크 확인 안 됨”, null은 “순서를 확인할 수 없어요”. 의도한 기준 안내라면 “확인 기준”을 분리 |
| A2-07 | 신호 출처 명칭 | `lesson-mixed.png`, `lesson-report.png`; signalLabel·badgeText·collapsedSignalLabel, 디자인 브리프 | 현재 UI는 실신호/시뮬레이션/미측정으로 일관됨. 브리프의 “시뮬”과 옛 덱의 칩 설명이 낡음. 체크 표 ✓/✗는 출처가 아니라 완료 여부 | A | 중 | 문서 규칙: “출처는 실신호·시뮬레이션·미측정, 결과는 확인·아직·미측정”; 두 축을 동의어로 합치지 않기 |
| A2-08 | 모션·코치 눈썹 | 모핑 스트립, `lesson-maneuver-guides.png`; SetupScreen tween(400), PosterSurface tween(260), 라운드 4·8 발주 | 브리프 ≤300ms와 조수석 눈썹은 이후 승인된 400ms 모핑·코치와 다름. 전환 코드 오류로 판단할 수 없음 | A | 중 | Claude가 브리프의 이력·예외를 정리. 모핑을 임의로 300ms로 줄이거나 코치 이름을 되돌리지 않기 |

## A3. 문구·톤 전수

읽은 범위: `SeedCatalog.kt`의 과제 설명 전체·주차 가이드 6단계·점검 가이드 7단계(벨트 문장 재사용)·모든 RemarkTemplate·퀴즈 5문항과 해설·예약/혜택 시드, `strings.xml` 전체, `HintRules` 전체, `CoachPort.kt`의 AdviceRules/FakeCoachPort, Cloud 프롬프트·검증, 상태기계의 브리핑·회차·퀴즈 발화, UI 예약 문장과 출처 매퍼. 저장된 이미지만으로 TTS 발음이 검증됐다고 하지 않는다. 아래 TTS 어색함은 문장 읽기에 근거한 검토다.

| 번호 | 화면 | 근거(캡처 파일명·코드 위치) | 발견 | 분류 | 심각도 | 제안 |
|---|---|---|---|---|---|---|
| A3-01 | 주차 가이드 | `lesson-maneuver-guides.png`; `SeedCatalog.kt:81`~`:86` | “매 주세요/돌리세요/후진하세요/기어 P.”가 섞이고 P·R·45도는 TTS에서 해석 부담. 45도는 조작 값으로서 횟수·점수와 다른 성격 | B | 중 | “핸들을 오른쪽 끝까지 돌려 주세요.” / “천천히 후진해요.” / “다 들어왔다고 판단되면 멈추고 주차 기어에 놓아 주세요.”로 존댓말·기어 설명 통일; 각도 안내 사실·판정식은 유지 |
| A3-02 | 점검 가이드 | `lesson-maneuver-checklist-pending.png`; `SeedCatalog.kt:94`~`:101` | “P 맞아요 / 이제 오른쪽 / 비상등 확인”의 어미 생략. 점검 전 과정을 같은 말투로 듣기 어려움 | B | 중 | “주차 기어를 확인했어요.” / “왼쪽을 확인했어요. 이제 오른쪽도 켜 봐요.” / “비상등을 확인했어요. 이제 끄고 버튼을 눌러 주세요.”로 내용만 다듬기 |
| A3-03 | 주차·점검 Done | `lesson-done.png`는 어미 혼용 예; `SeedCatalog.kt:129`~`:168`, `ports/CoachPort.kt`의 remark | 시드 서두 자체가 여러 문장이라 조언을 붙이면 세 문장 이상. “들어가긴 했어요 / 덜 헤맸어요 / 첫 회차는 원래 이래요”는 평가·책망으로 들릴 수 있고 “열었습니다”가 해요체와 섞임 | B | 중 | 모든 서두를 한 문장 해요체로: “다시 연습을 시작했어요.” / “핸들 움직임이 전보다 차분해졌어요.” / “처음부터 끝까지 잘 마쳤어요.”; 조언과 합쳐 두 문장으로 검수 |
| A3-04 | Done의 사실성·숫자 | 해당 멘트별 캡처 없음; `SeedCatalog.kt:130`, `:132`, `:140`, `:142`, `:149`, `:153`, `:159`, `:162`~`:165` | “마트 주차장은 됩니다 / 한 번에 들어갔어요 / 안전했어요 / 부딪히지 않았어요 / 제일 안전한 사람 / 다 켜졌어요”는 skill 밴드만으로 보장 못 하는 사실. “세 번만 반복”·“순서가 한 번”은 횟수 혼입 | B | 상 | 근거를 넘지 않는 서두로 교체: “오늘의 연습을 끝까지 마쳤어요.”, “출발 준비를 차근차근 익히고 있어요.”. 횟수 조언은 “이 순서를 다시 연습해 봐요.”. 태그·밴드·선택 구조는 유지 |
| A3-05 | 총평·고정 조언 | `session-report.png`; `CoachPort.kt`의 summarize·Advice enum | “안전 쪽은 걱정할 게 없어요”는 미측정이 있어도 skill 최고 회차의 safety만 보고 나옴. “뒤로 갈수록 힘이 빠졌지만”은 수치 하락을 피로로 단정. Advice 일부는 이미 두 문장이라 서두와 합치면 세 문장 | A | 상 | 총평은 “이번에 측정한 항목을 함께 돌아봐요.”처럼 관측 범위 한정, 피로 추정 제거. 포트 소유자가 Advice를 한 문장으로 정리하고 숫자·문장 수 정책 적용 |
| A3-06 | 힌트 | 별도 급가속 캡처 없음; `feature/lesson/HintRules.kt`의 harsh 분기 | `metrics.harshEvents.size` 증가를 모두 “제동이 급했어요”로 말함. 급가속도 같은 배열에 들어가므로 발화 의미 불일치 가능. 긴급 “멈추세요”는 책망이 아니라 즉시 행동 안내라 완곡하게 바꿀 대상 아님 | A | 상 | 사건 종류에 맞춰 가속/제동 문장을 고르는 규칙 검토. 긴급 정지 문장과 타이밍은 유지 |
| A3-07 | 과제 설명·예약·제안 | `lesson-setup-reserved.png`, `lesson-reservation.png`; SeedCatalog tasks/courses, LessonPresentation.setupProposal | “해볼까요/풀어 볼까요” 띄어쓰기 혼용, 과제 설명은 →·45°·3지선다로 짧게 쓰여 선택 후 본문에도 노출. 예약의 ··시간은 정차 선택 정보로 허용되고 TTS에 쓰이지 않음 | B | 중 | 제안은 “해 볼까요 / 익혀 볼까요”로 통일. 설명은 “핸들을 돌리고 천천히 후진하며 방향을 맞춰요.”, “정차 중에 문제를 풀고 이유를 함께 살펴봐요.”처럼 문장화. 예약 시각·거리·고지·코스명은 보존 |
| A3-08 | 퀴즈 해설 | `lesson-quiz-answered.png`; `SeedCatalog.kt:213`~`:217` | 대체로 해요체이나 “차선 변경엔 방향지시등, 터널엔 전조등 / 하향등”은 종결 생략. “눈을 멀게”는 공포·과장 표현, `20%`, `60 m`, 괄호·영문 단위는 음성 읽기 정책이 없음 | B | 중 | “차선을 바꿀 때는 방향지시등을 켜요.”, “상향등은 마주 오는 운전자의 시야를 방해할 수 있어요.”처럼 수정. 교육용 수치의 존치·내용 검수는 D1 결정 후 원래 뜻을 보존 |
| A3-09 | Quiz 결과·문항 읽기 | `lesson-quiz-done.png`; `LessonStateMachine.kt:174`의 quizRemark·speakQuestion | 실제 생성 문장은 “N문제 중 N개”, “틀린 게 더 많지만…”이며 PNG 일부는 별도 테스트 문장. 정차 교육 결과의 수치 예외가 현재 문서에 없음 | D | 중 | D1에서 교육 문제·결과 수치 범위 확정. 책망형 결과는 “이유를 함께 살펴보면 다음 연습에 도움이 돼요.”로 바꾸는 Claude 후속안 |
| A3-10 | AI 시연 패널 | `lesson-panel-open.png`; aiLine·DemoPanel | 파일명·GitHub·Authorize·Copilot은 운전자 안내가 아닌 시연 담당자의 인증 안내. 기술 단어 제거 시 오히려 연결 절차가 불명확해짐. Code 주소 https 생략은 이미 라운드 10 발주 | A | 중 | 기술 안내는 시연 도구에만 유지하고 운전자 멘트로 재사용하지 않기. 주소·직접 Text 수정은 라운드 10 완료 후 문서로 확인 |

가이드의 일반 요청형 “~해 주세요” 자체는 금지어가 아니다. 안전벨트·도어·긴급 정지처럼 행동이 분명해야 할 곳은 짧게 유지한다. 명확한 오탈자보다 어미·띄어쓰기·사실 단정·문장 수 불일치가 주된 발견이다.

## A4. 화면 × 상태 커버리지

○ 저장된 PNG 있음 / △ 비슷한 상태만 있음 / — 해당 상태의 PNG 없음 / N/A 현 모델의 정상 흐름에 없음. 캡처 출처는 전부 사외 Fake/계측 데이터이며 `LIVE` 표기가 보여도 사내 실측 화면이라는 뜻이 아니다.

| 화면 | 상태 | 근거 PNG / 코드 | 판정·빈 칸 |
|---|---|---|---|
| Setup | 기본 제안·예약 없음 | lesson-setup.png | ○ (라운드 8) |
| Setup | 예약 있음·취소 후 없음 | lesson-setup-reserved.png / 라운드 9 README의 취소 계약 | ○ 예약 / △ 취소 후 단독 PNG 없음 |
| Setup 시트 | 주차·주행·점검·지식 / 준비 중 / READY 비선택 | lesson-setup-sheet.png, -driving.png, -driving-end.png, -checklist.png, lesson-setup-knowledge.png, -ready-contract.png | ○ READY 비선택은 평행주차를 READY로 복사한 테스트 데이터; 제품의 구현 완료 과제로 제시 금지 |
| Briefing | 주차·점검·지식 | lesson-briefing.png / LessonStateMachine.begin | ○ 주차 / — 점검 / — 지식. 세 과제 모두 Briefing을 거치고 지식은 대기 후 Quiz로 전환 |
| Maneuver 주차 | 가이드·힌트·평가 | lesson-maneuver*.png는 주로 가이드; lesson-mixed.png | ○ 가이드 / — 힌트 단독 PNG / — 평가. flow 로그 존재가 상태별 캡처를 대신하지 않음 |
| Maneuver 주차 | 잠금·전체 누락·혼합 | lesson-locked.png, lesson-missing.png, lesson-mixed.png | ○ 세 경우. locked PNG는 20km/h, 5.1 경계는 기존 계측 기록만 |
| Maneuver 점검 | 완료·대기·실패·미측정 | lesson-maneuver-checklist.png, -pending.png, -bad.png, -missing.png | ○ 일곱 칩. — 점검 자체 잠금 PNG·실신호/시뮬 혼합 완료 칩 |
| Done | 주차 궤적·없음·도착 중 | lesson-done.png, -path.png, -no-path.png, -arrival-empty.png | ○ 라운드 8. — 라운드 9 이후 주차 Done 캡처 |
| Done | 점검 완료·실패·미측정 | lesson-done-checklist.png, -bad.png, -missing.png | ○ 라운드 9 |
| Done·Report·QuizDone | 결과 이후 다시 움직임 | 상태기계 onDelta / toReport / finishQuiz | — A1-01. 코드상 미보호 가능성이 있어 Claude 계약 선행 필요 |
| Report | 주차 감점 상세·요약 | lesson-details.png, lesson-report.png | ○ 상세 60/55는 테스트 데이터; 요약 PNG는 미측정 포함 fixture |
| Report | 만점 상세·100회·복수 회차 | lesson-report-checklist.png / lesson-report-100.png / session-report.png | ○ 점검 만점 상세 / ○ **100회** 표시 / △ 복수 회차 요약은 구버전. **lesson-report-100은 점수 100 캡처가 아님**; 주차 100/100 상세 PNG는 없음 |
| Report | 미측정 다수 | lesson-report-checklist-missing.png | ○ 시뮬 8·미측정 4의 테스트 데이터 / — 주차 전체 누락 요약·긴 미확인 단계 목록 |
| Report | 실신호 7·시뮬 1·미측정 0 / 실신호+미측정 | INTEGRATION B 10/1은 숫자 메모뿐; ReportProvenance | — 둘 다 없음. 배지·칩 데이터 구조는 지원하지만 사내 숫자를 재현한 외부 fixture 디자인 검증은 없음 |
| Quiz | 풀이·오답 공개·잠금·결과 | lesson-quiz.png, -answered.png, -locked.png, -done.png, -done-results.png | ○ / — 정답 선택 시 “내 답 · 정답” 전용 PNG, 긴 문항·해설, 잠금 중 교육 수치 문항 |
| AI 패널 | NoConfig·NeedsLogin·Code·Ready·Error | lesson-panel-open.png(NeedsLogin), lesson-panel-ai-code.png(Code); aiLine | — / ○ / ○ / — / —. Code는 공개용 고정 테스트 코드이며 실제 인증 정보 아님 |
| AI 패널 | null·최장 오류·Done 위 펼침 | DemoPanel·aiLine | — null은 노드 없음 계약, 최장 80자 오류·결과 화면 위 오버레이의 시각 자료 없음 |

| 번호 | 화면 | 근거(캡처 파일명·코드 위치) | 발견 | 분류 | 심각도 | 제안 |
|---|---|---|---|---|---|---|
| A4-01 | 위 매트릭스의 정상 가능 상태 | 빈 칸 및 해당 Screen·aiLine | 현재 코드가 지원해도 제출 전 시각적으로 확인되지 않은 상태가 남음 | B | 중 | 라운드 11 캡처 묶음: 점검/지식 Briefing, 주차 힌트/평가, 점검 잠금·혼합, 주차 Done, 주차 만점/복수회차 상세, 7/1/0·실신호+미측정 Report, 정답/긴 퀴즈, AI NoConfig/Ready/Error/null·최장 오류 |
| A4-02 | Report 미측정 다수·장문 출처 | ReportScreen의 ReportProvenance는 스크롤 밖 고정, 긴 목록을 무제한 LessonText로 표시 | 검증 공백: 미측정 키·미확인 가이드가 동시에 많을 때 본문과 고정 하단이 경쟁. 현재 네 누락 PNG만으로 최대치에서 버튼·본문이 보인다고 할 수 없음 | B | 중 | 배지와 돌아가기는 남기고 긴 설명은 본문 스크롤로 보내는 레이아웃 검토; 모든 누락 키·모든 미확인 단계 fixture 캡처로 먼저 판단 |

## A5. 대비와 운전석 맥락

`CoachStyle.kt`의 sRGB 값으로 계산했다. 알파는 **실제 바탕에 먼저 합성**(`out = α×fg + (1−α)×bg`), 채널을 선형화(`c ≤ 0.04045 → c/12.92`, 그 외 `((c+0.055)/1.055)^2.4`)한 뒤 `L = 0.2126R + 0.7152G + 0.0722B`, `(L밝음+0.05)/(L어두움+0.05)`을 적용했다. 표는 소수 둘째 자리 표시이고 판단은 반올림 전 값이다. [W3C 상대 휘도 정의](https://www.w3.org/WAI/WCAG22/Understanding/relative-luminance.html)를 따랐다.

이번 비교 목표는 발주대로 **4.5:1**이다. WCAG는 큰 글자에 3:1, 비활성 UI 등에 예외를 둔다. 따라서 4.5 미달을 모두 AA 위반이라고 단정하지 않는다. 차량 디스플레이 물리 크기·시거리·주광 환경과 DesignScale의 변환이 미확인이라 Android sp를 웹의 큰 글자 기준과 바로 등치하지 않는다. [W3C 대비 기준 설명](https://www.w3.org/WAI/WCAG22/Understanding/contrast-minimum.html).

| 조합(전경/바탕, 불투명은 역방향도 동일) | 대비비 | 실제 사용·4.5 기준 |
|---|---:|---|
| Ink / Paper | 19.05 | 본문·제목, 충족 |
| Periwinkle / Paper | 5.63 | 출처 배지·눈썹; 반전된 선택 칩의 Paper도 동일, 충족 |
| Paper 60% / Ink | 7.05 | 조향 풀이·Done 미측정·Report 회 단위, 충족 |
| Muted(=Ink 60%) / Paper | 5.20 | 부제·상세 수치·궤적 캡션, 충족 |
| Muted / Lavender | 4.88 | 미측정 점검 칩·예약 보조문·AI 패널 상세, 충족 |
| Signal / Paper | **3.82** | Quiz “내 답”·QuizDone 오답·Report ✗·선택 카테고리; 반전된 주 버튼 Paper 글자도 3.82, 미달 |
| Periwinkle / Ink | **3.39** | 점검 Done ✓, 미달; 도식의 선도 이 조합이지만 글자와 별도 평가 |
| Ink / Lavender | 15.76 | 일반 카드·칩, 충족 |
| Ink / Signal | 4.99 | 현 주 버튼은 이 조합이 아니라 Paper/Signal; 기존 토큰으로 비교할 수 있는 후보 |
| Paper / Lavender | **1.21** | 활성 글자 조합으로 확인되지 않음; 얇은 구분선·장식 차이 |
| Periwinkle / Lavender | 4.65 | 예약 확인 눈썹·선택 윤곽, 충족 |
| Periwinkle / Signal | **1.47** | 활성 글자 조합으로 확인되지 않음 |
| Signal / Lavender | **3.16** | Quiz 오답 테두리(비문자); 이 바탕 위 Signal 글자 조합은 현재 오답 눈썹이 바깥 Paper에 있어 해당하지 않음 |
| Periwinkle 60% / Paper | **2.52** | 닫힌 “주행” 카테고리와 “준비 중” 글자. 카테고리는 눌러 목록을 볼 수 있어 비활성 버튼 예외로 간주할 수 없음 |
| Ink / (Lavender 40% over Paper) | 17.69 | 계획 카드 바탕 합성값 RGB 242.8/243.2/246; 참고 조합 |
| Muted / (Lavender 40% over Paper) | 5.07 | 실제 준비 중 TaskBay 글자, 충족 |
| Periwinkle / (Lavender 40% over Paper) | 5.22 | 참고 조합, 현 준비 중 글자는 Muted |
| Signal / (Lavender 40% over Paper) | **3.55** | 참고 조합, 현 준비 중 카드 글자로 사용 안 함 |
| Paper / (Ink 60% over Lavender) | 5.90 | 점검 대기 칩, 충족 |

| 번호 | 화면 | 근거(캡처 파일명·코드 위치) | 발견 | 분류 | 심각도 | 제안 |
|---|---|---|---|---|---|---|
| A5-01 | 과제 시트 닫힌 주행 메뉴 | `lesson-setup-sheet.png`; TaskSheet.CategoryChoice의 ready=false | 2.52:1의 클릭 가능한 메뉴 글자. 준비 중 카드의 미측정색과도 다름 | B | 상 | “준비 중 카테고리도 탐색 가능한 제목은 최소 4.5:1” 규칙으로 Muted(5.20) 등 기존 토큰 사용; 펼친 Signal은 D6과 함께 결정 |
| A5-02 | 주 버튼·오답·점검 Done | PrimaryPill, QuizDoneScreen, DoneScreen; 해당 PNG | 3.82 또는 3.39:1. 글자 크기 예외 가능성과 운전석 가독성 목표 사이의 결정 필요 | D | 중 | D6: 현재 팔레트/큰 글자 유지와 4.5 이상 텍스트 조합을 나란히 캡처. 새 토큰을 임의 추가하지 않기 |
| A5-03 | 시연 알약·보조 링크 | `lesson-demo-toggle-pill.png`, `lesson-panel-open.png`; DemoPanel:44, LessonComponents.TextAction | 알약 시각 크기 64×32dp가 가장 작다. TextAction은 글자 폭+상하16dp만 명시하고 최소 가로 hit 영역이 없다. 실제 Compose 최소 터치 확장·물리 mm는 이번 정적 감사로 확정할 수 없음 | D | 중 | D5: 알약 외형 유지+터치영역만 확대 / 외형도 확대를 비교. “제휴 시험장”(32sp)·“취소” 링크를 정차 상태에서 운전석 실측 대상으로 지정 |

칩 96dp, 예약 카드 220dp, 주 버튼 140dp는 위 알약보다 충분히 큰 설계 영역이다. 이 수치만으로 실차 조작 적합 판정을 내리지는 않는다. 잠금 Maneuver/Quiz의 **보이는 터치 유도 잔재는 기존 캡처에서 0**이며, 다른 화면 재출발은 A1-01의 계약 문제다.

## A6. 시연 영상 컷별 영향

원본 [02_video_shotlist.md](../presentation/02_video_shotlist.md)는 라운드 3 설명과 라운드 8 녹화 메모가 섞여 있다. 지정 `build/demo-round8b.mp4`는 이 워크트리와 원래 프로젝트의 같은 경로 모두 없었다. 따라서 **영상 실물과 비교했다는 주장은 하지 않는다**. 저장된 두 MP4는 조향·궤적 짧은 클립이며 본편을 대신하지 않는다. 아래는 컷 목록·53장·기준 코드의 비교다.

| 번호 | 화면 | 근거(캡처 파일명·코드 위치) | 발견 | 분류 | 심각도 | 제안 |
|---|---|---|---|---|---|---|
| A6-01 | 컷 0·12 제목/마무리 | 제품명 strings.xml, 컷 목록 | 가칭 대신 드라이브 코치 사용 필요; 숫자 성능 주장 없는 “과정을 본다”는 유지 가능 | A | 중 | 편집 카드 문구 교체. 새 앱 녹화 자체는 불필요 |
| A6-02 | 컷 1·2 | lesson-setup-sheet.png, lesson-briefing.png | 점검 명칭·카드·브리핑 전체 항목은 라운드 9에서 바뀜. AI 패널이 열린 장면도 추가됨 | A | 중 | **1·2 재녹화**. 라운드 10 조사·주소 완료 뒤 녹화. AI 설명은 컷 1의 보충 장면으로 분리 |
| A6-03 | 컷 3·4·5·6 | lesson-mixed.png, ManeuverScreen·HintRules; 컷 6의 “붉은 막대” | 출처 중복 제거·잠금 눈썹 변경. 현 UI는 거리 값·가까워요이며 옛 붉은 막대 설명과 다름. 뒤 거리는 Fake 전용이라 사내 실거리로 소개 불가 | A | 상 | **3~6 재녹화** 권고. 시나리오 선택과 출처를 남기고 근접 자막에서 거리 시뮬레이션 명시 |
| A6-04 | 컷 7·8·9 | lesson-done-path.png(라운드 8); A1-02·A3 | 주차 궤적은 라운드 9 변경 없음. 컷 8의 “2회차예요”는 현재 코드에도 있는 숫자 발화 문제. 새 점검 Done 패널은 이 주차 컷 목록에 없는 확장 장면 | A | 상 | **8 재녹화**, A1-02 해결 뒤 연결상 **7·9도 같은 빌드로 촬영**. 점검 Done은 컷 7 보충 삽입으로 명확히 구분 |
| A6-05 | 컷 10·11 | lesson-report.png, lesson-details.png, lesson-certificate.png | “게이지/연습 기록/총평 세 문장”은 낡음. 현재는 연습한 회차·회, 상세 수치, 두 문장 의도. 7/1/0 사내 배지는 외부 화면에 합성할 수 없음 | A | 상 | **10·11 재녹화**. 컷 10에 요약→자세히 보기, 실제 출처 배지 그대로 유지; 진단서 예시 고지 유지 |
| A6-06 | 목록 밖 퀴즈 잠금·점검·AI | lesson-quiz-locked.png, lesson-done-checklist.png, lesson-panel-open.png | 현재 0~12 주차 컷에는 세 기능을 보여 줄 자리가 없음 | A | 중 | 기존 번호 유지 후 1-AI / 7-점검 / 11-퀴즈 보충 컷으로 검토. 본편 길이·주최 자막 허용은 사내에서 결정. 새 자막은 03 B3 |

필수 변화가 있는 원 컷은 **1·2·3~6·8·10·11**, 동일 빌드 연속성 때문에 7·9도 함께 다시 찍는 편이 낫다. 정지 캡처가 필요한 빈 상태는 A4-01의 B 요청이고, 영상 녹화·컷 목록 문서 갱신은 모두 Claude의 A 요청이다.

## 라운드 11 발주서 초안 — B 항목만

기준: 라운드 10 리뷰·머지 후 새 발주에서 베이스를 지정한다. **이 감사 PR에서는 실행하지 않는다.** A·D 항목을 묵시적으로 UI 작업에 넣지 않는다. D 결정·A 계약 변경이 오면 별도 발주에 추가한다. 코드·테스트·캡처는 다음 UI PR의 허용 범위를 그때 확정한다.

| 원 항목 | 지금 | 바꿀 것 | 확인할 캡처(새 파일명은 제안) |
|---|---|---|---|
| A1-06 | Headline 3줄 끝에서 긴 멘트 검증 없음 | 긴 메인 문장도 네 줄을 보존하는 전용 배치; 짧은 제목 두/세 줄 규칙은 유지 | `lesson-done-long.png`, `lesson-report-long.png`; 최대 허용 입력·명시 4줄·주 버튼 함께 |
| A2-06 | 점검 ✗/미측정 옆에 성공형 설명 | 결과별 상세 설명 분기 또는 명시적인 확인 기준 열. 기록 데이터·채점은 불변 | `lesson-report-checklist-bad.png`, `-missing.png`에 기호와 문장 의미 일치 |
| A3-01·02 | 가이드 어미·기어 읽기 혼용 | 시드 내용만 해요체/짧은 요청형으로 통일; 확인 조건·단계 수 불변 | 주차 R/P 안내, 점검 좌/우·비상등, 긴 문장도 전부 노출 |
| A3-03·04 | 여러 문장 서두·책망·횟수·미관측 사실 단정 | 각 시드 서두 한 문장, 숫자·충돌 없음·주차 성공·안전 단정 제거. 구조·선택 로직 불변 | 밴드별 주차·점검 Done; 실제 FakeCoach 조언과 결합한 두 문장 확인 |
| A3-07 | 띄어쓰기·과제 설명의 기호 문장 | 해 볼까요/익혀 볼까요 통일, 설명 문장화. 시간·거리·주차 3종·예시 고지는 보존 | 기본 제안·수동 주차/지식 선택·예약 있음/취소 후 |
| A3-08 | 퀴즈 해설 종결 생략·공포 표현 | 해요체·시야 방해 표현으로 교정. 교육 수치·정답·문항 의미는 D1 확정 전 유지 | 해설 다섯 종류; 긴 보기/해설·결과 스크롤 |
| A4-01 | 정상 상태 PNG 공백 | 위 매트릭스 빈 칸을 외부 fixture로 캡처; 사내 실신호 검증으로 명명 금지 | 점검/지식 Briefing, 점검 잠금/혼합, 주차 힌트/평가/Done/만점·복수회차 상세, Report 7/1/0 및 실신호+미측정, Quiz 정답·장문, AI 남은 상태 |
| A4-02 | Report 출처 목록이 고정 하단을 확장 | 최대 누락·미확인 입력부터 재현하고 필요 시 긴 설명만 스크롤 본문으로 이동 | `lesson-report-all-missing.png`; 요약·상세에서 배지·돌아가기와 본문 동시 노출 |
| A5-01 | 탐색 가능한 주행 카테고리 2.52:1 | 기존 Muted 등 4.5 이상 글자색 사용, 준비 중 과제의 클릭 비활성 유지 | 주차가 열린 시트의 닫힌 주행 메뉴, 주행이 열린 시트의 준비 중 카드 |

수락 기준: 라벨 13개·모드 라벨 보존, 주행 잠금 점수/터치 0, 완료 버튼 정차 한정, 기존 의미 있는 계약 유지, 각 변경과 대응 캡처를 PR에 명시. 실행 횟수·빌드·에뮬 사용 계획은 다음 발주가 정하며 이번의 금지 조건을 소급해 완화하지 않는다.

## 사용자에게 캡처로 물을 것 — D 항목

지금 결정을 요청하거나 시안을 생성하는 절이 아니라 다음 발주용 비교 목록이다.

| 결정 | 안 가 | 안 나 | 비교에 쓸 화면 |
|---|---|---|---|
| D1 숫자 경계(A1-03·A3-09) | 운전자 문장 숫자 금지, 계기·가이드/문항 번호·회차/프로필 메타데이터·정차 교육 수치는 명시 예외 | 계기·필수 교육 정보만 유지하고 회차·연차·결과 수치는 상세로 이동 | 잠금 주차/퀴즈, Setup, Done, QuizDone. 버튼 “한 번 더”는 라벨 계약상 그대로 |
| D2 결과의 빨강(A1-04) | 실패/오답/급조작 점을 회색으로, 맞음만 강조 | 정차 결과에 Signal 예외 명문화, 주행 경고와 구별 | Quiz 오답, 점검 Done/Report, 급조작 궤적 |
| D3 글자 하한(A1-05) | AI 상세도 32sp, 줄바꿈·패널 세로 공간 조정 | 시연 패널 상세 28sp만 예외, 운전자 본문·눈썹·출처는 32sp 유지 | 같은 NeedsLogin·Code·80자 Error. 라운드 10 결과 위에서 비교 |
| D4 편집 규칙(A2-01~04) | 현재 역할별 패널 폭·눈썹·자막 배치 유지하고 문서화 | 브랜드/단계 눈썹 기준선 통일, 무음 Briefing에도 실제 안내문 노출 | Setup/Briefing/Maneuver/Done/Report 나란히; 패널 폭 변경 시 정보량 확인 |
| D5 정차 조작(A2-05·A5-03) | 결과 재시작도 140dp, 시연 알약 외형은 두고 hit 영역 확대 | 현 크기를 시연 전용 예외로 유지하거나 알약 외형까지 확대 | Report·QuizDone·시연 패널·취소/제휴 시험장 링크 |
| D6 대비(A5-02) | 기존 팔레트와 큰 글자 예외 유지, 실차에서 가독성 확인 | 새 토큰 없이 버튼 글자는 Ink/Signal, Done ✓는 Paper 계열 등 4.5 이상 조합 | 주 버튼·Done 점검 기호·Quiz 내 답·선택 메뉴; D2와 함께 결정 |

## 부록. 확인한 PNG 53장

아래 목록의 마지막 변경 커밋은 `git log -1 -- <파일>`로 확인했다. **라운드 9에서 교체·추가된 PNG는 32장**이며, 이전 라운드 파일이 같은 폴더에 남아 있다고 최신 UI 캡처로 표시하지 않는다. 목록의 결과는 A1~A6에 합쳐 기록했다.

| 묶음 | 파일(모두 docs/screenshots/lesson/) | 마지막 변경·읽을 때 주의 |
|---|---|---|
| 주차 구버전 6장 | `lesson-demo-toggle-pill.png`, `lesson-maneuver-b.png`, `lesson-maneuver-collapsed.png`, `lesson-maneuver.png`, `session-report.png`, `lesson-maneuver-guides.png` | 앞 다섯 `090ec40` 라운드 4, guides `cedb9bf` 라운드 7 |
| 조향 스트립 1장 | `lesson-round7-steering-strip.png` | `cedb9bf` 라운드 7 |
| 주차 Done·Setup 9장 | `16_done_2.png`, `lesson-done-arrival-empty.png`, `lesson-done-no-path.png`, `lesson-done-path-contract.png`, `lesson-done-path.png`, `lesson-done.png`, `lesson-round8-done-strip.png`, `lesson-setup.png`, `lesson-reservation.png` | 앞 여덟 `71a5753` 라운드 8; reservation은 `d66f31d` 예약 PR |
| 예약 이전 자료 3장 | `lesson-setup-reserved.png`, `lesson-venue-slots.png`, `lesson-venues.png` | `d66f31d` 예약 PR. B1의 최신 캡처 후보에서 제외 |
| 준비·누락·잠금 4장 | `lesson-briefing.png`, `lesson-locked.png`, `lesson-missing.png`, `lesson-mixed.png` | `8dc2392` 라운드 9 |
| 점검 연습 4장 | `lesson-maneuver-checklist.png`, `lesson-maneuver-checklist-bad.png`, `lesson-maneuver-checklist-missing.png`, `lesson-maneuver-checklist-pending.png` | `8dc2392` 라운드 9 |
| 점검 Done 3장 | `lesson-done-checklist.png`, `lesson-done-checklist-bad.png`, `lesson-done-checklist-missing.png` | `8dc2392` 라운드 9 |
| 리포트·상세·진단서 7장 | `lesson-certificate.png`, `lesson-details.png`, `lesson-report.png`, `lesson-report-100.png`, `lesson-report-checklist.png`, `lesson-report-checklist-bad.png`, `lesson-report-checklist-missing.png` | `8dc2392` 라운드 9; 100은 회차 레이아웃 fixture |
| 퀴즈 5장 | `lesson-quiz.png`, `lesson-quiz-answered.png`, `lesson-quiz-locked.png`, `lesson-quiz-done.png`, `lesson-quiz-done-results.png` | `8dc2392` 라운드 9 |
| 과제·모핑·예약 목록 9장 | `lesson-setup-knowledge.png`, `lesson-setup-morph-return-strip.png`, `lesson-setup-morph-strip.png`, `lesson-setup-sheet-checklist.png`, `lesson-setup-sheet-driving-end.png`, `lesson-setup-sheet-driving.png`, `lesson-setup-sheet-ready-contract.png`, `lesson-setup-sheet.png`, `lesson-venues-booked.png` | `8dc2392` 라운드 9 |
| AI 패널 2장 | `lesson-panel-ai-code.png`, `lesson-panel-open.png` | `6463120` AI 패널 PR #66, 라운드 10 전 |

검증: 위 파일 목록·코드 검색·색 대비 계산·문서 링크 확인. 빌드는 [03의 B4](../presentation/03_submission_onepager.md#b4-심사자-readme-리허설)에서 새 clone으로 **한 번만** 실행해 성공(단위 테스트 203개). 앱·계측 재실행은 하지 않았다. 발견한 항목은 이 문서와 후속 발주 초안에만 기록했다.
