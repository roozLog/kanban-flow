package com.kanbanflow.service;

import com.kanbanflow.dto.CodeResponse;
import com.kanbanflow.dto.ColumnRequest;
import com.kanbanflow.exception.BadRequestException;
import com.kanbanflow.exception.NotFoundException;
import com.kanbanflow.mapper.CodeMapper;
import com.kanbanflow.mapper.ColumnMapper;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ColumnService {

    public static final String STATUS_GROUP = "STATUS000";
    public static final String DONE_STATUS = "STATUS003";
    public static final String MSG_LAST_DONE = "완료 컬럼은 최소 1개가 필요하므로 삭제할 수 없습니다.";
    public static final String MSG_HAS_CARDS = "카드가 있는 컬럼은 삭제할 수 없습니다. 카드를 옮기거나 삭제한 후 다시 시도해 주세요.";
    static final int TITLE_MAX = 100;

    private final ColumnMapper columnMapper;
    private final CodeMapper codeMapper;

    public ColumnService(ColumnMapper columnMapper, CodeMapper codeMapper) {
        this.columnMapper = columnMapper;
        this.codeMapper = codeMapper;
    }

    @Transactional
    public long create(long boardId, ColumnRequest req) {
        if (req.title() == null || req.title().isBlank()) {
            throw new BadRequestException("컬럼 이름은 필수입니다.");
        }
        if (req.title().codePointCount(0, req.title().length()) > TITLE_MAX) {
            throw new BadRequestException("컬럼 이름은 " + TITLE_MAX + "자 이하여야 합니다.");
        }
        if (req.statusCode() == null || codeMapper.countActive(STATUS_GROUP, req.statusCode()) == 0) {
            throw new BadRequestException("사용할 수 없는 상태 코드입니다: " + req.statusCode());
        }
        if (columnMapper.lockBoard(boardId) == null) {
            throw new NotFoundException("보드를 찾을 수 없습니다. id=" + boardId);
        }
        columnMapper.insertColumn(boardId, req.title().trim(), req.statusCode(),
                columnMapper.selectNextColumnPosition(boardId));
        return boardId;
    }

    /**
     * 컬럼 삭제. 검사와 삭제는 한 트랜잭션.
     * 잠금 순서: 컬럼 행 → 보드 행.
     * - 컬럼 행 잠금: 같은 컬럼으로의 카드 추가/이동(CardService도 컬럼 행을 잠금)과 직렬화되어,
     *   검사 후 카드가 끼어드는 것을 막는다.
     * - 보드 행 잠금: 같은 보드의 다른 완료 컬럼 삭제와 직렬화되어, 두 요청이 동시에
     *   "완료 컬럼 2개"를 보고 둘 다 지우는 것을 막는다.
     * 1) 마지막 완료 컬럼이면 거절(카드 유무와 상관없이 이 문구가 우선), 2) 카드가 있으면 거절.
     */
    @Transactional
    public long delete(long columnId) {
        Long boardId = columnMapper.lockColumn(columnId);
        if (boardId == null) {
            throw new NotFoundException("컬럼을 찾을 수 없습니다. id=" + columnId);
        }
        columnMapper.lockBoard(boardId);

        if (DONE_STATUS.equals(columnMapper.selectStatusCode(columnId))
                && columnMapper.countColumnsByStatus(boardId, DONE_STATUS) <= 1) {
            throw new BadRequestException(MSG_LAST_DONE);
        }
        if (columnMapper.countCards(columnId) > 0) {
            throw new BadRequestException(MSG_HAS_CARDS);
        }
        columnMapper.deleteColumn(columnId);
        return boardId;
    }

    @Transactional(readOnly = true)
    public List<CodeResponse> codes(String groupCd) {
        return codeMapper.selectActiveCodes(groupCd);
    }
}
