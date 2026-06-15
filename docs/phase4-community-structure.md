# Phase 4 공동체 구조

이 문서는 GMC Retreat App의 Phase 4 공동체 구조 구현 범위를 설명합니다.

## 목적

Phase 4는 교회 공동체의 중그룹과 셀 구조를 정규화해 참가자 등록 정보와 연결할 수 있게 합니다.

이 구조는 교회 공동체 소속을 표현하기 위한 것이며 수련회 조 편성이 아닙니다. 수련회 조 배정, 조장 권한, 공지, 일정, QR, 프론트엔드는 이 단계에서 구현하지 않습니다.

## 도메인 모델

중그룹:

- 테이블: `church_middle_groups`
- 주요 필드: `name`, `elder_name`, `description`, `display_order`, `active`
- `name`은 전체 중그룹에서 유일합니다.

셀:

- 테이블: `church_cells`
- 주요 필드: `church_middle_group_id`, `name`, `cell_leader_name`, `description`, `display_order`, `active`
- 같은 중그룹 안에서는 셀 이름이 유일합니다.
- 서로 다른 중그룹에서는 같은 셀 이름을 사용할 수 있습니다.

참가자 연결:

- `registrations.church_cell_id`는 nullable FK입니다.
- 연결하지 않은 참가자는 `church_cell_id = null`입니다.
- 기존 자유 입력 필드 `churchCellDepartment`는 계속 유지합니다.

## `churchCellDepartment`와 `churchCellId`

`churchCellDepartment`는 참가자가 등록 시 입력한 자유 텍스트입니다. 과거 데이터, 임시 입력, 정규화되지 않은 소속 정보를 보존하기 위해 유지합니다.

`churchCellId`는 관리자가 선택한 정규화된 교회 셀 링크입니다. 참가자 응답에는 링크가 있을 때 아래 정규화 정보가 함께 포함됩니다.

- `churchCellId`
- `churchCellName`
- `middleGroupId`
- `middleGroupName`

셀 link/unlink는 `churchCellDepartment`를 수정하지 않습니다.

## 역할 정책

역할 계층은 기존과 같습니다.

```text
STAFF < CHAIR < PASTOR < SYSTEM_ADMIN
```

- 공동체 조회 API: `STAFF` 이상
- 공동체 생성/수정/활성 상태 변경 API: `CHAIR` 이상
- 참가자 교회 셀 link/unlink: `CHAIR` 이상
- `SYSTEM_ADMIN`: 시스템 수준 관리와 예외 대응용 최상위 권한

장로와 셀 리더는 이 단계에서 관리자 역할이 아닙니다. `elder_name`과 `cell_leader_name`은 표시용 이름이며 `admin_users`와 연결하지 않습니다.

## API 요약

중그룹:

```text
GET   /api/admin/community/middle-groups
GET   /api/admin/community/middle-groups/{id}
POST  /api/admin/community/middle-groups
PATCH /api/admin/community/middle-groups/{id}
PATCH /api/admin/community/middle-groups/{id}/active
```

셀:

```text
GET   /api/admin/community/cells
GET   /api/admin/community/cells/{id}
POST  /api/admin/community/cells
PATCH /api/admin/community/cells/{id}
PATCH /api/admin/community/cells/{id}/active
```

셀 목록은 아래 query parameter를 지원합니다.

```text
middleGroupId
active
```

트리:

```text
GET /api/admin/community/tree
```

참가자 교회 셀 link/unlink:

```text
PATCH /api/admin/participants/{participantId}/church-cell
```

연결:

```json
{
  "churchCellId": 10
}
```

연결 해제:

```json
{
  "churchCellId": null
}
```

## 참가자 응답 변경

관리자 참가자 목록과 상세 응답은 기존 `churchCellDepartment`를 유지하면서 정규화된 공동체 정보를 추가합니다.

```json
{
  "churchCellDepartment": "Young Adults",
  "churchCellId": 10,
  "churchCellName": "A1",
  "middleGroupId": 3,
  "middleGroupName": "Alpha"
}
```

응답은 조회 키 해시를 노출하지 않습니다.

## 보안과 개인정보

- 모든 공동체 API는 관리자 JWT가 필요합니다.
- 공동체 구조는 관리자 권한 구조가 아닙니다.
- 참가자는 `admin_users`가 아닙니다.
- 장로/셀 리더 전화번호 저장이나 로그인은 구현하지 않습니다.
- 관리자 참가자 상세 조회의 개인정보 접근 로그 정책은 Phase 3를 유지합니다.

## 테스트 명령

Docker Desktop이 실행 중이어야 합니다. 테스트는 Testcontainers PostgreSQL을 사용합니다.

```bash
./gradlew clean test
```

## 샘플 curl

관리자 로그인:

```bash
curl -s -X POST http://localhost:8080/api/admin/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"admin@gmc.local","password":"admin1234!"}'
```

중그룹 생성:

```bash
curl -s -X POST http://localhost:8080/api/admin/community/middle-groups \
  -H 'Authorization: Bearer <accessToken>' \
  -H 'Content-Type: application/json' \
  -d '{
    "name":"Alpha",
    "elderName":"Elder A",
    "description":"Alpha middle group",
    "displayOrder":0
  }'
```

셀 생성:

```bash
curl -s -X POST http://localhost:8080/api/admin/community/cells \
  -H 'Authorization: Bearer <accessToken>' \
  -H 'Content-Type: application/json' \
  -d '{
    "middleGroupId":1,
    "name":"A1",
    "cellLeaderName":"Leader A1",
    "description":"A1 cell",
    "displayOrder":0
  }'
```

공동체 트리:

```bash
curl -s http://localhost:8080/api/admin/community/tree \
  -H 'Authorization: Bearer <accessToken>'
```

참가자 셀 연결:

```bash
curl -s -X PATCH http://localhost:8080/api/admin/participants/1/church-cell \
  -H 'Authorization: Bearer <accessToken>' \
  -H 'Content-Type: application/json' \
  -d '{"churchCellId":1}'
```

참가자 셀 연결 해제:

```bash
curl -s -X PATCH http://localhost:8080/api/admin/participants/1/church-cell \
  -H 'Authorization: Bearer <accessToken>' \
  -H 'Content-Type: application/json' \
  -d '{"churchCellId":null}'
```
