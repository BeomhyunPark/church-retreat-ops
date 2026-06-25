# Phase 2 참가자 등록

이 문서는 Retreat Ops의 Phase 2 참가자 등록 구현 범위를 설명합니다.

## 목적

Phase 2는 참가자가 관리자 계정 없이 수련회 등록을 제출하고, 발급받은 개인 조회 키로 본인 등록 정보를 조회하거나 수정할 수 있게 만드는 단계입니다.

관리자는 JWT 인증 후 등록 목록, 상세, 변경 이력을 읽을 수 있습니다. Phase 2의 관리자 API는 읽기 전용입니다.

## 참가자 등록 흐름

1. 참가자가 `POST /api/registrations`로 이름, 성별, 출생연도, 전화번호, 교구/셀/부서, 개인정보 동의 여부, 본인이 정한 6자리 숫자 조회 키(PIN), 참석 조사 항목(아래 "참석 조사" 절 참고)을 보냅니다.
2. 서버가 이름을 trim하고 전화번호를 숫자만 남긴 값으로 정규화합니다.
3. 전화번호는 10자리 또는 11자리 숫자여야 하며 DB에서도 `ck_registrations_phone_number`로 검증합니다.
4. 개인정보 동의는 반드시 `true`여야 합니다.
5. 서버는 참가자가 보낸 6자리 숫자 조회 키를 BCrypt로 해시해서만 DB에 저장합니다(서버가 키를 생성하지 않음).
6. 신규 등록이면 `registrations`에 행을 만들고 `CREATED` 이력을 저장합니다.
7. 동일 이름과 동일 정규화 전화번호의 활성 등록이 이미 있으면 기존 행을 덮어쓰고 `OVERWRITTEN` 이력을 저장합니다.
8. 응답에는 마스킹된 전화번호가 포함됩니다. 참가자가 직접 정한 조회 키는 다시 돌려주지 않습니다.

## 참석 조사

등록(생성/덮어쓰기)과 본인 수정 모두에서 참석 형태와 이동 수단을 받습니다.

- `attendanceType`: `FULL`(전체참석) / `PARTIAL`(부분참석) / `WORSHIP_ONLY`(집회만참석).
- `inboundTransportationMethod` / `outboundTransportationMethod`: 가는 방법과 오는 방법은 방향별로 분리해서 저장합니다.
  - 전체참석(`FULL`)의 참가자 신청/수정 플로우에서는 `OWN_CAR`, `GROUP_BUS`, `PUBLIC_TRANSIT`, `CARPOOL_NEEDED`만 허용합니다.
  - 전체참석에서 자차(`OWN_CAR`)는 왕복 세트로만 허용합니다. `OWN_CAR -> GROUP_BUS`, `GROUP_BUS -> OWN_CAR`처럼 한쪽 방향에만 자차가 들어가는 조합은 `INVALID_REQUEST`로 실패합니다.
  - `GROUP_BUS`, `PUBLIC_TRANSIT`, `CARPOOL_NEEDED`끼리는 방향별 조합을 허용합니다.
  - 부분참석(`PARTIAL`)은 `OWN_CAR`, `GROUP_BUS`, `WORSHIP_SHUTTLE`, `PUBLIC_TRANSIT`, `CARPOOL_NEEDED`, `NOT_DECIDED`를 허용합니다.
  - 집회만참석(`WORSHIP_ONLY`)은 `OWN_CAR`, `WORSHIP_SHUTTLE`, `PUBLIC_TRANSIT`, `CARPOOL_NEEDED`, `NOT_DECIDED`를 허용합니다. 전체 일정용 `GROUP_BUS`는 허용하지 않습니다.
  - 모든 참석 유형에서 자차(`OWN_CAR`)는 왕복 세트로만 허용합니다.
  - 이 조합 규칙은 DB 제약이 아니라 `RegistrationService`의 `validateAttendanceSurvey`에서 검증하며, 위반 시 `INVALID_REQUEST`로 실패합니다.
- `plannedArrivalAt` / `plannedDepartureAt`: 부분참석(`PARTIAL`)의 수련회장 도착/출발 예정 시각입니다.
  - 부분참석에서는 둘 다 필수이며, 출발 예정 시각은 도착 예정 시각보다 늦어야 합니다.
  - 전체참석과 집회만참석에서는 받지 않습니다.
- `partialAttendanceNote`: 부분참석 운영 메모입니다. 선택 입력이며 최대 300자입니다.
- `WORSHIP_SHUTTLE`은 집회 차량입니다. 전체 일정용 `GROUP_BUS`와 다른 이동수단이며, 부분참석자와 집회만참석자가 사용할 수 있습니다.
  - 가는 방향이 `WORSHIP_SHUTTLE`이면 `inboundWorshipBusRideSlot`이 필수이고, `DAY1_BEFORE_WORSHIP` 또는 `DAY2_BEFORE_WORSHIP`만 허용합니다.
  - 오는 방향이 `WORSHIP_SHUTTLE`이면 `outboundWorshipBusRideSlot`이 필수이고, `DAY1_AFTER_WORSHIP` 또는 `DAY2_AFTER_WORSHIP`만 허용합니다.
