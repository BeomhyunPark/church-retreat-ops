# 참가자 등록과 본인 수정

## 운영 상태

참가자는 기존 교인 명부 없이 신청할 때 생성됩니다. 수련회 상태와 신규 신청 접수 여부는 분리합니다.

```text
수련회: DRAFT → OPEN(운영 중) → CLOSED
신규 신청: OPEN 수련회 안에서 별도 열림/마감
```

- 신규 `POST /api/registrations`는 `OPEN` 수련회의 `registrationOpen=true`일 때만 가능합니다.
- 신규 신청을 마감해도 기존 참가자는 수련회가 `OPEN`인 동안 본인 조회·수정을 계속할 수 있습니다.
- 수련회를 `CLOSED`하면 본인 수정도 종료됩니다.
- 본인 수정은 `registration_histories`에 기록되고 `participantUpdatedAt`이 갱신되어 운영진 목록에 표시됩니다.

## 기본 정보와 소속

신청은 이름, 성별, 출생연도, 전화번호, 개인정보 동의, 본인이 정한 숫자 6자리 조회 키를 받습니다. `middleGroupName`, `cellName`은 선택 입력 문자열이며 별도 공동체 마스터에 연결하지 않습니다.

조회 키는 요청에서 받은 즉시 BCrypt로 해시해 저장합니다. 평문을 응답·로그·이력에 남기지 않으며 `lookupKeyHash` / `lookup_key_hash`도 외부에 노출하지 않습니다.

## 동적 참석·식사 선택

`GET /api/participation-options`는 운영 중인 수련회의 활성 항목을 날짜와 표시 순서대로 반환합니다. 항목 종류는 `PROGRAM` 또는 `MEAL`입니다.

신청과 본인 수정은 `selectedOptionIds`를 제출합니다.

- `FULL`: 서버가 현재 활성 항목을 전부 선택하고 두 숙박 항목을 `true`로 정규화합니다.
- `PARTIAL`: 제출한 활성 항목만 저장합니다. 도착·출발 예정 시각이 필수입니다.
- `WORSHIP_ONLY`: 제출한 활성 항목만 저장하고 숙박은 `false`로 정규화합니다.
- 다른 수련회 항목, 비활성 항목, 존재하지 않는 항목은 `INVALID_REQUEST`입니다.

과거의 고정 `attendDay*` DB 컬럼은 기존 데이터 호환용으로만 남고 새 API에서는 사용하지 않습니다.

## 교통 정책

- `OWN_CAR`: 자차. 왕복에 함께 선택해야 합니다.
- `GROUP_BUS`: 단체 이동 차량.
- `WORSHIP_SHUTTLE`: 집회 전후 차량. 방향에 맞는 탑승 슬롯이 필요합니다.
- `PUBLIC_TRANSIT`: 대중교통.
- `CARPOOL_NEEDED`: 화면에서는 `이동 지원 요청`으로 안내합니다.

새 신청과 수정에서는 `NOT_DECIDED`를 허용하지 않습니다. `CARPOOL_NEEDED`는 매칭이 보장되지 않으며 실제 탑승·하차 위치가 희망 지역과 달라질 수 있습니다. 희망 지역은 필수입니다.

자차로 이동 지원을 제공할 때만 좌석 수, 출발/경유 지역과 메모를 받습니다. 위치와 메모는 민감 정보이며 관리자 상세 조회 시 개인정보 접근 로그를 남깁니다.

## 중복과 이력

한 수련회 안에서 동일한 정규화 이름과 전화번호의 활성 신청이 있으면 기존 신청을 덮어씁니다. 새 수련회에서는 같은 사람도 새 신청으로 생성됩니다.

생성, 덮어쓰기, 본인 수정은 변경 스냅샷을 남깁니다. 스냅샷에는 동적 항목 ID 목록도 포함됩니다.

## API

공개 API:

```text
GET  /api/participation-options
POST /api/registrations
POST /api/registrations/self/lookup
PUT  /api/registrations/self
```

관리자 읽기 API:

```text
GET /api/admin/registrations
GET /api/admin/registrations/{id}
GET /api/admin/registrations/{id}/histories
```

## 부분 참석 신청 예시

```json
{
  "name": "홍길동",
  "gender": "MALE",
  "birthYear": 1998,
  "phoneNumber": "010-1234-5678",
  "middleGroupName": "드림 중그룹",
  "cellName": "사랑 셀",
  "privacyConsentAgreed": true,
  "lookupKey": "123456",
  "attendanceType": "PARTIAL",
  "lodgingNight1": true,
  "lodgingNight2": false,
  "selectedOptionIds": [1, 2, 3],
  "plannedArrivalAt": "2027-01-15T14:00:00+09:00",
  "plannedDepartureAt": "2027-01-16T20:00:00+09:00",
  "inboundTransportationMethod": "PUBLIC_TRANSIT",
  "outboundTransportationMethod": "CARPOOL_NEEDED",
  "outboundCarpoolPreferredArea": "수지구청역 근처"
}
```

## 검증

```bash
./gradlew clean test
```
