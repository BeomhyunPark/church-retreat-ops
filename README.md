# 청년2부 수련회 운영

`church-retreat-ops`는 지구촌교회 드림공동체 청년2부 수련회의 신청과 현장 운영을 위한 도구입니다.

현재는 관리자 인증, 참가자 등록·본인 수정, 동적 참석·식사 항목, 참가자 관리, 수련회 조 편성, 공지, 일정, 체크인, 참가비와 단일 현재 수련회 생명주기가 구현되어 있습니다. 프론트엔드는 `frontend/`에 Vite React 관리자/공개 앱이 구성되어 있습니다.

## 현재 완료된 단계

### Phase 0: 백엔드 기반

- Java 21 + Spring Boot 프로젝트 구성
- Gradle 빌드 구성
- PostgreSQL Docker Compose 구성
- Flyway 마이그레이션 구성
- MyBatis 구성
- Spring Security 기본 설정
- 공통 API 응답 형식 `ApiResponse`
- 공통 예외 처리 구조
- Health API
- Testcontainers 기반 통합 테스트

### Phase 1: 관리자 인증 / 역할 기반 권한 기초

- `admin_users` 테이블
- 관리자 도메인 모델
- 관리자 역할: `STAFF`, `CHAIR`, `PASTOR`, `SYSTEM_ADMIN`
- 관리자 상태: `ACTIVE`, `INACTIVE`, `LOCKED`
- BCrypt 비밀번호 해시 저장
- 초기 `SYSTEM_ADMIN` 부트스트랩
- 관리자 로그인 API
- JWT Access Token 발급
- JWT 인증 필터
- 현재 로그인한 관리자 프로필 API
- 기본 역할 계층 로직

### Phase 2: 참가자 등록 / 본인 조회 / 관리자 읽기 API

- `registrations`, `registration_histories` 테이블
- 참가자 등록 API
- 개인정보 동의 필수 검증
- 전화번호 정규화와 DB check constraint
- 참가자가 정한 6자리 조회 키를 BCrypt 해시로만 저장
- 동일 이름 + 전화번호 활성 등록 중복 시 기존 행 덮어쓰기
- 등록 생성, 덮어쓰기, 본인 수정 이력 저장
- 이름 + 조회 키 본인 조회, 전화번호 마지막 4자리 재확인 후 수정
- 설정 기반 본인 수정 허용/차단
- 신규 신청 마감 후에도 운영 중에는 본인 수정 허용
- 본인 수정 시각과 변경 이력을 운영진에게 표시
- 수련회별 동적 프로그램·식사 선택
- 관리자 등록 목록/상세/이력 읽기 API

자세한 내용은 [docs/phase2-participant-registration.md](docs/phase2-participant-registration.md)를 참고합니다.

시간표와 참석·식사 선택 연동은 [docs/dynamic-participation-options.md](docs/dynamic-participation-options.md)를 참고합니다.

### Phase 3: 관리자 참가자 관리

- 관리자 참가비 납부 상태 변경 API
- 관리자 등록 상태 변경 API
- 관리자 메모
- 새가족 여부와 돌봄 대상 태그
- 관리자 상세/이력 조회 시 개인정보 접근 로그 저장
- 역할 기반 운영 권한: `STAFF`는 조회, `CHAIR` 이상은 관리 변경

자세한 내용은 [docs/phase3-admin-participant-management.md](docs/phase3-admin-participant-management.md)를 참고합니다.

### Phase 4: 참가자 소속 정보

- 신청 시 중그룹 `middleGroupName`과 셀 `cellName` 선택 입력
- 별도 공동체 마스터와 관리 화면 없이 신청 당시 문자열 스냅샷 저장
- 관리자 참가자·체크인·참가비·조 편성 화면에서 소속 표시

자세한 내용은 [docs/phase4-community-structure.md](docs/phase4-community-structure.md)를 참고합니다.

### Phase 5: 수련회 조 편성

