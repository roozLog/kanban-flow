package com.kanbanflow.controller;

import com.kanbanflow.dto.BoardResponse;
import com.kanbanflow.service.BoardService;
import com.kanbanflow.service.ColumnService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/** 쓰기 API는 최신 보드 전체를 응답한다. */
@RestController
public class ColumnController {

    private final ColumnService columnService;
    private final BoardService boardService;

    public ColumnController(ColumnService columnService, BoardService boardService) {
        this.columnService = columnService;
        this.boardService = boardService;
    }

    @DeleteMapping("/api/columns/{columnId}")
    public BoardResponse delete(@PathVariable long columnId) {
        return boardService.getBoard(columnService.delete(columnId));
    }
}
