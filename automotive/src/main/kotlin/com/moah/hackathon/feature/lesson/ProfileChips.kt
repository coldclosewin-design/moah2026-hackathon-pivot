package com.moah.hackathon.feature.lesson

import android.util.Log
import java.io.File
import java.util.Properties

/**
 * 프로필 다섯 줄(라운드 22 결정 7 = 7b P2 "한 장", 10/5). 답은 **칩만** — 키보드·STT·숫자 입력 없음.
 * 칩은 [ProfileStatement] 의 기존 필드에 대표값을 넣고, 저장된 값에서 거꾸로 칩을 찾는다(필드 타입은 그대로 — 멘트·Copilot 프롬프트·제안이 그대로 읽는다).
 * 줄 순서 = 묻는 순서: 첫 실행은 [ONBOARDING](다섯 전부, 10/8), 건너뛴 줄은 리포트 끝 카드가 세션마다 하나씩.
 */
enum class ProfileField(val title: String, val question: String) {
    FEAR("무서운 상황", "운전하면서 제일 무서운 건 뭐예요?"),
    LAST_DRIVE("마지막 운전", "마지막으로 운전한 게 언제쯤이에요?"),
    GOAL("필요한 일", "운전이 필요한 일은 뭐예요?"),
    LICENSE("면허", "면허는 언제 따셨어요?"),
    CAR("타는 차", "타는 차는 어떤 차예요?"),
    ;

    companion object {
        /**
         * 첫 실행에 묻는 줄 — 다섯 전부(10/8 사용자: 둘만 묻고 시작하니 "필요한 일~타는 차가 설정 안 된 채 시작하는 버그" 로 보였다.
         * 그 전(7b 질문 5 기본값)은 첫 제안을 바꾸는 둘만 묻고 나머지는 리포트 끝 카드에서 하나씩). 건너뛰기는 그대로 둔다.
         */
        val ONBOARDING: List<ProfileField> = entries
    }
}

/** 답 칩. [id] 는 저장·로그용(영문), [label] 은 화면 글자(숫자 없음). */
data class ProfileChip(val id: String, val label: String)

/** 화면 한 줄 — 질문 · 지금 답(없으면 null = "아직") · 고를 칩. */
data class ProfileRow(val field: ProfileField, val answer: ProfileChip?, val chips: List<ProfileChip>)

/** 첫 실행 온보딩 — 이 줄들을 차례로 펼친다. 끝나면(또는 건너뛰면) `finishOnboarding`. */
data class ProfileOnboarding(val fields: List<ProfileField>)

object ProfileChips {
    private data class Option(val chip: ProfileChip, val apply: (ProfileStatement, Int) -> ProfileStatement)

    private fun opt(id: String, label: String, apply: (ProfileStatement, Int) -> ProfileStatement) = Option(ProfileChip(id, label), apply)

