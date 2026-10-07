package com.moah.hackathon.ports

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * 음성 출력 계약. 마지막으로 말한 문장을 [lastSpoken] 으로 노출해 UI 가 자막을 그린다.
 * (주행 중 화면 잠금 상태에서도 자막은 큰 글자로 보여준다.)
 */
interface TtsPort {
    /** 자막. 실제 음성 엔진에서는 **지금 말하기 시작한 문장**이다(큐에 넣은 문장이 아니다) — 음성과 자막이 어긋나지 않게. */
    val lastSpoken: StateFlow<String?>
    /**
     * 지금 말하는 중인가(엔진 준비 전 대기열 포함). 브리핑이 "음성이 끝난 뒤" 넘어가는 데 쓴다(라운드 26, 10/7).
     * 엔진이 없거나 실패한 구현은 늘 false — 그때 브리핑은 최소 시간만 머문다.
     */
    val speaking: StateFlow<Boolean> get() = NOT_SPEAKING
    fun speak(text: String, priority: SpeechPriority = SpeechPriority.NORMAL)
    /** 말하던 것과 대기열을 버리고 **자막도 지운다**(9/30 — 퀴즈 "그만하기" 뒤 Setup 에 지난 해설이 남던 것). */
    fun stop()
    fun dispose()
}

/**
 * 시연은 시간을 압축하므로(배율 10) 문장이 말하는 속도보다 빨리 쌓인다. 전부 순서대로 읽으면 음성이 화면보다 수십 초 밀린다.
 *  - [URGENT]: 공개·도착처럼 화면 전환과 같이 나와야 하는 문장. 말하던 것을 끊고 바로 말한다.
 *  - [NORMAL]: 주문 상태·다음 장소 안내. 순서대로.
 *  - [AMBIENT]: 스토리·힌트. 이미 밀려 있으면 버린다(낡은 이야기를 뒤늦게 읽지 않는다).
 */
enum class SpeechPriority { AMBIENT, NORMAL, URGENT }

private val NOT_SPEAKING: StateFlow<Boolean> = MutableStateFlow(false)

/**
 * 긴 안내(도착 멘트는 메모 + 예약 + 산책 + 실내 안내 + 마지막 메시지로 열 문장이 넘는다)를 자막 한 장에 들어가는 토막으로 나눈다.
 * 문장 끝(. ! ? …) 뒤의 공백에서만 자르고, 짧은 문장은 [maxChars] 를 넘지 않는 한 앞 토막에 붙인다
 * ("아버지의 메모예요." 와 그 내용이 한 자막에 같이 나오게). 한 문장이 [maxChars] 보다 길면 그대로 한 토막이다(문장 중간에서는 자르지 않는다).
 * "3.2 km"·"12:30" 처럼 뒤에 공백이 없는 점은 문장 끝이 아니다.
 *
 * 줄바꿈은 **주제의 경계**다(도착 멘트: 메모 / 가는 길 / 예약 / 산책 / 마지막 메시지). 토막은 줄을 넘어 합쳐지지 않는다 —
 * 화면이 카드마다 음성에 맞춰 주목시키므로 "…약 2분. 예약해 뒀어요." 처럼 두 주제가 한 자막에 섞이면 안 된다.
 */
internal fun splitForSpeech(text: String, maxChars: Int = 56): List<String> {
    val chunks = mutableListOf<String>()
    for (topic in text.lines().map { it.trim() }.filter { it.isNotEmpty() }) {
        val first = chunks.size   // 이 주제의 첫 토막. 그 앞의 토막에는 붙이지 않는다
        for (sentence in topic.split(Regex("(?<=[.!?…])\\s+")).filter { it.isNotBlank() }) {
            val last = chunks.getOrNull(chunks.size - 1).takeIf { chunks.size > first }
            if (last != null && last.length + 1 + sentence.length <= maxChars) chunks[chunks.size - 1] = "$last $sentence"
            else chunks += sentence
        }
    }
    return chunks
}

/** 전문을 한 번에 자막으로 보여 줄 때(엔진 없음): 주제 경계의 줄바꿈은 공백으로. */
internal fun String.asCaption(): String = lines().map { it.trim() }.filter { it.isNotEmpty() }.joinToString(" ")

/** 자막만 남기는 구현. 단위 테스트·오프라인용. [spoken] 에는 줄바꿈(주제 경계)이 든 원문이 남는다. */
class FakeTtsPort : TtsPort {
    private val _lastSpoken = MutableStateFlow<String?>(null)
    override val lastSpoken: StateFlow<String?> = _lastSpoken
    /** 테스트가 "말하는 중" 을 직접 정한다(Fake 는 시간을 모른다). */
    val speakingFlow = MutableStateFlow(false)
    override val speaking: StateFlow<Boolean> = speakingFlow
    val spoken = mutableListOf<String>()
    /** [spoken] 과 같은 순서의 우선순위(테스트가 "끊고 말해야 하는 문장"을 확인한다). */
    val priorities = mutableListOf<SpeechPriority>()

