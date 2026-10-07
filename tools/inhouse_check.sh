#!/usr/bin/env bash
# 사내 검증 스크립트 (2026-09-30 사내 이관 1차 작업 E · 10/1 사내 검증 #2 보강) — 사내 세션을 "pull → 빌드 → 설치 → 이 스크립트" 로 끝내기 위해. adb 만 쓴다.
#   준비: adb root(adbd 재시작 → wait-for-device) → Wi-Fi 켜고 AndroidWifi 접속(shell uid 로는 거부되므로 root 뒤) → logcat 버퍼 16M → 설치·실행
#   흐름: 시트에서 주차 → 후면 직각 주차 → 힌트 → 시작(제안 과제에 기대지 않는다 — 프로필·예약이 바뀌어도 같은 과제)
#         → 패널 못한 주차 → 다 됐어요 → 한 번 더 → 잘한 주차 → 다 됐어요 → 오늘은 여기까지
#   탭은 좌표 대신 uiautomator dump 에서 글자로 찾아 bounds 중심을 누른다(시연 패널이 화면마다 위치가 달라 좌표 탭이 뒤 버튼을 눌렀다).
#   패널은 단계가 바뀌면 닫힌다 → 누를 때마다 알약(우상단, content-desc "시연")을 다시 연다.
#   결과는 한 화면 요약으로만 — **캡처를 저장하지 않는다**(사내 캡처 반출 금지). FATAL 은 우리 프로세스(Process: com.moah.hackathon)만 센다.
# 사용:  bash tools/inhouse_check.sh [APK 경로]     (기본 automotive/build/outputs/apk/debug/automotive-debug.apk)
#        SKIP_INSTALL=1 이면 설치 생략, SKIP_WIFI=1 이면 Wi-Fi 단계 생략, SERIAL=<adb 시리얼> 로 기기 지정
#        REPO_ONLY=1 이면 "저장소" 단계(.gitignore 검사)만 하고 끝낸다 — 기기 없이, jar 복사 직후에
#        SKIP_COACH_TEXT=1 이면 마지막 "홈 코치 텍스트 대화" 단계를 건너뛴다
#   홈 코치 텍스트 대화(10/6, #198·#200): 리포트 → 다시 시작 → 준비실(DRIVE COACH 길게) "시뮬레이션 음성 입력 · 켬" → 홈 → 코치에게 말하기
#         → 영어 두 줄(에뮬 키보드에 한국어가 없어 adb input text — Copilot 은 영어도 알아듣고, Fake 키워드 규칙은 한국어만이라 되묻기로 답한다)
#         → 로그 `home coach: intent=… (ai|rule|fallback) N ms` 두 줄. 사내 목표: Copilot 로그인 뒤 (ai) 둘째 줄이 PIN_TASK(parking-parallel,…)
#   말 카드(10/6 밤, #211·#219): 준비실 "카드 + 글" → 코치와 대화 → 카드 두 장(한국어 — 키보드 없이) "오랜만이라 무서워요" → "평행 주차 힌트로 할래요"
#         → 다시 열어 영어 두 줄. intent 네 줄. 카드 둘째 줄은 Fake 규칙으로도 PIN_TASK(parking-parallel,HINT) 가 정상
set -u
APK="${1:-automotive/build/outputs/apk/debug/automotive-debug.apk}"
PKG=com.moah.hackathon
TASK="후면 직각 주차"; CATEGORY="주차"; MODE="힌트"
TAG="MOAH/LessonStateMachine:* MOAH/VehiclePortFactory:* MOAH/RealVehiclePort:* MOAH/HybridVehiclePort:* MOAH/CloudCoachPort:* MOAH/CopilotAuth:* AndroidRuntime:E *:S"
ADB_BIN="${ADB:-adb}"
adb() { if [ -n "${SERIAL:-}" ]; then command "$ADB_BIN" -s "$SERIAL" "$@" </dev/null; else command "$ADB_BIN" "$@" </dev/null; fi; }
TMP="${TMPDIR:-/tmp}/inhouse_check.$$"; mkdir -p "$TMP"; trap 'rm -rf "$TMP"' EXIT
# Windows Git Bash 에서는 adb.exe 에 Windows 경로를 줘야 한다(사내 Linux 는 그대로). MSYS 경로 변환도 끈다
export MSYS_NO_PATHCONV=1; TMPW=$(cygpath -w "$TMP" 2>/dev/null || echo "$TMP")
FAIL=0
note() { printf '  %s\n' "$*"; }
bad()  { printf '  !! %s\n' "$*"; FAIL=1; }

