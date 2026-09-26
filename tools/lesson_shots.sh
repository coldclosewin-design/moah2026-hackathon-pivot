#!/usr/bin/env bash
# 세션을 돌리지 않고 고정 데이터로 5화면(Setup·Briefing·Maneuver·Done·Report)을 그려 계측 계약을 검사하고 캡처를 꺼낸다.
# 계측 클래스는 Codex 가 만든다: com.moah.hackathon.ui.LessonScreenInstrumentation (docs/handoffs/2026-09-26_codex_lesson_screens.md).
#   검사: (a) Maneuver 잠금 상태에서 클릭 가능한 노드 0개 (b) Maneuver 접근성 트리에 "점수"·"감점"·"N점" 없음 (c) 5화면 캡처 저장
# 먼저: .\gradlew.bat assembleDebug :automotive:assembleDebugAndroidTest   (PowerShell)
set -u
export MSYS_NO_PATHCONV=1
OUT="$1"; mkdir -p "$OUT"
ADB="${ADB:-$(cygpath -u "${ANDROID_HOME:-$LOCALAPPDATA/Android/Sdk}")/platform-tools/adb.exe}"
PKG=com.moah.hackathon
INSTR=$PKG.ui.LessonScreenInstrumentation
exec > >(tee -a "$OUT/log.txt") 2>&1

LOCK="$(dirname "$OUT")/emu_flow.lock"; PIDFILE="$(dirname "$OUT")/emu_flow.pid"
if ! mkdir "$LOCK" 2>/dev/null; then echo "!! another instance is running (pid $(cat "$PIDFILE" 2>/dev/null)). abort."; exit 3; fi
echo $$ > "$PIDFILE"
trap 'rmdir "$LOCK" 2>/dev/null; rm -f "$PIDFILE"' EXIT
if ! command timeout 20 "$ADB" -s emulator-5554 get-state </dev/null 2>/dev/null | grep -q device; then echo "!! no device. abort."; exit 4; fi
adb() { command timeout 120 "$ADB" "$@" </dev/null; }

for f in automotive/build/outputs/apk/debug/automotive-debug.apk automotive/build/outputs/apk/androidTest/debug/automotive-debug-androidTest.apk; do
  [ -f "$f" ] || { echo "!! missing $f (build first)"; exit 2; }
  r=$(adb install -r "$(cygpath -w "$f")" 2>&1 | tail -1); echo "install $(basename "$f"): $r"
done

# 이전 실행의 캡처를 지운다. 앱은 user 10 으로 돌므로 --user 10 이 없으면 엉뚱한(user 0) 폴더를 지우고 옛 파일이 딸려 나온다.
adb shell "run-as $PKG --user 10 sh -c 'rm -f files/lesson-*.png'"
LEFT=$(adb shell run-as $PKG --user 10 ls files 2>/dev/null | tr -d '\r' | grep -c '^lesson-.*\.png$')
[ "$LEFT" = "0" ] || { echo "!! could not clear old captures ($LEFT left). abort."; exit 5; }

RESULT=$(adb shell am instrument --user 10 -w $PKG.test/$INSTR 2>&1)
echo "$RESULT" | tail -20

for n in $(adb shell run-as $PKG --user 10 ls files 2>/dev/null | tr -d '\r' | grep '^lesson-.*\.png$'); do
  adb exec-out run-as $PKG --user 10 cat "files/$n" > "$OUT/$n"; echo "  pulled $n"
done
echo "$RESULT" > "$OUT/contract.txt"
if echo "$RESULT" | grep -q "Lesson contract passed"; then echo "== result: PASS"; exit 0; else echo "== result: FAIL"; exit 1; fi
