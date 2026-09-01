# Phase 6 공지 도메인

이 문서는 Retreat Ops의 Phase 6 공지 도메인 구현 범위를 설명합니다.

## 목적

Phase 6는 수련회 운영자가 공지를 작성하고 관리할 수 있는 관리자용 MVP를 제공합니다.

이 단계는 공지 데이터를 저장하고 대상 조건을 설정하는 백엔드 관리 기능까지만 포함합니다. 카카오톡, SMS, 푸시, 이메일 발송, 참가자 화면, 읽음 확인, 첨부파일, 이미지 업로드, 예약 발송 워커, 발송 이력은 구현하지 않습니다.

## 도메인 모델

공지:

- 테이블: `announcements`
- 주요 필드: `title`, `content`, `is_pinned`, `is_active`, `visible_from`, `visible_until`
- 작성/수정 관리자: `created_by_admin_id`, `updated_by_admin_id`
- 노출 기간은 둘 다 있을 때 `visible_until >= visible_from`이어야 합니다.

공지 대상:

- 테이블: `announcement_targets`
- 주요 필드: `announcement_id`, `target_type`, `target_value`
- 같은 공지 안에서 같은 대상 조건은 중복 저장하지 않습니다.
- 여러 대상 행은 MVP에서 OR 조건으로 해석할 수 있도록 저장합니다.

지원 대상 타입:

```text
ALL
REGISTRATION_STATUS
PAYMENT_STATUS
NEWCOMER
CARE_TARGET
RETREAT_GROUP
ADMIN_ROLE
```

대상 값 정책:

- `ALL`: `targetValue` 없이 전체 참가자를 의미합니다.
- `REGISTRATION_STATUS`: `REGISTERED`, `CANCELLED`
- `PAYMENT_STATUS`: `PAID`, `UNPAID`
- `NEWCOMER`, `CARE_TARGET`: `TRUE`, `FALSE`
- `RETREAT_GROUP`: 수련회 조 id
- `ADMIN_ROLE`: `STAFF`, `CHAIR`, `PASTOR`, `SYSTEM_ADMIN`

`RETREAT_GROUP`은 참조 대상이 실제로 존재하는지 검증합니다. 과거 이력 역직렬화를 위해 enum에 남아 있는 `CHURCH_MIDDLE_GROUP`, `CHURCH_CELL`은 새 공지 생성·수정 요청에서는 거부합니다.

## 역할 정책

역할 계층은 기존과 같습니다.

```text
STAFF < CHAIR < PASTOR < SYSTEM_ADMIN
```

- 공지 목록/상세 조회: `STAFF` 이상
- 공지 생성/수정: `CHAIR` 이상
- 공지 활성/비활성 변경: `CHAIR` 이상
- 공지 고정/고정 해제: `CHAIR` 이상

## API 요약

```text
GET   /api/admin/announcements
GET   /api/admin/announcements/{id}
POST  /api/admin/announcements
PATCH /api/admin/announcements/{id}
PATCH /api/admin/announcements/{id}/active
PATCH /api/admin/announcements/{id}/pinned
```

생성/수정 요청:

```json
{
  "title": "Retreat check-in starts at 3 PM",
  "content": "Please arrive at the main lobby and check in with your group.",
  "pinned": true,
  "active": true,
  "visibleFrom": "2026-07-01T00:00:00Z",
  "visibleUntil": "2026-07-31T23:59:59Z",
  "targets": [
    {
      "targetType": "ALL"
    }
  ]
}
```

대상 지정 예:

```json
{
  "targetType": "RETREAT_GROUP",
  "targetValue": "1"
}
```

활성 상태 변경:

```json
{
  "active": false
}
```

고정 상태 변경:

```json
{
  "pinned": true
}
```

## 응답

응답은 공지 본문, 노출 설정, 대상 목록, 작성/수정 관리자 요약을 포함합니다.

```json
{
  "id": 1,
  "title": "Retreat check-in starts at 3 PM",
  "content": "Please arrive at the main lobby and check in with your group.",
  "pinned": true,
  "active": true,
  "visibleFrom": "2026-07-01T00:00:00Z",
  "visibleUntil": "2026-07-31T23:59:59Z",
  "targets": [
    {
      "id": 1,
      "targetType": "ALL",
      "targetValue": null
    }
  ],
  "createdBy": {
    "id": 1,
    "email": "admin@example.local",
    "name": "System Admin",
    "role": "SYSTEM_ADMIN"
  },
  "updatedBy": {
    "id": 1,
    "email": "admin@example.local",
    "name": "System Admin",
    "role": "SYSTEM_ADMIN"
  }
}
```

## 보안과 개인정보

- 모든 공지 API는 관리자 JWT가 필요합니다.
- 참가자 공개 공지 API는 아직 구현하지 않습니다.
- 공지 대상 조건은 권한 역할과 참가자 속성을 구분해 저장합니다.
- 참가자 조회 키 관련 민감 데이터는 공지 API 응답, 문서 예시, HTTP 예시에 포함하지 않습니다.

## 테스트 명령

Docker Desktop이 실행 중이어야 합니다. 테스트는 Testcontainers PostgreSQL을 사용합니다.

```bash
./gradlew clean test
```
