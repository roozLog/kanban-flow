package com.kanbanflow.controller;

import com.kanbanflow.dto.BoardResponse;
import com.kanbanflow.dto.ColumnRequest;
import com.kanbanflow.service.BoardService;
import com.kanbanflow.service.ColumnService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/boards")
public class BoardController {

    private final BoardService boardService;
    private final ColumnService columnService;

    public BoardController(BoardService boardService, ColumnService columnService) {
        this.boardService = boardService;
        this.columnService = columnService;
    }

    @GetMapping("/{boardId}")
    public BoardResponse getBoard(@PathVariable long boardId) {
        return boardService.getBoard(boardId);
    }

    @PostMapping("/{boardId}/columns")
    public BoardResponse addColumn(@PathVariable long boardId, @RequestBody ColumnRequest req) {
        return boardService.getBoard(columnService.create(boardId, req));
    }
}
