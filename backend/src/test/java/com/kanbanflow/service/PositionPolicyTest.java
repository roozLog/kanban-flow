package com.kanbanflow.service;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.kanbanflow.dto.CardPositionRow;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class PositionPolicyTest {

    private static CardPositionRow row(long id, String pos) {
        return new CardPositionRow(id, new BigDecimal(pos));
    }

    @Test
    void midpointWithEnoughGap_noRebalance() {
        var ordered = List.of(row(1, "1000"), row(3, "1500"), row(2, "2000"));
        assertFalse(PositionPolicy.needsRebalance(ordered, 3));
    }

    @Test
    void gapLessThanOneToPrev_rebalance() {
        var ordered = List.of(row(1, "1000"), row(3, "1000.5"), row(2, "2000"));
        assertTrue(PositionPolicy.needsRebalance(ordered, 3));
    }

    @Test
    void gapLessThanOneToNext_rebalance() {
        var ordered = List.of(row(1, "1000"), row(3, "1999.5"), row(2, "2000"));
        assertTrue(PositionPolicy.needsRebalance(ordered, 3));
    }

    @Test
    void gapExactlyOne_noRebalance() {
        var ordered = List.of(row(1, "1000"), row(3, "1001"), row(2, "2000"));
        assertFalse(PositionPolicy.needsRebalance(ordered, 3));
    }

    @Test
    void tieWithOtherCard_rebalance() {
        var ordered = List.of(row(1, "1000"), row(3, "1000"), row(2, "2000"));
        assertTrue(PositionPolicy.needsRebalance(ordered, 3));
    }

    @Test
    void nonPositivePosition_rebalance() {
        assertTrue(PositionPolicy.needsRebalance(List.of(row(3, "0"), row(1, "1000")), 3));
        assertTrue(PositionPolicy.needsRebalance(List.of(row(3, "-5"), row(1, "1000")), 3));
    }

    @Test
    void onlyCardInColumn_noRebalance() {
        assertFalse(PositionPolicy.needsRebalance(List.of(row(3, "1000")), 3));
    }
}
