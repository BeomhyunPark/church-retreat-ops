# 시간표와 신청 항목 통합

## 운영 화면

`/admin/schedules`는 현재 수련회 기간을 `1일차`, `2일차`처럼 나눈 카드형 시간표입니다. 운영진은 같은 화면에서 일정을 추가·수정하고 공개 상태와 신청 인원을 확인합니다.

별도의 참석·식사 설정 화면은 사용하지 않습니다. 기존 `/admin/participation-options` 화면 경로는 시간표로 이동합니다.

## 일정 모델

`retreat_schedule_items`의 주요 필드:

- `title`, `description`, `location`
- `schedule_date`
- 선택 입력인 `starts_at`, `ends_at`
- `category`, `target_audience`
- `is_active`, `display_order`
- `collect_participation`

지원 카테고리:

```text
PROGRAM
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

시작·종료 시각은 둘 다 입력하거나 둘 다 비워야 합니다. 비우면 시간표에서 `시간 미정`으로 표시합니다. 입력한 시각은 한국 시간 기준으로 `scheduleDate`와 같은 날이어야 하며 종료 시각이 시작 시각보다 뒤여야 합니다.

일정 날짜는 현재 수련회 기간 안이어야 합니다. 신청 인원을 받는 일정은 공개 신청서가 모든 참가자에게 동일하게 보이므로 대상이 `ALL`이어야 합니다.

## 신청서 연동

`collectParticipation=true`이면 연결된 `retreat_participation_options`를 생성하거나 갱신합니다.

- `MEAL` 일정은 신청 항목 종류 `MEAL`
- 그 외 일정은 신청 항목 종류 `PROGRAM`
- 일정 비공개 또는 `collectParticipation=false`이면 신청 항목도 비공개
- 연결된 신청 항목 ID와 기존 참가자 선택은 유지
- 일정 응답의 `selectionCount`로 현재 등록 참가자의 선택 인원 확인

## 수련회 기간 변경

수련회 시작일을 변경하면 시간표와 연결된 신청 항목을 기존 일차 차이만큼 함께 이동합니다. 기간을 줄여 새 종료일 밖으로 밀린 일정은 삭제하지 않고 비공개 처리합니다. 참가자 선택 기록도 보존합니다.

## API와 권한

```text
GET   /api/admin/schedules
GET   /api/admin/schedules/{id}
POST  /api/admin/schedules
PATCH /api/admin/schedules/{id}
PATCH /api/admin/schedules/{id}/active
```

- `STAFF` 이상: 목록·상세 조회
- `CHAIR` 이상: 생성·수정·공개 상태 변경
- `retreatId`를 지정한 목록 조회: 종료된 수련회 시간표 읽기

생성 예시:

```json
{
  "title": "저녁식사",
  "scheduleDate": "2027-01-15",
  "startsAt": "2027-01-15T18:00:00+09:00",
  "endsAt": "2027-01-15T19:00:00+09:00",
  "location": "식당",
  "category": "MEAL",
  "targetAudience": "ALL",
  "active": true,
  "displayOrder": 1080,
  "collectParticipation": true
}
```

`startsAt`, `endsAt`을 생략하면 시간 미정 일정으로 저장할 수 있습니다.

응답에는 `collectParticipation`, `participationOptionId`, `selectionCount`가 추가됩니다. 이관 과정에서 생성된 일정은 작성·수정 관리자가 `null`일 수 있습니다.

## 보안

- 모든 시간표 관리 API는 관리자 JWT가 필요합니다.
- 참가자 조회 키나 해시를 시간표 API에 노출하지 않습니다.
- 신청 항목 생성과 갱신은 일정 변경과 같은 트랜잭션에서 수행합니다.

## 검증

```bash
./gradlew clean test
```
