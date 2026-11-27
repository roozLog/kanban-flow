package com.kanbanflow.mapper;

import java.math.BigDecimal;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface ColumnMapper {

    /** 보드 행을 잠근다. 보드가 없으면 null. */
    Long lockBoard(@Param("boardId") long boardId);

    BigDecimal selectNextColumnPosition(@Param("boardId") long boardId);

    void insertColumn(@Param("boardId") long boardId, @Param("title") String title,
                      @Param("statusCode") String statusCode, @Param("position") BigDecimal position);

    /** 컬럼 행을 잠그고 소속 보드를 반환한다. 컬럼이 없으면 null. (삭제와 카드 추가/이동을 직렬화) */
    Long lockColumn(@Param("columnId") long columnId);

    String selectStatusCode(@Param("columnId") long columnId);

    /** 같은 보드에서 해당 상태 코드를 가진 컬럼 수. */
    int countColumnsByStatus(@Param("boardId") long boardId, @Param("statusCode") String statusCode);

    int countCards(@Param("columnId") long columnId);

    int deleteColumn(@Param("columnId") long columnId);
}