- 수련회 조 `retreat_groups`
- 수련회 조원 배정 `retreat_group_members`
- 한 참가자는 한 수련회 조에만 배정
- 한 수련회 조에는 한 명의 조장만 지정
- 수련회 조 CRUD와 활성 상태 변경 API
- 참가자 수련회 조 배정/해제 API
- 수련회 조장 지정/해제 API
- 관리자 참가자 목록/상세 응답에 수련회 조 정보 포함
- 신청 당시 중그룹·셀 정보와 수련회 조 정보를 분리해 유지

자세한 내용은 [docs/phase5-retreat-group-assignment.md](docs/phase5-retreat-group-assignment.md)를 참고합니다.

### Phase 6: 공지 도메인

- 관리자 공지 `announcements`
- 공지 대상 조건 `announcement_targets`
- 공지 생성/목록/상세/수정 API
- 공지 활성/비활성 상태 변경 API
- 공지 고정/고정 해제 API
- 노출 기간 `visibleFrom`, `visibleUntil`
- 전체, 등록 상태, 납부 상태, 새가족, 돌봄 대상, 수련회 조, 관리자 역할 대상 조건
- `STAFF` 이상 조회, `CHAIR` 이상 변경

자세한 내용은 [docs/phase6-announcement-domain.md](docs/phase6-announcement-domain.md)를 참고합니다.

### Phase 7: 시간표와 신청 항목 통합

- 관리자 일정 `retreat_schedule_items`
- 일정 생성/목록/상세/수정 API
- 날짜별 카드형 시간표와 일정 추가·수정 화면
- 선택 입력 가능한 시작/종료 시각과 `시간 미정` 일정
- `collectParticipation`으로 신청서 노출과 선택 인원 집계
- 식사·프로그램 일정과 참가자 선택을 같은 트랜잭션에서 동기화
- 수련회 기간 변경 시 일차 기준 이동, 범위 밖 일정 비공개
- `STAFF` 이상 조회, `CHAIR` 이상 변경

자세한 내용은 [docs/phase7-schedule-domain.md](docs/phase7-schedule-domain.md)를 참고합니다.

### Phase 8: 체크인 도메인

- 관리자 체크인 roster/query API
- 개인정보 보호형 체크인 목록/상세 응답
- 수동 체크인과 중복 체크인 방지
- 체크인 취소/되돌리기와 취소 사유 필수 입력
- 체크인/취소 이벤트 이력 저장
- 체크인/취소 수행 관리자와 시각 기록
- 향후 QR 체크인을 위한 관리자 토큰 발급/폐기 API
- `STAFF` 이상 조회/수동 체크인, `CHAIR` 이상 체크인 취소와 QR 토큰 관리

자세한 내용은 [docs/phase8-check-in-domain.md](docs/phase8-check-in-domain.md)를 참고합니다.

### Phase 9: 참가비 관리 도메인

- 관리자 참가비 roster/query API
- 개인정보 보호형 참가비 목록/상세 응답
- 기존 `registrations.fee_paid` 현재 상태 유지
- 참가비 상태 변경 시 변경 전/후 상태, 수행 관리자, 사유, 시각 이벤트 저장
- 이미 납부/미납 상태인 경우 중복 변경 방지
- 납부 취소(미납 전환)는 사유 필수
- `STAFF` 이상 조회, `CHAIR` 이상 참가비 상태 변경

자세한 내용은 [docs/phase9-fee-management-domain.md](docs/phase9-fee-management-domain.md)를 참고합니다.

### 단일 현재 수련회 생명주기

- 수련회 `retreats`와 `DRAFT → OPEN → CLOSED` 상태 전환
- `DRAFT` 또는 `OPEN` 수련회는 DB 전체에서 최대 하나
- 운영 상태 `OPEN`과 신규 신청 접수 `registrationOpen`을 분리
- 신규 신청을 마감해도 `OPEN` 동안 기존 참가자 본인 수정 가능
- 참가 신청, 참석·식사 항목, 수련회 조, 공지, 일정은 수련회별로 분리
- 수련회 종료 시 활성 참가 인원수를 요약으로 저장
- 종료된 수련회의 참가자 상세는 현재 운영 API에서 제외
- 종료된 수련회의 시간표는 `retreatId`로 조회 가능
- 관리자 수련회 생성, 설정 변경, 신규 신청 열기·마감, 운영 종료 화면