- `inboundCarpoolAvailable` / `outboundCarpoolAvailable`: 카풀 제공 가능 여부입니다. `OWN_CAR` 방향에서만 의미가 있고, 카풀 희망과는 별도 개념입니다.
  - 제공 가능이 `true`이면 해당 방향의 `carpoolSeats`(1~10)와 `carpoolArea`가 필수입니다.
  - `inboundCarpoolRouteArea` / `outboundCarpoolRouteArea`는 경유 가능 지역 또는 운행 메모용 지역 필드입니다.
  - 제공 가능이 `false`이면 좌석 수, 제공 지역, 경유 지역, 제공 메모는 비워야 합니다.
- `inboundCarpoolPreferredArea` / `outboundCarpoolPreferredArea`: 카풀 희망자의 탑승/하차 희망 지역입니다. 해당 방향의 이동 방식이 `CARPOOL_NEEDED`일 때 필수입니다.
  - 정확한 자택 주소를 강제하지 않습니다. 역명, 동네, 교회, 주요 건물처럼 운영진이 매칭할 수 있는 수준의 지역 정보를 받습니다.
- `inboundCarpoolNote`, `outboundCarpoolNote`, `inboundCarpoolPreferredNote`, `outboundCarpoolPreferredNote`: 방향별 카풀 제공/희망 메모입니다. 선택 입력이며 최대 200자입니다.
- `lodgingNight1` / `lodgingNight2`(1일차/2일차 숙박 여부)와 8개의 참석 일정 체크박스(`attendDay1Morning`, `attendDay1Afternoon`, `attendDay1Worship`, `attendDay2Morning`, `attendDay2Afternoon`, `attendDay2Worship`, `attendDay3Morning`, `attendDay3Afternoon`)는 참석 유형에 따라 서버가 정규화합니다.
  - `FULL`: 8개 일정 체크박스와 두 숙박 항목 모두 서버가 무조건 `true`로 저장합니다(전체 일정에 참석하고 양일 모두 숙박한다고 간주). 요청에 담긴 값은 무시됩니다.
  - `PARTIAL`: 요청에 담긴 값을 그대로 저장합니다(비어 있으면 `false`).
  - `WORSHIP_ONLY`: 일정 체크박스는 요청 값을 그대로 저장하지만, 숙박 두 항목은 항상 `false`로 강제합니다.
- 이 정규화는 `RegistrationService`의 `resolveAttendanceFields`에서 처리하며, 등록 생성/덮어쓰기/본인 수정 경로 모두에 동일하게 적용됩니다.
- 참석 조사 값은 `registration_histories`의 스냅샷에도 포함되어 변경 이력을 추적할 수 있습니다.
- 관리자 상세 조회와 변경 이력에서는 이 데이터를 확인할 수 있습니다. 전화번호와 카풀 위치/메모는 민감 정보이므로 관리자 JWT가 필요하고, 상세/이력 조회 시 기존 개인정보 접근 로그를 남깁니다.

## 개인 조회 키 정책

- 조회 키는 참가자가 등록 시 직접 정하는 6자리 숫자(PIN)입니다. 서버는 키를 생성하지 않습니다.
- DB에는 조회 키의 BCrypt 해시만 저장합니다.
- 해시는 BCrypt로 생성합니다.
- API 응답은 조회 키 평문과 해시 값을 모두 노출하지 않습니다.
- 시도 횟수 제한(rate limit)은 두지 않습니다. 폐쇄된 교회 공동체 내부 사용을 전제로 한 의도적인 선택입니다.

## 중복 덮어쓰기 정책

중복 기준은 활성 등록 중 `normalized_name + phone_number`입니다.

- `normalized_name`: 현재는 trim된 이름입니다.
- `phone_number`: 숫자만 남긴 정규화 전화번호입니다.
- 중복이면 새 행을 추가하지 않고 기존 활성 행을 업데이트합니다.
- 덮어쓰기 시 요청에 담긴 조회 키로 새 BCrypt 해시로 교체합니다.
- 덮어쓰기 전후 스냅샷은 `registration_histories`에 `OVERWRITTEN`으로 저장합니다.

## 본인 조회 / 수정 정책

본인 조회:

