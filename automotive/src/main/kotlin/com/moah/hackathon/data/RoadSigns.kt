package com.moah.hackathon.data

import com.moah.hackathon.feature.lesson.Difficulty
import com.moah.hackathon.feature.lesson.QuizItem
import com.moah.hackathon.feature.lesson.RoadFigure
import com.moah.hackathon.feature.lesson.Task
import com.moah.hackathon.feature.lesson.TaskStatus
import com.moah.hackathon.feature.lesson.TaskType

/**
 * 점검 분류의 **도로 표시 읽기**(라운드 25 결정 5 H — 시안 `docs/design/round25-proposals/5-road-knowledge.html` H).
 * 출발 전에 바닥 표시를 그림으로 보고 고르는 정차 중 퀴즈. 문항마다 [RoadFigure] 하나 — 화면(Codex)이 도식으로 그린다.
 * 법 내용은 `RoadKnowledge` 주석과 같은 확인 범위(10/6) 안에서만. 운전자 문장 숫자 없음. 문구는 Codex 가 다듬어도 된다.
 */
object RoadSigns {
    const val TASK_ROAD_SIGNS = "checklist-road-signs"

    val task: Task = Task(TASK_ROAD_SIGNS, "도로 표시 읽기", TaskType.CHECKLIST, Difficulty.EASY,
        "출발 전에 바닥 화살표와 선 그림을 보고 무슨 뜻인지 골라요.", listOf("노면 화살표", "선 색"), emptySet(),
        requiresDriving = false, status = TaskStatus.READY, quizOnly = true)

    val quiz: List<QuizItem> = listOf(
        QuizItem("sign-left-arrow", "이 화살표가 그려진 차로에서 갈 수 있는 방향은?",
            listOf("좌회전만", "직진만", "직진과 좌회전"), 0,
            "좌회전 화살표만 있으면 그 차로는 좌회전만 해요. 직진하려면 미리 옮겨요.", RoadFigure.LEFT_ARROW),
        QuizItem("sign-straight-left", "이 화살표가 그려진 차로에서 갈 수 있는 방향은?",
            listOf("좌회전만", "직진과 좌회전", "우회전만"), 1,
            "화살표가 둘로 갈라지면 두 방향 다 갈 수 있어요. 직진도 좌회전도 이 차로에서 해요.", RoadFigure.STRAIGHT_LEFT_ARROW),
        QuizItem("sign-white-solid", "같은 방향 차로 사이의 이 선을 넘어 차로를 바꿔도 될까요?",
            listOf("바꾸지 않아요", "천천히 바꿔요", "방향지시등만 켜면 돼요"), 0,
            "흰색 실선은 차로를 바꾸지 말라는 선이에요. 점선이 나올 때 바꿔요.", RoadFigure.WHITE_SOLID),
        QuizItem("sign-yellow-solid", "가운데 이 노란 실선은?",
            listOf("반대 방향과 나누는 중앙선 — 넘지 않아요", "앞지르기할 때 넘어도 돼요", "주차해도 되는 선이에요"), 0,
            "노란 선은 반대 방향과 나누는 중앙선이에요. 실선이면 넘지 않아요.", RoadFigure.YELLOW_SOLID_CENTER),
        QuizItem("sign-zigzag", "차로 양옆의 이 지그재그 선은?",
            listOf("서행 표시 — 속도를 줄여요", "주차 구역 표시", "차로가 늘어난다는 표시"), 0,
            "지그재그 선은 서행 표시예요. 보통 앞에 횡단보도가 있으니 속도를 줄이고 보행자를 살펴요.", RoadFigure.ZIGZAG),
        QuizItem("sign-blue-lane", "이 파란 선으로 나뉜 차로는?",
            listOf("버스전용차로", "자전거 도로", "앞지르기 차로"), 0,
            "파란 선은 버스전용차로예요. 일반 차는 다니지 않아요.", RoadFigure.BLUE_BUS_LANE),
    )
}
