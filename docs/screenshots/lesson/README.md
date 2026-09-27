# UI 라운드 5 검증 캡처

2026-09-28 · `codex/ui-round5` · 기준 `origin/main=1364e91` · CSTDe_API_34 2560×1440 · 기본 Fake, 배율 1.0. Done을 Maneuver와 같은 **뒤가 위** 관습으로 맞췄다. 라운드 4에서 확정한 알약 토글·`코치`는 유지한다.

- `lesson-done-path-contract.png`: 전후진·급정지·회전을 포함한 고정 입력. Ink 완료 차량이 위, Lavender 3dp 시작 윤곽이 아래에 있다. 계측에서 Canvas 내부 색상 bounds를 검사한다.
- `lesson-done-path.png` / `16_done_2.png`: 이번 `emu_flow`의 실제 잘한 주차 두 번째 회차 Done 원본(두 파일은 동일). 4초 대기 뒤의 정지 그림이다.
- `lesson-done.png`: 이번 `emu_flow`의 실제 못한 주차 첫 회차 Done(`14_done_1.png`), 역시 4초 대기 뒤 정지 그림.
- `lesson-setup-sheet.png`: 선택 카드의 12dp Ink 띠. 계측에서 띠 전체 폭과 바로 옆 Periwinkle 바탕을 픽셀로 확인한다.
- 그 외 화면은 변경이 없어 라운드 4 캡처를 유지한다. 전체 화면의 이번 재검증 캡처는 `build/round5-shots/`, 실제 흐름 캡처는 `build/round5-flow/`에 있다.

`flow.txt`: 수정 없는 원본 `tools/emu_flow.sh` **PASS**, uiautomator clashes 0, 리포트까지 120초. 첫 회차 60/55·이동 4회, 두 번째 100/100·이동 2회, 힌트 3종·잘한 주차 추가 힌트 0건·도어·배지 확인. main에 반영된 Done 4초 대기를 그대로 사용했다.

Done 재생 클립(저장소에 커밋하지 않음): `C:/Project/17_hackathon-pivot/build/round5-clips/lesson-round4-done.mp4`. 계측의 `/sdcard/lesson-round4-done.mp4`를 파일명·내용 그대로 가져왔다. 1280×720, `screenrecord --time-limit 5`의 원본이며 변화 없는 마지막 구간은 프레임으로 저장되지 않아 파일 길이는 약 3.723초다. 후진 중 셰브론 표시, 전진 보정 중 숨김, 마지막 정지 프레임에서 숨김을 확인했다. 기록 데이터·순수 함수·0.5초 대기와 3초 재생 속도는 그대로다.

`flow-round3-default.txt`는 수정 전 원본의 중복 조회로 시간 감점 경계를 넘어 첫 회차 58점이 된 실패 기록이다. `flow-round3-proposal.txt`는 같은 패치를 임시 사본으로 먼저 검증한 PASS 기록(109초)이며, `flow-{initial,baseline,proposal}.txt`는 라운드 2 이력이다.

`build-round5.txt`: `assembleDebug testDebugUnitTest :automotive:assembleDebugAndroidTest` 성공. 단위 테스트 154개, 실패·오류 0. `contract.txt`: **Lesson contract passed**. 회전 방향·선택 띠 픽셀 검사와 정지 상태의 셰브론 언급 없음 검사를 추가했다. 기존 알약 접근성·잠금·140dp × 720dp 이상 주 버튼·미측정·보조선·재생 중 다음 회차 전환·Done 숫자 금지·캡션 계약도 통과했다. `ui/`의 Bold 0건, 색 리터럴은 `CoachStyle.kt`의 5토큰만 확인했다.
