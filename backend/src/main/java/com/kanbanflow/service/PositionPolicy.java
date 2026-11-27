package com.kanbanflow.service;

import com.kanbanflow.dto.CardPositionRow;
import java.math.BigDecimal;
import java.util.List;

/** POSITION 규칙의 순수 로직 (DB 의존 없음). */
public final class PositionPolicy {

    public static final BigDecimal GAP = BigDecimal.valueOf(1000);
    private static final BigDecimal MIN_GAP = BigDecimal.ONE;

    private PositionPolicy() {
    }

    /**
     * 이동한 카드의 위치가 재정렬이 필요한지 판단한다.
     * ordered는 컬럼 내 카드를 (POSITION, 이동 카드는 동점 시 뒤, CARD_SEQ) 순으로 정렬한 목록이고
     * 이동한 카드를 포함한다. 앞/뒤 이웃과의 간격이 1 미만이거나 POSITION이 0 이하이면 true.
     */
    public static boolean needsRebalance(List<CardPositionRow> ordered, long movedCardId) {
        int idx = -1;
        for (int i = 0; i < ordered.size(); i++) {
            if (ordered.get(i).id() == movedCardId) {
                idx = i;
                break;
            }
        }
        if (idx < 0) {
            return false;
        }
        BigDecimal moved = ordered.get(idx).position();
        if (moved.signum() <= 0) {
            return true;
        }
        if (idx > 0 && moved.subtract(ordered.get(idx - 1).position()).compareTo(MIN_GAP) < 0) {
            return true;
        }
        return idx < ordered.size() - 1
                && ordered.get(idx + 1).position().subtract(moved).compareTo(MIN_GAP) < 0;
    }
}
