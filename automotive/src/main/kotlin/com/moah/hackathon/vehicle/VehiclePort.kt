package com.moah.hackathon.vehicle

import kotlinx.coroutines.flow.Flow

/**
 * 앱이 차량 데이터에 접근하는 **유일한** 계약.
 *
 * - 키는 VSS dot 경로 문자열이며 반드시 [mobis.vss.VssConstants] 상수를 쓴다.
 * - 값은 VSS 규약대로 항상 String 이다. 파싱은 [VssValues] 확장 함수로 한다.
 * - 구현체: [FakeVehiclePort] (외부 실행/시연), [RealVehiclePort] (사내, VSSManager 어댑터).
 *   선택은 [VehiclePortFactory] 한 곳에서만 한다.
 */
interface VehiclePort {

    /** 현재 값 조회. 알 수 없는 키는 결과에서 빠진다. 블로킹 없이 호출 가능(suspend). */
    suspend fun get(keys: List<String>): Map<String, String>

    /** 값 쓰기(actuator). 실패한 nodePath 목록을 반환하며 빈 리스트 = 전체 성공. */
    suspend fun set(values: Map<String, String>): List<String>

    /**
     * 변경 구독. 첫 emit 은 현재 스냅샷, 이후 변경분(변경된 키만 포함)이 흐른다.
     * collect 취소 시 구독이 해제된다. 화면/뷰모델 수명과 함께 반드시 취소할 것.
     */
    fun observe(keys: List<String>): Flow<Map<String, String>>

    /** 모든 구독·백그라운드 자원 해제. Application 종료 또는 포트 교체 시 호출. */
    fun dispose()
}
