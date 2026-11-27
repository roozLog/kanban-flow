# kanban-flow

칸반 보드 토이 프로젝트. 프론트(React)와 백엔드(Spring Boot + Oracle)를 한 저장소에서 관리한다.

```
frontend/   React 18 + TypeScript + Vite 5 + TanStack Query + @dnd-kit
backend/    Spring Boot 3.5 + MyBatis + Oracle (Gradle)
```

## 기능

- 보드 1개 고정(`BOARD_SEQ=1`), 컬럼/카드 추가·수정·삭제, 카드 드래그 이동(컬럼 간/컬럼 내)
- 상태(진행사항)는 컬럼에만 있다. 공통코드 `STATUS000`(할 일/진행 중/완료/보류)이고, 완료 판단은 컬럼 제목이 아니라 `STATUS_CD='STATUS003'`이다.
- 카드 순서는 `POSITION`(1000 간격, 끼울 때 중간값). 이동 시 카드 1건만 UPDATE하고, 앞뒤 간격이 1 미만이면 서버가 컬럼을 1000, 2000… 으로 재정렬한다. 동점이면 기존 카드가 앞, 이동한 카드가 뒤.
- 컬럼 삭제 규칙: 카드가 있으면 불가, 보드의 마지막 완료 컬럼은 불가(둘 다 해당하면 후자 문구). 서버와 프론트 모두 검사한다.
- 컬럼은 카드가 6개 이상이면 5개만 보여주고 "N개 더 보기/접기"(펼침 상태는 화면에서만 유지).

## 사전 준비

| 구분 | 필요한 것 |
| --- | --- |
| 공통 | Git |
| 프론트 | Node.js 18 이상 |
| 백엔드 | JDK 17 이상(개발은 JDK 21로 빌드, 타깃 17), Oracle(`XEPDB1` 서비스) |

### DB

이 저장소에는 **DDL이 없다.** 아래 테이블이 이미 만들어져 있어야 한다.

- `TG_BOARD`, `TG_BOARD_COLUMN`, `TG_CARD`, `TG_CODE`
- 시퀀스 `TG_BOARD_SEQ`, `TG_BOARD_COLUMN_SEQ`, `TG_CARD_SEQ`
- `TG_CODE`에 그룹 `STATUS000` (`STATUS001` 할 일, `STATUS002` 진행 중, `STATUS003` 완료, `STATUS004` 보류)

확인용 시드는 `backend/sql/seed.sql`이다. **자동 실행되지 않으니 직접 실행한다.**(보드를 `BOARD_SEQ=1`로 넣으므로 `TG_BOARD_SEQ`가 처음 상태여야 한다. 파일 상단 주석 참고)

## 백엔드 실행

접속 정보는 `backend/src/main/resources/application.yml`에 있고 비밀번호만 환경변수로 받는다.

- URL: `jdbc:oracle:thin:@//localhost:1521/XEPDB1`
- 사용자: `kanban`, 비밀번호: 환경변수 `DB_PASSWORD`
- 포트: `8080`

```powershell
# Windows PowerShell
$env:DB_PASSWORD = '비밀번호'
cd backend
.\gradlew.bat bootRun
```

```bash
# macOS / Linux / Git Bash
export DB_PASSWORD='비밀번호'
cd backend
./gradlew bootRun
```

확인: `curl http://localhost:8080/api/health` → `{"db":1,"status":"UP"}`

### 테스트

```powershell
cd backend
.\gradlew.bat test
```

- 같은 DB에 붙어서 실행한다(`DB_PASSWORD` 필요).
- 대부분의 통합 테스트는 `@Transactional`로 롤백된다. `ColumnDeleteConcurrencyTest`만 임시 보드(`TEST-CONC`)를 실제로 커밋했다가 테스트 후 직접 삭제한다. 시드 데이터는 건드리지 않지만, 시퀀스 값은 소모된다.

## 프론트엔드 실행

```bash
cd frontend
npm ci
npm run dev
```

- 기본은 **메모리 목 API**로 동작해 백엔드 없이 실행된다.
- 실제 백엔드를 쓰려면 `frontend/.env` 파일을 만들고(커밋되지 않음) 아래를 넣는다. Vite 프록시가 `/api`를 `localhost:8080`으로 전달한다.

```
VITE_USE_MOCK=false
```

- 빌드 확인: `npm run build` (타입 체크 후 번들)
- 5173 포트가 사용 중이면 Vite가 다른 포트를 잡는다. 터미널에 출력된 `Local:` 주소로 접속한다.

## API

쓰기 API(POST/PATCH/DELETE)는 성공 시 **최신 보드 전체**를 응답한다. 오류는 `{ "message", "code" }`(`VALIDATION_ERROR` 400, `NOT_FOUND` 404).

| 메서드 | 경로 | 요청 본문 | 설명 |
| --- | --- | --- | --- |
| GET | `/api/boards/{id}` | | 보드 + 컬럼 + 카드 |
| POST | `/api/boards/{id}/columns` | `{title, statusCode}` | 컬럼 추가(맨 끝). `statusCode`는 `STATUS000`의 사용 중 값만 |
| DELETE | `/api/columns/{id}` | | 컬럼 삭제(카드 있음/마지막 완료 컬럼이면 400) |
| POST | `/api/columns/{id}/cards` | `{title, description, columnId, dueDate}` | 카드 추가(맨 끝). 경로 id와 본문 `columnId`가 다르면 400 |
| PATCH | `/api/cards/{id}` | `{title, description, columnId, dueDate}` | 카드 수정. 컬럼이 바뀌면 대상 컬럼 맨 끝으로 |
| PATCH | `/api/cards/{id}/move` | `{toColumnId, position}` | 드래그 이동 + 필요 시 재정렬. 다른 보드의 컬럼이면 400 |
| DELETE | `/api/cards/{id}` | | 카드 삭제 |
| GET | `/api/codes/{groupCd}` | | 사용 중인 코드 목록 `[{code, name}]` (예: `STATUS000`) |
| GET | `/api/health` | | DB 연결 확인(`SELECT 1 FROM DUAL`) |

## 알려 둘 점

- 한글 입력: DB 컬럼이 BYTE 기준이라 한글만 쓰면 제목은 약 66자, 설명은 약 666자에서 `ORA-12899`가 난다. 서버가 400(`VALIDATION_ERROR`)으로 바꿔 응답한다. 서버 검증 자체는 글자 수 기준(제목 200자, 설명 2000자, 컬럼 이름 100자)이다.
- 1차 범위 밖: 컬럼 이름 변경, 다중 보드, 로그인/회원, 실시간 협업.