자세한 내용은 [docs/multi-retreat-foundation.md](docs/multi-retreat-foundation.md)를 참고합니다.

## 기술 스택

- Java 21
- Spring Boot 3.5.15
- Gradle
- PostgreSQL
- Docker Compose
- Flyway
- MyBatis
- Spring Security
- JUnit 5
- Testcontainers PostgreSQL
- Lombok

## 로컬 개발 준비물

- Java 21
- Docker Desktop
- Docker Compose
- PostgreSQL은 로컬 설치 대신 Docker Compose로 실행

## 로컬 DB 시작

```bash
docker compose up -d postgres
```

로컬 DB 기본값:

- Host: `localhost`
- Port: `5432`
- Database: `church_retreat_ops`
- User: `retreat_app`
- Password: `retreat_app_password`

DB 중지:

```bash
docker compose down
```

DB 볼륨까지 삭제:

```bash
docker compose down -v
```

## 백엔드 실행

```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

기본 포트는 `8080`입니다.

## 앱 아이덴티티 설정

화면에 표시되는 앱 이름, 교회/단체명, 행사명은 환경변수로 바꿀 수 있습니다. 현재 수련회가 있으면 응답의 행사명은 수련회 이름을 사용합니다.

- `APP_IDENTITY_APP_NAME`: 기본값 `청년2부 수련회`
- `APP_IDENTITY_ORGANIZATION_NAME`: 기본값 `지구촌교회 드림공동체 청년2부`
- `APP_IDENTITY_EVENT_NAME`: 현재 수련회가 없을 때 사용할 기본 행사명

현재 설정값 확인:

```bash
curl http://localhost:8080/api/app/identity
```

## 프론트엔드 실행

프론트엔드는 `frontend/` 디렉터리의 Vite React 앱입니다. 공개 참가자 화면은 스마트폰 우선, 관리자 화면은 데스크톱/태블릿 우선으로 구성합니다.

```bash
cd frontend
npm install
npm run dev
```

기본 포트는 `5173`입니다. 개발 서버는 `/api` 요청을 `http://localhost:8080` 백엔드로 프록시합니다.

주요 라우트:

- 공개 화면: `/public`, `/public/register`, `/public/self-lookup`, `/public/check-in`
- 관리자 화면: `/admin/login`, `/admin/dashboard`, `/admin/retreats`, `/admin/schedules`, `/admin/participants`, `/admin/retreat-groups`, `/admin/announcements`, `/admin/check-ins`, `/admin/fees`

프론트엔드 빌드 확인:

```bash
cd frontend
npm run build
```

## 테스트

```bash
./gradlew test
```

전체 클린 테스트:

```bash
./gradlew clean test
```

테스트는 Testcontainers PostgreSQL을 사용하므로 Docker Desktop이 실행 중이어야 합니다.

Phase 2 이후 통합 테스트도 같은 명령에 포함됩니다.

## Health Check

```bash
curl http://localhost:8080/api/health
```

## 앱 아이덴티티 조회

```bash
curl http://localhost:8080/api/app/identity
```

## 관리자 로그인 테스트

로컬 개발 기본 관리자 계정:

- Email: `admin@example.local`
- Password: `admin1234!`

```bash
curl -s -X POST http://localhost:8080/api/admin/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"admin@example.local","password":"admin1234!"}'
```

응답의 `data.accessToken` 값을 `/me` API 호출에 사용합니다.

## 현재 관리자 프로필 테스트

```bash
curl -s http://localhost:8080/api/admin/auth/me \
  -H 'Authorization: Bearer <accessToken>'
```

## 참가자 등록 테스트

참가자 등록은 운영진이 현재 수련회의 신규 신청을 연 동안만 가능합니다. 먼저 공개 참석·식사 항목을 조회합니다.

```bash
curl -s http://localhost:8080/api/participation-options
```

조회 키는 참가자가 정한 숫자 6자리이며 서버에는 BCrypt 해시만 저장됩니다.