# ── 저장소(.gitignore) ── 사내 검증 #3(10/2): `automotive/libs/      # 주석` 처럼 뒤에 붙인 주석이 패턴이 돼 jar 가 `??` 로 보였다.
#   check-ignore -v 의 출처가 **.gitignore 자체**인지 본다(사내 .git/info/exclude 가 대신 막아 주면 PASS 로 속기 때문). 추적된 파일도 잡는다.
echo "== 저장소"
REPO=skip
if git rev-parse --is-inside-work-tree >/dev/null 2>&1; then
  REPO=ok
  for p in automotive/libs/x.jar local.properties; do
    src=$(git check-ignore -v --no-index "$p" 2>/dev/null | cut -f1 | cut -d: -f1)
    [ "$src" = ".gitignore" ] && note "gitignore: $p ← .gitignore" || { bad "gitignore: $p 가 .gitignore 에 안 걸림(출처: ${src:-없음}) — 커밋에 jar/local.properties 가 들어갈 수 있다"; REPO=fail; }
  done
  tracked=$(git ls-files -- automotive/libs local.properties 2>/dev/null | tr '\n' ' ')
  [ -z "$tracked" ] && note "추적된 jar/local.properties 없음" || { bad "이미 추적 중: $tracked — git rm --cached 로 빼고 커밋하지 말 것"; REPO=fail; }
  trailing=$(grep -nE '^[^#[:space:]].*[[:space:]]#' .gitignore 2>/dev/null | tr '\n' ' ')
  [ -z "$trailing" ] && note ".gitignore 뒤 글자 주석 없음" || { bad ".gitignore 에 뒤 글자 주석(패턴이 된다): $trailing"; REPO=fail; }
else
  note "git 작업 트리가 아님(zip 반입?) — .gitignore 검사 생략. 커밋은 clone 에서만"
fi
[ "${REPO_ONLY:-0}" = "1" ] && { [ "$REPO" = "fail" ] && { echo "== result: FAIL (저장소)"; exit 1; } || { echo "== result: PASS (저장소만)"; exit 0; }; }

# ── 준비 ──
echo "== 준비"
adb get-state >/dev/null 2>&1 || { echo "!! 기기 없음(adb devices)"; exit 4; }
if adb root >/dev/null 2>&1; then note "adb root"; adb wait-for-device >/dev/null 2>&1; else note "adb root 안 됨(계속)"; fi
if [ "${SKIP_WIFI:-0}" != "1" ]; then
  adb shell cmd wifi set-wifi-enabled enabled >/dev/null 2>&1 && note "wifi enabled" || bad "wifi enable 실패"
  adb shell cmd wifi connect-network AndroidWifi open >/dev/null 2>&1 && note "wifi AndroidWifi" || note "wifi connect 거부(에뮬이 아니면 무시)"
fi
adb logcat -G 16M >/dev/null 2>&1 && note "logcat 16M" || note "logcat -G 16M 실패 — 기본 버퍼로 계속(세션이 길면 앞부분 로그가 잘릴 수 있음)"
if [ "${SKIP_INSTALL:-0}" != "1" ]; then
  [ -f "$APK" ] || { echo "!! APK 없음: $APK"; exit 2; }
  r=$(adb install -r "$APK" 2>&1 | tail -1); note "install: $r"
