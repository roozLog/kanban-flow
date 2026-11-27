import { useEffect, useState, type FormEvent } from 'react';
import { useAddColumn, useStatusCodes } from '../hooks/useBoard';
import { CloseIcon } from './Icons';

interface ColumnModalProps {
  onClose: () => void;
}

/** 컬럼 추가 모달. 상태는 공통코드(STATUS000) 목록에서 고른다. */
export function ColumnModal({ onClose }: ColumnModalProps) {
  const [title, setTitle] = useState('');
  const [status, setStatus] = useState('');
  const addColumn = useAddColumn();
  const statusCodes = useStatusCodes();

  // 선택하지 않았으면 목록의 첫 번째 상태를 기본값으로 쓴다
  const selectedStatus = status || statusCodes.data?.[0]?.code || '';

  // Esc로 닫기
  useEffect(() => {
    const onKey = (e: KeyboardEvent) => e.key === 'Escape' && onClose();
    window.addEventListener('keydown', onKey);
    return () => window.removeEventListener('keydown', onKey);
  }, [onClose]);

  const handleSubmit = (e: FormEvent) => {
    e.preventDefault();
    const trimmed = title.trim();
    if (!trimmed || !selectedStatus) return;
    addColumn.mutate({ title: trimmed, statusCode: selectedStatus }, { onSuccess: onClose });
  };

  return (
    <div className="overlay" onMouseDown={(e) => e.target === e.currentTarget && onClose()}>
      <form className="modal modal--sm" role="dialog" aria-modal="true" aria-label="컬럼 추가" onSubmit={handleSubmit}>
        <div className="modal__head">
          <h2 className="modal__heading">컬럼 추가</h2>
          <button type="button" className="icon-btn" aria-label="닫기" onClick={onClose}>
            <CloseIcon />
          </button>
        </div>

        <div className="modal__body">
          <div className="modal__main">
            <div className="field">
              <label htmlFor="new-column-title" className="field__label">
                컬럼 이름
              </label>
              <input
                id="new-column-title"
                className="input"
                placeholder="컬럼 이름"
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                autoFocus
                required
              />
            </div>
            <div className="field">
              <label htmlFor="new-column-status" className="field__label">
                상태
              </label>
              <select
                id="new-column-status"
                className="input"
                value={selectedStatus}
                onChange={(e) => setStatus(e.target.value)}
                disabled={!statusCodes.data}
              >
                {statusCodes.data?.map((s) => (
                  <option key={s.code} value={s.code}>
                    {s.name}
                  </option>
                ))}
              </select>
              {statusCodes.isError && <p className="error-text">상태 목록을 불러오지 못했어요.</p>}
            </div>
            {addColumn.error && <p className="error-text">{addColumn.error.message}</p>}
          </div>
        </div>

        <div className="modal__foot">
          <span />
          <div className="modal__foot-right">
            <button type="button" className="btn" onClick={onClose}>
              취소
            </button>
            <button type="submit" className="btn btn--primary" disabled={addColumn.isPending || !selectedStatus}>
              {addColumn.isPending ? '추가 중…' : '추가'}
            </button>
          </div>
        </div>
      </form>
    </div>
  );
}
