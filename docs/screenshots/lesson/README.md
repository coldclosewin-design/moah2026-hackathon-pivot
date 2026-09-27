# UI 라운드 4 검증 캡처

2026-09-28 · `codex/ui-round4` · 기준 `origin/main=12092f6` · CSTDe_API_34 2560×1440 · 기본 Fake, 배율 1.0. 사용자 결정 확정: D1 = (나) 알약, D2 = `코치` 유지. 텍스트 토글 구현·비교 캡처는 제거했다.

- `lesson-*.png`: 최신 계측 캡처. `contract.txt`에 `Lesson contract passed`.
- `lesson-done.png`: 실제 못한 주차 첫 회차 Done. `lesson-done-path.png`: 실제 잘한 주차 두 번째 회차 Done.
- `lesson-done-no-path.png`: 빈 경로의 기존 배치. `lesson-done-path-contract.png`: 전후진·급정지·회전 방향을 확인하는 고정 입력.
- `lesson-demo-toggle-pill.png`: 확정된 무텍스트 알약. 그림 64×32dp, 접근성 터치 영역은 Compose 최소 48dp 높이로 확장된다. 그림 아래와 속도 값 사이 24dp.
- `lesson-maneuver-guides.png` / `lesson-maneuver-b.png`: 오른쪽 450°의 앞바퀴·호·점선 두 줄·풀이. 발주서 §1 각도 구간표를 따라 `한 바퀴`로 표시한다(§2 예시의 `한 바퀴 반`과 불일치 — INTEGRATION C절).
- `lesson-panel-open.png`: 본문 폭을 유지하는 420dp 레일, 72dp 버튼·12dp 간격·구분선·2열 조작.
- `lesson-setup-sheet.png`: 유형 띠 12dp, 선택 주차 띠 Paper 반전. 첫 화면·시트 `시작`, `다 됐어요`, `한 번 더` 모두 높이 140dp·폭 720dp 이상.
- `session-report.png`: 실제 두 회차 후 도어 열림으로 진입한 Report.

`flow.txt`: 최종 코드로 수정 없는 원본 `tools/emu_flow.sh` PASS, uiautomator clashes 0, 리포트까지 116초. 첫 회차 60/55·이동 4회, 두 번째 100/100·이동 2회, 힌트 3종·잘한 주차 추가 힌트 0건·도어·배지 확인. 알약의 contentDescription으로 패널 열기·자동 접힘을 실제 시연에서 통과했다. 원본의 `14_done_1.png`·`16_done_2.png`는 2초 대기로 재생 중일 수 있어, 같은 실행에서 Done 로그 3.7초 뒤 추가한 `screencap`을 `lesson-done.png`·`lesson-done-path.png`로 썼다. 원본과 추가 정지 캡처는 `build/round4-ready-flow/`에 있다. 도구 수정 요청은 INTEGRATION C절.

애니메이션 리뷰 파일(저장소에 커밋하지 않음): `build/round4-ready-clips/steering.mp4`(0 → 오른쪽 450° → 0), `build/round4-ready-clips/done.mp4`(0.5초 대기 후 기록 시각에 맞춘 3초 재생). 각 5초·1280×720·30fps, 계측 고정 입력이다. `screenrecord --time-limit 5` 원본은 같은 폴더의 `*-screenrecord.mp4`. Android가 변화 없는 마지막 구간을 프레임으로 저장하지 않아 최종 프레임만 5초까지 유지했다. 움직임의 속도는 바꾸지 않았다.

`flow-round3-default.txt`는 수정 전 원본의 중복 조회로 시간 감점 경계를 넘어 첫 회차 58점이 된 실패 기록이다. `flow-round3-proposal.txt`는 같은 패치를 임시 사본으로 먼저 검증한 PASS 기록(109초)이며, `flow-{initial,baseline,proposal}.txt`는 라운드 2 이력이다.

`build-round4.txt`: 빌드·단위 테스트·계측 APK 빌드 성공. 단위 테스트 154개, 실패·오류 0. `contract.txt`: 무텍스트 알약의 접근성 클릭·잠금·주 버튼 bounds·미측정·풀이·보조선·재생 중 다음 회차 전환·재생 후 정지 캡처를 포함해 `Lesson contract passed`.