```bash
curl -s -X POST http://localhost:8080/api/registrations \
  -H 'Content-Type: application/json' \
  -d '{
    "name":"Grace Kim",
    "gender":"FEMALE",
    "birthYear":1991,
    "phoneNumber":"010-1234-5678",
    "middleGroupName":"드림 중그룹",
    "cellName":"사랑 셀",
    "privacyConsentAgreed":true,
    "lookupKey":"123456",
    "attendanceType":"FULL",
    "lodgingNight1":true,
    "lodgingNight2":true,
    "selectedOptionIds":[],
    "inboundTransportationMethod":"GROUP_BUS",
    "outboundTransportationMethod":"GROUP_BUS"
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

본인 수정의 전체 요청 예시는 [http/02-participant-registration.http](http/02-participant-registration.http)를 참고합니다. 신규 신청을 마감한 뒤에도 수련회가 `OPEN`인 동안 수정할 수 있으며 운영진 목록에 마지막 본인 수정 시각이 표시됩니다.

관리자 등록 목록과 상세는 JWT가 필요합니다.

```bash
curl -s http://localhost:8080/api/admin/registrations \
  -H 'Authorization: Bearer <accessToken>'

curl -s http://localhost:8080/api/admin/registrations/1 \
  -H 'Authorization: Bearer <accessToken>'
```

관리자 참가자 관리:

```bash
curl -s -X PATCH http://localhost:8080/api/admin/registrations/1/fee-paid \
  -H 'Authorization: Bearer <accessToken>' \
  -H 'Content-Type: application/json' \
  -d '{"feePaid":true}'

curl -s -X PATCH http://localhost:8080/api/admin/registrations/1/status \
  -H 'Authorization: Bearer <accessToken>' \
  -H 'Content-Type: application/json' \
  -d '{"status":"CANCELLED"}'

curl -s -X PATCH http://localhost:8080/api/admin/registrations/1/management \
  -H 'Authorization: Bearer <accessToken>' \
  -H 'Content-Type: application/json' \
  -d '{
    "adminMemo":"Needs first-time attendee follow-up.",
    "newcomer":true,
    "careTarget":true
  }'
```

## 수련회 조 편성 테스트

수련회 조는 신청 당시 중그룹·셀 정보와 별도의 임시 운영 구조입니다. `STAFF`는 조회, `CHAIR` 이상은 생성/수정/활성 상태 변경, 참가자 배정/해제, 조장 지정/해제를 수행할 수 있습니다.

```bash
curl -s -X POST http://localhost:8080/api/admin/retreat-groups \
  -H 'Authorization: Bearer <accessToken>' \
  -H 'Content-Type: application/json' \
  -d '{
    "name":"Group 1",
    "description":"First retreat group",
    "displayOrder":0
  }'

curl -s -X PATCH http://localhost:8080/api/admin/participants/1/retreat-group \
  -H 'Authorization: Bearer <accessToken>' \
  -H 'Content-Type: application/json' \
  -d '{"retreatGroupId":1}'

curl -s http://localhost:8080/api/admin/retreat-groups/1/members \
  -H 'Authorization: Bearer <accessToken>'

curl -s -X PATCH http://localhost:8080/api/admin/retreat-groups/1/leader \
  -H 'Authorization: Bearer <accessToken>' \
  -H 'Content-Type: application/json' \
  -d '{"participantId":1}'

curl -s http://localhost:8080/api/admin/retreat-groups/tree \
  -H 'Authorization: Bearer <accessToken>'
```

## 공지 도메인 테스트

공지 관리는 JWT가 필요합니다. `STAFF`는 조회, `CHAIR` 이상은 생성/수정/활성 상태 변경/고정 상태 변경을 수행할 수 있습니다.

```bash
curl -s -X POST http://localhost:8080/api/admin/announcements \
  -H 'Authorization: Bearer <accessToken>' \
  -H 'Content-Type: application/json' \
  -d '{
    "title":"Retreat check-in starts at 3 PM",
    "content":"Please arrive at the main lobby and check in with your group.",
    "pinned":true,
    "active":true,
    "visibleFrom":"2026-07-01T00:00:00Z",
    "visibleUntil":"2026-07-31T23:59:59Z",
    "targets":[
      {
        "targetType":"ALL"
      }
    ]
  }'

