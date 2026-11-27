import { useDroppable } from '@dnd-kit/core';
import { SortableContext, verticalListSortingStrategy } from '@dnd-kit/sortable';
import { COLLAPSED_CARD_LIMIT } from '../constants';
import type { Card, Column as ColumnType } from '../types/board';
import { SortableCard } from './Card';
import { ColumnMenu } from './ColumnMenu';
import { PlusIcon } from './Icons';

interface ColumnProps {
  column: ColumnType;
  isDone: boolean;
  expanded: boolean;
  onOpenCard: (card: Card) => void;
  onAddCard: (columnId: number) => void;
  onDeleteColumn: (column: ColumnType) => void;
  onToggleExpand: (columnId: number) => void;
}

export function Column({
  column,
  isDone,
  expanded,
  onOpenCard,
  onAddCard,
  onDeleteColumn,
  onToggleExpand,
}: ColumnProps) {
  // 비어 있는 컬럼에도 카드를 놓을 수 있도록 컬럼 자체를 드롭 영역으로 등록
  const { setNodeRef, isOver } = useDroppable({ id: `col-${column.id}` });

  // 카드가 한도보다 많을 때만 접기/펼치기가 의미가 있다 (헤더의 개수는 항상 전체 카드 수)
  const collapsible = column.cards.length > COLLAPSED_CARD_LIMIT;
  const visibleCards = collapsible && !expanded ? column.cards.slice(0, COLLAPSED_CARD_LIMIT) : column.cards;
  const hiddenCount = column.cards.length - visibleCards.length;

  return (
    <section
      ref={setNodeRef}
      aria-label={column.title}
      className={`column${isOver ? ' column--over' : ''}`}
    >
      <div className="column__head">
        <h2 className="column__title" title={column.title}>
          {column.title}
        </h2>
        <span className="column__count">{column.cards.length}</span>
        <div className="column__actions">
          <button
            type="button"
            className="column__icon-btn"
            aria-label={`${column.title} 컬럼에 카드 추가`}
            title="카드 추가 (맨 끝에 추가됩니다)"
            onClick={() => onAddCard(column.id)}
          >
            <PlusIcon />
          </button>
          <ColumnMenu columnTitle={column.title} onDelete={() => onDeleteColumn(column)} />
        </div>
      </div>

      {/* 화면에 그려진 카드만 정렬 대상으로 등록한다 (숨겨진 카드는 드롭 대상이 아님) */}
      <SortableContext
        items={visibleCards.map((c) => `card-${c.id}`)}
        strategy={verticalListSortingStrategy}
      >
        <div className="column__cards" role="list">
          {visibleCards.map((card) => (
            <SortableCard key={card.id} card={card} done={isDone} onOpen={onOpenCard} />
          ))}
        </div>
      </SortableContext>

      {collapsible && (
        <button
          type="button"
          className="column__more"
          aria-expanded={expanded}
          onClick={() => onToggleExpand(column.id)}
        >
          {expanded ? '접기' : `${hiddenCount}개 더 보기`}
        </button>
      )}
    </section>
  );
}
