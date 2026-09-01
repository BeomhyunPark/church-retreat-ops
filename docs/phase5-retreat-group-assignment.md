# Phase 5 수련회 조 편성

이 문서는 Retreat Ops의 Phase 5 수련회 조 편성 구현 범위를 설명합니다.

## 목적

Phase 5는 수련회 운영을 위한 임시 조를 만들고 참가자를 조에 배정할 수 있게 합니다.

중그룹과 셀은 참가자가 신청 시 입력한 소속 문자열입니다. 수련회 조는 이번 수련회 운영을 위한 별도의 임시 구조입니다. 수련회 조장은 조 안에서의 참가자 수준 역할이며 관리자가 아닙니다.

## 도메인 모델

수련회 조:

- 테이블: `retreat_groups`
- 주요 필드: `name`, `description`, `display_order`, `active`
- `name`은 전체 수련회 조에서 유일합니다.

수련회 조원:

- 테이블: `retreat_group_members`
- 주요 필드: `retreat_group_id`, `registration_id`, `leader`, `display_order`
- 한 참가자는 한 수련회 조에만 배정될 수 있습니다.
- 한 수련회 조에는 한 명의 조장만 둘 수 있습니다.
- 조장도 참가자이며 `admin_users`와 연결하지 않습니다.
- 조원 순서는 조별 `display_order`로 저장하며 조원 목록과 트리 조회에도 같은 순서를 사용합니다.
- 새 조원은 해당 조의 마지막에 추가됩니다.
- 조장 지정 시 해당 참가자는 맨 위로 이동하며, 조장 해제 후에도 이동된 순서를 유지합니다.

## 역할 정책

역할 계층은 기존과 같습니다.

```text
STAFF < CHAIR < PASTOR < SYSTEM_ADMIN
```

- 수련회 조 조회 API: `STAFF` 이상
- 수련회 조 생성/수정/활성 상태 변경 API: `CHAIR` 이상
- 참가자 수련회 조 배정/해제: `CHAIR` 이상
- 수련회 조장 지정/해제: `CHAIR` 이상
- 수련회 조원 순서 변경: `CHAIR` 이상

## API 요약

수련회 조:

```text
GET    /api/admin/retreat-groups
GET    /api/admin/retreat-groups/{id}
POST   /api/admin/retreat-groups
PATCH  /api/admin/retreat-groups/{id}
PATCH  /api/admin/retreat-groups/{id}/active
GET    /api/admin/retreat-groups/{id}/members
PATCH  /api/admin/retreat-groups/{id}/members/order
GET    /api/admin/retreat-groups/tree
```

참가자 조 배정:

```text
PATCH  /api/admin/participants/{participantId}/retreat-group
DELETE /api/admin/participants/{participantId}/retreat-group
```

배정 요청:

```json
{
  "retreatGroupId": 10
}
```

조원 순서 저장 요청은 현재 조원 전체를 원하는 순서로 전달합니다. 누락, 중복 또는 다른 조 참가자가 포함되면 거부됩니다.

```json
{
  "participantIds": [20, 15, 31]
}
```

수련회 조장:

```text
PATCH  /api/admin/retreat-groups/{groupId}/leader
DELETE /api/admin/retreat-groups/{groupId}/leader
```

조장 지정 요청:

```json
{
  "participantId": 20
}
```

조장을 지정하면 참가자가 아직 해당 조에 배정되지 않은 경우 조원으로도 추가됩니다. 이미 다른 조에 배정된 참가자는 중복 배정으로 거부됩니다.

## 참가자 응답 변경

관리자 참가자 목록과 상세 응답은 신청 시 입력한 소속과 수련회 조 정보를 함께 제공합니다.

```json
{
  "middleGroupName": "드림 중그룹",
  "cellName": "사랑 셀",
  "retreatGroupId": 7,
  "retreatGroupName": "Group 1",
  "retreatGroupLeader": true
}
```

`middleGroupName`과 `cellName`은 수련회 조 배정으로 수정되지 않습니다.

## 보안과 개인정보

- 모든 수련회 조 API는 관리자 JWT가 필요합니다.
- 참가자는 관리자 계정이 아닙니다.
- 수련회 조장은 관리자 역할이 아닙니다.
- 조회 키 해시는 API 응답, 문서 예시, HTTP 예시에 노출하지 않습니다.
- 조원 목록 응답은 전화번호를 포함하지 않습니다.
- 관리자 참가자 상세 조회의 개인정보 접근 로그 정책은 기존 Phase 3 동작을 유지합니다.

## 테스트 명령

Docker Desktop이 실행 중이어야 합니다. 테스트는 Testcontainers PostgreSQL을 사용합니다.

```bash
./gradlew clean test
```
