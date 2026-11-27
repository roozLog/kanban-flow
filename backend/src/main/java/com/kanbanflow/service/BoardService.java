package com.kanbanflow.service;

import com.kanbanflow.dto.BoardResponse;
import com.kanbanflow.dto.BoardRow;
import com.kanbanflow.dto.CardResponse;
import com.kanbanflow.dto.ColumnResponse;
import com.kanbanflow.dto.ColumnRow;
import com.kanbanflow.exception.NotFoundException;
import com.kanbanflow.mapper.BoardMapper;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class BoardService {

    private final BoardMapper boardMapper;

    public BoardService(BoardMapper boardMapper) {
        this.boardMapper = boardMapper;
    }

    /** 보드 + 컬럼 + 카드를 쿼리 3번으로 조립한다 (N+1 없음). */
    @Transactional(readOnly = true)
    public BoardResponse getBoard(long boardId) {
        BoardRow board = boardMapper.selectBoard(boardId);
        if (board == null) {
            throw new NotFoundException("보드를 찾을 수 없습니다. id=" + boardId);
        }

        Map<Long, List<CardResponse>> cardsByColumn = new LinkedHashMap<>();
        for (CardResponse card : boardMapper.selectCardsByBoard(boardId)) {
            cardsByColumn.computeIfAbsent(card.columnId(), k -> new ArrayList<>()).add(card);
        }

        List<ColumnResponse> columns = new ArrayList<>();
        for (ColumnRow c : boardMapper.selectColumns(boardId)) {
            columns.add(new ColumnResponse(c.id(), c.title(), c.statusCode(), c.position(),
                    cardsByColumn.getOrDefault(c.id(), List.of())));
        }
        return new BoardResponse(board.id(), board.title(), columns);
    }
}
