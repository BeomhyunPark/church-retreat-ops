# Phase 2 참가자 등록

이 문서는 GMC Retreat App의 Phase 2 참가자 등록 구현 범위를 설명합니다.

## 목적

Phase 2는 참가자가 관리자 계정 없이 수련회 등록을 제출하고, 발급받은 개인 조회 키로 본인 등록 정보를 조회하거나 수정할 수 있게 만드는 단계입니다.

관리자는 JWT 인증 후 등록 목록, 상세, 변경 이력을 읽을 수 있습니다. Phase 2의 관리자 API는 읽기 전용입니다.

## 참가자 등록 흐름

1. 참가자가 `POST /api/registrations`로 이름, 성별, 출생연도, 전화번호, 교구/셀/부서, 개인정보 동의 여부를 보냅니다.
2. 서버가 이름을 trim하고 전화번호를 숫자만 남긴 값으로 정규화합니다.
3. 전화번호는 10자리 또는 11자리 숫자여야 하며 DB에서도 `ck_registrations_phone_number`로 검증합니다.
4. 개인정보 동의는 반드시 `true`여야 합니다.
5. 서버가 개인 조회 키를 생성하고 BCrypt 해시만 DB에 저장합니다.
6. 신규 등록이면 `registrations`에 행을 만들고 `CREATED` 이력을 저장합니다.
7. 동일 이름과 동일 정규화 전화번호의 활성 등록이 이미 있으면 기존 행을 덮어쓰고 `OVERWRITTEN` 이력을 저장합니다.
8. 응답에는 마스킹된 전화번호와 평문 조회 키가 포함됩니다.

## 개인 조회 키 정책

- 조회 키는 등록 또는 중복 덮어쓰기 응답에서만 평문으로 한 번 표시됩니다.
- DB에는 `lookup_key_hash`만 저장합니다.
- 해시는 BCrypt로 생성합니다.
- 조회 키를 분실하면 현재 Phase 2 API만으로는 다시 확인할 수 없습니다.
- API 응답은 `lookupKeyHash` 또는 `lookup_key_hash`를 노출하지 않습니다.

## 중복 덮어쓰기 정책

중복 기준은 활성 등록 중 `normalized_name + phone_number`입니다.

- `normalized_name`: 현재는 trim된 이름입니다.
- `phone_number`: 숫자만 남긴 정규화 전화번호입니다.
- 중복이면 새 행을 추가하지 않고 기존 활성 행을 업데이트합니다.
- 덮어쓰기 시 새 조회 키를 발급하고 새 BCrypt 해시로 교체합니다.
- 덮어쓰기 전후 스냅샷은 `registration_histories`에 `OVERWRITTEN`으로 저장합니다.

## 본인 조회 / 수정 정책

본인 조회:

- `POST /api/registrations/self/lookup`
- 입력값: 이름, 전화번호 마지막 4자리, 조회 키
- 서버는 이름과 마지막 4자리로 후보를 찾고 BCrypt로 조회 키를 검증합니다.
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
    "privacyConsentAgreed":true
  }'
```

본인 조회:

```bash
curl -s -X POST http://localhost:8080/api/registrations/self/lookup \
  -H 'Content-Type: application/json' \
  -d '{
    "name":"Grace Kim",
    "phoneLastFour":"5678",
    "lookupKey":"<lookupKey>"
  }'
```

본인 수정:

```bash
curl -s -X PUT http://localhost:8080/api/registrations/self \
  -H 'Content-Type: application/json' \
  -d '{
    "name":"Grace Kim",
    "phoneLastFour":"5678",
    "lookupKey":"<lookupKey>",
    "update":{
      "gender":"FEMALE",
      "birthYear":1992,
      "phoneNumber":"010-9999-0000",
      "churchCellDepartment":"Updated Cell"
    }
  }'
```

관리자 로그인:

```bash
curl -s -X POST http://localhost:8080/api/admin/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"admin@gmc.local","password":"admin1234!"}'
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