fi
adb shell am force-stop $PKG >/dev/null 2>&1; adb logcat -c
# 관리자 프리셋(#187): Hybrid 기본 빌드는 장롱 10년차 프로필로 시작하고 첫 실행 프로필 질문을 건너뛴다. 순수 Real 은 준비실이 없어 무시된다
adb shell am start -n $PKG/.ui.MainActivity --es preset rear-two >/dev/null 2>&1; sleep 5

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
center() { # center "라벨" → "x y" (text 또는 content-desc) — 라벨은 글자 그대로("카드 + 글" 의 + 같은 정규식 문자를 이스케이프)
  local re b; re=$(printf '%s' "$1" | sed 's/[][\.*^$+?(){}|]/\\&/g')
  b=$(grep -oE "(text|content-desc)=\"$re\"[^>]*bounds=\"\[[0-9]+,[0-9]+\]\[[0-9]+,[0-9]+\]\"" "$TMP/ui.xml" | head -1 | grep -oE 'bounds="[^"]*"' | grep -oE '[0-9]+')
  [ -z "$b" ] && return 1; set -- $b; echo "$(( ($1+$3)/2 )) $(( ($2+$4)/2 ))"
}
press() { # press "라벨" ["다른 라벨"…] — 먼저 보이는 것을 누른다, 최대 3회 다시 본다
  local try l xy; for try in 1 2 3; do
    dump; for l in "$@"; do xy=$(center "$l") && { adb shell input tap $xy >/dev/null 2>&1; return 0; }; done; sleep 2
  done
  bad "'$1' 못 찾음: $(texts | cut -c1-200)"; return 1
}
panel() { dump; texts | grep -qE "못한 주차|잘한 주차" || { bad "아래 띠가 안 보임 — 준비실(홈 DRIVE COACH 약 2초 길게)에서 '세션 중 패널 · 아래 띠'"; return 1; }; }
# 과제 시트("제휴 시험장" 이 보이면 열린 것). 첫 탭이 먹지 않을 때가 있어 3회
open_sheet() { local i; for i in 1 2 3; do dump; texts | grep -q "제휴 시험장" && return 0; tap "과제·모드 바꾸기" || return 1; sleep 2; done; dump; texts | grep -q "제휴 시험장"; }
wait_log() { local i; for i in $(seq 1 "$2"); do adb logcat -d -s $TAG 2>/dev/null | grep -qE "$1" && return 0; sleep 1; done; bad "로그 대기 초과: $1"; return 1; }
now_ms() { date +%s%3N; }
elapsed_to() { # elapsed_to "패턴" 최대초 → 로그가 뜰 때까지 ms
  local t0=$(now_ms) i; for i in $(seq 1 $(( $2 * 4 ))); do adb logcat -d -s $TAG 2>/dev/null | grep -qE "$1" && { echo $(( $(now_ms) - t0 )); return 0; }; sleep 0.25; done; echo "-1"; return 1
}

# ── 흐름 ──
echo "== 흐름: 시트($CATEGORY → $TASK → $MODE) → 시작 → 못한 주차 → 다 됐어요 → 한 번 더 → 잘한 주차 → 다 됐어요 → 오늘은 여기까지"
# 첫 실행 프로필 질문 화면이면 과제 시트가 없다 — 안내하고 멈춘다(사내 피드백 #4, 10/6)
dump; if texts | grep -q "건너뛰기" && texts | grep -q "내 프로필"; then
  echo "!! 첫 실행 프로필 질문 화면이다 — 준비실 프리셋부터: 홈 'DRIVE COACH' 약 2초 길게 → '후면 주차 두 회차' → '이 설정으로 홈' 뒤 다시 실행"
  echo "   (순수 Real 빌드는 준비실이 없다 — 화면에서 '건너뛰기' 를 누르고 다시 실행)"
  exit 1
fi
open_sheet || { echo "!! 과제 시트가 열리지 않음"; exit 1; }
tap "$CATEGORY"; sleep 1; tap "$TASK"; sleep 1; tap "$MODE"; sleep 1; tap "시작"; sleep 1
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

