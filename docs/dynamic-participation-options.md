# 시간표 기반 참석·식사 선택

## 목적

운영진은 시간표와 신청 항목을 따로 입력하지 않습니다. `/admin/schedules`의 날짜별 시간표에서 일정을 만들고 `collectParticipation`을 켜면 해당 일정이 참가자 신청·본인 수정 화면에도 나타납니다.

식사 카테고리는 화면에서 신청 인원 수집이 기본 선택됩니다. 프로그램·집회 등도 필요한 경우 운영진이 직접 켤 수 있습니다.

## 저장 구조

- `retreat_schedule_items`: 운영진이 보는 시간표 원본
- `retreat_participation_options`: 신청서에 노출할 일정의 내부 투영
- `registration_participation_options`: 참가자의 일정 선택

신청 항목은 `schedule_item_id`로 시간표에 연결됩니다. 일정의 이름, 날짜, 종류, 순서, 공개 상태를 수정하면 연결된 신청 항목도 같은 트랜잭션에서 갱신됩니다.

`collectParticipation`을 끄거나 일정을 비공개로 바꿔도 과거 참가자 선택은 삭제하지 않습니다. 신청서 노출만 중단하며 운영진은 시간표 카드에서 기존 선택 인원을 확인할 수 있습니다.

## 공개 API

```text
GET /api/participation-options
```

현재 `OPEN` 수련회의 활성 신청 항목만 반환합니다. 신규 신청을 마감한 뒤에도 수련회가 운영 중이면 기존 참가자의 본인 수정을 위해 계속 조회할 수 있습니다.

신청 규칙:

- `FULL`: 서버가 현재 활성 신청 항목을 전부 선택
- `PARTIAL`, `WORSHIP_ONLY`: 요청의 `selectedOptionIds`를 검증해 저장
- 다른 수련회, 비활성 또는 존재하지 않는 항목은 거부

## 기존 데이터 이관

Flyway V23은 기존 참석·식사 항목의 ID와 참가자 선택을 보존한 채 `시간 미정` 일정으로 연결합니다. 현재 수련회 기간과 어긋난 기존 날짜는 첫 항목의 일차를 기준으로 현재 시작일에 맞춰 이동합니다.

## 검증

```bash
./gradlew clean test
```
