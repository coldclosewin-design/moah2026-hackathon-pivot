# Claude Code 인계 — 프로필 원목 자동차

## 요청과 준비 상태

사용자가 선택한 원목 장난감 자동차 5종을 홈 화면에서 오른쪽으로 보이도록 준비했습니다.
폴더: `assets/profile/`. 먼저 `README.md`와 `manifest.json`을 읽어 주세요.
이번 변경은 에셋과 인계 자료만 추가한 상태이며, 앱 화면 연결은 후속 작업입니다.

## 확인한 현재 코드

- 프로필 차종: `automotive/src/main/kotlin/com/moah/hackathon/feature/lesson/ProfileChips.kt`
  - `compact` → `경차`, `small` → `준중형`, `mid-suv` → `중형 SUV`, `large` → `큰 차`.
- 홈 자동차 그림: `automotive/src/main/kotlin/com/moah/hackathon/ui/lesson/VehicleSilhouette.kt`의 `HomeVehicle`.
- 홈 구성: `automotive/src/main/kotlin/com/moah/hackathon/ui/lesson/CoachHome.kt`의 `HomeGallery`.
- 홈 진입: `automotive/src/main/kotlin/com/moah/hackathon/ui/lesson/SetupScreen.kt`.
- 프로필 표시·편집: `automotive/src/main/kotlin/com/moah/hackathon/ui/lesson/ProfileScreen.kt`.

## 후속 연결

1. manifest의 매핑을 확인하고, 필요한 PNG를 `automotive/src/main/res/drawable-nodpi/`에 같은 파일명으로 복사합니다.
2. 홈으로 들어오는 `Profile.statement.car`에 따라 차량 리소스를 선택합니다.
3. `큰 차`에는 대형 SUV를 사용합니다. 미니밴은 현재 프로필 선택지에 없으므로 별도 차종 확장 때 활성화합니다.
4. 미응답·null·알 수 없는 값에는 기존 `HomeVehicle`을 유지합니다.
5. 원목 색이 유지되도록 tint 없이 이미지를 표시하고 종횡비를 보존합니다. 기본은 `ContentScale.Fit`.
6. 파일 자체가 오른쪽 방향이므로 화면에서 다시 좌우 반전하지 않습니다.
7. 파일별 투명 여백과 차량 크기가 다릅니다. 같은 이미지 박스에서 확인하고, 경차의 작고 둥근 형태와 세단의 낮고 긴 비율이 유지되도록 조정합니다. 작은 알파 노이즈가 있을 수 있으므로 자동 크롭에 alpha > 0만을 기준으로 쓰지 않습니다.
8. 본 에셋은 정차 중 홈용입니다. 주차 도식의 방향·실시간 조향·차량 신호 표현과는 별도 표시입니다.

## 적용 후 확인

- 프로필의 네 차종을 바꿀 때 홈 차량이 즉시 바뀌는지.
- 프로필 저장 후 재실행해도 해당 차량이 유지되는지.
- 미응답·알 수 없는 값의 기본 표시가 정상인지.
- 흰 홈 배경에서 잘림·불필요한 배경·색상 tint·좌우 이중 반전이 없는지.
- 실제 홈의 이미지 박스와 시작 버튼이 서로 침범하지 않는지.
- 앱 연결 변경 뒤 저장소의 빌드·단위 테스트 및 필요한 화면 확인을 수행할 것.

향후 사용자 프로필용 시각 에셋은 이 폴더 아래 카테고리·스타일·버전 규칙으로 확장합니다.
