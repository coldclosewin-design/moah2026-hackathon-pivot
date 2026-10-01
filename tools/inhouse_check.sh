#!/usr/bin/env bash
# 사내 검증 스크립트 (2026-09-30 사내 이관 1차 작업 E) — 사내 세션을 "pull → 빌드 → 설치 → 이 스크립트" 로 끝내기 위해. adb 만 쓴다.
#   준비: adb root → Wi-Fi 켜고 AndroidWifi 접속(shell uid 로는 거부되므로 root 뒤) → logcat 버퍼 16M → 설치·실행
#   흐름: 시작 → 패널 못한 주차 → 대기 → 다 됐어요 → 한 번 더 → 잘한 주차 → 다 됐어요 → 오늘은 여기까지
#   탭은 좌표 대신 uiautomator dump 에서 글자로 찾아 bounds 중심을 누른다(시연 패널이 화면마다 위치가 달라 좌표 탭이 뒤 버튼을 눌렀다).
#   패널은 단계가 바뀌면 닫힌다 → 누를 때마다 알약(우상단, content-desc "시연")을 다시 연다.
#   결과는 한 화면 요약으로만 — **캡처를 저장하지 않는다**(사내 캡처 반출 금지).
# 사용:  bash tools/inhouse_check.sh [APK 경로]     (기본 automotive/build/outputs/apk/debug/automotive-debug.apk)
#        SKIP_INSTALL=1 이면 설치 생략, SKIP_WIFI=1 이면 Wi-Fi 단계 생략, SERIAL=<adb 시리얼> 로 기기 지정
set -u
APK="${1:-automotive/build/outputs/apk/debug/automotive-debug.apk}"
PKG=com.moah.hackathon
TAG="MOAH/LessonStateMachine:* MOAH/VehiclePortFactory:* MOAH/RealVehiclePort:* MOAH/HybridVehiclePort:* MOAH/CloudCoachPort:* MOAH/CopilotAuth:* AndroidRuntime:E *:S"
ADB_BIN="${ADB:-adb}"
adb() { if [ -n "${SERIAL:-}" ]; then command "$ADB_BIN" -s "$SERIAL" "$@" </dev/null; else command "$ADB_BIN" "$@" </dev/null; fi; }
TMP="${TMPDIR:-/tmp}/inhouse_check.$$"; mkdir -p "$TMP"; trap 'rm -rf "$TMP"' EXIT
# Windows Git Bash 에서는 adb.exe 에 Windows 경로를 줘야 한다(사내 Linux 는 그대로). MSYS 경로 변환도 끈다
export MSYS_NO_PATHCONV=1; TMPW=$(cygpath -w "$TMP" 2>/dev/null || echo "$TMP")
FAIL=0
note() { printf '  %s\n' "$*"; }
bad()  { printf '  !! %s\n' "$*"; FAIL=1; }

# ── 준비 ──
echo "== 준비"
adb get-state >/dev/null 2>&1 || { echo "!! 기기 없음(adb devices)"; exit 4; }
adb root >/dev/null 2>&1 && note "adb root" || note "adb root 안 됨(계속)"
sleep 2
if [ "${SKIP_WIFI:-0}" != "1" ]; then
  adb shell cmd wifi set-wifi-enabled enabled >/dev/null 2>&1 && note "wifi enabled" || bad "wifi enable 실패"
  adb shell cmd wifi connect-network AndroidWifi open >/dev/null 2>&1 && note "wifi AndroidWifi" || note "wifi connect 거부(에뮬이 아니면 무시)"
fi
adb logcat -G 16M >/dev/null 2>&1 && note "logcat 16M"
if [ "${SKIP_INSTALL:-0}" != "1" ]; then
  [ -f "$APK" ] || { echo "!! APK 없음: $APK"; exit 2; }
  r=$(adb install -r "$APK" 2>&1 | tail -1); note "install: $r"
fi
adb shell am force-stop $PKG >/dev/null 2>&1; adb logcat -c
adb shell am start -n $PKG/.ui.MainActivity >/dev/null 2>&1; sleep 5

