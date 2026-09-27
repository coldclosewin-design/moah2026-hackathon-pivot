#!/usr/bin/env bash
# 운전 연수(후면 직각 주차) 세션을 에뮬에서 자동 재생하고 단계별 스크린샷을 남긴다.
#   Setup(힌트 모드 선택 → 시작) → Briefing → Maneuver(시나리오 "못한 주차") → 다 됐어요 → Done
#   → 한 번 더 → Maneuver("잘한 주차") → 다 됐어요 → Done → 문 열기 → Report
# 판정은 앱 로그(MOAH/LessonStateMachine)로, 화면 텍스트는 정지 화면(Setup·Done·Report)에서만 믿는다.
# 화면 라벨은 docs/handoffs/2026-09-26_codex_lesson_screens.md 의 문자열과 **정확히** 일치해야 한다.
set -u
export MSYS_NO_PATHCONV=1
OUT="$1"; mkdir -p "$OUT"
OUTW=$(cygpath -w "$OUT")
ADB="${ADB:-$(cygpath -u "${ANDROID_HOME:-$LOCALAPPDATA/Android/Sdk}")/platform-tools/adb.exe}"
DISPLAY_ID=4619827259835644672
TAG="MOAH/LessonStateMachine:*"
exec > >(tee -a "$OUT/log.txt") 2>&1

# 동시 실행 방지: 두 인스턴스가 겹치면 탭·덤프가 서로 간섭해 결과가 오염된다 (16번 2026-09-18 교훈).
# 이 환경에는 pkill 이 없다. 멈출 때는:  kill "$(cat <출력폴더 상위>/emu_flow.pid)"
LOCK="$(dirname "$OUT")/emu_flow.lock"; PIDFILE="$(dirname "$OUT")/emu_flow.pid"
if ! mkdir "$LOCK" 2>/dev/null; then echo "!! another instance is running (lock: $LOCK, pid $(cat "$PIDFILE" 2>/dev/null)). abort."; exit 3; fi
echo $$ > "$PIDFILE"
trap 'rmdir "$LOCK" 2>/dev/null; rm -f "$PIDFILE"' EXIT
if ! command timeout 20 "$ADB" -s emulator-5554 get-state </dev/null 2>/dev/null | grep -q device; then echo "!! no device. abort."; exit 4; fi

