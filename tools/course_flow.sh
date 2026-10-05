#!/usr/bin/env bash
# 코스 과제(10/4) — 장내기능 모의시험을 에뮬에서 평가(시험) 모드로 두 번 돌리고 단계별 스크린샷을 남긴다.
#   Setup(시트 → 주행 → 장내기능 모의시험 → 평가 → 시작) → Drive("못한 시험") → 다 됐어요 → Done
#   → 한 번 더 → Drive("잘한 시험") → 다 됐어요 → Done → 문 열기 → Report
# 판정은 앱 로그(MOAH/LessonStateMachine): 못한 시험 = 코스 70 불합격·감점 3, 잘한 시험 = 100 합격·감점 0(CourseScenariosTest 고정값).
# 사용: bash tools/course_flow.sh <출력폴더>     (emu_flow.sh 와 동시에 돌리지 않는다)
set -u
export MSYS_NO_PATHCONV=1
OUT="$1"; mkdir -p "$OUT"
OUTW=$(cygpath -w "$OUT")
ADB="${ADB:-$(cygpath -u "${ANDROID_HOME:-$LOCALAPPDATA/Android/Sdk}")/platform-tools/adb.exe}"
DISPLAY_ID=4619827259835644672
TAG="MOAH/LessonStateMachine:*"
exec > >(tee -a "$OUT/log.txt") 2>&1
LOCK="$(dirname "$OUT")/emu_flow.lock"
if ! mkdir "$LOCK" 2>/dev/null; then echo "!! another flow is running (lock: $LOCK). abort."; exit 3; fi
trap 'rmdir "$LOCK" 2>/dev/null' EXIT
if ! command timeout 20 "$ADB" -s emulator-5554 get-state </dev/null 2>/dev/null | grep -q device; then echo "!! no device. abort."; exit 4; fi

adb() { command timeout 60 "$ADB" "$@" </dev/null; }
dump() {
  rm -f "$OUT/ui.xml"; adb shell rm -f /sdcard/ui.xml >/dev/null 2>&1
  adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1
  adb pull /sdcard/ui.xml "$OUTW\\ui.xml" >/dev/null 2>&1
  [ -s "$OUT/ui.xml" ] || { echo "<DUMP_FAILED/>" > "$OUT/ui.xml"; return 1; }
}
texts() { grep -oE 'text="[^"]*"' "$OUT/ui.xml" | sed 's/text="//;s/"$//' | grep -v '^$' | tr '\n' '|'; }
now_texts() { dump; texts; }
declare -A BOUNDS
bounds_of() { grep -oE "(text|content-desc)=\"$1\"[^>]*bounds=\"\[[0-9]+,[0-9]+\]\[[0-9]+,[0-9]+\]\"" "$OUT/ui.xml" | head -1 | grep -oE 'bounds="[^"]*"' | grep -oE '[0-9]+' | tr '\n' ' '; }
tap_text() {
  local b="" try
  for try in 1 2 3 4 5; do
    if dump; then b=$(bounds_of "$1"); [ -n "$b" ] && BOUNDS[$1]="$b"; elif [ -n "${BOUNDS[$1]:-}" ]; then b="${BOUNDS[$1]}"; fi
    [ -n "$b" ] && break
    echo "  .. '$1' not yet (try $try)"; sleep 2
  done
  if [ -z "$b" ]; then echo "  !! '$1' not found: $(texts)"; return 1; fi
  local label="$1"; set -- $b; adb shell input tap $(( ($1+$3)/2 )) $(( ($2+$4)/2 )); echo "  tap '$label'"
}
shot() { adb shell screencap -p -d $DISPLAY_ID /sdcard/s.png && adb pull /sdcard/s.png "$OUTW\\$1.png" >/dev/null 2>&1; echo "  shot $1"; }
wait_log() {
  local i; for i in $(seq 1 "$2"); do
    adb logcat -d -s "$TAG" | grep -qE "$1" && { echo "  log '$1' after ${i}s"; return 0; }; sleep 1
  done
  echo "  !! timeout waiting log '$1'"; return 1
}
open_demo() { dump; texts | grep -q "$1" && return 0; tap_text "시연"; sleep 1; }
FAIL=0

