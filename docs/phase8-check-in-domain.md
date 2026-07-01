# Phase 8 체크인 도메인

이 문서는 Retreat Ops의 Phase 8 체크인 도메인 구현 범위를 설명합니다.

## 목적

Phase 8은 참가자 도착 QR 발급과 수련회 운영자의 현장 QR 체크인을 제공합니다.

참가자는 신청 완료 또는 본인 조회에서 QR 이미지를 저장하고, 수련회장 도착 시 로그인한 운영자가 관리자 스캐너로 QR을 읽어 체크인합니다. 퇴장 체크인은 포함하지 않습니다.

## 도메인 모델

현재 체크인 상태:

- 테이블: `retreat_check_ins`
- 참가자별 현재 체크인 상태를 한 행으로 저장합니다.
- 주요 필드: 참가자 id, 체크인 여부, 체크인 시각, 체크인 방식, 체크인 수행 관리자, 취소 시각, 취소 수행 관리자, 취소 사유
- 참가자별 현재 상태 행은 하나만 가질 수 있습니다.

체크인 이벤트:

- 테이블: `retreat_check_in_events`
- 체크인과 취소 이벤트를 append-only 이력으로 저장합니다.
- 지원 action: `CHECKED_IN`, `CANCELLED`
- 지원 method: `MANUAL`, `QR`
- 취소 이벤트는 사유가 필요합니다.

QR 토큰:

- 테이블: `participant_check_in_tokens`
- 참가자 도착 체크인을 위한 토큰을 저장합니다.
- 원문 토큰은 발급 응답에서 한 번만 반환합니다.
- DB에는 원문 토큰을 저장하지 않고, 검증용 해시만 저장합니다.
- 토큰은 만료 시각과 폐기 시각을 가집니다.
- 자동 발급 QR은 `2026-08-18T23:59:59+09:00`에 만료됩니다.
- 본인 조회에서 QR을 재발급하면 이전 활성 QR은 폐기됩니다.

## 역할 정책

역할 계층은 기존과 같습니다.

```text
STAFF < CHAIR < PASTOR < SYSTEM_ADMIN
```

- 체크인 roster 조회: `STAFF` 이상
- 체크인 상세 조회: `STAFF` 이상
- 수동 체크인: `STAFF` 이상
- QR 스캔 체크인: `STAFF` 이상
- 체크인 취소: `CHAIR` 이상
- QR 토큰 발급: `CHAIR` 이상
- QR 토큰 폐기: `CHAIR` 이상

## API 요약

```text
GET   /api/admin/check-ins
GET   /api/admin/check-ins/{participantId}
POST  /api/admin/check-ins/{participantId}
POST  /api/admin/check-ins/qr
PATCH /api/admin/check-ins/{participantId}/cancel
POST  /api/admin/check-ins/tokens/{participantId}
PATCH /api/admin/check-ins/tokens/{participantId}/revoke
POST  /api/registrations/self/check-in-qr
```

Roster 필터:

```text
checkedIn=true
retreatGroupId=1
churchCellId=1
keyword=Grace
page=0
size=20
```

수동 체크인 요청은 본문이 없습니다.

QR 스캔 체크인 요청:

```json
{
  "token": "<scanned-qr-token>"
}
```

체크인 취소 요청:

```json
{
  "reason": "Checked in under a duplicate registration."
}
```

QR 토큰 발급 요청:

```json
{
  "expiresAt": "2026-07-01T23:59:59Z"
}
```

토큰 폐기 요청은 본문이 없습니다.

## 응답

체크인 roster와 상세 응답은 개인정보 보호형 필드만 포함합니다.

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
  "checkedIn": true,
  "checkedInAt": "2026-07-01T08:45:00Z",
  "checkInMethod": "MANUAL",
  "checkedInBy": {
    "id": 1,
    "name": "System Admin"
  },
  "cancelledAt": null,
  "cancelledBy": null
}
```

QR 토큰 발급 응답:

```json
{
  "participantId": 1,
  "token": "<shown-once-token>",
  "expiresAt": "2026-07-01T23:59:59Z",
  "notice": "This check-in token is shown only once."
}
```

## 체크인 생명주기

- 아직 체크인하지 않은 참가자는 `checkedIn=false`로 조회됩니다.
- QR 스캔 체크인은 현재 상태를 `checkedIn=true`, method를 `QR`로 기록하고 이벤트를 추가합니다.
- 수동 체크인은 현재 상태를 `checkedIn=true`로 변경하고 체크인 이벤트를 추가합니다.
- 이미 체크인된 참가자를 다시 체크인하면 business error를 반환합니다.
- 체크인 취소는 현재 상태를 `checkedIn=false`로 변경하고 취소 이벤트를 추가합니다.
- 체크인 취소는 원래 상태 행을 삭제하지 않습니다.
- 체크인 취소는 `CHAIR` 이상만 수행할 수 있고 사유가 필요합니다.

## 보안과 개인정보

- 실제 체크인 처리 API는 관리자 JWT가 필요하며 `STAFF` 이상만 호출할 수 있습니다.
- 참가자 본인 QR 재발급은 이름과 6자리 조회 키 검증 후에만 허용합니다.
- 체크인 목록/상세 응답에는 full phone number를 포함하지 않고 `phoneLast4`만 포함합니다.
- 참가자 lookup key, lookup key 저장값, QR 토큰 저장값은 API 응답, 문서 예시, HTTP 예시에 포함하지 않습니다.
- QR 원문 토큰은 발급 응답에서 한 번만 반환합니다.
- QR 토큰 폐기는 저장된 활성 토큰을 폐기 표시하며 저장값을 반환하지 않습니다.
- QR 원문만으로 호출 가능한 공개 체크인 endpoint는 제공하지 않습니다.

## 제외 범위

- 공개 QR 스캔/체크인 API
- 퇴장 체크인과 퇴장 QR
- GPS geofencing
- 카카오톡, SMS, 이메일, 푸시 알림
- 통계 dashboard
- 출석 기반 일정/강의 세부 tracking

## 테스트 명령

Docker Desktop이 실행 중이어야 합니다. 테스트는 Testcontainers PostgreSQL을 사용합니다.

```bash
./gradlew clean test
```
