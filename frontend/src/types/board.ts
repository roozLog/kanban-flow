export interface Card {
  id: number;
  columnId: number;
  title: string;
  description: string | null; // 서버는 설명이 없으면 null을 내려준다
  position: number; // 컬럼 내 순서. 1000 간격으로 두고 사이에 끼울 때 중간값 사용
  dueDate: string | null; // 'YYYY-MM-DD'
}

export interface Column {
  id: number;
  title: string;
  statusCode: string; // 상태(진행사항)는 컬럼에만 있다. 카드에는 없다.
  position: number;
  cards: Card[];
}

export interface Board {
  id: number;
  title: string;
  columns: Column[];
}

export interface CardInput {
  title: string;
  description: string;
  columnId: number;
  dueDate: string | null;
}

export interface ColumnInput {
  title: string;
  statusCode: string;
}

/** GET /api/codes/STATUS000 의 항목 */
export interface StatusCode {
  code: string;
  name: string;
}

export interface MoveCardInput {
  cardId: number;
  toColumnId: number;
  position: number;
}
