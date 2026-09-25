#!/usr/bin/env bash
# Gift Drive 다중 정거장 흐름을 에뮬에서 자동 재생하고 단계별 스크린샷을 남긴다.
set -u
export MSYS_NO_PATHCONV=1
OUT="$1"; mkdir -p "$OUT"
OUTW=$(cygpath -w "$OUT")
ADB="${ADB:-$(cygpath -u "${ANDROID_HOME:-$LOCALAPPDATA/Android/Sdk}")/platform-tools/adb.exe}"
DISPLAY_ID=4619827259835644672
exec > >(tee -a "$OUT/log.txt") 2>&1

# 동시 실행 방지: 두 인스턴스가 겹치면 탭·덤프가 서로 간섭해 결과가 오염된다 (2026-09-18 교훈).
# 이 환경에는 pkill 이 없다. 멈출 때는:  kill "$(cat <scratchpad>/emu_flow.pid)"
LOCK="$(dirname "$OUT")/emu_flow.lock"; PIDFILE="$(dirname "$OUT")/emu_flow.pid"
if ! mkdir "$LOCK" 2>/dev/null; then echo "!! another instance is running (lock: $LOCK, pid $(cat "$PIDFILE" 2>/dev/null)). abort."; exit 3; fi
echo $$ > "$PIDFILE"
trap 'rmdir "$LOCK" 2>/dev/null; rm -f "$PIDFILE"' EXIT
# 기기가 없으면 시작하지 않는다 (기기 대기 중에 좀비로 남는 것 방지)
if ! command timeout 20 "$ADB" -s emulator-5554 get-state </dev/null 2>/dev/null | grep -q device; then echo "!! no device. abort."; exit 4; fi

