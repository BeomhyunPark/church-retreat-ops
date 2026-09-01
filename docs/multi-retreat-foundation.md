# 단일 현재 수련회 생명주기

## 목적

이 시스템은 한 교회 청년부가 순차적으로 여는 수련회를 운영합니다. 여러 단체를 지원하거나 여러 수련회의 신청을 동시에 받지 않습니다. 참가자 정보는 교인 명부에서 미리 가져오지 않고, 각 수련회의 신청이 접수될 때 생성합니다.

## 수련회 상태

```text
DRAFT → OPEN → CLOSED
```

- `DRAFT`: 일정과 운영 구성을 준비하는 상태입니다.
- `OPEN`: 현재 운영 중인 수련회입니다. 기존 참가자는 본인 조회·수정이 가능합니다.
- `CLOSED`: 종료된 수련회입니다. 신규 신청과 본인 수정이 모두 끝나며 다시 열 수 없습니다.
- `DRAFT` 또는 `OPEN` 상태의 수련회는 전체 시스템에서 최대 하나입니다.

`OPEN` 안에서 신규 신청 접수 여부는 `registrationOpen`으로 별도 관리합니다. `DRAFT → OPEN` 전환 시 접수가 함께 열리며 운영진은 수련회를 종료하지 않고도 신규 신청만 마감하거나 다시 열 수 있습니다. 신규 신청 마감은 이미 신청한 참가자의 본인 수정을 막지 않습니다.

수련회 종료 시 `REGISTERED` 상태인 참가자 수를 `retreats.participant_count`에 저장합니다. 취소된 신청은 종료 요약 인원에서 제외합니다.

## 데이터 귀속

다음 운영 데이터는 `retreat_id`로 수련회에 귀속됩니다.

- `registrations`
- `retreat_participation_options`
- `retreat_groups`
- `announcements`
- `retreat_schedule_items`

체크인, 참가비 이력, 신청 변경 이력과 참석·식사 선택은 신청 레코드를 통해 간접적으로 같은 수련회에 귀속됩니다. 중그룹과 셀은 별도 마스터가 아니라 신청 시점의 선택 입력 문자열로 저장합니다.

같은 이름과 전화번호의 활성 신청 중복 제약은 전체 기간이 아니라 한 수련회 안에서만 적용됩니다. 따라서 같은 사람이 다음 수련회에 다시 신청하면 새로운 신청 레코드가 만들어집니다. 이름이나 전화번호만으로 과거 신청과 자동 병합하지 않습니다.

## API

모든 수련회 관리 API는 관리자 JWT가 필요합니다.

```text
GET   /api/admin/retreats
GET   /api/admin/retreats/current
POST  /api/admin/retreats
PATCH /api/admin/retreats/{id}
PATCH /api/admin/retreats/{id}/status
PATCH /api/admin/retreats/{id}/registration-open
```

- 목록과 현재 수련회 조회: `STAFF` 이상
- 생성, 설정 변경, 상태 전환: `CHAIR` 이상
- 공개 `POST /api/registrations`: `OPEN` 수련회의 `registrationOpen`이 `true`가 아니면 `REGISTRATION_NOT_OPEN`
- 공개 본인 조회·수정: 신규 신청을 마감해도 수련회가 `OPEN`인 동안 가능
- `GET /api/admin/schedules?retreatId={id}`: 종료된 수련회의 시간표 조회

상태 전환은 `DRAFT → OPEN`, `OPEN → CLOSED`만 허용합니다. 신규 신청 접수 변경은 `OPEN`에서만 허용합니다. 종료된 수련회는 수정하거나 다시 열 수 없습니다.

현재 수련회 기간을 변경하면 시간표와 연결된 참석·식사 항목은 기존 시작일과의 일차 차이를 유지한 채 함께 이동합니다. 새 기간 밖으로 밀린 일정과 신청 항목은 삭제하지 않고 비공개 처리해 기존 선택 기록을 보존합니다.

## 기존 데이터 이관

Flyway V19는 기존 데이터를 보존하기 위해 현재 수련회 한 건을 `OPEN` 상태로 생성합니다. 기존 신청, 수련회 조, 공지, 일정은 모두 해당 수련회에 귀속됩니다. V22는 기존 `OPEN` 수련회의 신규 신청 접수를 열린 상태로 이관합니다. 운영에 적용된 기존 Flyway 파일은 수정하지 않습니다.

이관 후 관리자는 `/admin/retreats`에서 기존 수련회의 이름과 기간을 실제 값으로 수정할 수 있습니다.

## 과거 기록과 개인정보

현재 운영 API는 `DRAFT` 또는 `OPEN` 수련회의 참가자, 조, 공지, 체크인, 참가비만 조회합니다. 종료된 수련회는 수련회 목록의 참가 인원 요약과 당시 시간표만 일반 운영 화면에 표시합니다.

이번 구현은 기존 개인정보를 자동 삭제하지 않습니다. 삭제 또는 익명화 시점은 운영상 필요한 정산·분쟁 대응 기간과 개인정보 보존 정책을 정한 뒤 별도 Flyway/배치 작업으로 구현해야 합니다.

## 검증

Docker Desktop과 Testcontainers PostgreSQL을 사용할 수 있는 환경에서 실행합니다.

```bash
./gradlew clean test
```

프론트엔드 빌드:

```bash
cd frontend
npm run build
```
