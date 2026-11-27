import type { Board, Card, CardInput, ColumnInput, MoveCardInput, StatusCode } from '../types/board';
import { getColumnDeleteBlockReason } from '../utils/columnDelete';

/**
 * 백엔드(Spring + Oracle)가 붙기 전까지 쓰는 메모리 DB.
 * 응답 구조와 함수 시그니처는 실제 API 목록과 동일하게 맞춰 두었다.
 */

const LATENCY_MS = 400; // 낙관적 업데이트가 눈에 보이도록 일부러 지연

const wait = <T,>(value: T): Promise<T> =>
  new Promise((resolve) => setTimeout(() => resolve(structuredClone(value)), LATENCY_MS));

let nextId = 100;

/** 공통코드 STATUS000 (GET /api/codes/STATUS000 의 mock) */
const statusCodes: StatusCode[] = [
  { code: 'STATUS001', name: '할 일' },
  { code: 'STATUS002', name: '진행 중' },
  { code: 'STATUS003', name: '완료' },
  { code: 'STATUS004', name: '보류' },
];

const db: Board = {
  id: 1,
  title: '나의 프로젝트 보드',
  columns: [
    {
      id: 1,
      title: '할 일',
      statusCode: 'STATUS001',
      position: 1000,
      cards: [
        { id: 1, columnId: 1, position: 1000, title: 'Oracle 스키마 DDL 작성', description: 'BOARD, BOARD_COLUMN, CARD 테이블과 인덱스 생성', dueDate: '2026-11-20' },
        { id: 2, columnId: 1, position: 2000, title: '카드 이동 API 설계', description: 'POSITION 간격 방식으로 순서 관리, 재정렬 기준 정하기', dueDate: '2026-11-16' },
        { id: 3, columnId: 1, position: 3000, title: '드래그앤드롭 라이브러리 검토', description: '', dueDate: '2026-11-24' },
        { id: 4, columnId: 1, position: 4000, title: 'README에 설계 의도 정리', description: '낙관적 업데이트를 선택한 이유 기록', dueDate: null },
      ],
    },
    {
      id: 2,
      title: '진행 중',
      statusCode: 'STATUS002',
      position: 2000,
      cards: [
        { id: 5, columnId: 2, position: 1000, title: '보드 조회 API (Spring)', description: '보드, 컬럼, 카드를 한 번에 내려주는 GET 엔드포인트', dueDate: '2026-11-19' },
        { id: 6, columnId: 2, position: 2000, title: '보드 렌더링 (읽기 전용)', description: 'TanStack Query로 조회, 로딩과 빈 상태 처리', dueDate: '2026-11-22' },
        { id: 7, columnId: 2, position: 3000, title: 'Vite + React + TS 초기 세팅', description: 'ESLint, Prettier, 폴더 구조 정리', dueDate: null },
      ],
    },
    {
      id: 3,
      title: '완료',
      statusCode: 'STATUS003',
      position: 3000,
      cards: [
        { id: 8, columnId: 3, position: 1000, title: '요구사항 정리와 화면 범위 확정', description: '', dueDate: null },
        { id: 9, columnId: 3, position: 2000, title: 'GitHub 저장소와 브랜치 규칙 설정', description: '', dueDate: null },
      ],
    },
  ],
};

const sortCards = (cards: Card[]) => cards.sort((a, b) => a.position - b.position);

function findCard(cardId: number) {
  for (const column of db.columns) {
    const index = column.cards.findIndex((c) => c.id === cardId);
    if (index >= 0) return { column, index, card: column.cards[index] };
  }
  throw new Error('카드를 찾을 수 없습니다.');
}

function findColumn(columnId: number) {
  const column = db.columns.find((c) => c.id === columnId);
  if (!column) throw new Error('컬럼을 찾을 수 없습니다.');
  return column;
}

/** 간격이 너무 좁아졌을 때 컬럼의 POSITION을 1000 단위로 다시 매긴다 (서버가 해야 할 일). */
function rebalance(columnId: number) {
  sortCards(findColumn(columnId).cards).forEach((card, i) => {
    card.position = (i + 1) * 1000;
  });
}

export const mockApi = {
  getBoard: () => {
    db.columns.sort((a, b) => a.position - b.position).forEach((c) => sortCards(c.cards));
    return wait(db);
  },

  getStatusCodes: () => wait(statusCodes),

  addColumn: (input: ColumnInput) => {
    const last = db.columns[db.columns.length - 1];
    db.columns.push({
      id: nextId++,
      title: input.title,
      statusCode: input.statusCode,
      position: (last?.position ?? 0) + 1000,
      cards: [],
    });
    return wait(db);
  },

  deleteColumn: (columnId: number) => {
    const column = findColumn(columnId);
    const reason = getColumnDeleteBlockReason(db, column);
    if (reason) return Promise.reject(new Error(reason));
    db.columns.splice(db.columns.indexOf(column), 1);
    return wait(db);
  },

  createCard: (input: CardInput) => {
    const column = findColumn(input.columnId);
    const last = column.cards[column.cards.length - 1];
    column.cards.push({ id: nextId++, ...input, position: (last?.position ?? 0) + 1000 });
    return wait(db);
  },

  updateCard: (cardId: number, input: CardInput) => {
    const { column, index, card } = findCard(cardId);
    if (input.columnId !== card.columnId) {
      // 컬럼이 바뀌면 대상 컬럼의 맨 뒤로 보낸다
      column.cards.splice(index, 1);
      const target = findColumn(input.columnId);
      const last = target.cards[target.cards.length - 1];
      target.cards.push({ ...card, ...input, position: (last?.position ?? 0) + 1000 });
    } else {
      Object.assign(card, input);
    }
    return wait(db);
  },

  moveCard: ({ cardId, toColumnId, position }: MoveCardInput) => {
    const { column, index, card } = findCard(cardId);
    column.cards.splice(index, 1);
    const target = findColumn(toColumnId);
    target.cards.push({ ...card, columnId: toColumnId, position });
    sortCards(target.cards);
    const gaps = target.cards.slice(1).map((c, i) => c.position - target.cards[i].position);
    if (gaps.some((g) => g < 1)) rebalance(toColumnId);
    return wait(db);
  },

  deleteCard: (cardId: number) => {
    const { column, index } = findCard(cardId);
    column.cards.splice(index, 1);
    return wait(db);
  },
};