# ── 화면 도우미(글자로 찾아 누른다) ──
dump() { rm -f "$TMP/ui.xml"; adb shell rm -f /sdcard/ui.xml >/dev/null 2>&1; adb shell uiautomator dump /sdcard/ui.xml >/dev/null 2>&1; adb pull /sdcard/ui.xml "$TMPW/ui.xml" >/dev/null 2>&1; [ -s "$TMP/ui.xml" ]; }
texts() { grep -oE 'text="[^"]*"' "$TMP/ui.xml" 2>/dev/null | sed 's/text="//;s/"$//' | grep -v '^$' | tr '\n' '|'; }
tap() { # tap "라벨"  — text 또는 content-desc, 최대 4회 재시도
  local b="" try; for try in 1 2 3 4; do
    dump && b=$(grep -oE "(text|content-desc)=\"$1\"[^>]*bounds=\"\[[0-9]+,[0-9]+\]\[[0-9]+,[0-9]+\]\"" "$TMP/ui.xml" | head -1 | grep -oE 'bounds="[^"]*"' | grep -oE '[0-9]+')
    [ -n "$b" ] && break; sleep 2
  done
  [ -z "$b" ] && { bad "'$1' 못 찾음: $(texts | cut -c1-200)"; return 1; }
  set -- $b; adb shell input tap $(( ($1+$3)/2 )) $(( ($2+$4)/2 )) >/dev/null 2>&1; return 0
}
panel() { dump; texts | grep -qE "못한 주차|잘한 주차" || { tap "시연" || return 1; sleep 1; }; }
wait_log() { local i; for i in $(seq 1 "$2"); do adb logcat -d -s $TAG 2>/dev/null | grep -qE "$1" && return 0; sleep 1; done; bad "로그 대기 초과: $1"; return 1; }
now_ms() { date +%s%3N; }
elapsed_to() { # elapsed_to "패턴" 최대초 → 로그가 뜰 때까지 ms
  local t0=$(now_ms) i; for i in $(seq 1 $(( $2 * 4 ))); do adb logcat -d -s $TAG 2>/dev/null | grep -qE "$1" && { echo $(( $(now_ms) - t0 )); return 0; }; sleep 0.25; done; echo "-1"; return 1
}

# ── 흐름 ──
echo "== 흐름: 시작 → 못한 주차 → 다 됐어요 → 한 번 더 → 잘한 주차 → 다 됐어요 → 오늘은 여기까지"
dump; texts | grep -q "힌트" || { tap "과제·모드 바꾸기"; sleep 1; }
tap "힌트"; sleep 1; tap "시작"; sleep 1
wait_log "begin parking-rear-perpendicular HINT" 10 || { echo "!! 세션이 시작되지 않음"; adb logcat -d -s $TAG | tail -20; exit 1; }
wait_log "attempt 1 start" 15
panel && tap "못한 주차"
wait_log "asked done \(attempt 1" 90
sleep 1; tap "다 됐어요"; D1=$(elapsed_to "attempt 1: skill=" 15)
wait_log "attempt 1: skill=" 5
sleep 4; tap "한 번 더"; sleep 2
wait_log "attempt 2 start" 15
panel && tap "잘한 주차"
wait_log "asked done \(attempt 2" 90
sleep 1; tap "다 됐어요"; D2=$(elapsed_to "attempt 2: skill=" 15)
wait_log "attempt 2: skill=" 5
sleep 4; tap "오늘은 여기까지"; sleep 3
wait_log "report: attempts=2" 15

# ── 요약(한 화면) ──
LOG=$(adb logcat -d -s $TAG 2>/dev/null)
echo
echo "================ 사내 검증 요약 ================"
g() { echo "$LOG" | grep -E "$1" | sed 's/^.*MOAH\///' | cut -c1-160; }
echo "$LOG" | grep -q "RealVehiclePort ready"            && echo "PASS RealVehiclePort ready"        || { echo "FAIL RealVehiclePort ready 없음(Fake 로 폴백? VehiclePortFactory 로그 확인)"; FAIL=1; }
echo "$LOG" | grep -q "attempt 1 start"                  && echo "PASS attempt 1 start"              || { echo "FAIL attempt 1 start"; FAIL=1; }
echo "$LOG" | grep -q "attempt 2: skill="                && echo "PASS attempt 2 채점"                || { echo "FAIL attempt 2 채점"; FAIL=1; }
echo "$LOG" | grep -q "report: attempts=2"               && echo "PASS report"                       || { echo "FAIL report"; FAIL=1; }
echo "-- 배지(실신호 수): $(echo "$LOG" | grep -oE 'badge=AvailabilityBadge\([^)]*\)' | tail -1)"
echo "-- missing: $(echo "$LOG" | grep -oE 'attempt 1 start \(HINT\) missing=\[[^]]*\]' | head -1)"
echo "-- live/forced: $(echo "$LOG" | grep -E 'HybridVehiclePort: (live \+|setVSS rejected)' | sed 's/^.*HybridVehiclePort: //' | tr '\n' ';' | cut -c1-300)"
FATAL=$(echo "$LOG" | grep -cE "FATAL EXCEPTION.*|AndroidRuntime.*$PKG"); [ "$FATAL" = "0" ] && echo "PASS FATAL 0" || { echo "FAIL FATAL $FATAL"; FAIL=1; }
FB=$(echo "$LOG" | grep -cE "CloudCoachPort.*fallback"); echo "-- CloudCoachPort fallback: $FB (0 이 목표)"
echo "-- Copilot: $(echo "$LOG" | grep -E 'CopilotAuth' | tail -1 | sed 's/^.*CopilotAuth: //' | cut -c1-120)"
echo "-- 다 됐어요 → 채점 지연: 회차 1 ${D1} ms · 회차 2 ${D2} ms"
echo "-- hints: $(echo "$LOG" | grep -c 'hint: ') (못한 주차 3종 기대)"
echo "================================================"
[ "$FAIL" = "0" ] && { echo "== result: PASS"; exit 0; } || { echo "== result: FAIL"; exit 1; }
