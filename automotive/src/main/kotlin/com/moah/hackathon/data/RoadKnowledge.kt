package com.moah.hackathon.data

import com.moah.hackathon.feature.lesson.Difficulty
import com.moah.hackathon.feature.lesson.QuizItem
import com.moah.hackathon.feature.lesson.Task
import com.moah.hackathon.feature.lesson.TaskStatus
import com.moah.hackathon.feature.lesson.TaskType

/**
 * 알고도 헷갈리는 도로 위 상식(라운드 25 결정 5 — 시안 `docs/design/round25-proposals/5-road-knowledge.html` A 주제 과제 + C′ 헷갈리는 상식).
 * 지식 분류 아래 과제 다섯: 주제 넷(노면 표시·차선 · 신호와 우회전 · 교차로 우선 · 운전 매너) + 어려운 묶음(헷갈리는 상식, 맞아요/아니에요).
 *
 * **법 내용은 확인한 것만 문항으로 썼다**(10/6 확인 — 근거는 아래 주석, 운전자 문장에는 조항·연도 숫자를 넣지 않는다):
 *  - 우회전: 우회전 신호등이 있으면 녹색 화살표일 때만, 없으면 앞 차량 신호가 적색일 때 정지선·횡단보도 앞에서 일시정지 후(도로교통법 시행규칙, 2023-01-22 시행).
 *    신호에 맞춰 돌아도 횡단보도에 보행자가 건너거나 건너려 하면 일시정지(2022-07 개정).
 *  - 점멸: 적색 점멸 = 일시정지 후 주의하며 진행, 황색 점멸 = 주의하며 진행(시행규칙 별표 2).
 *  - 교통정리가 없는 교차로(도로교통법 제26조): 이미 들어간 차 우선, 동시에 들어가면 오른쪽 도로의 차 우선, 좌회전 차는 직진·우회전 차에 양보.
 *  - 비보호 좌회전: 녹색 신호에서 마주 오는 차·보행자를 방해하지 않을 때만, 적색에선 신호 위반.
 *  - 차선: 백색 = 같은 방향 차로(실선 = 변경 금지, 점선 = 허용), 황색 = 중앙선(점선은 앞지르기 때만 넘을 수 있음), 청색 = 버스전용차로.
 *  - 지그재그 = 서행 표시(주로 횡단보도 앞) — 그 이상(차로 변경 금지 효과 등)은 확인하지 못해 문항에 넣지 않았다.
 *  - 황색 신호: 이미 교차로에 들어갔으면 신속히 빠져나간다. 경찰관 신호·지시가 신호기와 다르면 경찰관을 따른다(제5조).
 *  - 비상등 감사 표시·지퍼 합류·주차장 기다리기는 **법이 아니라 관행** — 문장에 그렇게 밝힌다.
 * 문구는 Codex 가 다듬어도 되지만 위 사실은 바꾸지 않는다. 운전자 문장 숫자 없음.
 */
object RoadKnowledge {
    const val TASK_MARKING = "knowledge-marking"
    const val TASK_TURN = "knowledge-turn-signal"
    const val TASK_PRIORITY = "knowledge-priority"
    const val TASK_MANNER = "knowledge-manner"
    const val TASK_TRICKY = "knowledge-tricky"

    private fun task(id: String, title: String, difficulty: Difficulty, summary: String, watch: List<String>) =
        Task(id, title, TaskType.KNOWLEDGE, difficulty, summary, watch, emptySet(), requiresDriving = false, status = TaskStatus.READY)

