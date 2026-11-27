import { useState } from 'react';
import { useBoard } from './hooks/useBoard';
import { Board } from './components/Board';
import { CardModal, type ModalState } from './components/CardModal';
import { ColumnModal } from './components/ColumnModal';
import { PlusIcon } from './components/Icons';

export default function App() {
  const { data: board, isLoading, isError, refetch } = useBoard();
  const [modal, setModal] = useState<ModalState | null>(null);
  const [addingColumn, setAddingColumn] = useState(false);
  // 컬럼별 펼침 상태. 보드 데이터가 갱신돼도 유지되고, 새로고침하면 모두 접힌 상태로 돌아간다.
  const [expandedColumns, setExpandedColumns] = useState<ReadonlySet<number>>(new Set());

  const toggleColumn = (columnId: number) =>
    setExpandedColumns((prev) => {
      const next = new Set(prev);
      if (!next.delete(columnId)) next.add(columnId);
      return next;
    });

  const expandColumn = (columnId: number) =>
    setExpandedColumns((prev) => (prev.has(columnId) ? prev : new Set(prev).add(columnId)));

  return (
    <div className="app">
      <header className="app-header">
        <div>
          <span className="app-header__label">BOARD</span>
          <h1 className="app-header__title">{board?.title ?? '칸반 보드'}</h1>
        </div>
        <div className="app-header__actions">
          <button type="button" className="btn" onClick={() => setAddingColumn(true)} disabled={!board}>
            <PlusIcon />
            컬럼 추가
          </button>
          <button
            type="button"
            className="btn btn--primary"
            disabled={!board || board.columns.length === 0}
            onClick={() => board && setModal({ mode: 'create', columnId: board.columns[0].id })}
          >
            <PlusIcon />
            새 카드
          </button>
        </div>
      </header>

      {isLoading && <p className="status">보드를 불러오는 중…</p>}

      {isError && (
        <p className="status status--error">
          보드를 불러오지 못했어요.{' '}
          <button type="button" className="btn" onClick={() => refetch()}>
            다시 시도
          </button>
        </p>
      )}

      {board && (
        <Board
          board={board}
          onOpenCard={(card) => setModal({ mode: 'edit', card })}
          onAddCard={(columnId) => setModal({ mode: 'create', columnId })}
          expandedColumns={expandedColumns}
          onToggleColumn={toggleColumn}
          onExpandColumn={expandColumn}
        />
      )}

      {board && modal && (
        <CardModal
          // 카드가 바뀌면 폼 상태를 새로 초기화
          key={modal.mode === 'edit' ? `edit-${modal.card.id}` : `create-${modal.columnId}`}
          state={modal}
          columns={board.columns}
          onExpandColumn={expandColumn}
          onClose={() => setModal(null)}
        />
      )}

      {board && addingColumn && <ColumnModal onClose={() => setAddingColumn(false)} />}
    </div>
  );
}