adb() { command timeout 60 "$ADB" "$@" </dev/null; }
# 계속 갱신되는 화면(Maneuver 도식)에서는 uiautomator 가 "could not get idle state" 로 실패하고 파일을 만들지 않는다.
# 예전 파일을 읽어 낡은 화면을 믿지 않도록 매번 양쪽 파일을 지우고 실패를 명시한다.
dump() {
  local dst="$OUTW\\ui.xml"
  rm -f "$OUT/ui.xml"; adb shell rm -f /sdcard/ui.xml >/dev/null 2>&1
  adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1
  adb pull /sdcard/ui.xml "$dst" >/dev/null 2>&1
  [ -s "$OUT/ui.xml" ] || { echo "<DUMP_FAILED/>" > "$OUT/ui.xml"; return 1; }
}
now_texts() { dump; texts; }
texts() { grep -oE 'text="[^"]*"' "$OUT/ui.xml" | sed 's/text="//;s/"$//' | grep -v '^$' | tr '\n' '|'; }
# 시연 패널·회차 버튼은 화면마다 같은 자리다. 덤프가 될 때 좌표를 기억해 두고, 도식이 움직여 덤프가 안 되면 기억한 좌표로 누른다.
declare -A BOUNDS
remember_bounds() {
  local label b
  for label in "정차" "출발" "문 열기" "문 닫기" "잘한 주차" "못한 주차" "다 됐어요" "시연"; do
    b=$(grep -oE "(text|content-desc)=\"$label\"[^>]*bounds=\"\[[0-9]+,[0-9]+\]\[[0-9]+,[0-9]+\]\"" "$OUT/ui.xml" | head -1 | grep -oE 'bounds="[^"]*"' | grep -oE '[0-9]+' | tr '\n' ' ')
    [ -n "$b" ] && BOUNDS[$label]="$b"
  done
}
tap_text() { # tap_text "라벨"  (최대 4회 재시도, 덤프가 안 되면 기억한 좌표). 라벨은 text 또는 content-desc(무텍스트 알약 토글 대응, 라운드 4)
  local b="" try
  for try in 1 2 3 4; do
    if dump; then remember_bounds; elif [ -n "${BOUNDS[$1]:-}" ]; then b="${BOUNDS[$1]}"; echo "  .. dump failed (screen is animating) → remembered bounds for '$1'"; break; fi
    b=$(grep -oE "(text|content-desc)=\"$1\"[^>]*bounds=\"\[[0-9]+,[0-9]+\]\[[0-9]+,[0-9]+\]\"" "$OUT/ui.xml" | head -1 | grep -oE 'bounds="[^"]*"' | grep -oE '[0-9]+' )
    [ -n "$b" ] && break
    echo "  .. '$1' not yet (try $try)"; sleep 2
  done
  if [ -z "$b" ]; then echo "  !! '$1' not found: $(texts)"; return 1; fi
  set -- $b; local x=$(( ($1+$3)/2 )) y=$(( ($2+$4)/2 ))
  adb shell input tap $x $y; echo "  tap @ $x,$y"
}
# The finish control stays at the same position throughout each Maneuver. Its bounds
# were recorded before playback; wait_log asked-done confirms it is enabled again.
# Avoid adding a fresh UI dump to the measured attempt duration just before finishing.
tap_cached() {
  local b="${BOUNDS[$1]:-}"
  if [ -z "$b" ]; then tap_text "$1"; return $?; fi
  set -- $b; local x=$(( ($1+$3)/2 )) y=$(( ($2+$4)/2 ))
  adb shell input tap $x $y; echo "  tap cached finish @ $x,$y"
}

shot() {
  local dst="$OUTW\\$1.png"
  adb shell screencap -p -d $DISPLAY_ID /sdcard/s.png && adb pull /sdcard/s.png "$dst" >/dev/null 2>&1
  echo "  shot $1"
}
wait_log() { # wait_log "패턴" 최대초
  local i; for i in $(seq 1 "$2"); do
    adb logcat -d -s "$TAG" | grep -qE "$1" && { echo "  log '$1' after ${i}s"; return 0; }; sleep 1
  done
  echo "  !! timeout waiting log '$1'"; return 1
}
# 시연 패널이 접혀 있으면 연다(라벨 "시연"). 이미 열려 있으면 "못한 주차" 가 보인다.
# 라운드 3 부터 패널은 기본 접힘·시나리오 재생 시 자동 접힘 — 누를 때마다 연다. 이미 열려 있으면 시나리오 라벨이 보인다
open_demo_panel() {
  dump || return 1
  remember_bounds
  texts | grep -qE "못한 주차|못한 점검" && return 0
  # Reuse the fresh dump. tap_text below waits for the opened panel before playing.
  tap_cached "시연"
}
T0=$(date +%s); mark() { echo "  [t+$(( $(date +%s) - T0 ))s] $1"; }
FAIL=0

echo "== start"; adb shell am force-stop com.moah.hackathon; adb logcat -c
adb shell am start -n com.moah.hackathon/.ui.MainActivity >/dev/null; sleep 4
shot 10_setup; echo "  texts: $(now_texts)"
# UI 라운드 2 부터 모드 선택은 "과제·모드 바꾸기" 시트 안에 있을 수 있다 — 첫 화면에 "힌트" 가 없으면 시트를 먼저 연다
dump; texts | grep -q "힌트" || { tap_text "과제·모드 바꾸기"; sleep 1; }
tap_text "힌트"; sleep 1; tap_text "시작"; sleep 1
wait_log "begin parking-rear-perpendicular HINT" 10 || { echo "  !! session did not begin"; exit 1; }
T0=$(date +%s); mark "session began"
shot 11_briefing
wait_log "attempt 1 start" 15 || exit 1; mark "attempt 1 (hint)"

