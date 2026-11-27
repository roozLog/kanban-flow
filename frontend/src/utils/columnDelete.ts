import { DONE_STATUS, MSG_COLUMN_HAS_CARDS, MSG_LAST_DONE_COLUMN } from '../constants';
import type { Board, Column } from '../types/board';

/**
 * 컬럼을 삭제할 수 없는 이유(문구)를 반환한다. 삭제 가능하면 null.
 * 1) 같은 보드의 마지막 완료(STATUS003) 컬럼이면 카드 유무와 상관없이 거절 (상태 코드로 판단)
 * 2) 카드가 있으면 거절
 * 서버도 같은 규칙으로 검사한다. 이쪽은 불필요한 요청을 막기 위한 사전 확인이다.
 */
export function getColumnDeleteBlockReason(board: Board, column: Column): string | null {
  const doneCount = board.columns.filter((c) => c.statusCode === DONE_STATUS).length;
  if (column.statusCode === DONE_STATUS && doneCount <= 1) return MSG_LAST_DONE_COLUMN;
  if (column.cards.length > 0) return MSG_COLUMN_HAS_CARDS;
  return null;
}
