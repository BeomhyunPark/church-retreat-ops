# Phase 7 일정 도메인

이 문서는 GMC Retreat App의 Phase 7 일정 도메인 구현 범위를 설명합니다.

## 목적

Phase 7은 수련회 운영자가 일정 항목을 작성하고 관리할 수 있는 관리자용 MVP를 제공합니다.

이 단계는 일정 데이터를 저장하고 관리자 API로 조회/관리하는 기능까지만 포함합니다. 참가자 화면, 모바일 UI, 캘린더 연동, 알림, 반복 일정, 출석 추적, QR 체크인, 장소/자원 예약, 드래그 앤 드롭 정렬은 구현하지 않습니다.

## 도메인 모델

일정:

- 테이블: `retreat_schedule_items`
- 주요 필드: `title`, `description`, `schedule_date`, `starts_at`, `ends_at`, `location`
- 분류: `category`
- 대상: `target_audience`
- 운영 상태: `is_active`
- 같은 날 표시 순서: `display_order`
- 작성/수정 관리자: `created_by_admin_id`, `updated_by_admin_id`

시간 정책:

- `schedule_date`는 일정 목록 필터와 일자별 표시를 위한 `DATE`입니다.
- `starts_at`, `ends_at`은 타임존 포함 시각인 `TIMESTAMPTZ`입니다.
- `ends_at`은 반드시 `starts_at`보다 이후여야 합니다.
- `starts_at`과 `ends_at`의 로컬 날짜는 모두 `schedule_date`와 같아야 합니다.
- Phase 7 MVP에서는 날짜 필터링을 단순하게 유지하기 위해 자정을 넘는 일정은 허용하지 않습니다.
- `display_order`는 0 이상이어야 합니다.

지원 카테고리:

```text
WORSHIP
PRAYER
MEAL
GROUP_ACTIVITY
LECTURE
BREAK
MOVE
CHECK_IN
CHECK_OUT
NOTICE
ETC
```

지원 대상:

```text
ALL
STAFF_ONLY
LEADERS_ONLY
NEWCOMERS
CARE_TARGETS
```

대상은 Phase 7 MVP에서 단순 값으로만 저장합니다. 수련회 조, 교회 셀, 중그룹 기반의 고급 대상 지정은 아직 구현하지 않습니다.

## 역할 정책

역할 계층은 기존과 같습니다.

```text
STAFF < CHAIR < PASTOR < SYSTEM_ADMIN
```

- 일정 목록/상세 조회: `STAFF` 이상
- 일정 생성/수정: `CHAIR` 이상
- 일정 활성/비활성 변경: `CHAIR` 이상

## API 요약

```text
GET   /api/admin/schedules
GET   /api/admin/schedules/{id}
POST  /api/admin/schedules
PATCH /api/admin/schedules/{id}
PATCH /api/admin/schedules/{id}/active
```

목록 필터:

```text
date=2026-07-01
category=WORSHIP
active=true
```

생성/수정 요청:

```json
{
  "title": "Opening Worship",
  "description": "Retreat opening worship in the main chapel.",
  "scheduleDate": "2026-07-01",
  "startsAt": "2026-07-01T09:00:00Z",
  "endsAt": "2026-07-01T10:30:00Z",
  "location": "Main Chapel",
  "category": "WORSHIP",
  "targetAudience": "ALL",
  "active": true,
  "displayOrder": 0
}
```

활성 상태 변경:

```json
{
  "active": false
}
```

## 응답

응답은 일정 내용, 시간, 분류, 대상, 작성/수정 관리자 요약을 포함합니다.

```json
{
  "id": 1,
  "title": "Opening Worship",
  "description": "Retreat opening worship in the main chapel.",
  "scheduleDate": "2026-07-01",
  "startsAt": "2026-07-01T09:00:00Z",
  "endsAt": "2026-07-01T10:30:00Z",
  "location": "Main Chapel",
  "category": "WORSHIP",
  "targetAudience": "ALL",
  "active": true,
  "displayOrder": 0,
  "createdBy": {
    "id": 1,
    "email": "admin@gmc.local",
    "name": "System Admin",
    "role": "SYSTEM_ADMIN"
  },
  "updatedBy": {
    "id": 1,
    "email": "admin@gmc.local",
    "name": "System Admin",
    "role": "SYSTEM_ADMIN"
  }
}
```

## 보안과 개인정보

- 모든 일정 API는 관리자 JWT가 필요합니다.
- 참가자 공개 일정 API는 아직 구현하지 않습니다.
- `STAFF_ONLY`와 같은 대상 값은 일정 항목 속성일 뿐이며 관리자 권한 역할과 분리해서 다룹니다.
- 참가자 조회 키 관련 민감 데이터는 일정 API 응답, 문서 예시, HTTP 예시에 포함하지 않습니다.

## 테스트 명령

Docker Desktop이 실행 중이어야 합니다. 테스트는 Testcontainers PostgreSQL을 사용합니다.

```bash
./gradlew clean test
```
