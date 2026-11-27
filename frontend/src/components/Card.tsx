import { useSortable } from '@dnd-kit/sortable';
import { CSS } from '@dnd-kit/utilities';
import type { Card as CardType } from '../types/board';
import { formatDue, isOverdue } from '../utils/date';
import { CheckCircleIcon } from './Icons';

interface CardViewProps {
  card: CardType;
  done?: boolean;
  overlay?: boolean;
  onOpen?: (card: CardType) => void;
}

/** 카드의 생김새만 담당 (드래그 로직은 SortableCard가 감싼다) */
export function CardView({ card, done, overlay, onOpen }: CardViewProps) {
  const overdue = card.dueDate !== null && !done && isOverdue(card.dueDate);

  return (
    <>
      <div className="card__row">
        {done && (
          <span className="card__check">
            <CheckCircleIcon />
          </span>
        )}
        <button
          type="button"
          className="card__title"
          onClick={() => onOpen?.(card)}
          tabIndex={overlay ? -1 : 0}
        >
          {card.title}
        </button>
      </div>
      {card.description && !done && <p className="card__desc">{card.description}</p>}
      {card.dueDate && !done && (
        <span className={`card__due${overdue ? ' card__due--overdue' : ''}`}>
          {formatDue(card.dueDate)}
        </span>
      )}
    </>
  );
}

interface SortableCardProps {
  card: CardType;
  done: boolean;
  onOpen: (card: CardType) => void;
}

export function SortableCard({ card, done, onOpen }: SortableCardProps) {
  const { attributes, listeners, setNodeRef, setActivatorNodeRef, transform, transition, isDragging } =
    useSortable({ id: `card-${card.id}` });

  return (
    <article
      ref={(node) => {
        setNodeRef(node);
        setActivatorNodeRef(node);
      }}
      style={{ transform: CSS.Transform.toString(transform), transition }}
      className={`card${done ? ' card--done' : ''}${isDragging ? ' card--placeholder' : ''}`}
      {...attributes}
      {...listeners}
      role="listitem"
    >
      <CardView card={card} done={done} onOpen={onOpen} />
    </article>
  );
}