curl -s http://localhost:8080/api/admin/announcements \
  -H 'Authorization: Bearer <accessToken>'

curl -s -X PATCH http://localhost:8080/api/admin/announcements/1/pinned \
  -H 'Authorization: Bearer <accessToken>' \
  -H 'Content-Type: application/json' \
  -d '{"pinned":false}'
```

## 시간표 테스트

시간표 관리는 JWT가 필요합니다. `STAFF`는 조회, `CHAIR` 이상은 생성/수정/공개 상태 변경을 수행할 수 있습니다. `collectParticipation`을 켜면 같은 일정이 참가 신청서에도 표시됩니다.

```bash
curl -s -X POST http://localhost:8080/api/admin/schedules \
  -H 'Authorization: Bearer <accessToken>' \
  -H 'Content-Type: application/json' \
  -d '{
    "title":"저녁식사",
    "scheduleDate":"2027-01-15",
    "startsAt":"2027-01-15T18:00:00+09:00",
    "endsAt":"2027-01-15T19:00:00+09:00",
    "location":"식당",
    "category":"MEAL",
    "targetAudience":"ALL",
    "active":true,
    "displayOrder":1080,
    "collectParticipation":true
  }'

curl -s 'http://localhost:8080/api/admin/schedules?date=2027-01-15&category=MEAL&active=true' \
  -H 'Authorization: Bearer <accessToken>'

curl -s -X PATCH http://localhost:8080/api/admin/schedules/1/active \
  -H 'Authorization: Bearer <accessToken>' \
  -H 'Content-Type: application/json' \
  -d '{"active":false}'
```

## 체크인 도메인 테스트

체크인 관리는 JWT가 필요합니다. `STAFF`는 roster 조회와 수동 체크인을 수행할 수 있고, `CHAIR` 이상은 체크인 취소와 QR 토큰 발급/폐기를 수행할 수 있습니다.

```bash
curl -s http://localhost:8080/api/admin/check-ins \
  -H 'Authorization: Bearer <accessToken>'

curl -s -X POST http://localhost:8080/api/admin/check-ins/1 \
  -H 'Authorization: Bearer <accessToken>'

curl -s -X PATCH http://localhost:8080/api/admin/check-ins/1/cancel \
  -H 'Authorization: Bearer <accessToken>' \
  -H 'Content-Type: application/json' \
  -d '{"reason":"Checked in under a duplicate registration."}'

curl -s -X POST http://localhost:8080/api/admin/check-ins/tokens/1 \
  -H 'Authorization: Bearer <accessToken>' \
  -H 'Content-Type: application/json' \
  -d '{"expiresAt":"2026-07-01T23:59:59Z"}'
```

## 참가비 관리 테스트

참가비 관리는 JWT가 필요합니다. `STAFF`는 목록/상세/이력 조회만 가능하고, `CHAIR` 이상은 납부 상태를 변경할 수 있습니다.

```bash
curl -s 'http://localhost:8080/api/admin/fees?feePaid=false' \
  -H 'Authorization: Bearer <accessToken>'

curl -s http://localhost:8080/api/admin/fees/1 \
  -H 'Authorization: Bearer <accessToken>'

curl -s -X PATCH http://localhost:8080/api/admin/fees/1 \
  -H 'Authorization: Bearer <accessToken>' \
  -H 'Content-Type: application/json' \
  -d '{"feePaid":true,"reason":"Confirmed by treasurer"}'

curl -s -X PATCH http://localhost:8080/api/admin/fees/1 \
  -H 'Authorization: Bearer <accessToken>' \
  -H 'Content-Type: application/json' \
  -d '{"feePaid":false,"reason":"Marked paid by mistake"}'
