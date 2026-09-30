# 07 — 사외 ↔ 사내 두 자리 작업 방식 (2026-09-30 사내 이관 1차 뒤 확정)

사내 첫 이관에서 굳은 규칙. 요지는 셋: **사외에서 만들고, 사내에서는 검증·녹화·제출만 하며, 사내 관찰은 사람이 요지로만 옮긴다.**

## 1. 역할

| 자리 | 하는 일 | 완료 기준 |
|---|---|---|
| **사외**(개인 PC, 이 저장소) | 개발 전부 — UI·상태기계·채점·프롬프트·테스트·문서. Codex 화면 + Claude 인프라 | Fake 로 `emu_flow` 전체 흐름 PASS + 단위 테스트 통과 |
| **사내**(WebIDE·infoLINK 에뮬·VSS 실물) | 검증·녹화·제출뿐. **사내에서 코드를 고치지 않는다**(고친 것은 사외로 못 돌아가 갈라진다) | `tools/inhouse_check.sh` 합격, A안 녹화, `submission` push, MarketUploader |

사내 → 사외 전달 단위는 **태그**(`inhouse-YYYYMMDD-N`). 사내 결과는 `docs/INTEGRATION.md` B절 형식의 **관찰 노트**로, 사람이 요지만 사외로 옮긴다(소스·jar·캡처·로그 원문은 반출하지 않는다). 사외는 그 문서만 보고 `main` 에 구현하고 다음 태그를 딴다.

## 2. 사외에서 태그를 따기 전

1. `.\gradlew.bat assembleDebug testDebugUnitTest` 통과(Fake), `bash tools/emu_flow.sh <폴더>` → `result: PASS`.
2. `git tag inhouse-YYYYMMDD-N && git push origin inhouse-YYYYMMDD-N`. 같은 날 두 번째면 `-2`.
3. `docs/NEXT.md` 에 "사내에서 확인할 것" 체크리스트(확인 방법·합격 기준)를 적는다.

## 3. 사내 세션(순서대로)

```bash
# 코드
git fetch && git checkout inhouse-YYYYMMDD-N
cp <사내 jar> automotive/libs/mobis.framework.core.jar          # gitignore
echo "mobis.vss.jar=automotive/libs/mobis.framework.core.jar" >> local.properties   # 추적 안 됨 — 이 한 줄이 Real 빌드
./gradlew assembleDebug                                          # 로그 첫 줄 "mobis.vss: jar … → USE_FAKE_VSS=false"
# 기기
adb root
adb shell cmd wifi set-wifi-enabled enabled && adb shell cmd wifi connect-network AndroidWifi open   # shell uid 로는 거부 — root 뒤
adb push copilot_config.json /data/local/tmp/copilot_config.json   # client_id(·endpoint·model). 저장소에 없음
adb logcat -G 16M
# 검증(설치·실행·흐름·요약까지)
bash tools/inhouse_check.sh
```

로그인 함정: AI 코치는 device code 방식이라 패널의 `AI 연결` 뒤 GitHub 에서 코드를 넣고 **Authorize 까지** 눌러야 한다(15분 만료). OAuth 는 device-protected 저장소라 `install -r` 뒤에도 남는다.

`inhouse_check.sh` 합격 기준: `RealVehiclePort ready` · `attempt 1 start` · `attempt 2:` · `report:` · `badge=…live=` 값 출력 · FATAL 0 · `CloudCoachPort.*fallback` 개수(0 이 목표) · "다 됐어요" → 채점 지연(ms). 결과는 한 화면 요약뿐, 캡처는 저장하지 않는다.

## 4. 제출(`submission` 브랜치)

- 심사자가 빌드할 수 있게 **사내 jar 를 커밋**한다 — 그 브랜치에서만(`.gitignore` 의 `automotive/libs/` 를 그 브랜치에서 풀고 `local.properties` 대신 `gradle.properties` 나 README 에 한 줄). `main` 에는 절대 넣지 않는다.
- `submission` = 사외 태그 + `inhouse:` 커밋 1개(사내 jar).
- MarketUploader: 카테고리 VEHICLE, 재업로드마다 `versionCode` +1.

## 5. 커밋 규칙(사내 브랜치에 먼저 넣은 것과 같은 내용 — AGENTS.md "브랜치 · 커밋")

1. 한 커밋 = 한 논리 변경. 제목 `<scope>: <무엇을 어떻게>` 한국어 50자 안팎·마침표 없음, `fix`/`update`/`wip` 금지.
2. 본문 `- ` 불릿 2~5개(무엇·왜·영향), 끝에 `검증: 빌드 · 단위 테스트 N`(+ 실제로 한 확인만).
3. 코드+테스트+관련 문서는 같은 커밋, 이름 바꾸기·포맷팅과 동작 변경은 분리.
4. 모든 커밋이 빌드·테스트 통과.
5. push 전 실험·진단 커밋은 `git commit --fixup` → `GIT_SEQUENCE_EDITOR=: git rebase -i --autosquash <기준>` 으로 합치고, push 뒤에는 이력 재작성·force push 금지.
6. PR 제목 = squash 커밋 제목(같은 형식).
7. scope 에 `inhouse` 추가 — 사내 전용 변경 표시. `submission` = 사외 태그 + `inhouse:` 커밋 1개(사내 jar).
