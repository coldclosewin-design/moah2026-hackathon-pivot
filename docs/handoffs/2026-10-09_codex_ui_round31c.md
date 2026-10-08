# Codex 오더 — 라운드 31c: 앱 로고 교체 (2026-10-09)

```
프로젝트: C:\Project\17_hackathon-pivot. 먼저 AGENTS.md, 28a 발주서(제약·완료 기준)를 읽는다.
시작 조건: 31b 머지 뒤 origin/main 최신에서 새 워크트리, 브랜치 codex/ui-round31c, scope ui, PR 하나(작다 — 31b 와 합치지 않는다).
완료 기준: 런처(사내 런처 방식 = 동그라미 안 그림)에서 아이콘이 보이는 캡처 + 72/48 dp 로 줄인 캡처 · `aapt dump badging` 으로 라벨 `Drive Coach` 확인 · 빌드 · 단위 테스트 · lesson_shots 1회(아이콘은 화면 계약과 무관하지만 리소스 이름 충돌 확인). 에뮬은 emulator-5556 만.
```

사용자 결정(10/9): 로고 = **라운드 31 로고 3차 20번(E4)** — 핸들 속 새싹: 링 안에서 **노란 잎(왼쪽, 10시)이 링에 닿아 살이 되고, 검은 잎(오른쪽, 2시)은 노란 잎의 60 % 길이로 링에 닿지 않으며, 줄기는 아래로 내려오되 링에 닿지 않는다**. 운전대로도, 원 안의 새싹으로도 읽힌다. 시안 칸: `docs/design/round31-proposals/5-app-logo-sprout-sizes.html` 의 20 · `5-app-logo-sprout-sizes-b.png`.

## 항목

| # | 할 것 | 핵심 · 주의 |
|---|---|---|
| ① | `ic_launcher` 를 VectorDrawable 로 교체 | 아래 SVG 를 `automotive/src/main/res/drawable/ic_launcher.xml`(108×108 viewport) 로 옮긴다. **흰 바탕 원 + Onyx 그림 + 노란 잎**(런처가 원형으로 자른다 — 바탕을 꽉 채운 흰 사각형 위에 그린다, 안전 원 r 33 안에 그림이 있다). `stroke-linecap/linejoin round` → `android:strokeLineCap="round"` `strokeLineJoin="round"`. circle 없이 path 만이라 그대로 옮겨진다 |
| ② | 적응형 아이콘 | `mipmap-anydpi-v26/ic_launcher.xml` 로 `<adaptive-icon>`(background = 흰 `#FFFFFF` 색, foreground = ①의 그림 벡터) 를 두고 매니페스트 `android:icon` 을 그것으로. 그림은 foreground 108 dp 의 가운데 66 dp 안전 원 안 — 시안 좌표가 이미 그 안이다. `roundIcon` 도 같은 것 |
| ③ | 라벨 | 31a ⑫ 로 `app_name` = `Drive Coach` 가 되어 있어야 한다. 안 되어 있으면 여기서 |
| ④ | 어두운 바탕 변형은 만들지 않는다 | 런처가 흰 원 아이콘을 그대로 쓴다. 홈 워드마크 `DRIVE COACH` 글자는 바꾸지 않는다 |

## 시안 SVG(20 · E4 — 그대로 옮긴다)

```svg
<svg viewBox="0 0 108 108" fill="none" stroke-linecap="round" stroke-linejoin="round">
  <path d="M26,54 A28,28 0 1 1 82,54 A28,28 0 1 1 26,54 Z" stroke="#222526" stroke-width="6"/>
  <path d="M54,54 C55.4,46.3 61.2,43 68.5,45.6 C67.1,53.3 61.3,56.6 54,54 Z" fill="#222526"/>
  <path d="M54,54 C42.8,56.7 33.1,51.1 29.8,40 C41,37.3 50.7,42.9 54,54 Z" fill="#F2C230" stroke="#222526" stroke-width="2.5"/>
  <path d="M54,54 V72" stroke="#222526" stroke-width="6"/>
</svg>
```

(링 = 호 둘 · 검은 잎 = 채움 · 노란 잎 = 채움 + Onyx 테두리 2.5 · 줄기 = 선 6. 노란 잎 끝이 링 중심선 r 28 에 닿는다.)

## 불변

28a 와 같다. 화면 코드·계약은 건드리지 않는다. 리소스 이름은 `ic_launcher` 그대로(매니페스트·도구가 그 이름을 쓴다).

머지 뒤 Claude 가 리뷰하고 새 태그로 사내 검증·녹화.