- `POST /api/registrations/self/lookup`
- 입력값: 이름, 조회 키(숫자 6자리)
- 서버는 이름으로 활성 등록 후보를 찾고 BCrypt로 조회 키를 검증합니다. 전화번호는 입력받지 않습니다.
- 실패 시 어떤 값이 틀렸는지 구분하지 않고 조회 실패로 응답합니다.

본인 수정:

- `PUT /api/registrations/self`
- 입력값: 이름, 전화번호 마지막 4자리, 조회 키, 수정할 성별/출생연도/전화번호/교구 셀 부서
- `app.registration.self-edit-enabled=true`일 때만 허용됩니다.
- `APP_REGISTRATION_SELF_EDIT_ENABLED=false`이면 `REGISTRATION_EDIT_CLOSED`로 실패합니다.
- 수정 성공 시 `SELF_UPDATED` 이력을 저장합니다.
- 본인 수정은 이름과 조회 키를 바꾸지 않습니다.

## 관리자 읽기 API

아래 API는 JWT가 필요합니다.

```text
GET /api/admin/registrations
GET /api/admin/registrations/{id}
GET /api/admin/registrations/{id}/histories
```

- 목록 API는 페이지 응답을 반환합니다.
- 목록의 전화번호는 마스킹됩니다.
- 상세 API는 운영 확인을 위해 정규화 전화번호를 반환합니다.
- 관리자 API는 Phase 2에서 등록 수정이나 삭제를 제공하지 않습니다.

## 개인정보 / 보안 규칙

- 개인정보 동의가 `false`이면 등록을 받지 않습니다.
- 전화번호는 애플리케이션에서 정규화하고 DB check constraint로 한 번 더 제한합니다.
- 출생연도 검증은 애플리케이션 Bean Validation에 둡니다. 시간이 지남에 따라 정책이 바뀔 수 있어 DB에 하드코딩하지 않습니다.
- 조회 키 평문은 저장하지 않습니다.
- 조회 키 해시는 응답 DTO에 포함하지 않습니다.
- 참가자는 관리자 계정이 아니며 JWT를 발급받지 않습니다.
- 관리자 등록 API는 JWT 없이는 접근할 수 없습니다.

## 테스트 명령

Docker Desktop이 실행 중이어야 합니다. 테스트는 Testcontainers PostgreSQL을 사용합니다.

```bash
./gradlew clean test
```

## 샘플 curl

등록:

```bash
curl -s -X POST http://localhost:8080/api/registrations \
  -H 'Content-Type: application/json' \
  -d '{
    "name":"Grace Kim",
    "gender":"FEMALE",
    "birthYear":1991,
    "phoneNumber":"010-1234-5678",
    "churchCellDepartment":"Young Adults",
    "privacyConsentAgreed":true,
    "lookupKey":"123456",
    "attendanceType":"FULL",
    "inboundTransportationMethod":"OWN_CAR",
    "outboundTransportationMethod":"OWN_CAR",
    "inboundCarpoolAvailable":true,
    "inboundCarpoolSeats":2,
    "inboundCarpoolArea":"교회에서 출발",
    "inboundCarpoolNote":"잠실 경유 가능",
    "outboundCarpoolAvailable":false
  }'
```

본인 조회:

```bash
curl -s -X POST http://localhost:8080/api/registrations/self/lookup \
  -H 'Content-Type: application/json' \
  -d '{
    "name":"Grace Kim",
    "lookupKey":"123456"
  }'
```

본인 수정:

```bash
curl -s -X PUT http://localhost:8080/api/registrations/self \
  -H 'Content-Type: application/json' \
  -d '{
    "name":"Grace Kim",
    "phoneLastFour":"5678",
    "lookupKey":"123456",
    "update":{
      "gender":"FEMALE",
      "birthYear":1992,
      "phoneNumber":"010-9999-0000",
      "churchCellDepartment":"Updated Cell",
      "attendanceType":"FULL",
      "inboundTransportationMethod":"GROUP_BUS",
      "outboundTransportationMethod":"CARPOOL_NEEDED",
      "outboundCarpoolPreferredArea":"서울역 근처",
      "outboundCarpoolPreferredNote":"교회까지만 와도 괜찮음"
    }
  }'
```

관리자 로그인:

```bash
curl -s -X POST http://localhost:8080/api/admin/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"admin@example.local","password":"admin1234!"}'
```

관리자 등록 목록:

```bash
curl -s 'http://localhost:8080/api/admin/registrations?page=0&size=20' \
  -H 'Authorization: Bearer <accessToken>'
```

관리자 등록 상세:

```bash
curl -s http://localhost:8080/api/admin/registrations/1 \
  -H 'Authorization: Bearer <accessToken>'
```

관리자 등록 이력:

```bash
curl -s http://localhost:8080/api/admin/registrations/1/histories \
  -H 'Authorization: Bearer <accessToken>'
```