echo "== start"; adb shell am force-stop com.moah.hackathon; adb logcat -c
# 관리자 프리셋(라운드 22 결정 5): 시연 프로필로 시작하고 첫 실행 프로필 질문을 건너뛴다. 화면이 extra 를 읽기 전(라운드 23b)에는 무시된다
adb shell am start -n com.moah.hackathon/.ui.MainActivity --es preset exam-fail-pass >/dev/null; sleep 4
for i in 1 2 3; do tap_text "과제·모드 바꾸기"; sleep 2; now_texts | grep -q "제휴 시험장" && break; done
tap_text "주행"; sleep 1; tap_text "장내기능 모의시험"; sleep 1; shot 20_sheet_driving
tap_text "평가"; sleep 1; tap_text "시작"
wait_log "begin track-exam EVALUATE" 10 || { echo "  !! session did not begin"; exit 1; }
wait_log "attempt 1 start" 15 || exit 1; sleep 1; shot 21_drive_start

echo "== attempt 1: 못한 시험 (감점 셋 · 불합격)"
open_demo "못한 시험"; tap_text "못한 시험"; sleep 20; shot 22_drive_slope
wait_log "course zone: exam-parking" 90 || FAIL=1; sleep 4; shot 23_drive_parking
wait_log "course zone: exam-emergency" 120 || FAIL=1; sleep 3; shot 24_drive_emergency
wait_log "asked done \(attempt 1" 120 || FAIL=1; sleep 1; shot 25_drive_finish
tap_text "다 됐어요"; wait_log "attempt 1: skill=" 10 || exit 1; sleep 4; shot 26_done_1
R1=$(adb logcat -d -s "$TAG" | grep -oE "attempt 1: .* course=[0-9]+ passed=[a-z]+ deductions=[0-9]+" | grep -oE "course=.*" | tail -1); echo "  $R1"
[ "$R1" = "course=70 passed=false deductions=3" ] || { echo "  !! attempt 1 expected course=70 passed=false deductions=3"; FAIL=1; }
for r in "뒤로 밀림" "검지선 접촉" "비상등 미점등"; do adb logcat -d -s "$TAG" | grep -q "course deduction: $r" || { echo "  !! missing deduction $r"; FAIL=1; }; done

echo "== attempt 2: 잘한 시험 (감점 없음 · 합격)"
tap_text "한 번 더"; wait_log "attempt 2 start" 10 || exit 1
open_demo "잘한 시험"; tap_text "잘한 시험"
wait_log "asked done \(attempt 2" 200 || FAIL=1; sleep 1
tap_text "다 됐어요"; wait_log "attempt 2: skill=" 10 || exit 1; sleep 4; shot 27_done_2
R2=$(adb logcat -d -s "$TAG" | grep -oE "attempt 2: .* course=[0-9]+ passed=[a-z]+ deductions=[0-9]+" | grep -oE "course=.*" | tail -1); echo "  $R2"
[ "$R2" = "course=100 passed=true deductions=0" ] || { echo "  !! attempt 2 expected course=100 passed=true deductions=0"; FAIL=1; }

echo "== report"
open_demo "문 열기"; tap_text "문 열기"; wait_log "report: attempts=2" 15 || FAIL=1; sleep 3; shot 28_report
echo "== logcat"; adb logcat -d -s "$TAG" | grep -v "beginning of" | cut -c20-200 | grep -v "hint:" | tail -60
echo "== result: $([ $FAIL = 0 ] && echo PASS || echo FAIL) (장내기능 못함 70 불합격 → 잘함 100 합격)"
exit $FAIL