    /** 지식 분류에 기존 과제 뒤로 붙는다(시트 카드 순서). */
    val tasks: List<Task> = listOf(
        task(TASK_MARKING, "노면 표시·차선", Difficulty.EASY, "화살표·선 색·지그재그처럼 도로 바닥이 알려 주는 것을 읽어요.", listOf("노면 화살표", "차선 색")),
        task(TASK_TURN, "신호와 우회전", Difficulty.MEDIUM, "빨간불 우회전, 우회전 신호등, 깜빡이는 신호에서 언제 서고 가는지 정리해요.", listOf("우회전", "점멸 신호")),
        task(TASK_PRIORITY, "교차로 우선", Difficulty.MEDIUM, "신호 없는 교차로와 비보호 좌회전에서 누가 먼저인지 익혀요.", listOf("양보", "비보호 좌회전")),
        task(TASK_MANNER, "운전 매너", Difficulty.EASY, "법에는 없지만 도로에서 서로 편한 관행을 알아 둬요.", listOf("감사 표시", "합류")),
        task(TASK_TRICKY, "헷갈리는 상식", Difficulty.HARD, "알고도 틀리기 쉬운 것만 모았어요. 맞아요, 아니에요로 빠르게 확인해요.", listOf("황색 신호", "경찰관 신호")),
    )

    private val YES_NO = listOf("맞아요", "아니에요")

