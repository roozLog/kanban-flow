const GAP = 1000;

/**
 * 카드를 prev와 next 사이에 끼울 때 쓸 POSITION 값을 계산한다.
 * - 둘 다 없음: 빈 컬럼의 첫 카드
 * - prev만 없음: 맨 앞으로 이동
 * - next만 없음: 맨 뒤로 이동
 * - 둘 다 있음: 중간값 (간격이 너무 좁아지면 서버에서 재정렬해야 한다)
 */
export function calcPosition(prev?: number, next?: number): number {
  if (prev === undefined && next === undefined) return GAP;
  if (prev === undefined) return (next as number) / 2;
  if (next === undefined) return prev + GAP;
  return (prev + next) / 2;
}
