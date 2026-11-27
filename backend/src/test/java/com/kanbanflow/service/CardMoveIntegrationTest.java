package com.kanbanflow.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.kanbanflow.dto.BoardResponse;
import com.kanbanflow.dto.CardRequest;
import com.kanbanflow.dto.ColumnRequest;
import com.kanbanflow.dto.ColumnResponse;
import com.kanbanflow.dto.MoveRequest;
import com.kanbanflow.exception.BadRequestException;
import com.kanbanflow.exception.NotFoundException;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * 실제 Oracle에 붙지만 @Transactional로 테스트 후 전부 롤백된다.
 * 시퀀스 값만 소모된다. 기존 시드 데이터는 건드리지 않고 테스트용 임시 보드를 만든다.
 */
@SpringBootTest
@Transactional
class CardMoveIntegrationTest {

    @Autowired JdbcTemplate jdbc;
    @Autowired CardService cardService;
    @Autowired ColumnService columnService;
    @Autowired BoardService boardService;

    long boardId;
    long col1;
    long col2;
    long a;
    long b;
    long c;

    @BeforeEach
    void setUp() {
        boardId = newBoard();
        col1 = newColumn(boardId, "C1", 1000);
        col2 = newColumn(boardId, "C2", 2000);
        a = newCard(col1, "A", 1000);
        b = newCard(col1, "B", 2000);
        c = newCard(col1, "C", 3000);
    }

    @Test
    void midpointInsert_updatesOnlyMovedCard_noRebalance() {
        cardService.move(c, new MoveRequest(col1, bd("1500")));

        ColumnResponse col = column(col1);
        assertEquals(List.of(a, c, b), ids(col));
        assertPositions(col, "1000", "1500", "2000");
    }

    @Test
    void gapLessThanOne_rebalancesColumnKeepingDropOrder() {
        cardService.move(c, new MoveRequest(col1, bd("1000.5")));

        ColumnResponse col = column(col1);
        assertEquals(List.of(a, c, b), ids(col));
        assertPositions(col, "1000", "2000", "3000");
    }

    @Test
    void tieWithExistingCard_movedCardGoesAfterIt() {
        cardService.move(c, new MoveRequest(col1, bd("1000")));

        ColumnResponse col = column(col1);
        assertEquals(List.of(a, c, b), ids(col));
        assertPositions(col, "1000", "2000", "3000");
    }

    @Test
    void moveToAnotherColumn() {
        cardService.move(c, new MoveRequest(col2, bd("1000")));

        assertEquals(List.of(a, b), ids(column(col1)));
        ColumnResponse target = column(col2);
        assertEquals(List.of(c), ids(target));
        assertPositions(target, "1000");
    }

    @Test
    void moveToColumnOfAnotherBoard_isRejected() {
        long otherBoard = newBoard();
        long otherCol = newColumn(otherBoard, "X", 1000);

        assertThrows(BadRequestException.class, () -> cardService.move(c, new MoveRequest(otherCol, bd("1000"))));
        assertEquals(List.of(a, b, c), ids(column(col1))); // 변화 없음
    }

    @Test
    void moveToMissingColumn_is404() {
        assertThrows(NotFoundException.class, () -> cardService.move(c, new MoveRequest(-1L, bd("1000"))));
    }

    @Test
    void updateColumnChange_goesToEndOfTarget_andRejectsOtherBoard() {
        newCard(col2, "D", 5000);
        cardService.update(a, new CardRequest("A", null, col2, null));
        ColumnResponse target = column(col2);
        assertEquals(a, target.cards().get(1).id());
        assertPositions(target, "5000", "6000");

        long otherCol = newColumn(newBoard(), "X", 1000);
        assertThrows(BadRequestException.class, () -> cardService.update(b, new CardRequest("B", null, otherCol, null)));
    }

    @Test
    void createCard_appendsAtEnd_andPathMismatchIs400() {
        cardService.create(col2, new CardRequest("N1", null, col2, null));
        cardService.create(col2, new CardRequest("N2", "d", col2, null));
        assertPositions(column(col2), "1000", "2000");
        assertThrows(BadRequestException.class, () -> cardService.create(col2, new CardRequest("N", null, col1, null)));
    }

    @Test
    void addColumn_validatesStatusCode() {
        columnService.create(boardId, new ColumnRequest("검토", "STATUS004"));
        List<ColumnResponse> cols = boardService.getBoard(boardId).columns();
        assertEquals("STATUS004", cols.get(2).statusCode());
        assertEquals(0, cols.get(2).position().compareTo(bd("3000")));
        assertTrue(cols.get(2).cards().isEmpty());

        assertThrows(BadRequestException.class, () -> columnService.create(boardId, new ColumnRequest("x", "STATUS999")));
    }

    // ---- helpers ----
    private long newBoard() {
        long id = jdbc.queryForObject("SELECT TG_BOARD_SEQ.NEXTVAL FROM DUAL", Long.class);
        jdbc.update("INSERT INTO TG_BOARD (BOARD_SEQ, BOARD_TITLE, REG_DATE, UPD_DATE) VALUES (?, ?, SYSDATE, SYSDATE)",
                id, "TEST-" + id);
        return id;
    }

    private long newColumn(long board, String title, int pos) {
        long id = jdbc.queryForObject("SELECT TG_BOARD_COLUMN_SEQ.NEXTVAL FROM DUAL", Long.class);
        jdbc.update("INSERT INTO TG_BOARD_COLUMN (COLUMN_SEQ, BOARD_SEQ, COLUMN_TITLE, STATUS_CD, POSITION, REG_DATE, UPD_DATE)"
                + " VALUES (?, ?, ?, 'STATUS001', ?, SYSDATE, SYSDATE)", id, board, title, pos);
        return id;
    }

    private long newCard(long column, String title, int pos) {
        long id = jdbc.queryForObject("SELECT TG_CARD_SEQ.NEXTVAL FROM DUAL", Long.class);
        jdbc.update("INSERT INTO TG_CARD (CARD_SEQ, COLUMN_SEQ, CARD_TITLE, POSITION, REG_DATE, UPD_DATE)"
                + " VALUES (?, ?, ?, ?, SYSDATE, SYSDATE)", id, column, title, pos);
        return id;
    }

    private ColumnResponse column(long columnId) {
        BoardResponse board = boardService.getBoard(boardId);
        return board.columns().stream().filter(x -> x.id() == columnId).findFirst().orElseThrow();
    }

    private static List<Long> ids(ColumnResponse col) {
        return col.cards().stream().map(x -> x.id()).toList();
    }

    private static void assertPositions(ColumnResponse col, String... expected) {
        assertEquals(expected.length, col.cards().size());
        for (int i = 0; i < expected.length; i++) {
            assertEquals(0, col.cards().get(i).position().compareTo(bd(expected[i])),
                    "position[" + i + "]=" + col.cards().get(i).position() + " expected " + expected[i]);
        }
    }

    private static BigDecimal bd(String s) {
        return new BigDecimal(s);
    }
}