# ── 홈 코치 텍스트 대화(10/6) — 관리자 준비실이 있는 빌드(Fake/Hybrid)만. 순수 Real 은 건너뛴다 ──
COACH_TEXT="skip"
if [ "${SKIP_COACH_TEXT:-0}" != "1" ]; then
  echo "== 홈 코치 대화: 다시 시작 → 준비실 음성 입력(카드 + 글) → 코치와 대화 → 말 카드 두 장 → 다시 열어 영어 두 줄"
  COACH_TEXT="fail"
  FAIL_BEFORE=$FAIL   # 이 단계에서 난 실패만 본다
  press "메인으로 →" "메인으로" "다시 시작 →" "다시 시작"; sleep 3   # 라운드 26: 결과 화면 `다시 시작` → `메인으로`(옛 태그도 찾는다)
  dump; xy=$(center "DRIVE COACH")
  if [ -z "$xy" ]; then note "DRIVE COACH 못 찾음 — 건너뜀"; COACH_TEXT="skip"
  else
    adb shell input swipe $xy $xy 3000 >/dev/null 2>&1; sleep 2; dump
    if ! texts | grep -q "시뮬레이션 음성 입력"; then note "준비실 없음(순수 Real 빌드) — 건너뜀"; COACH_TEXT="skip"
    else
      press "카드 + 글" "켬"; sleep 1; press "이 설정으로 홈 →" "이 설정으로 홈"; sleep 2
      press "코치와 대화" "코치에게 말하기"; sleep 2
      say_line() { # say_line "영어%s문장" 번호
        press "코치에게 글로 말해 보세요" || return 1; sleep 1
        adb shell input text "$1" >/dev/null 2>&1; sleep 1
        press "보내기 →" "보내기" || return 1
        local i n; for i in $(seq 1 12); do n=$(adb logcat -d -s $TAG 2>/dev/null | grep -c "home coach: intent="); [ "$n" -ge "$2" ] && return 0; sleep 1; done
        bad "home coach 응답 $2 없음(12 s)"; return 1
      }
      wait_intents() { local i n; for i in $(seq 1 12); do n=$(adb logcat -d -s $TAG 2>/dev/null | grep -c "home coach: intent="); [ "$n" -ge "$1" ] && return 0; sleep 1; done
        bad "home coach 응답 $1 없음(12 s)"; return 1; }
      CARDS=0
      # 말 카드(카드가 없는 옛 태그면 건너뛰고 영어 두 줄만 — 그때 기대 intent 수는 둘)
      dump; if texts | grep -q "오랜만이라 무서워요"; then
        CARDS=2
        press "오랜만이라 무서워요" && wait_intents 1 && sleep 2
        # 사외 피드백 #6(10/7): 첫 카드에 AI 가 곧바로 과제를 고르면 시트가 닫혀 둘째 카드(되물은 뒤에만 보임)가 없다.
        # 그 경우 의도는 PASS 로 세고(앱 버그 아님) 둘째 카드 없이 영어 두 줄로 간다. #224 부터는 앱이 첫 감정 말에 한 번 되묻는다.
        FIRST=$(adb logcat -d -s $TAG 2>/dev/null | grep -oE 'home coach: intent=[^ ]+' | tail -1 | sed 's/^.*intent=//')
        dump; if texts | grep -q "평행 주차 힌트로 할래요"; then
          press "평행 주차 힌트로 할래요" && wait_intents 2
        else
          CARDS=1; note "첫 카드에 바로 $FIRST — 시트가 닫혀 둘째 카드 없음(의도는 PASS 로 셈) · 영어 두 줄로 진행"
          case "$FIRST" in OPEN_SHEET*) adb shell input keyevent 4 >/dev/null 2>&1; sleep 1;; esac   # 열린 과제 시트를 닫는다
        fi
        sleep 3; press "코치와 대화" "코치에게 말하기"; sleep 2   # PIN_TASK 면 시트가 닫힌다 — 다시 연다
      else note "말 카드 없음(옛 태그) — 영어 두 줄만"; fi
      say_line "hi,%sI%sam%snervous%sabout%sdriving%sagain" $((CARDS + 1)) && say_line "I%swant%sto%spractice%sparallel%sparking%swith%shints" $((CARDS + 2))
      [ "$FAIL" = "$FAIL_BEFORE" ] && COACH_TEXT="ok"
      xy=$(dump && center "← 돌아가기") && adb shell input tap $xy >/dev/null 2>&1
    fi
  fi
fi

