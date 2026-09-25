# 에이전트 작업 분담 — Claude Code × Codex

1인 팀이므로 두 에이전트는 **레이어별 소유권**으로 나누고, 한 시점에 한 에이전트만 working tree를 수정한다(교대). 공통 지침은 [AGENTS.md](../AGENTS.md) 한 곳에 두고 `CLAUDE.md`가 이를 임포트한다.

## 소유권

| 영역 | 소유 | 이유 |
|---|---|---|
| `build.gradle.kts`, `settings.gradle.kts`, `gradle/`, `gradle.properties` | **Claude** | 사내 머지 접점 (compileOnly 한 줄, 플래그) |
| `vss-stub/` (mobis.vss 스텁) | **Claude** | 사내 jar 시그니처와 1:1, 함정 6개 |
| `automotive/.../vehicle/` (VehiclePort, Fake/Real, Factory, VssValues) | **Claude** | 외부↔사내 전환의 핵심 |
| `.../ports/` (TtsPort·LocationPort·CoachPort + Fake), `.../scoring/` (채점 엔진), `.../feature/lesson/` (상태기계·ViewModel), `.../data/SeedCatalog.kt` 구조, `App.kt` | **Claude** | 외부 의존과 흐름 제어 |
| `docs/`, `INTEGRATION.md`, `AGENTS.md`, `CLAUDE.md`, `README.md` | **Claude** | 사내 수렴 체크리스트 관리 |
| `automotive/.../ui/`, `automotive/.../feature/{setup,driving,report}/`, `res/`, 테마, 시드 **내용**(문구·좌표) | **Codex** | 화면·기능 구현 |
| `automotive/src/test/`, `automotive/src/androidTest/` | **Codex** | 단위/UI 테스트 |
| 최종 코드 리뷰, PR 머지, 사내 이관 패키징 | **Claude** | 머지 리스크 통제 |

경계 규칙:
- Codex가 `vehicle/`에 새 신호나 메서드가 필요하면 **코드를 고치지 말고** `INTEGRATION.md`의 "요청" 섹션에 적는다 → Claude가 반영.
- Claude가 화면을 바꿔야 할 때도 마찬가지로 Codex에 핸드오프한다. 급한 한 줄 수정은 허용하되 커밋 메시지에 `[cross]` 표기.
- 둘 다 `VSSManager`를 UI/feature 코드에서 직접 참조하지 않는다. 오직 `VehiclePort`.

## 브랜치 · 커밋

- `main`: 항상 빌드 통과 상태. 직접 커밋은 Claude의 인프라 변경만.
- `claude/<topic>`, `codex/<topic>`: 작업 브랜치. 완료 시 PR → Claude 리뷰 → squash merge.
- 커밋 메시지: `<scope>: <요약>` (scope 예: `vehicle`, `ui`, `stub`, `docs`, `build`). 한글/영문 무관.
- 동시 작업이 꼭 필요하면 `git worktree add ../16_hackathon-codex codex/<topic>` 로 분리.

## 작업 사이클 (기능 1개 기준)

1. **Claude**: 필요한 VSS 신호를 `VssConstants`에 추가, `VehiclePort`/Fake 시뮬레이션 확장, `INTEGRATION.md`에 가정 기록. `main`에 커밋.
2. **Codex**: `codex/<feature>` 브랜치에서 화면·기능·테스트 구현. `assembleDebug testDebugUnitTest` 통과 후 PR.
3. **Claude**: 리뷰(함정 6개 체크리스트, VehiclePort 외 의존 없음, 스레드 규칙) → 머지 → 에뮬 확인.
4. 반복.

## Codex 핸드오프 프롬프트 템플릿

```
프로젝트: C:\Project\17_hackathon-pivot (AAOS 앱, Kotlin/Compose). 먼저 AGENTS.md 와 docs/topics/01_driving_coach.md 를 읽어라.
브랜치: codex/<feature> 를 main에서 새로 만들어 작업.
작업: <화면/기능 설명>
사용 가능한 차량 데이터: VehiclePort (automotive/src/main/kotlin/com/moah/hackathon/vehicle/VehiclePort.kt)
  - 읽기 키: <VssConstants 상수 목록>
  - 쓰기 키: <...>
제약: vehicle/ 패키지와 build 파일은 수정 금지. 필요하면 docs/INTEGRATION.md "요청" 섹션에 적어라.
완료 기준: .\gradlew.bat assembleDebug testDebugUnitTest 통과, 커밋 후 PR 생성(gh pr create).
```

## Claude 리뷰 체크리스트

- [ ] `RemoteException` import/catch 없음 (주석 제외)
- [ ] `VSSManager` 직접 참조는 `RealVehiclePort` 뿐
- [ ] 신호 경로 리터럴 없음 (`VssConstants`만)
- [ ] 숫자/불리언은 `VssValues` 파서 경유
- [ ] 구독 해제 경로 존재 (`dispose()` 호출 위치)
- [ ] 새 가정이 `INTEGRATION.md`에 기록됨
- [ ] 빌드·테스트 통과
