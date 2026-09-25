#!/usr/bin/env bash
# 디자인 컨셉 공통 계약 검사 + 비교 스크린샷 회수. 여정을 돌리지 않고 고정 데이터로 주행·도착 화면을 바로 그린다(약 30초/컨셉).
#   bash tools/concept_shots.sh <출력폴더> [컨셉이름]      # 이름 생략 = 등록된 컨셉 전부
# 먼저: .\gradlew.bat assembleDebug :automotive:assembleDebugAndroidTest
set -u
export MSYS_NO_PATHCONV=1
OUT="$1"; ONLY="${2:-}"; mkdir -p "$OUT"
ADB="${ADB:-$(cygpath -u "${ANDROID_HOME:-$LOCALAPPDATA/Android/Sdk}")/platform-tools/adb.exe}"
adb() { command timeout 180 "$ADB" -s emulator-5554 "$@" </dev/null; }
APK=automotive/build/outputs/apk
PKG=com.moah.hackathon

# emu_flow 와 같은 잠금을 쓴다: 둘이 겹치면 uiautomator 가 충돌한다 (tools/README.md)
LOCK="$(dirname "$OUT")/emu_flow.lock"
if ! mkdir "$LOCK" 2>/dev/null; then echo "!! another emulator script is running (lock: $LOCK). abort."; exit 3; fi
trap 'rmdir "$LOCK" 2>/dev/null' EXIT
adb get-state 2>/dev/null | grep -q device || { echo "!! no device. abort."; exit 4; }

for f in "$APK/debug/automotive-debug.apk" "$APK/androidTest/debug/automotive-debug-androidTest.apk"; do
  [ -f "$f" ] || { echo "!! missing $f (build first)"; exit 5; }
  r=$(adb install -r "$(cygpath -w "$f")" 2>&1 | tail -1); echo "install $(basename "$f"): $r"
  [ "$r" = "Success" ] || exit 6
done

# 이전 실행의 스크린샷을 지운다. 앱은 user 10 으로 돌므로 --user 10 이 없으면 엉뚱한(user 0) 폴더를 지우고
# 옛 파일이 남아 이번 실행 결과처럼 딸려 나온다 (2026-09-18: b-cinema 만 돌렸는데 a-oneword·classic 옛 파일이 같이 회수됨).
adb shell "run-as $PKG --user 10 sh -c 'rm -f files/concept-*.png'"
LEFT=$(adb shell run-as $PKG --user 10 ls files 2>/dev/null | tr -d '' | grep -c '^concept-.*\.png$')
[ "$LEFT" = "0" ] || { echo "!! $LEFT stale screenshot(s) could not be removed. abort."; exit 7; }
ARGS=(); [ -n "$ONLY" ] && ARGS=(-e concept "$ONLY")
RESULT=$(adb shell am instrument --user 10 -w "${ARGS[@]}" $PKG.test/$PKG.ui.ConceptContractInstrumentation 2>&1)
echo "$RESULT" | tee "$OUT/contract.txt"

# filesDir 은 앱 전용이라 run-as 로 꺼낸다. AAOS 는 앱이 user 10 → --user 10
for n in $(adb shell run-as $PKG --user 10 ls files 2>/dev/null | tr -d '\r' | grep '^concept-.*\.png$'); do
  adb exec-out run-as $PKG --user 10 cat "files/$n" > "$OUT/$n"
  [ -s "$OUT/$n" ] && echo "  shot $n" || echo "  !! empty $n"
done

if echo "$RESULT" | grep -q "Concept contract passed"; then echo "== result: PASS"; else echo "== result: FAIL"; exit 1; fi
