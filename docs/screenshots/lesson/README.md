# UI 라운드 3 검증 캡처

2026-09-27 · `codex/ui-round3` · 기준 `origin/main=10e4d1a` · CSTDe_API_34 2560×1440 · 기본 Fake, 배율 1.0.

- `lesson-*.png`: 최신 계측 캡처. `contract.txt`에 `Lesson contract passed`.
- `lesson-done.png`: 실제 못한 주차 첫 회차 Done. `lesson-done-path.png`: 실제 잘한 주차 두 번째 회차 Done.
- `lesson-done-no-path.png`: 빈 경로의 기존 배치. `lesson-done-path-contract.png`: 전후진·급정지·회전 방향을 확인하는 고정 입력.
- `lesson-maneuver-b.png`: 오른쪽 조향 450°와 후진 셰브론. `lesson-panel-open.png`: 본문 폭을 유지하는 420dp 레일.
- `lesson-setup-sheet.png`: 30/70 배치, 같은 높이 카드, 220×96dp 모드 칩.
- `session-report.png`: 실제 두 회차 후 도어 열림으로 진입한 Report.

실제 세션 3장은 사용자 승인으로 중복 패널 조회를 제거한 원본 `tools/emu_flow.sh` 실행에서 가져왔다. `flow.txt`: 첫 회차 60/55·이동 4회, 두 번째 100/100·이동 2회, 힌트 3종·잘한 주차 추가 힌트 0건·도어/배지 확인, PASS·uiautomator clashes 0, 리포트까지 108초. 채점·배율·기대값은 유지했다.

`flow-round3-default.txt`는 수정 전 원본의 중복 조회로 시간 감점 경계를 넘어 첫 회차 58점이 된 실패 기록이다. `flow-round3-proposal.txt`는 같은 패치를 임시 사본으로 먼저 검증한 PASS 기록(109초)이며, `flow-{initial,baseline,proposal}.txt`는 라운드 2 이력이다.

`build-round3.txt`: 빌드·단위 테스트·계측 APK 빌드 성공. 단위 테스트 147개, 실패·오류·건너뜀 0.
