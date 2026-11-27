import type { Board, CardInput, ColumnInput, MoveCardInput, StatusCode } from '../types/board';
import { mockApi } from './mockDb';

/**
 * 화면은 이 파일의 `boardApi`만 바라본다.
 * 기본은 목 API. Spring이 준비되면 `.env`에 VITE_USE_MOCK=false 를 넣으면 실제 API를 호출한다.
 */
const USE_MOCK = import.meta.env.VITE_USE_MOCK !== 'false';
const BOARD_ID = 1; // 보드는 하나로 고정

async function http<T>(url: string, init?: RequestInit): Promise<T> {
  const res = await fetch(url, {
    headers: { 'Content-Type': 'application/json' },
    ...init,
  });
  if (!res.ok) {
    // 서버가 { message, code }를 내려주면 그 문구를 그대로 쓴다
    let message = `요청 실패 (${res.status})`;
    try {
      const body = await res.json();
      if (typeof body?.message === 'string') message = body.message;
    } catch {
      // 본문이 없거나 JSON이 아니면 기본 문구
    }
    throw new Error(message);
  }
  return res.status === 204 ? (undefined as T) : res.json();
}

const realApi = {
  getBoard: () => http<Board>(`/api/boards/${BOARD_ID}`),

  getStatusCodes: () => http<StatusCode[]>('/api/codes/STATUS000'),

  addColumn: (input: ColumnInput) =>
    http<Board>(`/api/boards/${BOARD_ID}/columns`, { method: 'POST', body: JSON.stringify(input) }),

  deleteColumn: (columnId: number) => http<Board>(`/api/columns/${columnId}`, { method: 'DELETE' }),

  createCard: (input: CardInput) =>
    http<Board>(`/api/columns/${input.columnId}/cards`, { method: 'POST', body: JSON.stringify(input) }),

  updateCard: (cardId: number, input: CardInput) =>
    http<Board>(`/api/cards/${cardId}`, { method: 'PATCH', body: JSON.stringify(input) }),

  moveCard: ({ cardId, toColumnId, position }: MoveCardInput) =>
    http<Board>(`/api/cards/${cardId}/move`, {
      method: 'PATCH',
      body: JSON.stringify({ toColumnId, position }),
    }),

  deleteCard: (cardId: number) => http<Board>(`/api/cards/${cardId}`, { method: 'DELETE' }),
};

export const boardApi = USE_MOCK ? mockApi : realApi;
