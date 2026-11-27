package com.kanbanflow.service;

import com.kanbanflow.dto.CardInfoRow;
import com.kanbanflow.dto.CardPositionRow;
import com.kanbanflow.dto.CardRequest;
import com.kanbanflow.dto.MoveRequest;
import com.kanbanflow.exception.BadRequestException;
import com.kanbanflow.exception.NotFoundException;
import com.kanbanflow.mapper.CardMapper;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** 쓰기 메서드는 모두 소속 boardId를 반환한다. 컨트롤러가 최신 보드를 조회해 응답한다. */
@Service
public class CardService {

    static final int TITLE_MAX = 200;
    static final int DESC_MAX = 2000;

    private final CardMapper cardMapper;

    public CardService(CardMapper cardMapper) {
        this.cardMapper = cardMapper;
    }

    @Transactional
    public long create(long columnId, CardRequest req) {
        if (req.columnId() == null || req.columnId() != columnId) {
            throw new BadRequestException("경로의 columnId와 본문의 columnId가 다릅니다.");
        }
        validate(req);
        Long boardId = cardMapper.lockColumn(columnId);
        if (boardId == null) {
            throw new NotFoundException("컬럼을 찾을 수 없습니다. id=" + columnId);
        }
        cardMapper.insertCard(columnId, req.title().trim(), req.description(), req.dueDate(),
                cardMapper.selectNextCardPosition(columnId));
        return boardId;
    }

    @Transactional
    public long update(long cardId, CardRequest req) {
        validate(req);
        if (req.columnId() == null) {
            throw new BadRequestException("columnId는 필수입니다.");
        }
        CardInfoRow card = requireCard(cardId);
        BigDecimal newPosition = null;
        if (req.columnId() != card.columnId().longValue()) {
            // 컬럼 변경: 같은 보드 검증 후 대상 컬럼의 맨 끝으로
            Long targetBoard = cardMapper.lockColumn(req.columnId());
            if (targetBoard == null) {
                throw new NotFoundException("컬럼을 찾을 수 없습니다. id=" + req.columnId());
            }
            if (!targetBoard.equals(card.boardId())) {
                throw new BadRequestException("다른 보드의 컬럼으로는 이동할 수 없습니다.");
            }
            newPosition = cardMapper.selectNextCardPosition(req.columnId());
        }
        cardMapper.updateCard(cardId, req.title().trim(), req.description(), req.dueDate(), req.columnId(), newPosition);
        return card.boardId();
    }

    @Transactional
    public long delete(long cardId) {
        CardInfoRow card = requireCard(cardId);
        cardMapper.deleteCard(cardId);
        return card.boardId();
    }

    /**
     * 드래그 이동: 이동한 카드 1건만 UPDATE하고, 앞/뒤 이웃과의 간격이 1 미만이면 컬럼 전체를 재정렬한다.
     * 이동과 재정렬은 한 트랜잭션. 동점은 기존 카드가 앞, 이동한 카드가 뒤.
     */
    @Transactional
    public long move(long cardId, MoveRequest req) {
        if (req.toColumnId() == null || req.position() == null) {
            throw new BadRequestException("toColumnId와 position은 필수입니다.");
        }
        CardInfoRow card = requireCard(cardId);
        long toColumnId = req.toColumnId();
        Long targetBoard = cardMapper.lockColumn(toColumnId);
        if (targetBoard == null) {
            throw new NotFoundException("컬럼을 찾을 수 없습니다. id=" + toColumnId);
        }
        if (!targetBoard.equals(card.boardId())) {
            throw new BadRequestException("다른 보드의 컬럼으로는 이동할 수 없습니다.");
        }

        cardMapper.updateCardPosition(cardId, toColumnId, req.position());

        List<CardPositionRow> ordered = cardMapper.selectCardPositions(toColumnId, cardId);
        if (PositionPolicy.needsRebalance(ordered, cardId)) {
            cardMapper.rebalanceColumn(toColumnId, cardId);
        }
        return card.boardId();
    }

    private CardInfoRow requireCard(long cardId) {
        CardInfoRow card = cardMapper.selectCardInfo(cardId);
        if (card == null) {
            throw new NotFoundException("카드를 찾을 수 없습니다. id=" + cardId);
        }
        return card;
    }

    /** 길이는 바이트가 아닌 글자 수 기준 (DB의 BYTE 초과는 ORA-12899 → 400으로 변환). */
    private void validate(CardRequest req) {
        if (req.title() == null || req.title().isBlank()) {
            throw new BadRequestException("제목은 필수입니다.");
        }
        if (req.title().codePointCount(0, req.title().length()) > TITLE_MAX) {
            throw new BadRequestException("제목은 " + TITLE_MAX + "자 이하여야 합니다.");
        }
        if (req.description() != null
                && req.description().codePointCount(0, req.description().length()) > DESC_MAX) {
            throw new BadRequestException("설명은 " + DESC_MAX + "자 이하여야 합니다.");
        }
    }
}
