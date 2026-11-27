/** 완료 상태 코드 (공통코드 STATUS000). 완료 여부는 컬럼 제목이 아니라 이 값으로 판단한다. */
export const DONE_STATUS = 'STATUS003';

/** 컬럼이 접힌 상태에서 보여주는 카드 수. 카드가 이보다 많을 때만 "더 보기/접기"를 표시한다. */
export const COLLAPSED_CARD_LIMIT = 5;

/** 컬럼 삭제 불가 문구 (서버 문구와 동일해야 한다) */
export const MSG_LAST_DONE_COLUMN = '완료 컬럼은 최소 1개가 필요하므로 삭제할 수 없습니다.';
export const MSG_COLUMN_HAS_CARDS =
  '카드가 있는 컬럼은 삭제할 수 없습니다. 카드를 옮기거나 삭제한 후 다시 시도해 주세요.';
