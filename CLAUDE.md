@AGENTS.md

# Claude Code 전용 지침

- 나는 이 저장소에서 **인프라·VSS 포트·채점 엔진·상태기계·문서·리뷰 소유자**다. Gradle 파일, `vss-stub/`, `automotive/.../vehicle/`, `.../ports/`, `.../scoring/`, `.../feature/lesson/`, `docs/`는 내가 고친다. `ui/`, 화면 feature, 테스트는 Codex 담당이므로 필요하면 `docs/04_agent_workflow.md`의 핸드오프 템플릿으로 `docs/handoffs/YYYY-MM-DD_codex_<topic>.md` 를 써서 사용자에게 전달한다.
- Codex PR 리뷰 시 `docs/04_agent_workflow.md`의 리뷰 체크리스트를 그대로 적용한다. 그 브랜치를 직접 빌드하고 캡처를 눈으로 본다.
- `vss-stub/` 또는 `VehiclePort`를 바꾸면 `docs/02_vss_api_contract.md`, `docs/INTEGRATION.md`를 같은 커밋에서 갱신한다.
- 프로젝트 폴더(`C:\Project\17_hackathon-pivot`) 밖 쓰기(Android SDK, `~/.android`, `~/.gitconfig`, **`C:\Project\16_hackathon` 포함**)는 상위 `C:\Project\CLAUDE.md` 규칙대로 매번 사용자 승인을 받는다. 16번은 읽기만 한다. git 사용자 정보는 repo-local(`git config user.*`)로만 설정한다.
- 사내 스크린샷·에뮬 이미지·사내 소스는 절대 커밋하지 않는다. `.gitignore` 확인 후 커밋.
- **셸 함정**: 큰 heredoc 에 한국어 본문을 넣으면 Bash 가 통째로 파싱 실패한다(9/25 에도 겪음). 커밋 메시지·PR 본문은 파일로 써서 `-F`/`--body-file`, 긴 파일은 Write 도구로.
- **개발일지**: 세션을 마칠 때(또는 사용자가 "일지" 라고 하면) `docs/journal/YYYY-MM-DD.md` 를 `_template.md` 형식으로 쓰거나 이어 쓴다. 특히 "결정과 이유", "막힌 것과 해결"을 근거 링크(커밋·PR·문서)와 함께 남긴다. Codex 작업은 PR 본문에서 가져온다. `docs/journal/README.md` 인덱스 표도 갱신한다.
- **세션 시작**: `docs/NEXT.md` 를 먼저 읽는다. **세션 끝**: 일지와 함께 `docs/NEXT.md` 의 "남은 일"을 갱신한다.
- 응답은 한국어.
