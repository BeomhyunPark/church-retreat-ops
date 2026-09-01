# Phase 3 관리자 참가자 관리

이 문서는 Retreat Ops의 Phase 3 관리자 참가자 관리 구현 범위를 설명합니다.

## 목적

Phase 3는 수련회 운영진이 등록된 참가자를 운영 관점에서 관리할 수 있게 합니다.

참가자는 여전히 `admin_users`가 아니며 관리자 JWT를 발급받지 않습니다. 새가족 여부도 역할이 아니라 참가자 돌봄 속성입니다.

## 역할 정책

관리자 역할 계층은 기존 Phase 1과 동일합니다.

```text
STAFF < CHAIR < PASTOR < SYSTEM_ADMIN
```

- `STAFF`: 참가자 목록과 상세, 이력 조회 가능
- `CHAIR`: 참가비, 등록 상태, 관리자 메모, 새가족/돌봄 태그 변경 가능
- `PASTOR`: `CHAIR` 권한 포함
- `SYSTEM_ADMIN`: 시스템 수준 운영과 예외 대응을 위한 최상위 권한

역할 검사는 `AdminRole.hasAuthorityAtLeast(...)`를 사용합니다.

## 관리자 관리 필드

`registrations`에 아래 운영 필드를 추가합니다.

- `fee_paid`: 참가비 납부 여부
- `status`: 등록 상태, 현재 `REGISTERED` 또는 `CANCELLED`
- `admin_memo`: 관리자용 메모
- `newcomer`: 새가족 여부
- `care_target`: 돌봄 대상 여부

신청 당시의 `middleGroupName`, `cellName`은 관리자 역할과 분리된 참가자 소속 정보입니다.

## API

모든 Phase 3 API는 JWT가 필요합니다.

조회 API:

```text
GET /api/admin/registrations
GET /api/admin/registrations/{id}
GET /api/admin/registrations/{id}/histories
```

`GET /api/admin/registrations`는 운영 명단 관리를 위해 아래 쿼리 파라미터를 지원합니다.

- `keyword`: 이름, 휴대폰 끝 4자리, 중그룹, 셀, 수련회 조 검색
- `status`: `REGISTERED`, `CANCELLED`
- `feePaid`, `newcomer`, `careTarget`, `checkedIn`
- `retreatGroupAssigned`, `cellAssigned`
- `attendanceType`: `FULL`, `PARTIAL`, `WORSHIP_ONLY`
- `transportationNeed`: `CARPOOL_NEEDED`, `CARPOOL_AVAILABLE`
- `sort`: `created_desc`, `name_asc`, `fee_unpaid_first`, `check_in_pending_first`, `group_asc`
- `page`, `size`

목록 응답은 체크인 여부, 참석 항목 ID, 숙박 요약, 참가자가 마지막으로 직접 수정한 `participantUpdatedAt`을 포함하지만 전화번호와 교통 세부 민감 정보는 마스킹 또는 생략합니다.

관리 변경 API:

```text
PATCH /api/admin/registrations/{id}/fee-paid
PATCH /api/admin/registrations/{id}/status
PATCH /api/admin/registrations/{id}/management
```

`STAFF`는 관리 변경 API를 호출할 수 없습니다. `CHAIR` 이상만 변경할 수 있습니다.

## 변경 이력

관리자 변경은 `registration_histories`에 남깁니다.

- 참가비 변경: `FEE_PAYMENT_UPDATED`
- 등록 상태 변경: `STATUS_UPDATED`
- 관리자 메모/새가족/돌봄 태그 변경: `ADMIN_MANAGEMENT_UPDATED`

관리자 변경 이력은 `actor_type=ADMIN`과 `actor_admin_user_id`를 저장합니다.

참가자의 본인 수정은 `SELF_UPDATED` 이력과 `participantUpdatedAt`으로 운영진에게 표시됩니다. 신규 신청이 마감된 뒤에도 현재 수련회가 `OPEN`인 동안 본인 수정은 허용됩니다.

## 개인정보 접근 로그

민감한 참가자 정보를 관리자가 조회하면 `registration_privacy_access_logs`에 접근 로그를 저장합니다.

현재 로그 대상:

- 상세 조회: `DETAIL_VIEW`, `sensitive_fields=phone_number,transportation_carpool_fields`
- 이력 조회: `HISTORY_VIEW`, `sensitive_fields=history_snapshots`

목록 API는 전화번호를 마스킹해서 반환하므로 상세 전화번호 접근 로그 대상이 아닙니다.

## 보안 규칙

- 참가자 응답은 조회 키 해시 값을 노출하지 않습니다.
- 조회 키 평문은 등록 또는 덮어쓰기 응답에서만 표시됩니다.
- 관리자 상세 API는 운영 목적상 정규화 전화번호를 반환하며 접근 로그를 남깁니다.
- 관리자 이력 API는 스냅샷에 민감 정보가 포함될 수 있어 접근 로그를 남깁니다.
- `SYSTEM_ADMIN`은 일반 운영 역할이 아니라 시스템 수준 예외 대응과 관리 목적의 상위 권한입니다.

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
  -d '{"email":"admin@example.local","password":"admin1234!"}'
```

참가비 납부 상태 변경:

```bash
curl -s -X PATCH http://localhost:8080/api/admin/registrations/1/fee-paid \
  -H 'Authorization: Bearer <accessToken>' \
  -H 'Content-Type: application/json' \
  -d '{"feePaid":true}'
```

등록 상태 변경:

```bash
curl -s -X PATCH http://localhost:8080/api/admin/registrations/1/status \
  -H 'Authorization: Bearer <accessToken>' \
  -H 'Content-Type: application/json' \
  -d '{"status":"CANCELLED"}'
```

관리자 메모와 돌봄 태그 변경:

```bash
curl -s -X PATCH http://localhost:8080/api/admin/registrations/1/management \
  -H 'Authorization: Bearer <accessToken>' \
  -H 'Content-Type: application/json' \
  -d '{
    "adminMemo":"Needs first-time attendee follow-up.",
    "newcomer":true,
    "careTarget":true
  }'
```

상세 조회:

```bash
curl -s http://localhost:8080/api/admin/registrations/1 \
  -H 'Authorization: Bearer <accessToken>'
```

이력 조회:

```bash
curl -s http://localhost:8080/api/admin/registrations/1/histories \
  -H 'Authorization: Bearer <accessToken>'
```

## Phase 3에서 제외한 것

- 조 편성
- 공지
- 일정
- QR 체크인
- 참가자용 관리자 권한 또는 참가자 로그인 계정
