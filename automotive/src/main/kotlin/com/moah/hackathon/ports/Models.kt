package com.moah.hackathon.ports

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

data class LatLng(val lat: Double, val lng: Double)

/** 두 지점 간 대원 거리(m). */
fun LatLng.distanceTo(other: LatLng): Double {
    val r = 6_371_000.0
    val dLat = Math.toRadians(other.lat - lat)
    val dLng = Math.toRadians(other.lng - lng)
    val a = sin(dLat / 2) * sin(dLat / 2) +
        cos(Math.toRadians(lat)) * cos(Math.toRadians(other.lat)) * sin(dLng / 2) * sin(dLng / 2)
    return 2 * r * atan2(sqrt(a), sqrt(1 - a))
}

/**
 * 이름 뒤에 주격 조사를 붙인다: 받침이 있으면 "이", 없으면 "가" — "지훈이", "엄마가", "아버지가".
 * 코칭 문장에 사람·장소 이름을 끼워 넣을 때 조사를 문장에 박아 두면 "지훈가"가 된다.
 * 한글 음절로 끝나지 않으면(영문·숫자·빈 문자열) "가"를 붙인다.
 */
fun String.withSubjectParticle(): String = withParticle(closed = "이", open = "가")

/** 목적격 조사 — 받침이 있으면 "을", 없으면 "를". ("뒤 거리를", "기어 전환을") */
fun String.withObjectParticle(): String = withParticle(closed = "을", open = "를")

/** 보조사 — 받침이 있으면 "은", 없으면 "는". ("주차는", "점검은") */
fun String.withTopicParticle(): String = withParticle(closed = "은", open = "는")

/** 접속 조사 — 받침이 있으면 "과", 없으면 "와". ("핸들 방향과", "거리와") */
fun String.withAndParticle(): String = withParticle(closed = "과", open = "와")

/** 한글 음절로 끝나지 않으면(영문·숫자·빈 문자열) 받침 없는 쪽을 붙인다. 빈 문자열은 그대로. */
private fun String.withParticle(closed: String, open: String): String {
    val last = lastOrNull() ?: return this
    if (last !in '가'..'힣') return this + open
    val hasFinalConsonant = (last - '가') % 28 != 0
    return this + if (hasFinalConsonant) closed else open
}

/** "A과 B과 C를" — 나열의 마지막에 목적격 조사, 앞은 접속 조사. 비어 있으면 빈 문자열. */
fun List<String>.joinAsObjects(): String {
    if (isEmpty()) return ""
    val head = dropLast(1).map { it.withAndParticle() }
    return (head + last().withObjectParticle()).joinToString(" ")
}
