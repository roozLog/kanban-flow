package com.kanbanflow.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.kanbanflow.exception.BadRequestException;
import com.kanbanflow.exception.NotFoundException;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/** 롤백되는 통합 테스트. 기존 시드는 건드리지 않고 임시 보드를 만든다. */
@SpringBootTest
@Transactional
class ColumnDeleteIntegrationTest {

    @Autowired JdbcTemplate jdbc;
    @Autowired ColumnService columnService;
    @Autowired BoardService boardService;

    long boardId;

    @BeforeEach
    void setUp() {
        boardId = jdbc.queryForObject("SELECT TG_BOARD_SEQ.NEXTVAL FROM DUAL", Long.class);
        jdbc.update("INSERT INTO TG_BOARD (BOARD_SEQ, BOARD_TITLE, REG_DATE, UPD_DATE) VALUES (?, ?, SYSDATE, SYSDATE)",
                boardId, "TEST-" + boardId);
    }

    @Test
    void deleteEmptyColumn_succeeds_andReturnsBoardWithoutIt() {
        long todo = column("STATUS001", 1000);
        long done = column("STATUS003", 2000);
        long empty = column("STATUS002", 3000);

        long returned = columnService.delete(empty);

        assertEquals(boardId, returned);
        assertEquals(List.of(todo, done), columnIds());
    }

    @Test
    void deleteColumnWithCards_isRejected_andColumnKept() {
        column("STATUS003", 1000);
        long todo = column("STATUS001", 2000);
        card(todo);

        BadRequestException e = assertThrows(BadRequestException.class, () -> columnService.delete(todo));
        assertEquals(ColumnService.MSG_HAS_CARDS, e.getMessage());
        assertTrue(columnIds().contains(todo));
    }

    @Test
    void deleteLastDoneColumn_isRejected_evenWhenEmpty() {
        long done = column("STATUS003", 1000);
        column("STATUS001", 2000);

        BadRequestException e = assertThrows(BadRequestException.class, () -> columnService.delete(done));
        assertEquals(ColumnService.MSG_LAST_DONE, e.getMessage());
        assertTrue(columnIds().contains(done));
    }

    @Test
    void lastDoneColumnWithCards_showsLastDoneMessageFirst() {
        long done = column("STATUS003", 1000);
        card(done);

        BadRequestException e = assertThrows(BadRequestException.class, () -> columnService.delete(done));
        assertEquals(ColumnService.MSG_LAST_DONE, e.getMessage());
    }

    @Test
    void deleteEmptyDoneColumn_succeedsWhenTwoOrMoreDone() {
        long done1 = column("STATUS003", 1000);
        long done2 = column("STATUS003", 2000);

        columnService.delete(done2);

        assertEquals(List.of(done1), columnIds());
        // 이제 마지막 하나이므로 더는 삭제 불가
        assertThrows(BadRequestException.class, () -> columnService.delete(done1));
    }

    @Test
    void doneColumnWithCards_whenTwoDone_showsCardsMessage() {
        column("STATUS003", 1000);
        long done2 = column("STATUS003", 2000);
        card(done2);

        BadRequestException e = assertThrows(BadRequestException.class, () -> columnService.delete(done2));
        assertEquals(ColumnService.MSG_HAS_CARDS, e.getMessage());
    }

    @Test
    void doneCountIsPerBoard() {
        column("STATUS003", 1000); // 이 보드의 유일한 완료 컬럼
        long otherBoard = jdbc.queryForObject("SELECT TG_BOARD_SEQ.NEXTVAL FROM DUAL", Long.class);
        jdbc.update("INSERT INTO TG_BOARD (BOARD_SEQ, BOARD_TITLE, REG_DATE, UPD_DATE) VALUES (?, ?, SYSDATE, SYSDATE)",
                otherBoard, "TEST-" + otherBoard);
        long otherDone = jdbc.queryForObject("SELECT TG_BOARD_COLUMN_SEQ.NEXTVAL FROM DUAL", Long.class);
        jdbc.update("INSERT INTO TG_BOARD_COLUMN (COLUMN_SEQ, BOARD_SEQ, COLUMN_TITLE, STATUS_CD, POSITION, REG_DATE, UPD_DATE)"
                + " VALUES (?, ?, 'X', 'STATUS003', 1000, SYSDATE, SYSDATE)", otherDone, otherBoard);

        // 다른 보드에 완료 컬럼이 있어도, 자기 보드의 마지막 완료 컬럼은 지울 수 없다
        long mine = jdbc.queryForObject(
                "SELECT COLUMN_SEQ FROM TG_BOARD_COLUMN WHERE BOARD_SEQ = ? AND STATUS_CD = 'STATUS003'", Long.class, boardId);
        assertThrows(BadRequestException.class, () -> columnService.delete(mine));
    }

    @Test
    void deleteMissingColumn_is404() {
        assertThrows(NotFoundException.class, () -> columnService.delete(-1L));
    }

    // ---- helpers ----
    private long column(String status, int pos) {
        long id = jdbc.queryForObject("SELECT TG_BOARD_COLUMN_SEQ.NEXTVAL FROM DUAL", Long.class);
        jdbc.update("INSERT INTO TG_BOARD_COLUMN (COLUMN_SEQ, BOARD_SEQ, COLUMN_TITLE, STATUS_CD, POSITION, REG_DATE, UPD_DATE)"
                + " VALUES (?, ?, ?, ?, ?, SYSDATE, SYSDATE)", id, boardId, "C" + id, status, pos);
        return id;
    }

    private void card(long columnId) {
        long id = jdbc.queryForObject("SELECT TG_CARD_SEQ.NEXTVAL FROM DUAL", Long.class);
        jdbc.update("INSERT INTO TG_CARD (CARD_SEQ, COLUMN_SEQ, CARD_TITLE, POSITION, REG_DATE, UPD_DATE)"
                + " VALUES (?, ?, 'c', 1000, SYSDATE, SYSDATE)", id, columnId);
    }

    private List<Long> columnIds() {
        return boardService.getBoard(boardId).columns().stream().map(c -> c.id()).toList();
    }
}
