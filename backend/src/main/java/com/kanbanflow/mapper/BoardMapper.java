package com.kanbanflow.mapper;

import com.kanbanflow.dto.BoardRow;
import com.kanbanflow.dto.CardResponse;
import com.kanbanflow.dto.ColumnRow;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface BoardMapper {

    BoardRow selectBoard(@Param("boardId") long boardId);

    List<ColumnRow> selectColumns(@Param("boardId") long boardId);

    /** 보드의 모든 카드를 한 번에 (컬럼, POSITION 순). */
    List<CardResponse> selectCardsByBoard(@Param("boardId") long boardId);
}
