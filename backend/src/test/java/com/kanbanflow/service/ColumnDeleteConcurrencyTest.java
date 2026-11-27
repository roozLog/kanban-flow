package com.kanbanflow.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.kanbanflow.exception.BadRequestException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 동시 삭제 검증. 서로 다른 커넥션이 필요해 트랜잭션 롤백을 쓸 수 없으므로 임시 보드를 실제로 커밋하고
 * 테스트 후 직접 정리한다 (기존 시드 데이터는 건드리지 않는다).
 */
@SpringBootTest
class ColumnDeleteConcurrencyTest {

    @Autowired JdbcTemplate jdbc;
    @Autowired ColumnService columnService;

    long boardId = -1;

    @AfterEach
    void cleanup() {
        if (boardId < 0) {
            return;
        }
        jdbc.update("DELETE FROM TG_CARD WHERE COLUMN_SEQ IN (SELECT COLUMN_SEQ FROM TG_BOARD_COLUMN WHERE BOARD_SEQ = ?)", boardId);
        jdbc.update("DELETE FROM TG_BOARD_COLUMN WHERE BOARD_SEQ = ?", boardId);
        jdbc.update("DELETE FROM TG_BOARD WHERE BOARD_SEQ = ?", boardId);
    }

    /** 완료 컬럼 2개를 동시에 삭제하면 정확히 1개만 삭제되고 1개는 남아야 한다. */
    @Test
    void twoConcurrentDeletesOfDoneColumns_leaveExactlyOne() throws Exception {
        for (int round = 0; round < 5; round++) {
            cleanup();
            boardId = jdbc.queryForObject("SELECT TG_BOARD_SEQ.NEXTVAL FROM DUAL", Long.class);
            jdbc.update("INSERT INTO TG_BOARD (BOARD_SEQ, BOARD_TITLE, REG_DATE, UPD_DATE) VALUES (?, 'TEST-CONC', SYSDATE, SYSDATE)", boardId);
            long a = doneColumn(1000);
            long b = doneColumn(2000);

            int ok = 0;
            int rejected = 0;
            for (Future<Boolean> f : runConcurrently(() -> delete(a), () -> delete(b))) {
                if (f.get()) ok++; else rejected++;
            }

            assertEquals(1, ok, "round " + round + ": exactly one delete should succeed");
            assertEquals(1, rejected);
            assertEquals(1, jdbc.queryForObject(
                    "SELECT COUNT(*) FROM TG_BOARD_COLUMN WHERE BOARD_SEQ = ? AND STATUS_CD = 'STATUS003'", Integer.class, boardId));
        }
    }

    private boolean delete(long columnId) {
        try {
            columnService.delete(columnId);
            return true;
        } catch (BadRequestException e) {
            return false;
        }
    }

    private long doneColumn(int pos) {
        long id = jdbc.queryForObject("SELECT TG_BOARD_COLUMN_SEQ.NEXTVAL FROM DUAL", Long.class);
        jdbc.update("INSERT INTO TG_BOARD_COLUMN (COLUMN_SEQ, BOARD_SEQ, COLUMN_TITLE, STATUS_CD, POSITION, REG_DATE, UPD_DATE)"
                + " VALUES (?, ?, ?, 'STATUS003', ?, SYSDATE, SYSDATE)", id, boardId, "D" + id, pos);
        return id;
    }

    @SafeVarargs
    private static List<Future<Boolean>> runConcurrently(Callable<Boolean>... tasks) throws Exception {
        ExecutorService pool = Executors.newFixedThreadPool(tasks.length);
        try {
            CountDownLatch ready = new CountDownLatch(tasks.length);
            CountDownLatch go = new CountDownLatch(1);
            List<Future<Boolean>> futures = new ArrayList<>();
            for (Callable<Boolean> t : tasks) {
                futures.add(pool.submit(() -> {
                    ready.countDown();
                    go.await();
                    return t.call();
                }));
            }
            ready.await();
            go.countDown();
            for (Future<Boolean> f : futures) {
                f.get();
            }
            return futures;
        } finally {
            pool.shutdown();
        }
    }
}
