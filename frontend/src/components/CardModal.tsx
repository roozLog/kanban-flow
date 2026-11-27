import { useEffect, useState, type FormEvent } from 'react';
import type { Card, Column } from '../types/board';
import { COLLAPSED_CARD_LIMIT } from '../constants';
import { useCreateCard, useDeleteCard, useUpdateCard } from '../hooks/useBoard';
import { CloseIcon, TrashIcon } from './Icons';

export type ModalState =
  | { mode: 'create'; columnId: number }
  | { mode: 'edit'; card: Card };

interface CardModalProps {
  state: ModalState;
  columns: Column[];
  onExpandColumn: (columnId: number) => void;
  onClose: () => void;
}

export function CardModal({ state, columns, onExpandColumn, onClose }: CardModalProps) {
  const editing = state.mode === 'edit' ? state.card : null;

  const [title, setTitle] = useState(editing?.title ?? '');
  const [description, setDescription] = useState(editing?.description ?? '');
  const [columnId, setColumnId] = useState(editing?.columnId ?? (state.mode === 'create' ? state.columnId : columns[0].id));
  const [dueDate, setDueDate] = useState(editing?.dueDate ?? '');

  const createCard = useCreateCard();
  const updateCard = useUpdateCard();
  const deleteCard = useDeleteCard();
  const pending = createCard.isPending || updateCard.isPending || deleteCard.isPending;
  const error = createCard.error ?? updateCard.error ?? deleteCard.error;

  // Esc로 닫기
  useEffect(() => {
    const onKey = (e: KeyboardEvent) => e.key === 'Escape' && onClose();
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [onClose]);

  const columnTitle = columns.find((c) => c.id === columnId)?.title ?? '';

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault();
    if (!title.trim()) return;
    const input = { title: title.trim(), description, columnId, dueDate: dueDate || null };
    // 새 카드이거나 컬럼이 바뀐 카드는 대상 컬럼의 맨 끝에 놓인다. 그 위치가 6번째 이후면 컬럼을 펼친다.
    const landsAtEnd = !editing || columnId !== editing.columnId;
    const endIndex = columns.find((c) => c.id === columnId)?.cards.length ?? 0;
    const options = {
      onSuccess: () => {
        if (landsAtEnd && endIndex >= COLLAPSED_CARD_LIMIT) onExpandColumn(columnId);
        onClose();
      },
    };
    if (editing) updateCard.mutate({ cardId: editing.id, input }, options);
    else createCard.mutate(input, options);
  };

  const handleDelete = () => {
    if (!editing) return;
    if (!window.confirm('삭제하시겠습니까?')) return;
    deleteCard.mutate(editing.id, { onSuccess: onClose });
  };

  return (
    <div className="overlay" onMouseDown={(e) => e.target === e.currentTarget && onClose()}>
      <form className="modal" role="dialog" aria-modal="true" aria-label="카드 상세" onSubmit={handleSubmit}>
        <div className="modal__head">
          <div style={{ flex: 1 }}>
            <span className="modal__sub">{columnTitle} 컬럼의 카드</span>
            <label htmlFor="card-title" className="sr-only">
              카드 제목
            </label>
            <input
              id="card-title"
              className="input input--title"
              placeholder="카드 제목"
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              autoFocus
              required
            />
          </div>
          <button type="button" className="icon-btn" aria-label="닫기" onClick={onClose}>
            <CloseIcon />
          </button>
        </div>

        <div className="modal__body">
          <div className="modal__main">
            <label htmlFor="card-desc" className="field__label">
              설명
            </label>
            <textarea
              id="card-desc"
              className="textarea"
              value={description}
              onChange={(e) => setDescription(e.target.value)}
            />
          </div>

          <div className="modal__side">
            <div className="field">
              <label htmlFor="card-col" className="field__label">
                컬럼
              </label>
              <select
                id="card-col"
                className="input"
                value={columnId}
                onChange={(e) => setColumnId(Number(e.target.value))}
              >
                {columns.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.title}
                  </option>
                ))}
              </select>
            </div>
            <div className="field">
              <label htmlFor="card-due" className="field__label">
                마감일
              </label>
              <input
                id="card-due"
                type="date"
                className="input"
                value={dueDate}
                onChange={(e) => setDueDate(e.target.value)}
              />
            </div>
            {error && <p className="error-text">저장하지 못했어요. 잠시 후 다시 시도해 주세요.</p>}
          </div>
        </div>

        <div className="modal__foot">
          {editing ? (
            <button
              type="button"
              className="btn btn--danger"
              onClick={handleDelete}
              disabled={pending}
            >
              <TrashIcon />
              카드 삭제
            </button>
          ) : (
            <span />
          )}
          <div className="modal__foot-right">
            <button type="button" className="btn" onClick={onClose}>
              취소
            </button>
            <button type="submit" className="btn btn--primary" disabled={pending}>
              {pending ? '저장 중…' : '저장'}
            </button>
          </div>
        </div>
      </form>
    </div>
  );
}
