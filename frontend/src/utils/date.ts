function startOfToday(): Date {
  const d = new Date();
  d.setHours(0, 0, 0, 0);
  return d;
}

function parse(dueDate: string): Date {
  const [y, m, d] = dueDate.split('-').map(Number);
  return new Date(y, m - 1, d);
}

export function isOverdue(dueDate: string): boolean {
  return parse(dueDate).getTime() < startOfToday().getTime();
}

/** '2026-11-20' -> '11월 20일' (지났으면 ' · 지남' 추가) */
export function formatDue(dueDate: string): string {
  const d = parse(dueDate);
  const label = `${d.getMonth() + 1}월 ${d.getDate()}일`;
  return isOverdue(dueDate) ? `${label} · 지남` : label;
}
