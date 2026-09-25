#!/usr/bin/env bash
# GpsLocationPort 검증: 에뮬에 geo fix 를 흘려 넣어 전체 여정(서초 → 운중동 픽업 → 현대백화점 판교점)을 GPS 로 달린다.
# 확인하는 것: GPS 갱신 시작(폴백 아님), 두 구간의 공개(남은 거리 기준), 공개 후 거리 갱신, 정차+도어 도착, 최종 도착의 실내 안내.
set -u
export MSYS_NO_PATHCONV=1
OUT="$1"; mkdir -p "$OUT"
OUTW=$(cygpath -w "$OUT")
ADB="${ADB:-$(cygpath -u "${ANDROID_HOME:-$LOCALAPPDATA/Android/Sdk}")/platform-tools/adb.exe}"
DISPLAY_ID=4619827259835644672
exec > >(tee -a "$OUT/log.txt") 2>&1

# 동시 실행 방지 (emu_flow.sh 와 같은 잠금을 공유: 에뮬은 하나뿐이다). 멈출 때: kill "$(cat <scratchpad>/emu_flow.pid)"
LOCK="$(dirname "$OUT")/emu_flow.lock"; PIDFILE="$(dirname "$OUT")/emu_flow.pid"
if ! mkdir "$LOCK" 2>/dev/null; then echo "!! another instance is running (pid $(cat "$PIDFILE" 2>/dev/null)). abort."; exit 3; fi
echo $$ > "$PIDFILE"
trap 'rmdir "$LOCK" 2>/dev/null; rm -f "$PIDFILE"' EXIT
if ! command timeout 20 "$ADB" -s emulator-5554 get-state </dev/null 2>/dev/null | grep -q device; then echo "!! no device. abort."; exit 4; fi

adb() { command timeout 60 "$ADB" "$@" </dev/null; }
# 주행 화면처럼 계속 갱신되는 화면에서는 uiautomator dump 가 실패하고 파일을 만들지 않는다.
# 낡은 파일을 읽지 않도록 매번 지우고, 실패를 명시한다.
dump() {
  local dst="$OUTW\\ui.xml"
  rm -f "$OUT/ui.xml"; adb shell rm -f /sdcard/ui.xml >/dev/null 2>&1
  adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1
  adb pull /sdcard/ui.xml "$dst" >/dev/null 2>&1
  [ -s "$OUT/ui.xml" ] || { echo "<DUMP_FAILED/>" > "$OUT/ui.xml"; return 1; }
}
now_texts() { dump; texts; }
texts() { grep -oE 'text="[^"]*"' "$OUT/ui.xml" | sed 's/text="//;s/"$//' | grep -v '^$' | tr '\n' '|'; }
# 라벨은 정확히 일치해야 한다. 접두사 매칭이던 때 "정차" 가 공개 화면의 안내 문구
# "정차 후 문을 열면 선물이 열려요" 에 먼저 걸려 시연 버튼을 누르지 못했다(2026-09-19).
# 시연 버튼은 모든 화면에서 같은 자리라, 덤프가 될 때 좌표를 기억했다가 안 될 때 그 좌표로 누른다(emu_flow.sh 와 같은 규칙).
declare -A BOUNDS
bounds_of() { grep -oE "text=\"$1\"[^>]*bounds=\"\[[0-9]+,[0-9]+\]\[[0-9]+,[0-9]+\]\"" "$OUT/ui.xml" | head -1 | grep -oE 'bounds="[^"]*"' | grep -oE '[0-9]+' | tr '\n' ' '; }
remember_bounds() { local label b; for label in "정차" "출발" "문 열기" "문 닫기"; do b=$(bounds_of "$label"); [ -n "$b" ] && BOUNDS[$label]="$b"; done; }
tap_text() {
  local b="" try
  for try in 1 2 3 4; do
    if dump; then remember_bounds; elif [ -n "${BOUNDS[$1]:-}" ]; then b="${BOUNDS[$1]}"; echo "  .. dump failed (screen is animating) → remembered bounds for '$1'"; break; fi
    b=$(bounds_of "$1"); [ -n "$b" ] && break
    echo "  .. '$1' not yet (try $try)"; sleep 2
  done
  if [ -z "$b" ]; then echo "  !! '$1' not found: $(texts)"; return 1; fi
  local label="$1"; set -- $b; local x=$(( ($1+$3)/2 )) y=$(( ($2+$4)/2 ))
  adb shell input tap $x $y; echo "  tap '$label' @ $x,$y"
}
shot() { local dst="$OUTW\\$1.png"; adb shell screencap -p -d $DISPLAY_ID /sdcard/s.png && adb pull /sdcard/s.png "$dst" >/dev/null 2>&1; echo "  shot $1"; }
geo() { adb emu geo fix "$1" "$2" >/dev/null 2>&1; }   # lng lat
# 주행·공개 화면은 덤프가 안 될 수 있으므로 진행 상황은 앱 로그(ground truth)로 본다
applog() { adb logcat -d -s "MOAH/JourneyStateMachine:*" "MOAH/GpsLocationPort:*" | grep -v "beginning of"; }
wait_log() { # wait_log "패턴" 최대초
  local i; for i in $(seq 1 "$2"); do applog | grep -qE "$1" && { echo "  log '$1' after ${i}s"; return 0; }; sleep 1; done
  echo "  !! timeout waiting log '$1'"; return 1
}
# feed <출발 lat> <출발 lng> <도착 lat> <도착 lng> <공개 로그 패턴>: 직선 보간 40개를 1.5초 간격으로 흘린다. 공개가 찍히면 남은 fix 도 끝까지 보낸다(공개 후 거리 갱신 확인용).
feed() {
  local n=40 i lat lng
  for i in $(seq 1 $n); do
    lat=$(awk -v a=$1 -v b=$3 -v i=$i -v n=$n 'BEGIN{printf "%.6f", a+(b-a)*i/n}')
    lng=$(awk -v a=$2 -v b=$4 -v i=$i -v n=$n 'BEGIN{printf "%.6f", a+(b-a)*i/n}')
    geo $lng $lat
    if [ $((i % 8)) -eq 0 ]; then echo "  step $i/$n fix=($lat,$lng) log: $(applog | tail -1 | cut -c20-160)"; fi
    sleep 1.5
  done
}
FAIL=0