echo "== attempt 1: 못한 주차 (힌트 모드 — 벨트·근접·급정지 힌트가 나와야 한다)"
open_demo_panel; tap_text "못한 주차"; sleep 6; shot 12_maneuver_hint
wait_log "hint: 안전벨트" 30 || FAIL=1
wait_log "hint: 뒤가 가까워요" 60 || FAIL=1
wait_log "hint: 제동이 급했어요" 20 || FAIL=1
wait_log "asked done \(attempt 1" 60 || FAIL=1; mark "asked done (1)"; sleep 1; shot 13_maneuver_parked
# Done 은 진입 0.5 s 뒤 궤적을 3 s 재생한다(라운드 4) — 정지 그림을 찍으려면 4 s. 채점은 끝난 뒤라 회차 시간에 안 들어간다
tap_cached "다 됐어요"; wait_log "attempt 1: skill=" 10 || exit 1; mark "done (1)"; sleep 4; shot 14_done_1
echo "  texts: $(now_texts)"
R1=$(adb logcat -d -s "$TAG" | grep -oE "attempt 1: skill=[0-9]+ safety=[0-9]+ segments=[0-9]+" | tail -1); echo "  $R1"
[ "$R1" = "attempt 1: skill=60 safety=55 segments=4" ] || { echo "  !! attempt 1 expected skill=60 safety=55 segments=4 (ParkingRecorderScenarioTest 고정값)"; FAIL=1; }

echo "== attempt 2: 잘한 주차 (힌트 없이 끝나야 한다)"
tap_text "한 번 더"; wait_log "attempt 2 start" 10 || exit 1; mark "attempt 2"
HINTS_BEFORE=$(adb logcat -d -s "$TAG" | grep -c "hint: ")
open_demo_panel; tap_text "잘한 주차"; sleep 6; shot 15_maneuver_good
wait_log "asked done \(attempt 2" 60 || FAIL=1; mark "asked done (2)"   # attempt 번호로 한정 — 1회차 로그에 매칭되던 버그(Codex, C절 9/26)
tap_cached "다 됐어요"; wait_log "attempt 2: skill=" 10 || exit 1; mark "done (2)"; sleep 4; shot 16_done_2
HINTS_AFTER=$(adb logcat -d -s "$TAG" | grep -c "hint: ")
[ "$HINTS_AFTER" -eq "$HINTS_BEFORE" ] || { echo "  !! good parking produced hints ($((HINTS_AFTER-HINTS_BEFORE)))"; FAIL=1; }
R2=$(adb logcat -d -s "$TAG" | grep -oE "attempt 2: skill=[0-9]+ safety=[0-9]+ segments=[0-9]+" | tail -1); echo "  $R2"
[ "$R2" = "attempt 2: skill=100 safety=100 segments=2" ] || { echo "  !! attempt 2 expected skill=100 safety=100 segments=2"; FAIL=1; }

echo "== report: 정차 + 운전석 도어 열림"
open_demo_panel; tap_text "문 열기"; wait_log "report: attempts=2" 15 || exit 1; mark "report (= 시연 길이)"; sleep 3; shot 17_report
now_texts | grep -q "다시 시작" || { echo "  !! report screen not reached: $(texts)"; FAIL=1; }
now_texts | grep -qE "실신호|시뮬레이션|미측정" || { echo "  !! report has no availability badge"; FAIL=1; }
echo "  texts: $(texts)"

echo "== logcat"; adb logcat -d -s "$TAG" | grep -v "beginning of" | cut -c20-220
echo "== uiautomator clashes (0 = clean single-instance run): $(adb logcat -d | grep -c "UiAutomationService.*already registered")"
echo "== result: $([ $FAIL = 0 ] && echo PASS || echo FAIL) (힌트 3종 · 회차 2 채점 고정값 · 리포트 배지)"
echo "== done"; exit $FAIL
