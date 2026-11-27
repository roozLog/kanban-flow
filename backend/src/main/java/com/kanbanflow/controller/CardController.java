package com.kanbanflow.controller;

import com.kanbanflow.dto.BoardResponse;
import com.kanbanflow.dto.CardRequest;
import com.kanbanflow.dto.MoveRequest;
import com.kanbanflow.service.BoardService;
import com.kanbanflow.service.CardService;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/** 쓰기 API는 모두 최신 보드 전체를 응답한다. */
@RestController
public class CardController {

    private final CardService cardService;
    private final BoardService boardService;

    public CardController(CardService cardService, BoardService boardService) {
        this.cardService = cardService;
        this.boardService = boardService;
    }

    @PostMapping("/api/columns/{columnId}/cards")
    public BoardResponse create(@PathVariable long columnId, @RequestBody CardRequest req) {
        return boardService.getBoard(cardService.create(columnId, req));
    }

    @PatchMapping("/api/cards/{cardId}")
    public BoardResponse update(@PathVariable long cardId, @RequestBody CardRequest req) {
        return boardService.getBoard(cardService.update(cardId, req));
    }

    @PatchMapping("/api/cards/{cardId}/move")
    public BoardResponse move(@PathVariable long cardId, @RequestBody MoveRequest req) {
        return boardService.getBoard(cardService.move(cardId, req));
    }

    @DeleteMapping("/api/cards/{cardId}")
    public BoardResponse delete(@PathVariable long cardId) {
        return boardService.getBoard(cardService.delete(cardId));
    }
}