adb() { command timeout 60 "$ADB" "$@" </dev/null; }
# 주의: 화면이 계속 갱신되는 단계(Driving)에서는 uiautomator 가 "could not get idle state" 로 실패하고
# 파일을 만들지 않는다. 예전 파일을 읽어 낡은 화면을 믿지 않도록, 매번 양쪽 파일을 지우고 실패를 명시한다.
dump() {
  local dst="$OUTW\\ui.xml"
  rm -f "$OUT/ui.xml"; adb shell rm -f /sdcard/ui.xml >/dev/null 2>&1
  adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1
  adb pull /sdcard/ui.xml "$dst" >/dev/null 2>&1
  [ -s "$OUT/ui.xml" ] || { echo "<DUMP_FAILED/>" > "$OUT/ui.xml"; return 1; }
}
# texts 는 "마지막 덤프"를 읽는다. 지금 화면을 보려면 now_texts (덤프 후 읽기). 탭·대기 뒤에 texts 만 부르면 한 단계 전 화면이 나온다.
now_texts() { dump; texts; }
texts() { grep -oE 'text="[^"]*"' "$OUT/ui.xml" | sed 's/text="//;s/"$//' | grep -v '^$' | tr '\n' '|'; }
# 시연 버튼(정차/출발/문 열기/문 닫기)은 모든 화면에서 같은 자리에 있다. 덤프가 될 때마다 좌표를 기억해 두었다가,
# 화면이 계속 움직여(지도 애니메이션) 덤프가 안 되는 단계에서는 기억한 좌표로 누른다.
declare -A BOUNDS
remember_bounds() {
  local label b
  for label in "정차" "출발" "문 열기" "문 닫기"; do
    b=$(grep -oE "text=\"$label\"[^>]*bounds=\"\[[0-9]+,[0-9]+\]\[[0-9]+,[0-9]+\]\"" "$OUT/ui.xml" | head -1 | grep -oE 'bounds="[^"]*"' | grep -oE '[0-9]+' | tr '
' ' ')
    [ -n "$b" ] && BOUNDS[$label]="$b"
  done
}
tap_text() { # tap_text "라벨"  (최대 4회 재시도, 덤프가 안 되면 기억한 좌표)
  local b="" try
  for try in 1 2 3 4; do
    if dump; then remember_bounds; elif [ -n "${BOUNDS[$1]:-}" ]; then b="${BOUNDS[$1]}"; echo "  .. dump failed (screen is animating) → remembered bounds for '$1'"; break; fi
    b=$(grep -oE "text=\"$1\"[^>]*bounds=\"\[[0-9]+,[0-9]+\]\[[0-9]+,[0-9]+\]\"" "$OUT/ui.xml" | head -1 | grep -oE 'bounds="[^"]*"' | grep -oE '[0-9]+' )
    [ -n "$b" ] && break
    echo "  .. '$1' not yet (try $try)"; sleep 2
  done
  if [ -z "$b" ]; then echo "  !! '$1' not found: $(texts)"; return 1; fi
  set -- $b; local x=$(( ($1+$3)/2 )) y=$(( ($2+$4)/2 ))
  adb shell input tap $x $y; echo "  tap @ $x,$y"
}
shot() {
  local dst="$OUTW\\$1.png"
  adb shell screencap -p -d $DISPLAY_ID /sdcard/s.png && adb pull /sdcard/s.png "$dst" >/dev/null 2>&1
  echo "  shot $1"
}
wait_text() { # wait_text "패턴" 최대초
  local i; for i in $(seq 1 "$2"); do dump; if texts | grep -qE "$1"; then echo "  saw '$1' after ${i}s"; return 0; fi; sleep 1; done
  echo "  !! timeout waiting '$1': $(texts)"; return 1
}

# 앱 로그(ground truth)에 패턴이 찍힐 때까지 대기. 주행 화면은 덤프가 안 되므로 주행 중 판정은 전부 이것으로 한다.
wait_log() { # wait_log "패턴" 최대초
  local i; for i in $(seq 1 "$2"); do
    adb logcat -d -s "MOAH/JourneyStateMachine:*" | grep -qE "$1" && { echo "  log '$1' after ${i}s"; return 0; }; sleep 1
  done
  echo "  !! timeout waiting log '$1'"; return 1
}
T0=$(date +%s); mark() { echo "  [t+$(( $(date +%s) - T0 ))s] $1"; }  # 시연 길이 측정용
FAIL=0

echo "== start"; adb shell am force-stop com.moah.hackathon; adb logcat -c
adb shell am start -n com.moah.hackathon/.ui.MainActivity >/dev/null; sleep 4
tap_text "배고픔"; sleep 1; tap_text "1시간"; sleep 1; tap_text "드라이브 시작"; sleep 3
# 시작 여부는 앱 로그(ground truth)로 확인한다. 주행 화면은 덤프가 안 되므로 화면 텍스트로 판단하면 안 된다.
for i in 1 2 3; do adb logcat -d -s "MOAH/JourneyStateMachine:*" | grep -q "leg 0" && { echo "  journey started (app log)"; break; }; echo "  retry start $i"; tap_text "드라이브 시작"; sleep 3; done
T0=$(date +%s); mark "journey started"
wait_log "order placed" 10 || FAIL=1; shot 10_driving_hidden_placed
echo "== leg0 → 운중동 (hidden pickup): 주문 칩은 화면 덤프가 안 되므로 로그로 전이를 확인하고 스크린샷만 남긴다"
wait_log "PLACED → PREPARING" 120 || FAIL=1; mark "order preparing"; sleep 1; shot 10b_driving_hidden_preparing
wait_log "PREPARING → READY" 120 || FAIL=1; mark "order ready"; sleep 1; shot 10c_driving_hidden_ready
wait_log "reveal 0/" 180 || exit 1; mark "reveal pickup"; sleep 1; shot 11_reveal_pickup
# 공개 화면은 남은 거리·지도가 계속 갱신돼 덤프가 안 될 수 있다 → 칩 검사는 덤프가 될 때만(안 되면 건너뛰고 표시)
if dump; then texts | grep -q "준비 완료" || { echo "  !! reveal has no order chip: $(texts)"; FAIL=1; }; else echo "  .. reveal screen is animating: chip check skipped (covered by concept_shots)"; fi
tap_text "정차"; sleep 2; tap_text "문 열기"; sleep 3; shot 12_arrival_pickup
wait_log "READY → PICKED_UP" 10 || FAIL=1; mark "arrival pickup"
now_texts | grep -q "픽업 완료" || { echo "  !! arrival has no picked-up chip: $(texts)"; FAIL=1; }
echo "  texts: $(texts)"
tap_text "다음 장소로"; sleep 3; shot 13_driving_announced; echo "  texts: $(now_texts)"
tap_text "문 닫기"; sleep 1; tap_text "출발"; sleep 2
echo "== leg1 → 현대백화점 (announced)"; wait_log "reveal 1/" 120 || exit 1; mark "reveal final"; sleep 1; shot 14_reveal_final
tap_text "정차"; sleep 2; tap_text "문 열기"; sleep 3; shot 15_arrival_final; mark "arrival final (= 시연 길이)"
now_texts | grep -q "다시 떠나기" || { echo "  !! final arrival not reached: $(texts)"; FAIL=1; }; echo "  texts: $(texts)"
echo "== logcat"; adb logcat -d -s "MOAH/JourneyStateMachine:*" | grep -v "beginning of" | cut -c20-200
echo "== uiautomator clashes (0 = clean single-instance run): $(adb logcat -d | grep -c "UiAutomationService.*already registered")"
echo "== result: $([ $FAIL = 0 ] && echo PASS || echo FAIL) (order 4단계·칩 표시)"
echo "== done"; exit $FAIL
