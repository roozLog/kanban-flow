package com.kanbanflow.mapper;

import com.kanbanflow.dto.CardInfoRow;
import com.kanbanflow.dto.CardPositionRow;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface CardMapper {

    CardInfoRow selectCardInfo(@Param("cardId") long cardId);

    /** 컬럼 행을 잠그고 소속 보드를 반환한다. 컬럼이 없으면 null. (동시 이동/추가 직렬화) */
    Long lockColumn(@Param("columnId") long columnId);

    BigDecimal selectNextCardPosition(@Param("columnId") long columnId);

    void insertCard(@Param("columnId") long columnId, @Param("title") String title,
                    @Param("description") String description, @Param("dueDate") LocalDate dueDate,
                    @Param("position") BigDecimal position);

    /** position이 null이면 POSITION은 바꾸지 않는다. */
    void updateCard(@Param("cardId") long cardId, @Param("title") String title,
                    @Param("description") String description, @Param("dueDate") LocalDate dueDate,
                    @Param("columnId") long columnId, @Param("position") BigDecimal position);

    /** 이동: COLUMN_SEQ, POSITION, UPD_DATE만 갱신. */
    void updateCardPosition(@Param("cardId") long cardId, @Param("columnId") long columnId,
                            @Param("position") BigDecimal position);

    /** 컬럼 내 카드를 현재 순서(POSITION, 이동 카드는 동점 시 뒤, CARD_SEQ)로. */
    List<CardPositionRow> selectCardPositions(@Param("columnId") long columnId, @Param("movedCardId") long movedCardId);

    /** 컬럼 전체를 1000, 2000, ... 으로 재정렬 (POSITION이 바뀌는 행만). */
    int rebalanceColumn(@Param("columnId") long columnId, @Param("movedCardId") long movedCardId);

    int deleteCard(@Param("cardId") long cardId);
}
