package com.moah.hackathon.scoring

import com.moah.hackathon.vehicle.Gear

/** 시각(ms, 세션 기준 단조 증가)이 붙은 관측값. 채점기는 전부 이 목록만 받는다 — 안드로이드·코루틴 의존 없음. */
data class Sample<T>(val tMillis: Long, val value: T)

typealias SpeedSample = Sample<Float>      // km/h, 크기만
typealias AngleSample = Sample<Float>      // degree, 양수 = 왼쪽
typealias GearSample = Sample<Gear>
typealias DistanceSample = Sample<Float>   // cm
typealias FlagSample = Sample<Boolean>