    private val options: Map<ProfileField, List<Option>> = mapOf(
        ProfileField.FEAR to listOf(
            opt("parking", "주차") { s, _ -> s.copy(fear = "주차") },
            opt("highway", "고속도로") { s, _ -> s.copy(fear = "고속도로") },
            opt("night", "야간") { s, _ -> s.copy(fear = "야간") },
            opt("lane", "차선 바꾸기") { s, _ -> s.copy(fear = "차선 변경") },
            opt("unsure", "잘 모르겠어요") { s, _ -> s.copy(fear = null) },
        ),
        ProfileField.LAST_DRIVE to listOf(
            opt("recent", "요즘도 해요") { s, _ -> s.copy(monthsSinceLastDrive = 0) },
            opt("last-year", "작년쯤") { s, _ -> s.copy(monthsSinceLastDrive = 12) },
            opt("few-years", "몇 년 전") { s, _ -> s.copy(monthsSinceLastDrive = 36) },
            opt("ten-plus", "십 년 넘게") { s, _ -> s.copy(monthsSinceLastDrive = 120) },
            opt("unknown", "기억이 안 나요") { s, _ -> s.copy(monthsSinceLastDrive = null) },
        ),
        ProfileField.GOAL to listOf(
            opt("commute", "출퇴근") { s, _ -> s.copy(goal = "출퇴근") },
            opt("school-run", "아이 등하원") { s, _ -> s.copy(goal = "아이 등하원") },
            opt("groceries", "장보기") { s, _ -> s.copy(goal = "장보기") },
            opt("travel", "여행") { s, _ -> s.copy(goal = "여행") },
        ),
        ProfileField.LICENSE to listOf(
            opt("last-year", "작년쯤") { s, y -> s.copy(licenseYear = y - 1) },
            opt("few-years", "몇 년 전") { s, y -> s.copy(licenseYear = y - 3) },
            opt("ten-years", "십 년쯤 전") { s, y -> s.copy(licenseYear = y - 10) },
            opt("long-ago", "더 오래") { s, y -> s.copy(licenseYear = y - 20) },
        ),
        ProfileField.CAR to listOf(
            opt("compact", "경차") { s, _ -> s.copy(car = "경차") },
            opt("small", "준중형") { s, _ -> s.copy(car = "준중형") },
            opt("mid-suv", "중형 SUV") { s, _ -> s.copy(car = "중형 SUV") },
            opt("large", "큰 차") { s, _ -> s.copy(car = "큰 차") },
        ),
    )

    fun chips(field: ProfileField): List<ProfileChip> = options.getValue(field).map { it.chip }

    /** 칩을 진술에 반영. 모르는 칩이면 null. [thisYear] 는 면허 칩의 대표 연도를 정하는 데만 쓴다. */
    fun apply(statement: ProfileStatement, field: ProfileField, chipId: String, thisYear: Int): ProfileStatement? =
        options.getValue(field).firstOrNull { it.chip.id == chipId }?.apply?.invoke(statement, thisYear)

    /** 저장된 진술에서 칩을 찾는다. [answered] 가 아니면 null(= "아직"). 값이 비었는데 답했다면 "모르겠어요" 쪽 칩. */
    fun answerOf(statement: ProfileStatement, field: ProfileField, answered: Boolean, thisYear: Int): ProfileChip? {
        if (!answered) return null
        val c = chips(field)
        fun id(x: String) = c.first { it.id == x }
        return when (field) {
            ProfileField.FEAR -> when (statement.fear) {
                null -> id("unsure"); "주차" -> id("parking"); "고속도로" -> id("highway"); "야간" -> id("night"); "차선 변경" -> id("lane")
                else -> statement.fear?.let { f -> c.firstOrNull { f.contains(it.label) } }
            }
            ProfileField.LAST_DRIVE -> when (val m = statement.monthsSinceLastDrive) {
                null -> id("unknown"); in 0..5 -> id("recent"); in 6..23 -> id("last-year"); in 24..95 -> id("few-years"); else -> id("ten-plus")
            }
            ProfileField.GOAL -> c.firstOrNull { it.label == statement.goal }
            ProfileField.LICENSE -> statement.licenseYear?.let { y ->
                when (thisYear - y) { in Int.MIN_VALUE..1 -> id("last-year"); in 2..5 -> id("few-years"); in 6..14 -> id("ten-years"); else -> id("long-ago") }
            }
            ProfileField.CAR -> c.firstOrNull { it.label == statement.car }
        }
    }

    /** 진술에 값이 들어 있는 줄 — 프리셋·시드 프로필을 "답한 것" 으로 볼 때. */
    fun filledFields(statement: ProfileStatement): Set<ProfileField> = buildSet {
        if (statement.fear != null) add(ProfileField.FEAR)
        if (statement.monthsSinceLastDrive != null) add(ProfileField.LAST_DRIVE)
        if (statement.goal != null) add(ProfileField.GOAL)
        if (statement.licenseYear != null) add(ProfileField.LICENSE)
        if (statement.car != null) add(ProfileField.CAR)
    }

