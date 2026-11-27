# 칸반 보드

React + TypeScript + Vite 기반 칸반 보드. 백엔드(Spring + Oracle)가 붙기 전에는 메모리 목 API로 동작한다.

## 실행

```bash
npm ci
npm run dev
```

백엔드 없이 실행하면 목 API로 동작한다. 실제 API는 `.env`에 `VITE_USE_MOCK=false`를 넣는다. (저장소 루트 README 참고)

## 스택

- React 18, TypeScript, Vite
- TanStack Query: 서버 상태, 낙관적 업데이트
- @dnd-kit: 드래그앤드롭 (컬럼 간 이동, 컬럼 안 순서 변경)

## 구조

```
src/
  api/         boardApi.ts (목/실제 API 전환), mockDb.ts (메모리 DB)
  hooks/       useBoard.ts (조회, 생성, 수정, 삭제, 이동)
  components/  Board, Column, ColumnMenu, ColumnModal, Card, CardModal, Icons
  utils/       position.ts (순서 계산), date.ts (마감일 표시), columnDelete.ts (컬럼 삭제 가능 여부)
  constants.ts DONE_STATUS, 접힘 카드 수, 컬럼 삭제 문구
  types/       board.ts
```

## 설계 메모

- **카드 순서(POSITION)**: 1000 간격으로 두고, 카드를 끼울 때 앞뒤 카드의 중간값을 쓴다. 이동할 때 다른 카드를 UPDATE하지 않아도 된다. 간격이 1 미만이 되면 서버가 컬럼을 1000 단위로 재정렬한다.
- **낙관적 업데이트**: 카드 이동은 요청 전에 캐시를 먼저 바꿔 화면을 즉시 갱신하고, 실패하면 이전 상태로 되돌린다. (`useMoveCard`)
- **API 응답**: 쓰기 API는 모두 최신 보드 전체를 반환한다. 단순하지만 보드가 커지면 변경분만 반환하도록 바꾸는 것을 고려한다.
- **완료 컬럼 표시**: 컬럼 제목이 아니라 `column.statusCode === DONE_STATUS`(`constants.ts`, `STATUS003`)로 판단해 체크 아이콘과 취소선을 보여준다.

## Spring 연결

`.env`에 `VITE_USE_MOCK=false`를 넣으면 `/api`로 요청하고, Vite 프록시가 `localhost:8080`으로 넘긴다.

| 메서드 | 경로 | 요청 본문 | 응답 |
|---|---|---|---|
| GET | `/api/boards/1` | | 보드 |
| POST | `/api/boards/1/columns` | `{ title, statusCode }` | 보드 |
| DELETE | `/api/columns/{id}` | | 보드 (카드가 있거나 마지막 완료 컬럼이면 400) |
| GET | `/api/codes/STATUS000` | | `[{ code, name }]` 컬럼 추가 시 상태 선택용 |
| POST | `/api/columns/{id}/cards` | `{ title, description, columnId, dueDate }` | 보드 |
| PATCH | `/api/cards/{id}` | `{ title, description, columnId, dueDate }` | 보드 |
| PATCH | `/api/cards/{id}/move` | `{ toColumnId, position }` | 보드 |
| DELETE | `/api/cards/{id}` | | 보드 |

보드 응답 형태는 `src/types/board.ts`를 따른다. (`columns[].cards[]`, 각각 position 오름차순)

## 다음에 할 것

- 컬럼 이름 변경
- 모달 포커스 트랩, 모바일 레이아웃
- Vitest로 `calcPosition`, 이동 로직 테스트
- GitHub Actions(린트, 빌드), Vercel 배포
