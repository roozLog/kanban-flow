import { useEffect, useRef, useState } from 'react';
import { MoreIcon, TrashIcon } from './Icons';

interface ColumnMenuProps {
  columnTitle: string;
  onDelete: () => void;
}

/** 컬럼 헤더의 더보기(⋯) 메뉴. 지금은 "컬럼 삭제" 한 항목만 있다. */
export function ColumnMenu({ columnTitle, onDelete }: ColumnMenuProps) {
  const [open, setOpen] = useState(false);
  const rootRef = useRef<HTMLDivElement>(null);
  const itemRef = useRef<HTMLButtonElement>(null);
  const triggerRef = useRef<HTMLButtonElement>(null);

  // 열려 있는 동안: 바깥 클릭/Esc로 닫기. 열리면 첫 항목에 포커스.
  useEffect(() => {
    if (!open) return;
    itemRef.current?.focus();
    const onPointerDown = (e: PointerEvent) => {
      if (!rootRef.current?.contains(e.target as Node)) setOpen(false);
    };
    const onKey = (e: KeyboardEvent) => {
      if (e.key !== 'Escape') return;
      e.stopPropagation();
      setOpen(false);
      triggerRef.current?.focus();
    };
    document.addEventListener('pointerdown', onPointerDown);
    document.addEventListener('keydown', onKey);
    return () => {
      document.removeEventListener('pointerdown', onPointerDown);
      document.removeEventListener('keydown', onKey);
    };
  }, [open]);

  return (
    <div className="column-menu" ref={rootRef}>
      <button
        type="button"
        ref={triggerRef}
        className="column__icon-btn"
        aria-label={`${columnTitle} 컬럼 메뉴`}
        aria-haspopup="menu"
        aria-expanded={open}
        title="더보기"
        onClick={() => setOpen((v) => !v)}
      >
        <MoreIcon />
      </button>

      {open && (
        <div className="column-menu__list" role="menu">
          <button
            type="button"
            ref={itemRef}
            role="menuitem"
            className="column-menu__item column-menu__item--danger"
            onClick={() => {
              setOpen(false);
              onDelete();
            }}
          >
            <TrashIcon />
            컬럼 삭제
          </button>
        </div>
      )}
    </div>
  );
}
