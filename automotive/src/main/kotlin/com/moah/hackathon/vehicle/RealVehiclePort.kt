package com.moah.hackathon.vehicle

import android.content.Context
import android.util.Log
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.withContext
import mobis.vss.VSSAppData
import mobis.vss.VSSManager
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * mobis.vss.VSSManager 어댑터. 외부에서는 컴파일만 되고(스텁) 사내에서 실행된다.
 *
 * 함정 반영 (docs/02_vss_api_contract.md):
 *  - getInstance null → IllegalStateException (Factory 가 Fake 로 폴백)
 *  - getVSS/setVSS 동기 → 전용 단일 스레드에서만 호출
 *  - RemoteException 아님 → RuntimeException 만 catch
 *  - 리스너 동일성으로 unsubscribe → 인스턴스 보관
 */
class RealVehiclePort(context: Context) : VehiclePort {

    private val executor: ExecutorService = Executors.newSingleThreadExecutor { r ->
        Thread(r, "moah-vss").apply { isDaemon = true }
    }
    private val dispatcher: CoroutineDispatcher = executor.asCoroutineDispatcher()

    private val vss: VSSManager = VSSManager.getInstance(context.applicationContext)
        ?: throw IllegalStateException("VSSManager.getInstance() returned null (vss service not available)")

    override suspend fun get(keys: List<String>): Map<String, String> = withContext(dispatcher) {
        try {
            vss.getVSS(keys).orEmpty()
                .filter { it.nodePath != null && it.value != null }
                .associate { it.nodePath to it.value }
        } catch (e: RuntimeException) {
            Log.w(TAG, "getVSS failed for $keys", e)
            emptyMap()
        }
    }

    override suspend fun set(values: Map<String, String>): List<String> = withContext(dispatcher) {
        if (values.isEmpty()) return@withContext emptyList()
        try {
            val failed = vss.setVSS(values.map { (k, v) -> VSSAppData(k, v) })
            failed.orEmpty()
        } catch (e: RuntimeException) {
            Log.w(TAG, "setVSS failed for ${values.keys}", e)
            values.keys.toList()
        }
    }

    override fun observe(keys: List<String>): Flow<Map<String, String>> = callbackFlow {
        // 첫 emit: 현재 스냅샷
        trySend(get(keys))

        val listener = VSSManager.OnDataChangedListener { data ->
            val delta = data.orEmpty()
                .filter { it.nodePath != null && it.value != null }
                .associate { it.nodePath to it.value }
            if (delta.isNotEmpty()) trySend(delta)
        }
        try {
            withContext(dispatcher) { vss.subscribeVSS(keys, executor, listener) }
        } catch (e: RuntimeException) {
            Log.w(TAG, "subscribeVSS failed for $keys", e)
            close(e)
            return@callbackFlow
        }
        awaitClose {
            try {
                vss.unsubscribeVSS(listener)
            } catch (e: RuntimeException) {
                Log.w(TAG, "unsubscribeVSS failed", e)
            }
        }
    }

    override fun dispose() {
        executor.shutdownNow()
    }

    private companion object {
        const val TAG = "MOAH/RealVehiclePort"
    }
}
