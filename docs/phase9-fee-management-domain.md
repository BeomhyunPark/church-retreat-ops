# Phase 9 참가비 관리 도메인

이 문서는 Retreat Ops의 Phase 9 참가비 관리 도메인 구현 범위를 설명합니다.

## 목적

Phase 9는 수련회 운영진이 참가자의 참가비 납부 상태를 현장에서 조회하고 변경할 수 있는 관리자용 MVP를 제공합니다.

이 단계는 기존 `registrations.fee_paid` 현재 상태를 유지하면서, 별도 이벤트 이력과 현재 상태 변경 메타데이터를 추가합니다. 실 결제 gateway, 영수증 업로드, 환불 workflow, 정산 자동화, 프론트엔드는 구현하지 않습니다.

## 도메인 모델

현재 참가비 상태:

- 테이블: `registrations`
- 현재 상태 필드: `fee_paid`
- 현재 상태 메타데이터: `fee_status_updated_at`, `fee_status_updated_by_admin_id`

참가비 이벤트:

- 테이블: `registration_fee_events`
- 참가비 상태 변경을 append-only 이력으로 저장합니다.
- 주요 필드: 참가자 id, 이전 납부 여부, 새 납부 여부, 변경 관리자, 사유, 생성 시각
- 실제 상태가 바뀐 경우에만 이벤트를 저장합니다.
- 납부 취소(`feePaid=false`)는 사유가 필요합니다.

## 역할 정책

역할 계층은 기존과 같습니다.

```text
STAFF < CHAIR < PASTOR < SYSTEM_ADMIN
```

- 참가비 roster 조회: `STAFF` 이상
- 참가비 상세 조회: `STAFF` 이상
- 참가비 이벤트 조회: `STAFF` 이상
- 참가비 상태 변경: `CHAIR` 이상

## API 요약

```text
GET   /api/admin/fees
GET   /api/admin/fees/{participantId}
PATCH /api/admin/fees/{participantId}
GET   /api/admin/fees/{participantId}/events
```

Roster 필터:

```text
feePaid=true
retreatGroupId=1
churchCellId=1
keyword=Grace
page=0
size=20
```

납부 처리 요청:

```json
{
  "feePaid": true,
  "reason": "Confirmed by treasurer"
}
```

미납으로 되돌리기 요청:

```json
{
  "feePaid": false,
  "reason": "Marked paid by mistake"
}
```

## 응답

참가비 roster 응답은 현장 운영에 필요한 개인정보 최소값만 포함합니다.

```json
{
  "participantId": 1,
  "name": "Grace Kim",
  "gender": "FEMALE",
  "birthYear": 1991,
  "phoneLast4": "5678",
  "churchCellId": 1,
  "churchCellName": "A1",
  "middleGroupId": 1,
  "middleGroupName": "Alpha",
  "retreatGroupId": 1,
  "retreatGroupName": "Group 1",
  "retreatGroupLeader": false,
  "feePaid": true,
  "feeStatusUpdatedAt": "2026-07-01T08:45:00Z",
  "feeStatusUpdatedBy": {
    "id": 1,
    "name": "System Admin"
  }
}
```

상세 응답은 참가자 현재 상태와 최근 변경 이벤트를 함께 반환합니다.

```json
{
  "participant": {
    "participantId": 1,
    "name": "Grace Kim",
    "gender": "FEMALE",
    "birthYear": 1991,
    "phoneLast4": "5678",
    "feePaid": true,
    "feeStatusUpdatedAt": "2026-07-01T08:45:00Z",
    "feeStatusUpdatedBy": {
      "id": 1,
      "name": "System Admin"
    }
  },
  "events": [
    {
      "id": 10,
      "participantId": 1,
      "previousFeePaid": false,
      "newFeePaid": true,
      "changedBy": {
        "id": 1,
        "name": "System Admin"
      },
      "reason": "Confirmed by treasurer",
      "createdAt": "2026-07-01T08:45:00Z"
    }
  ]
}
```

## 참가비 상태 생명주기

- 새 등록은 기존 정책처럼 `feePaid=false`로 시작합니다.
- `CHAIR` 이상은 `feePaid=true`로 납부 처리할 수 있습니다.
- 이미 납부 상태인 참가자를 다시 납부 처리하면 business error를 반환하고 이벤트를 만들지 않습니다.
- `CHAIR` 이상은 사유를 입력해 `feePaid=false`로 되돌릴 수 있습니다.
- 이미 미납 상태인 참가자를 다시 미납 처리하면 business error를 반환하고 이벤트를 만들지 않습니다.
- 상태 변경과 이벤트 저장은 같은 transaction 안에서 처리됩니다.

## 참가자 본인 조회

기존 참가자 본인 조회 응답은 자기 자신의 `feePaid`만 포함합니다. Phase 9에서는 별도 참가자용 fee API를 추가하지 않습니다.

## 보안과 개인정보

- 모든 관리자 참가비 API는 관리자 JWT가 필요합니다.
- 참가비 목록/상세 응답에는 full phone number를 포함하지 않고 `phoneLast4`만 포함합니다.
- 참가자 lookup key, lookup key 저장값, QR token 저장값, 내부 보안 hash는 API 응답, 문서 예시, HTTP 예시에 포함하지 않습니다.
- 참가자는 다른 참가자의 납부 상태를 조회할 수 없습니다.

## 제외 범위

- 실 결제 gateway 연동
- KakaoPay, Toss, Stripe, NaverPay 연동
- 영수증 업로드
- 환불 workflow
- 정산/accounting 자동화
- 프론트엔드 화면
- 알림 발송
- 통계 dashboard

## 테스트 명령

Docker Desktop이 실행 중이어야 합니다. 테스트는 Testcontainers PostgreSQL을 사용합니다.

```bash
./gradlew clean test
```
