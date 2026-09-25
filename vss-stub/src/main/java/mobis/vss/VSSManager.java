package mobis.vss;

import android.content.Context;

import java.util.List;
import java.util.concurrent.Executor;

/**
 * 컴파일 전용 스텁. 사내 시스템 jar(mobis.framework.core.jar)의 시그니처를 문자 그대로 재현한다.
 * 근거: docs/02_vss_api_contract.md (VehicleAPI Reference, pageId 1037767644)
 *
 * <p>주의: 어떤 메서드에도 {@code throws RemoteException}을 붙이지 않는다. 실물은 unchecked
 * RuntimeException을 던진다. 이 스텁이 런타임에 호출되면 즉시 UnsupportedOperationException으로 드러난다.
 */
public class VSSManager {

    private VSSManager() {
    }

    /** nullable — 호출부는 반드시 null 체크. */
    public static VSSManager getInstance(Context ctx) {
        throw new UnsupportedOperationException("vss-stub: VSSManager is compile-only");
    }

    /** 동기 메서드. UI 스레드에서 호출 금지. */
    public List<VSSAppData> getVSS(List<String> keys) {
        throw new UnsupportedOperationException("vss-stub: compile-only");
    }

    /** 실패한 nodePath 목록 반환. null 또는 빈 리스트 = 전체 성공. */
    public List<String> setVSS(List<VSSAppData> data) {
        throw new UnsupportedOperationException("vss-stub: compile-only");
    }

    public void subscribeVSS(List<String> keys, Executor executor, OnDataChangedListener listener) {
        throw new UnsupportedOperationException("vss-stub: compile-only");
    }

    public void unsubscribeVSS(OnDataChangedListener listener) {
        throw new UnsupportedOperationException("vss-stub: compile-only");
    }

    public interface OnDataChangedListener {
        void onDataChanged(List<VSSAppData> data);
    }
}