    val quiz: Map<String, List<QuizItem>> = mapOf(
        TASK_MARKING to listOf(
            QuizItem("marking-left-arrow", "좌회전 화살표만 그려진 차로에서 직진해도 되나요?",
                listOf("앞이 비어 있으면 돼요", "안 돼요, 그 차로는 좌회전만", "초록불이면 돼요"), 1,
                "바닥 화살표는 그 차로에서 갈 수 있는 방향이에요. 직진하려면 미리 직진 표시가 있는 차로로 옮겨요."),
            QuizItem("marking-white-solid", "흰색 실선으로 나뉜 차로에서는?",
                listOf("차로를 바꾸지 않아요", "천천히 바꾸면 돼요", "방향지시등만 켜면 돼요"), 0,
                "흰색은 같은 방향 차로를 나누는 선이에요. 점선이면 바꿀 수 있고, 실선이면 바꾸지 않아요."),
            QuizItem("marking-zigzag", "차로 양옆에 지그재그 모양 선이 보이면?",
                listOf("주차해도 되는 곳이에요", "속도를 줄이라는 서행 표시예요", "차로가 하나 늘어나요"), 1,
                "지그재그 선은 서행 표시예요. 보통 앞에 횡단보도가 있으니 속도를 줄이고 보행자를 살펴요."),
            QuizItem("marking-blue-lane", "파란 선으로 나뉜 차로는?",
                listOf("버스전용차로", "자전거 도로", "앞지르기 차로"), 0,
                "파란 선은 버스전용차로예요. 일반 차는 다니지 않고, 점선 구간에서만 우회전처럼 잠깐 지날 수 있어요."),
        ),
        TASK_TURN to listOf(
            QuizItem("turn-red-right", "우회전 신호등이 없는 교차로, 앞 차량 신호가 빨간불이면 우회전은?",
                listOf("그대로 천천히 돌아요", "정지선 앞에서 일단 멈춘 뒤 돌아요", "초록불까지 기다려요"), 1,
                "앞 신호가 빨간불이면 정지선이나 횡단보도 앞에서 먼저 완전히 멈춰요. 보행자가 없는 것을 보고 천천히 돌아요."),
            QuizItem("turn-right-arrow", "우회전 신호등이 있는 교차로에서 우회전은 언제?",
                listOf("앞 신호가 초록이면", "우회전 신호가 초록 화살표일 때만", "언제든 천천히"), 1,
                "우회전 신호등이 있으면 그 신호를 따라요. 초록 화살표일 때만 돌아요."),
            QuizItem("turn-flashing-red", "빨간불이 깜빡이는 신호에서는?",
                listOf("서지 않고 천천히 지나가요", "일단 멈춘 뒤 주위를 보고 지나가요", "초록불이 될 때까지 기다려요"), 1,
                "빨간 점멸은 일단 멈춤, 노란 점멸은 주의하며 천천히 지나가라는 뜻이에요."),
        ),
        TASK_PRIORITY to listOf(
            QuizItem("priority-same-time", "신호 없는 교차로에 두 차가 동시에 들어서면 누가 먼저?",
                listOf("왼쪽 길에서 오는 차", "오른쪽 길에서 오는 차", "더 큰 차"), 1,
                "동시에 들어서면 오른쪽 길에서 오는 차에게 양보해요. 이미 교차로에 들어가 있는 차가 있으면 그 차가 먼저예요."),
            QuizItem("priority-unprotected-left", "비보호 좌회전은 언제 할 수 있나요?",
                listOf("빨간불이어도 차가 없으면", "초록불에 마주 오는 차와 사람을 방해하지 않을 때", "좌회전 신호가 켜질 때만"), 1,
                "비보호 좌회전은 초록불에서만 해요. 마주 오는 차와 건너는 사람이 먼저이고, 빨간불에 돌면 신호 위반이에요."),
            QuizItem("priority-left-vs-straight", "신호 없는 교차로에서 좌회전하려는데 마주 오는 차가 직진하려 하면?",
                listOf("내가 먼저 돌아요", "직진하는 차에게 양보해요", "경적을 울리고 돌아요"), 1,
                "좌회전하는 차는 직진하거나 우회전하는 차에게 양보해요."),
        ),
        TASK_MANNER to listOf(
            QuizItem("manner-hazard-thanks", "끼어들기를 양보받은 뒤 비상등을 짧게 켜는 건?",
                listOf("법으로 정해진 신호예요", "고마움을 전하는 운전자들의 관행이에요", "고장 났다는 뜻이에요"), 1,
                "법에 정해진 신호는 아니지만 많이 쓰는 감사 표시예요. 짧게 켰다가 바로 꺼요."),
            QuizItem("manner-zipper", "차로가 줄어 두 줄이 합쳐지는 곳에서는?",
                listOf("한 대씩 번갈아 들어가요", "빠른 차가 먼저 들어가요", "끝까지 비켜 주지 않아요"), 0,
                "지퍼처럼 한 대씩 번갈아 들어가면 모두 빨라져요. 법이 아니라 서로 편한 관행이에요."),
            QuizItem("manner-parking-wait", "주차장에서 나가려는 차를 기다릴 때는?",
                listOf("바로 뒤에 붙어 기다려요", "나갈 공간을 두고 방향지시등으로 기다린다고 알려요", "경적으로 재촉해요"), 1,
                "나가는 차가 편히 빠질 수 있게 거리를 두고, 방향지시등으로 그 자리를 기다린다고 알려요."),
        ),
        TASK_TRICKY to listOf(
            QuizItem("tricky-yellow-inside", "교차로에 이미 들어간 뒤 노란불로 바뀌면 그 자리에 멈춰야 한다.", YES_NO, 1,
                "이미 교차로에 들어가 있으면 신속히 빠져나가요. 노란불에 멈추는 곳은 정지선이나 교차로 앞이에요."),
            QuizItem("tricky-green-crosswalk", "초록불에 우회전하는 중이어도 횡단보도에 사람이 건너려 하면 멈춰야 한다.", YES_NO, 0,
                "보행자가 건너고 있거나 건너려 하면 신호와 상관없이 횡단보도 앞에서 멈춰요."),
            QuizItem("tricky-police-signal", "경찰관 손신호와 신호등이 다르면 신호등을 따른다.", YES_NO, 1,
                "신호등과 경찰관 신호가 다르면 경찰관의 신호나 지시를 따라요."),
            QuizItem("tricky-yellow-dashed", "노란 점선 중앙선은 앞지르기할 때 넘을 수 있다.", YES_NO, 0,
                "노란 점선은 반대편을 충분히 살핀 뒤 앞지르기할 때만 넘을 수 있어요. 노란 실선은 넘지 않아요."),
        ),
    )
}
