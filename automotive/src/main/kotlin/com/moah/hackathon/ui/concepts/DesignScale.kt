package com.moah.hackathon.ui.concepts

import android.util.Log
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

/**
 * 모든 화면은 "가로 2560 × 세로 1268 dp 의 창"을 기준으로 설계됐다(외부 에뮬 CSTDe_API_34: 2560×1440 px, 밀도 160 → 1 dp = 1 px,
 * 시스템 바를 뺀 앱 창이 2560×1268). 기기의 밀도가 다르면 같은 dp·sp 가 더 많은 픽셀을 차지해 배치가 넘친다 —
 * 외부 에뮬에서 밀도만 200 으로 올려도 기분 선택 화면의 시간 버튼이 잘리고 "드라이브 시작"이 화면 밖으로 나갔다(2026-09-21).
 *
 * 그래서 화면 루트에서 밀도를 다시 정한다: 창이 몇 픽셀이든 **설계 크기가 창 안에 통째로 들어가는 가장 큰 밀도**.
 * 창의 비율이 설계와 다르면 남는 쪽에 dp 가 더 생길 뿐(모든 화면이 fillMaxSize 라 늘어난다) 잘리지 않는다.
 * 글꼴 배율(시스템 설정)은 1 로 고정한다 — 주행 화면의 글자 크기는 앱이 책임진다.
 *
 * 이것은 "전부 같은 비율로 키우기"용 손잡이가 아니다. 설계 크기를 줄여 글자를 키우면 내용이 창을 넘친다 —
 * 글자를 키우려면 화면(컨셉)에서 글자 크기와 배치를 같이 고쳐야 한다.
 */
@Composable
internal fun DesignScale(content: @Composable () -> Unit) {
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val widthPx = constraints.maxWidth
        val heightPx = constraints.maxHeight
        val system = LocalDensity.current
        val density = designDensity(widthPx, heightPx) ?: system.density
        LaunchedEffect(widthPx, heightPx, system.density) {
            // 사내 첫날 확인용: 창 크기와 기기 밀도, 우리가 고른 밀도
            Log.i("MOAH/DesignScale", "window=${widthPx}x${heightPx}px systemDensity=${system.density} fontScale=${system.fontScale} → density=$density")
        }
        CompositionLocalProvider(LocalDensity provides Density(density, fontScale = 1f), content = content)
    }
}

internal const val DESIGN_WIDTH_DP = 2560f
internal const val DESIGN_HEIGHT_DP = 1268f

/** 설계 크기(2560×1268 dp)가 창 안에 통째로 들어가는 가장 큰 밀도. 창 크기를 모르면(0 이하·무한) null. */
internal fun designDensity(widthPx: Int, heightPx: Int): Float? {
    if (widthPx <= 0 || heightPx <= 0 || widthPx == Int.MAX_VALUE || heightPx == Int.MAX_VALUE) return null
    return minOf(widthPx / DESIGN_WIDTH_DP, heightPx / DESIGN_HEIGHT_DP)
}