    override fun speak(text: String, priority: SpeechPriority) {
        spoken += text
        priorities += priority
        _lastSpoken.value = text.asCaption()
    }

    override fun stop() { _lastSpoken.value = null }
    override fun dispose() {}
}

/**
 * Android TextToSpeech. 엔진 준비 전 요청은 버퍼링했다가 초기화 후 순서대로 말한다.
 * 자막은 엔진이 그 문장을 **말하기 시작할 때**(onStart) 바뀐다. 엔진이 없거나 아직 준비 전이면 즉시 바뀐다(자막이 유일한 전달 수단).
 *
 * 한 번의 [speak] 는 [splitForSpeech] 토막들로 나뉘어 차례로 큐에 들어간다 — 자막은 12문장짜리 글자 벽이 아니라 지금 말하는 토막이다.
 * 엔진이 없으면(자막만) 언제 넘길지 알 수 없으므로 전문을 한 번에 보여 준다(화면은 네 줄까지 받는다).
 */
class AndroidTtsPort(
    context: Context,
    private val locale: Locale = Locale.KOREAN,
    /** 쓰고 싶은 음성 이름(로그의 `ko voices` 목록). 비면 로컬 음성 중 최고 품질. 네트워크 음성도 지정할 수 있다 — 합성이 실패하면 로컬로 되돌린다. */
    private val preferredVoice: String = "",
) : TtsPort {
    private val _lastSpoken = MutableStateFlow<String?>(null)
    override val lastSpoken: StateFlow<String?> = _lastSpoken

    @Volatile private var ready = false
    @Volatile private var failed = false
    private val pending = ArrayDeque<Pair<String, SpeechPriority>>()
    private val texts = ConcurrentHashMap<String, String>()   // utteranceId("moah-<발화>-<토막>") → 토막 (아직 끝나지 않은 것)
    private var utteranceSeq = 0
    private val _speaking = MutableStateFlow(false)
    override val speaking: StateFlow<Boolean> = _speaking
    /** 남은 토막(또는 준비 전 대기열)이 있으면 말하는 중. 콜백·큐 변경마다 다시 계산한다. */
    private fun refreshSpeaking() { _speaking.value = texts.isNotEmpty() || synchronized(pending) { pending.isNotEmpty() } }

    private val tts: TextToSpeech = TextToSpeech(context.applicationContext) { status ->
        if (status == TextToSpeech.SUCCESS) {
            val result = ttsSafeSetLanguage(locale)
            selectBestVoice(locale)
            Log.i(TAG, "TTS ready, setLanguage($locale)=$result")
            ready = true
            synchronized(pending) { while (pending.isNotEmpty()) pending.removeFirst().let { enqueue(it.first, it.second) } }
            refreshSpeaking()
        } else {
            failed = true
            synchronized(pending) { pending.clear() }
            refreshSpeaking()
            Log.w(TAG, "TTS init failed: $status (자막만 표시)")
        }
    }

    init {
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            // start/done 로그의 시각으로 도착 멘트의 시간표(ui/…/ArrivalTiming)를 잰다 — 음성을 바꾸면 다시 잰다
            override fun onStart(utteranceId: String) { texts[utteranceId]?.let { _lastSpoken.value = it; Log.d(TAG, "start $utteranceId ${it.take(16)}") } }
            override fun onDone(utteranceId: String) { Log.d(TAG, "done $utteranceId"); texts.remove(utteranceId); refreshSpeaking() }
            override fun onStop(utteranceId: String, interrupted: Boolean) { texts.remove(utteranceId); refreshSpeaking() }
            @Deprecated("platform callback")
            override fun onError(utteranceId: String) {
                // 합성 실패(예: 네트워크 음성인데 오프라인): 음성은 못 냈어도 자막은 보여준다
                texts.remove(utteranceId)?.let { _lastSpoken.value = it; Log.w(TAG, "utterance failed → subtitle only") }
                refreshSpeaking()
                if (tts.voice?.isNetworkConnectionRequired == true) {
                    Log.w(TAG, "network voice failed → back to the best local voice")
                    selectBestVoice(locale, localOnly = true)
                }
            }
        })
    }

    private fun ttsSafeSetLanguage(locale: Locale): Int = try {
        tts.setLanguage(locale)
    } catch (e: RuntimeException) {
        Log.w(TAG, "setLanguage failed", e); TextToSpeech.LANG_NOT_SUPPORTED
    }

    /**
     * 설치된 한국어 음성 중 가장 품질이 높은 것을 고른다. 네트워크가 필요한 음성은 제외한다
     * (사내·시연 네트워크를 믿을 수 없고, 실패하면 그 문장은 소리 없이 지나간다).
     */
    private fun selectBestVoice(locale: Locale, preferred: String = preferredVoice, localOnly: Boolean = false) {
        try {
            val candidates = tts.voices.orEmpty().filter {
                it.locale.language == locale.language && !it.isNetworkConnectionRequired &&
                    TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED !in it.features.orEmpty()
            }
            // 목록은 항상 찍는다 — 사내 첫 실행에서 이 두 줄로 엔진의 음성 구성을 안다
            Log.i(TAG, "ko voices (local): " + candidates.joinToString { "${it.name}(q=${it.quality},lat=${it.latency})" })
            Log.i(TAG, "ko voices (network): " + tts.voices.orEmpty()
                .filter { it.locale.language == locale.language && it.isNetworkConnectionRequired }
                .joinToString { "${it.name}(q=${it.quality})" })
            if (!localOnly) tts.voices.orEmpty().firstOrNull { preferred.isNotBlank() && it.name == preferred }?.let {
                tts.voice = it
                tts.setSpeechRate(SPEECH_RATE)
                Log.i(TAG, "voice=${it.name} (requested, network=${it.isNetworkConnectionRequired})")
                return
            }
            if (preferred.isNotBlank() && !localOnly) Log.w(TAG, "requested voice '$preferred' not installed → local")
            // 같은 계열의 로컬 음성(…-kob-network → …-kob-local)이 있으면 그쪽 — 도중에 복귀해도 목소리가 덜 바뀐다
            candidates.firstOrNull { it.name == preferred.replace("-network", "-local") }?.let {
                tts.voice = it
                tts.setSpeechRate(SPEECH_RATE)
                Log.i(TAG, "voice=${it.name} (local twin of '$preferred')")
                return
            }
            candidates.maxWithOrNull(compareBy<Voice>({ it.quality }, { -it.latency }))?.let {
                tts.voice = it
                Log.i(TAG, "voice=${it.name} quality=${it.quality}")
            }
            tts.setSpeechRate(SPEECH_RATE)
        } catch (e: RuntimeException) {
            Log.w(TAG, "voice selection failed, engine default stays", e)
        }
    }

    override fun speak(text: String, priority: SpeechPriority) {
        when {
            failed -> _lastSpoken.value = text.asCaption()
            !ready -> { _lastSpoken.value = text.asCaption(); synchronized(pending) { pending.addLast(text to priority) } }
            else -> enqueue(text, priority)
        }
        refreshSpeaking()
    }

    private fun enqueue(text: String, priority: SpeechPriority) {
        // 밀린 정도는 토막이 아니라 발화(speak 호출) 수로 센다 — 긴 안내 하나가 스토리를 전부 밀어내지 않게
        val backlog = texts.keys.map { it.substringBeforeLast('-') }.distinct().size
        if (priority == SpeechPriority.AMBIENT && backlog >= AMBIENT_DROP_BACKLOG) {
            Log.i(TAG, "drop ambient (backlog $backlog): ${text.take(24)}…")
            return
        }
        val group = "moah-${utteranceSeq++}"
        if (priority == SpeechPriority.URGENT) texts.clear()
        var index = 0
        text.lines().filter { it.isNotBlank() }.forEachIndexed { topic, line ->
            // 주제가 바뀔 때 짧게 쉰다(자막은 앞 토막 그대로). 지인의 메모를 읽은 직후에 곧바로 길 안내로 넘어가면 숨이 차다.
            // 무음은 texts 에 넣지 않는다 — 자막도, 밀린 발화 수도 바꾸지 않는다.
            if (topic > 0) tts.playSilentUtterance(TOPIC_GAP_MS, TextToSpeech.QUEUE_ADD, "$group-gap$topic")
            splitForSpeech(line).forEach { chunk ->
                val id = "$group-$index"
                // 끊고 말하기는 첫 토막만. 나머지는 그 뒤에 이어 붙는다
                val mode = if (priority == SpeechPriority.URGENT && index == 0) TextToSpeech.QUEUE_FLUSH else TextToSpeech.QUEUE_ADD
                index++
                texts[id] = chunk
                if (tts.speak(chunk, mode, null, id) != TextToSpeech.SUCCESS) {
                    texts.remove(id); _lastSpoken.value = chunk
                }
            }
        }
    }

    override fun stop() {
        synchronized(pending) { pending.clear() }
        texts.clear()
        if (ready) tts.stop()
        _lastSpoken.value = null
        refreshSpeaking()
    }

    override fun dispose() {
        stop()
        tts.shutdown()
    }

    private companion object {
        const val TAG = "MOAH/AndroidTtsPort"
        /** 말하는 중 1 + 대기 1 이면 스토리·힌트는 더 쌓지 않는다. */
        const val AMBIENT_DROP_BACKLOG = 2
        const val SPEECH_RATE = 1.05f
        /** 주제(줄) 사이의 쉼. 길면 도착 멘트 전체가 늘어진다(주제 5개 → 쉼 4번). */
        const val TOPIC_GAP_MS = 700L
    }
}
