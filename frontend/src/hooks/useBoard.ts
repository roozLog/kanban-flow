import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query';
import { boardApi } from '../api/boardApi';
import type { Board, CardInput, ColumnInput, MoveCardInput } from '../types/board';

const BOARD_KEY = ['board'] as const;

export function useBoard() {
  return useQuery({ queryKey: BOARD_KEY, queryFn: boardApi.getBoard });
}

/** 컬럼 추가 UI용 상태 코드 목록 (STATUS000). 거의 바뀌지 않으므로 오래 캐시한다. */
export function useStatusCodes() {
  return useQuery({
    queryKey: ['codes', 'STATUS000'],
    queryFn: boardApi.getStatusCodes,
    staleTime: 5 * 60 * 1000,
  });
}

/** 서버 응답(최신 보드)을 캐시에 그대로 반영하는 공통 처리 */
function useBoardMutation<TVars>(mutationFn: (vars: TVars) => Promise<Board>) {
  const queryClient = useQueryClient();
  return useMutation({
    mutationFn,
    onSuccess: (board) => queryClient.setQueryData(BOARD_KEY, board),
  });
}

export const useAddColumn = () => useBoardMutation((input: ColumnInput) => boardApi.addColumn(input));

export const useDeleteColumn = () => useBoardMutation((columnId: number) => boardApi.deleteColumn(columnId));

export const useCreateCard =() => useBoardMutation((input: CardInput) => boardApi.createCard(input));

export const useUpdateCard = () =>
  useBoardMutation(({ cardId, input }: { cardId: number; input: CardInput }) =>
    boardApi.updateCard(cardId, input),
  );

export const useDeleteCard = () => useBoardMutation((cardId: number) => boardApi.deleteCard(cardId));

/**
 * 카드 이동: 낙관적 업데이트.
 * 1) 요청 전에 캐시를 먼저 바꿔 화면을 즉시 갱신하고
 * 2) 실패하면 이전 상태로 되돌리고
 * 3) 끝나면 서버 기준으로 다시 맞춘다.
 */
export function useMoveCard() {
  const queryClient = useQueryClient();

  return useMutation({
    mutationFn: (input: MoveCardInput) => boardApi.moveCard(input),

    onMutate: async ({ cardId, toColumnId, position }) => {
      await queryClient.cancelQueries({ queryKey: BOARD_KEY });
      const previous = queryClient.getQueryData<Board>(BOARD_KEY);

      queryClient.setQueryData<Board>(BOARD_KEY, (board) => {
        if (!board) return board;
        const moving = board.columns.flatMap((c) => c.cards).find((c) => c.id === cardId);
        if (!moving) return board;
        return {
          ...board,
          columns: board.columns.map((column) => {
            const rest = column.cards.filter((c) => c.id !== cardId);
            if (column.id !== toColumnId) return { ...column, cards: rest };
            const moved = { ...moving, columnId: toColumnId, position };
            return { ...column, cards: [...rest, moved].sort((a, b) => a.position - b.position) };
          }),
        };
      });

      return { previous };
    },

    onError: (_error, _input, context) => {
      if (context?.previous) queryClient.setQueryData(BOARD_KEY, context.previous);
    },

    onSettled: () => queryClient.invalidateQueries({ queryKey: BOARD_KEY }),
  });
}
