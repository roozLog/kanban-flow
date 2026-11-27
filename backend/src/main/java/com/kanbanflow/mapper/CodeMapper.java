package com.kanbanflow.mapper;

import com.kanbanflow.dto.CodeResponse;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CodeMapper {

    @Select("""
            SELECT CODE_CD AS code, CODE_NAME AS name
              FROM TG_CODE
             WHERE GROUP_CD = #{groupCd} AND USE_YN = 'Y'
             ORDER BY SORT_ORDER, CODE_CD
            """)
    List<CodeResponse> selectActiveCodes(@Param("groupCd") String groupCd);

    @Select("""
            SELECT COUNT(*) FROM TG_CODE
             WHERE GROUP_CD = #{groupCd} AND CODE_CD = #{codeCd} AND USE_YN = 'Y'
            """)
    int countActive(@Param("groupCd") String groupCd, @Param("codeCd") String codeCd);
}
