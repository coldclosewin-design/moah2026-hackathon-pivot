package com.moah.hackathon.feature.lesson

import com.moah.hackathon.ports.SpeechPriority
import com.moah.hackathon.scoring.HarshKind
import com.moah.hackathon.scoring.ParkingMetrics

data class Hint(val text: String, val priority: SpeechPriority)

/**
 * 힌트 모드: 조용히 있다가 **틀린 순간만** 말한다 (§3.2). 규칙 기반이라 지연이 없다.
 * 채점기가 이미 세고 있는 지표([ParkingMetrics])의 **증가분**을 보므로 채점과 힌트가 같은 사실을 말한다.
 * 같은 규칙은 [cooldownMillis] 안에서 한 번만.
 *
 * @param checklist 출발 전 점검 과제면 true — 주차 규칙 대신 점검 규칙(시동보다 벨트 먼저, 움직이지 않기)만 본다.
 */
class HintRules(private val cooldownMillis: Long = 5_000L, private val checklist: Boolean = false) {
    private var previous: ParkingMetrics? = null
    private val lastFiredAt = HashMap<String, Long>()

    fun reset() {
        previous = null
        lastFiredAt.clear()
    }

    fun evaluate(nowMillis: Long, metrics: ParkingMetrics?, snapshot: VehicleSnapshot): List<Hint> {
        val out = ArrayList<Hint>()
        val prev = previous
        if (metrics != null) {
            fun grew(now: Int?, before: Int?) = now != null && now > (before ?: 0)

            if (checklist) {
                val pd = metrics.preDrive
                // 시동이 켜지는 순간 벨트가 아직이면 — 순서가 바뀌었다. 한 번만(전이).
                val ignitionJustOn = pd.ignitionOnMillis != null && prev?.preDrive?.ignitionOnMillis == null
                if (ignitionJustOn && pd.beltOnMillis == null) {
                    fire(out, "order", nowMillis, "시동보다 안전벨트가 먼저예요. 지금 매 주세요.", SpeechPriority.URGENT)
                }
                // 7단계(9/28): 시동 순간의 도어·브레이크 — 한 번만(전이). 신호가 없으면(null) 말하지 않는다
                if (ignitionJustOn && pd.doorClosedBeforeIgnition == false) {
                    fire(out, "door", nowMillis, "문이 아직 열려 있어요. 닫고 시작해요.", SpeechPriority.NORMAL)
                }
                if (ignitionJustOn && pd.brakeBeforeIgnition == false) {
                    fire(out, "brake", nowMillis, "시동은 브레이크를 밟고 켜요.", SpeechPriority.NORMAL)
                }
                if (grew(metrics.motion.movingSegments, prev?.motion?.movingSegments)) {
                    fire(out, "moved", nowMillis, "아직 출발 전이에요. 차는 세운 채로 점검만 해요.", SpeechPriority.NORMAL)
                }
                previous = metrics
                return out
            }

            if (grew(metrics.proximity?.warnings, prev?.proximity?.warnings) || (snapshot.obstacleWarning == true && (snapshot.rearDistanceCm ?: Float.MAX_VALUE) < 40f)) {
                fire(out, "proximity", nowMillis, "뒤가 가까워요. 멈추세요.", SpeechPriority.URGENT)
            }
            if (grew(metrics.harshEvents.size, prev?.harshEvents?.size)) {
                // 급가속과 급제동은 다른 조언 — 마지막 사건의 종류로 고른다(감사 08 A3-06: 전부 "제동" 으로 말하던 것)
                val text = if (metrics.harshEvents.last().kind == HarshKind.ACCELERATION) "출발이 급했어요. 가속은 천천히 해요."
                    else "제동이 급했어요. 브레이크는 천천히 밟아요."
                fire(out, "harsh", nowMillis, text, SpeechPriority.URGENT)
            }
            if (metrics.preDrive.beltBeforeFirstMove == false && prev?.preDrive?.beltBeforeFirstMove != false) {
                fire(out, "belt", nowMillis, "안전벨트가 아직이에요.", SpeechPriority.URGENT)
            }
            val reversals = metrics.steering?.reversals
            if (grew(reversals, prev?.steering?.reversals) && (reversals ?: 0) > 1) {
                fire(out, "steering", nowMillis, "핸들을 조금 더 유지해 보세요. 되돌리는 횟수가 늘고 있어요.", SpeechPriority.NORMAL)
            }
            if (grew(metrics.gear?.reverseDriveShifts, prev?.gear?.reverseDriveShifts)) {
                fire(out, "gear", nowMillis, "전진으로 보정할 때는 핸들을 반대로 돌려 두세요.", SpeechPriority.NORMAL)
            }
            previous = metrics
        }
        return out
    }

    private fun fire(out: MutableList<Hint>, rule: String, now: Long, text: String, priority: SpeechPriority) {
        val last = lastFiredAt[rule]
        if (last != null && now - last < cooldownMillis) return
        lastFiredAt[rule] = now
        out += Hint(text, priority)
    }
}
