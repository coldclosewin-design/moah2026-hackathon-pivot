package com.moah.hackathon.vehicle

import mobis.vss.VssConstants

/**
 * 장내기능 "장치 조작" 확인용 차량 키 — 전조등(하향등)·앞 와이퍼.
 * 2026-10-06 사내 피드백 #4: 두 경로가 사내 jar 의 `VssConstants` 에 같은 이름으로 있어 `SimOnlySignals` 에서 옮겼다.
 * Hybrid 에서는 실물이 값을 주면 live, 안 주면 Fake 가 채운다(배지가 말한다). 와이퍼 값의 사내 문자열 포맷은 미확인(INTEGRATION B 10/6).
 */
object DeviceSignals {
    val KEYS: Set<String> = setOf(
        VssConstants.VEHICLE_BODY_LIGHTS_BEAM_LOW_ISON,
        VssConstants.VEHICLE_BODY_WINDSHIELD_FRONT_WIPING_MODE,
    )
}

/** 와이퍼 모드 → 작동 중인가. "OFF" 만 false, 모르는 값은 null. */
fun String?.toWiperOn(): Boolean? = when (this?.trim()?.uppercase()) {
    null, "" -> null
    "OFF" -> false
    "SLOW", "MEDIUM", "FAST", "INTERVAL", "RAINSENSOR" -> true
    else -> null
}