# ── 요약(한 화면) ──
LOG=$(adb logcat -d -s $TAG 2>/dev/null)
echo
echo "================ 사내 검증 요약 ================"
case "$REPO" in ok) echo "PASS 저장소 .gitignore(automotive/libs/·local.properties)";; fail) echo "FAIL 저장소 .gitignore — 위 '== 저장소' 줄 참고";; *) echo "-- 저장소: git 아님(검사 생략)";; esac
echo "$LOG" | grep -q "RealVehiclePort ready"           && echo "PASS RealVehiclePort ready"        || { echo "FAIL RealVehiclePort ready 없음(Fake 로 폴백? VehiclePortFactory 로그 확인 — 매니페스트 uses-library mobis.framework 가 있는지)"; FAIL=1; }
echo "$LOG" | grep -q "attempt 1 start"                  && echo "PASS attempt 1 start"              || { echo "FAIL attempt 1 start"; FAIL=1; }
echo "$LOG" | grep -q "attempt 2: skill="                && echo "PASS attempt 2 채점"                || { echo "FAIL attempt 2 채점"; FAIL=1; }
echo "$LOG" | grep -q "report: attempts=2"               && echo "PASS report"                       || { echo "FAIL report"; FAIL=1; }
echo "-- 배지(실신호 수): $(echo "$LOG" | grep -oE 'badge=AvailabilityBadge\([^)]*\)' | tail -1)"
echo "-- missing: $(echo "$LOG" | grep -oE 'attempt 1 start \(HINT\) missing=\[[^]]*\]' | head -1)"
echo "-- live/forced: $(echo "$LOG" | grep -E 'HybridVehiclePort: (live \+|setVSS rejected)' | sed 's/^.*HybridVehiclePort: //' | tr '\n' ';' | cut -c1-300)"
# FATAL 은 우리 프로세스만 — 사내 에뮬의 기본 키보드 앱(ai.umos.inputmethod) 크래시가 FAIL 로 잡혔다(10/1)
FATAL=$(echo "$LOG" | grep -cE "AndroidRuntime.*Process: $PKG,"); OTHER=$(( $(echo "$LOG" | grep -cE "FATAL EXCEPTION") - FATAL ))
[ "$FATAL" = "0" ] && echo "PASS FATAL 0 (우리 앱 · 다른 앱 FATAL $OTHER 은 무시)" || { echo "FAIL FATAL $FATAL (다른 앱 $OTHER)"; echo "$LOG" | grep -A3 "Process: $PKG," | head -8 | sed 's/^/     /'; FAIL=1; }
FB=$(echo "$LOG" | grep -cE "CloudCoachPort.*fallback"); echo "-- CloudCoachPort fallback: $FB (로그인 뒤 0 이 목표 · 로그인 전엔 회차 수만큼 나오는 게 정상)"
CP=$(echo "$LOG" | grep -E 'CopilotAuth' | tail -1 | sed 's/^.*CopilotAuth: //' | cut -c1-120)
echo "-- Copilot: ${CP:-로그 없음(CLOUD_COACH=false 또는 설정 파일 없음)}"
echo "-- 다 됐어요 → 채점 지연: 회차 1 ${D1} ms · 회차 2 ${D2} ms"
echo "-- hints: $(echo "$LOG" | grep -c 'hint: ') (못한 주차 3종 기대)"
case "$COACH_TEXT" in
  ok)   echo "PASS 홈 코치 대화(말 카드 + 영어 두 줄)"
        echo "$LOG" | grep -oE 'home coach: (say .*|intent=.*)' | sed 's/^/     /'
        echo "$LOG" | grep -oE 'home coach: card .*' | sed 's/^/     /'
        echo "     (ai = Copilot 응답 사용 · rule = 전송 계층 없음 · fallback = AI 응답 거절/시간 초과 → 키워드 규칙. 사내 목표: 로그인 뒤 전부 ai — 카드 둘째 · 영어 둘째가 PIN_TASK(parking-parallel,HINT))";;
  fail) echo "FAIL 홈 코치 텍스트 대화 — 위 '== 홈 코치' 줄 참고";;
  *)    echo "-- 홈 코치 텍스트 대화: 건너뜀(SKIP_COACH_TEXT=1 또는 준비실 없음)";;
esac
echo "================================================"
[ "$FAIL" = "0" ] && { echo "== result: PASS"; exit 0; } || { echo "== result: FAIL"; exit 1; }