    /**
     * 리포트 끝 "하나만 물어볼게요" 카드에 띄울 줄 — 아직 답하지 않았고 "다음에요" 를 [MAX_SKIPS] 번 누르지 않은 첫 줄(줄 순서대로).
     * 그 줄은 시트에서는 언제든 답할 수 있다.
     */
    fun nextAsk(answered: Set<ProfileField>, skips: Map<ProfileField, Int>): ProfileField? =
        ProfileField.entries.firstOrNull { it !in answered && (skips[it] ?: 0) < MAX_SKIPS }

    const val MAX_SKIPS = 2
}

/** 저장되는 프로필 — 진술 + 답한 줄 + "다음에요" 횟수 + 첫 실행을 마쳤는지. */
data class StoredProfile(
    val statement: ProfileStatement,
    val answered: Set<ProfileField>,
    val skips: Map<ProfileField, Int> = emptyMap(),
    val onboarded: Boolean = false,
)

/** 프로필 저장소(7b 질문 8: 앱 내부 파일 하나, 운전자 한 명). 초기화는 관리자 모드에서만. */
interface ProfileStore {
    fun load(): StoredProfile?
    fun save(profile: StoredProfile)
    fun clear()
}

/** 앱 수명 동안만 — 테스트와 기본값. */
class MemoryProfileStore(private var stored: StoredProfile? = null) : ProfileStore {
    override fun load(): StoredProfile? = stored
    override fun save(profile: StoredProfile) { stored = profile }
    override fun clear() { stored = null }
}

/**
 * 앱 내부 파일(`filesDir/profile.properties`) — `java.util.Properties` 라 JVM 테스트에서도 그대로 돈다.
 * 읽기·쓰기 실패는 로그만 남기고 "저장 없음" 으로 다룬다(프로필이 없어도 앱은 돈다). 1 KB 미만이라 호출 스레드에서 쓴다.
 */
class FileProfileStore(private val file: File) : ProfileStore {
    override fun load(): StoredProfile? = runCatching {
        if (!file.exists()) return null
        val p = Properties().apply { file.inputStream().use { load(it) } }
        StoredProfile(
            statement = ProfileStatement(
                licenseYear = p.getProperty("licenseYear")?.toIntOrNull(),
                monthsSinceLastDrive = p.getProperty("monthsSinceLastDrive")?.toIntOrNull(),
                car = p.getProperty("car"), goal = p.getProperty("goal"), fear = p.getProperty("fear"),
            ),
            answered = p.getProperty("answered").orEmpty().split(',').mapNotNull { n -> ProfileField.entries.firstOrNull { it.name == n } }.toSet(),
            skips = ProfileField.entries.mapNotNull { f -> p.getProperty("skip.${f.name}")?.toIntOrNull()?.let { f to it } }.toMap(),
            onboarded = p.getProperty("onboarded") == "true",
        )
    }.getOrElse { Log.w(TAG, "load failed: $it"); null }

    override fun save(profile: StoredProfile) {
        runCatching {
            val p = Properties()
            profile.statement.licenseYear?.let { p.setProperty("licenseYear", it.toString()) }
            profile.statement.monthsSinceLastDrive?.let { p.setProperty("monthsSinceLastDrive", it.toString()) }
            profile.statement.car?.let { p.setProperty("car", it) }
            profile.statement.goal?.let { p.setProperty("goal", it) }
            profile.statement.fear?.let { p.setProperty("fear", it) }
            p.setProperty("answered", profile.answered.joinToString(",") { it.name })
            profile.skips.forEach { (f, n) -> p.setProperty("skip.${f.name}", n.toString()) }
            p.setProperty("onboarded", profile.onboarded.toString())
            file.parentFile?.mkdirs()
            file.outputStream().use { p.store(it, "drive coach profile") }
        }.onFailure { Log.w(TAG, "save failed: $it") }
    }

    override fun clear() {
        runCatching { file.delete() }.onFailure { Log.w(TAG, "clear failed: $it") }
    }

    private companion object { const val TAG = "MOAH/ProfileStore" }
}
