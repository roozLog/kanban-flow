import { useState } from 'react';
import {
  DndContext,
  DragOverlay,
  KeyboardSensor,
  PointerSensor,
  closestCorners,
  useSensor,
  useSensors,
  type DragEndEvent,
  type DragStartEvent,
} from '@dnd-kit/core';
import { sortableKeyboardCoordinates } from '@dnd-kit/sortable';
import type { Board as BoardType, Card, Column as ColumnType } from '../types/board';
import { COLLAPSED_CARD_LIMIT, DONE_STATUS } from '../constants';
import { useDeleteColumn, useMoveCard } from '../hooks/useBoard';
import { getColumnDeleteBlockReason } from '../utils/columnDelete';
import { calcPosition } from '../utils/position';
import { CardView } from './Card';
import { Column } from './Column';

interface BoardProps {
  board: BoardType;
  onOpenCard: (card: Card) => void;
  onAddCard: (columnId: number) => void;
  expandedColumns: ReadonlySet<number>;
  onToggleColumn: (columnId: number) => void;
  onExpandColumn: (columnId: number) => void;
}

const parseId = (id: string | number) => Number(String(id).split('-')[1]);

export function Board({
  board,
  onOpenCard,
  onAddCard,
  expandedColumns,
  onToggleColumn,
  onExpandColumn,
}: BoardProps) {
  const [activeCard, setActiveCard] = useState<Card | null>(null);
  const moveCard = useMoveCard();
  const deleteColumn = useDeleteColumn();

  const sensors = useSensors(
    // 5px 이상 움직여야 드래그로 인식 -> 그냥 클릭(카드 열기)과 구분
    useSensor(PointerSensor, { activationConstraint: { distance: 5 } }),
    useSensor(KeyboardSensor, { coordinateGetter: sortableKeyboardCoordinates }),
  );

  const allCards = board.columns.flatMap((c) => c.cards);

  const handleDragStart = ({ active }: DragStartEvent) => {
    setActiveCard(allCards.find((c) => c.id === parseId(active.id)) ?? null);
  };

  const handleDragEnd = ({ active, over }: DragEndEvent) => {
    setActiveCard(null);
    if (!over) return;

    const cardId = parseId(active.id);
    const fromColumn = board.columns.find((c) => c.cards.some((card) => card.id === cardId));
    if (!fromColumn) return;

    const overId = String(over.id);
    const overCardId = overId.startsWith('card-') ? parseId(overId) : null;

    // 놓은 위치의 컬럼 찾기 (카드 위에 놓았는지, 컬럼 빈 영역에 놓았는지)
    const toColumn =
      overCardId !== null
        ? board.columns.find((c) => c.cards.some((card) => card.id === overCardId))
        : board.columns.find((c) => c.id === parseId(overId));
    if (!toColumn) return;

    // 옮기는 카드를 뺀 목록에서 끼워 넣을 위치를 계산
    const rest = toColumn.cards.filter((c) => c.id !== cardId);
    let insertAt = rest.length; // 컬럼 빈 영역: 맨 뒤
    if (overCardId !== null) {
      const overIndexInRest = rest.findIndex((c) => c.id === overCardId);
      const fromIndex = fromColumn.cards.findIndex((c) => c.id === cardId);
      const overIndex = toColumn.cards.findIndex((c) => c.id === overCardId);
      // 같은 컬럼에서 아래로 내릴 때는 대상 카드의 뒤, 그 외에는 앞에 끼운다
      const movingDown = fromColumn.id === toColumn.id && fromIndex < overIndex;
      insertAt = movingDown ? overIndexInRest + 1 : overIndexInRest;
    }

    const sameSpot =
      fromColumn.id === toColumn.id && insertAt === fromColumn.cards.findIndex((c) => c.id === cardId);
    if (sameSpot) return;

    const position = calcPosition(rest[insertAt - 1]?.position, rest[insertAt]?.position);
    // 6번째 이후에 놓이면 접힌 컬럼에서 보이지 않으므로 자동으로 펼친다
    if (insertAt >= COLLAPSED_CARD_LIMIT) onExpandColumn(toColumn.id);
    moveCard.mutate({ cardId, toColumnId: toColumn.id, position });
  };

  const handleDeleteColumn = (column: ColumnType) => {
    // 1) 삭제 가능 여부를 먼저 확인하고, 불가능하면 안내만 한다
    const reason = getColumnDeleteBlockReason(board, column);
    if (reason) return window.alert(reason);
    // 2) 확인한 경우에만 API 호출. 실패하면 컬럼은 그대로 두고 서버 문구를 보여준다
    if (!window.confirm('삭제하시겠습니까?')) return;
    deleteColumn.mutate(column.id, { onError: (error) => window.alert(error.message) });
  };

  return (
    <DndContext
      sensors={sensors}
      collisionDetection={closestCorners}
      onDragStart={handleDragStart}
      onDragEnd={handleDragEnd}
      onDragCancel={() => setActiveCard(null)}
    >
      <main className="board">
        {board.columns.map((column) => (
          <Column
            key={column.id}
            column={column}
            isDone={column.statusCode === DONE_STATUS}
            expanded={expandedColumns.has(column.id)}
            onOpenCard={onOpenCard}
            onAddCard={onAddCard}
            onToggleExpand={onToggleColumn}
            onDeleteColumn={handleDeleteColumn}
          />
        ))}
      </main>

      <DragOverlay dropAnimation={null}>
        {activeCard ? (
          <div className="card card--overlay">
            <CardView card={activeCard} overlay />
          </div>
        ) : null}
      </DragOverlay>
    </DndContext>
  );
}
