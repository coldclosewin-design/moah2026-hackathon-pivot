package com.moah.hackathon.data

import com.moah.hackathon.feature.lesson.CardNeed
import com.moah.hackathon.feature.lesson.CardStage
import com.moah.hackathon.feature.lesson.SpeechCard

/**
 * 홈 코치 시트의 **말 카드**(라운드 25 결정 7, 사내 검증 #5 — 사내 에뮬은 화면 키보드가 뜨지 않아 한국어 입력이 불가).
 * 누르면 그 문장이 운전자 말로 보내져 AI(사외는 키워드 규칙)가 답한다. STT 자리를 대신하는 시뮬레이션이다.
 * 첫 화면([CardStage.OPENING]) = 감정·인사, 코치가 한 번 되물은 뒤([CardStage.FOLLOW_UP]) = 과제·상황.
 * [CardNeed] 가 있는 카드는 예약·지난 기록이 있을 때만 보인다. 문구는 Codex 가 다듬어도 되지만 id 는 테스트·도구가 쓴다 —
 * 바꾸면 `SpeechCardsTest` 가 같은 의도로 가는지 다시 확인한다(숫자 없음 · 한 줄).
 */
object SpeechCards {
    val all: List<SpeechCard> = listOf(
        SpeechCard("long-time", "오랜만이라 무서워요", CardStage.OPENING),
        SpeechCard("near-miss", "어제 긁을 뻔했어요", CardStage.OPENING),
        SpeechCard("what-first", "뭐부터 하면 좋을까요?", CardStage.OPENING),
        SpeechCard("kid-school", "아이 등하원 때문에 배워요", CardStage.OPENING),
        SpeechCard("parallel-hint", "평행 주차 힌트로 할래요", CardStage.FOLLOW_UP),
        SpeechCard("rear-again", "후면 주차 다시 해 볼래요", CardStage.FOLLOW_UP),
        SpeechCard("mock-exam", "모의시험 볼래요", CardStage.FOLLOW_UP),
        SpeechCard("venue-practice", "예약한 시험장 연습할래요", CardStage.FOLLOW_UP, CardNeed.BOOKING),
        SpeechCard("continue-last", "지난번 이어서요", CardStage.FOLLOW_UP, CardNeed.LAST),
        SpeechCard("road", "도로 주행이 궁금해요", CardStage.FOLLOW_UP),
        SpeechCard("profile", "내 프로필 고칠래요", CardStage.FOLLOW_UP),
    )
}