```

## 환경 변수

운영 환경에서는 아래 값을 환경 변수로 주입해야 합니다.

```text
APP_JWT_SECRET
APP_SYSTEM_ADMIN_EMAIL
APP_SYSTEM_ADMIN_PASSWORD
APP_SYSTEM_ADMIN_NAME
APP_REGISTRATION_SELF_EDIT_ENABLED
```

로컬 기본값은 개발 편의를 위한 값입니다. 운영 환경에서 기본 관리자 계정 정보와 JWT secret을 그대로 사용하면 안 됩니다.

## 공개 API와 보호 API

현재 공개 API:

- `GET /api/health`
- `GET /api/app/identity`
- `POST /api/admin/auth/login`
- `GET /api/participation-options`
- `POST /api/registrations`
- `POST /api/registrations/self/lookup`
- `PUT /api/registrations/self`
- Swagger/OpenAPI 경로가 추가될 경우를 대비한 기본 공개 경로

JWT가 필요한 API:

- `GET /api/admin/auth/me`
- `GET /api/admin/retreats`
- `GET /api/admin/retreats/current`
- `POST /api/admin/retreats`
- `PATCH /api/admin/retreats/{id}`
- `PATCH /api/admin/retreats/{id}/status`
- `PATCH /api/admin/retreats/{id}/registration-open`
- `GET /api/admin/participation-options`
- `GET /api/admin/registrations`
- `GET /api/admin/registrations/{id}`
- `GET /api/admin/registrations/{id}/histories`
- `PATCH /api/admin/registrations/{id}/fee-paid`
- `PATCH /api/admin/registrations/{id}/status`
- `PATCH /api/admin/registrations/{id}/management`
- `GET /api/admin/retreat-groups`
- `GET /api/admin/retreat-groups/{id}`
- `POST /api/admin/retreat-groups`
- `PATCH /api/admin/retreat-groups/{id}`
- `PATCH /api/admin/retreat-groups/{id}/active`
- `GET /api/admin/retreat-groups/{id}/members`
- `GET /api/admin/retreat-groups/tree`
- `PATCH /api/admin/participants/{participantId}/retreat-group`
- `DELETE /api/admin/participants/{participantId}/retreat-group`
- `PATCH /api/admin/retreat-groups/{groupId}/leader`
- `DELETE /api/admin/retreat-groups/{groupId}/leader`
- `GET /api/admin/announcements`
- `GET /api/admin/announcements/{id}`
- `POST /api/admin/announcements`
- `PATCH /api/admin/announcements/{id}`
- `PATCH /api/admin/announcements/{id}/active`
- `PATCH /api/admin/announcements/{id}/pinned`
- `GET /api/admin/schedules` (`retreatId`를 지정하면 종료된 수련회 시간표 조회)
- `GET /api/admin/schedules/{id}`
- `POST /api/admin/schedules`
- `PATCH /api/admin/schedules/{id}`
- `PATCH /api/admin/schedules/{id}/active`
- `GET /api/admin/check-ins`
- `GET /api/admin/check-ins/{participantId}`
- `POST /api/admin/check-ins/{participantId}`
- `PATCH /api/admin/check-ins/{participantId}/cancel`
- `POST /api/admin/check-ins/tokens/{participantId}`
- `PATCH /api/admin/check-ins/tokens/{participantId}/revoke`
- `GET /api/admin/fees`
- `GET /api/admin/fees/{participantId}`
- `PATCH /api/admin/fees/{participantId}`
- `GET /api/admin/fees/{participantId}/events`
- 그 외 API는 기본적으로 인증 필요

## 아직 구현하지 않은 범위

아래 기능은 현재 단계에서 의도적으로 제외되어 있습니다.

- 참가자 로그인 계정
- 장로/셀 리더 로그인 또는 `admin_users` 연결
- 카카오톡, SMS, 푸시, 이메일 공지 발송
- 참가자 공개 공지 화면과 읽음 확인
- 참가자 공개 일정 화면, 일정 알림, 캘린더 연동, 반복 일정, 출석 추적
- 참가자 공개 체크인 화면, 공개 QR 스캔 API, QR scanner UI, 체크인 알림, 체크인 통계 dashboard
- 실 결제 gateway, 영수증 업로드, 환불 workflow, 정산 자동화
- 종료된 수련회 참가자 개인정보 자동 삭제/익명화 정책

참가자는 관리자 계정이 아닙니다. 관리자 로그인 계정은 수련회 운영진과 시스템 관리자를 위한 `admin_users`에만 저장됩니다.