# 서초 출발지 → 운중동 카페거리(첫 정거장, 숨김·픽업) → 현대백화점 판교점(공개, 실내 안내). 좌표는 SeedCatalog 와 같다.
O_LAT=37.4837; O_LNG=127.0324; S0_LAT=37.3940; S0_LNG=127.0680; S1_LAT=37.3935; S1_LNG=127.1120
echo "== setup: location on, permission, initial fix"
adb shell cmd location set-location-enabled true >/dev/null 2>&1
adb shell cmd location set-location-enabled --user 10 true >/dev/null 2>&1
adb shell settings put secure location_mode 3 >/dev/null 2>&1
adb shell pm grant --user 10 com.moah.hackathon android.permission.ACCESS_FINE_LOCATION
adb shell pm grant --user 10 com.moah.hackathon android.permission.ACCESS_COARSE_LOCATION
adb shell am force-stop com.moah.hackathon; adb logcat -c
geo $O_LNG $O_LAT; sleep 1; geo $O_LNG $O_LAT
adb shell am start -n com.moah.hackathon/.ui.MainActivity >/dev/null; sleep 4
dump; texts | grep -q "While using the app" && { tap_text "While using the app"; sleep 2; }
tap_text "배고픔"; sleep 1; tap_text "1시간"; sleep 1; tap_text "드라이브 시작"; sleep 3
for i in 1 2 3; do applog | grep -q "leg 0" && { echo "  journey started (app log)"; break; }; echo "  retry start $i"; tap_text "드라이브 시작"; sleep 3; done
# 이 줄이 없으면 GPS 가 아니라 Fake 폴백으로 달린 것이다(Fake 빌드를 설치했거나 권한·제공자 문제) → 이 실행은 GPS 검증이 아니다
wait_log "GPS updates started" 10 || { echo "  !! not running on GPS (fake-location build installed? permission?)"; FAIL=1; }

echo "== leg0 → 운중동 (hidden pickup), 40 fixes"
feed $O_LAT $O_LNG $S0_LAT $S0_LNG
wait_log "reveal 0/" 10 || FAIL=1; shot 20_gps_reveal_pickup
if dump; then echo "  reveal texts: $(texts)"; else echo "  .. reveal screen is animating: texts skipped"; fi
tap_text "정차"; sleep 2; tap_text "문 열기"; sleep 3
wait_log "arrival 0/" 10 || FAIL=1; shot 21_gps_arrival_pickup; echo "  arrival texts: $(now_texts)"

echo "== leg1 → 현대백화점 (announced), 40 fixes"
tap_text "다음 장소로"; sleep 3; tap_text "문 닫기"; sleep 1; tap_text "출발"; sleep 2
wait_log "leg 1/" 10 || FAIL=1
# 두 번째 구간도 GPS 구독을 새로 시작해야 한다(구간마다 observeRemaining 을 다시 부른다)
[ "$(applog | grep -c "GPS updates started")" -ge 2 ] || { echo "  !! leg1 did not restart GPS updates"; FAIL=1; }
feed $S0_LAT $S0_LNG $S1_LAT $S1_LNG
wait_log "reveal 1/" 10 || FAIL=1; shot 22_gps_reveal_final
tap_text "정차"; sleep 2; tap_text "문 열기"; sleep 3
wait_log "arrival 1/" 10 || FAIL=1; shot 23_gps_arrival_final
now_texts | grep -q "다시 떠나기" || { echo "  !! final arrival not reached: $(texts)"; FAIL=1; }; echo "  final texts: $(texts)"
if tap_text "가는 길"; then sleep 9; shot 24_gps_indoor; else echo "  !! no indoor guide button"; FAIL=1; fi

echo "== logcat"; adb logcat -d -s "MOAH/GpsLocationPort:*" "MOAH/JourneyStateMachine:*" "MOAH/MainActivity:*" "AndroidRuntime:E" | grep -v "beginning of" | cut -c20-200
echo "== uiautomator clashes (0 = clean single-instance run): $(adb logcat -d | grep -c "UiAutomationService.*already registered")"
echo "== result: $([ $FAIL = 0 ] && echo PASS || echo FAIL) (GPS 구독 2회·공개 2회·도착 2회·실내 안내)"
echo "== done"; exit $FAIL
