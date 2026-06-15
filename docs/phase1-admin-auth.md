# Phase 1 관리자 인증 / 역할 기반 권한 기초

이 문서는 GMC Retreat App의 Phase 1 구현 범위를 설명합니다.

## 목적

Phase 1의 목적은 수련회 운영진이 사용할 관리자 인증 기반을 만드는 것입니다.

이 단계에서는 관리자 로그인, JWT 인증, 관리자 역할 계층, 초기 시스템 관리자 계정 생성까지만 다룹니다. 참가자 등록이나 수련회 운영 기능은 아직 구현하지 않습니다.

## 관리자와 참가자를 분리하는 이유

참가자는 수련회 신청자 또는 참석자입니다. 관리자는 수련회를 운영하는 스태프, 준비위원장, 목회자, 시스템 관리자입니다.

두 대상은 권한, 데이터 접근 범위, 보안 요구사항이 다르기 때문에 같은 사용자 테이블로 섞지 않습니다.

- 관리자 계정: `admin_users`
- 참가자 정보: 향후 별도 참가자/등록 도메인에서 구현

Phase 1에서는 참가자 테이블과 참가자 로그인 기능을 만들지 않습니다.

## 관리자 역할 계층

지원하는 관리자 역할은 다음과 같습니다.

```text
STAFF < CHAIR < PASTOR < SYSTEM_ADMIN
```

- `STAFF`: 일반 운영 스태프
- `CHAIR`: 준비위원장
- `PASTOR`: 목회자, `CHAIR`보다 높은 권한
- `SYSTEM_ADMIN`: 시스템 계정 관리와 예외적 운영을 위한 최상위 권한

역할 계층은 `AdminRole.hasAuthorityAtLeast(...)`로 표현됩니다.

## 로그인 흐름

1. 클라이언트가 `POST /api/admin/auth/login`으로 이메일과 비밀번호를 보냅니다.
2. 서버가 `admin_users`에서 이메일을 조회합니다.
3. BCrypt로 저장된 `password_hash`와 입력 비밀번호를 비교합니다.
4. 관리자 상태가 `ACTIVE`인지 확인합니다.
5. 성공하면 JWT Access Token을 발급합니다.
6. 실패하면 이메일과 비밀번호 중 무엇이 틀렸는지 노출하지 않고 동일한 로그인 실패 응답을 반환합니다.

## JWT 인증 흐름

1. 클라이언트가 보호 API 요청에 `Authorization: Bearer <token>` 헤더를 보냅니다.
2. JWT 필터가 토큰을 파싱합니다.
3. HS256 서명을 검증합니다.
4. 만료 시간을 검증합니다.
5. 필수 클레임과 역할 값을 검증합니다.
6. 유효하면 Spring Security 인증 컨텍스트에 관리자 Principal을 설정합니다.

잘못된 서명, 만료된 토큰, 누락된 클레임, 잘못된 `sub`, 알 수 없는 `role`은 인증 실패로 처리됩니다.

## Bootstrap SYSTEM_ADMIN

애플리케이션 시작 시 설정된 이메일의 관리자 계정이 없으면 초기 `SYSTEM_ADMIN` 계정을 생성합니다.

사용하는 환경 변수:

```text
APP_SYSTEM_ADMIN_EMAIL
APP_SYSTEM_ADMIN_PASSWORD
APP_SYSTEM_ADMIN_NAME
```

비밀번호는 평문으로 저장하지 않고 BCrypt 해시로 저장합니다.

이미 같은 이메일의 계정이 있으면 새로 만들지 않습니다.

## 공개 API

```text
GET  /api/health
POST /api/admin/auth/login
```

Swagger/OpenAPI 경로는 향후 문서 도구 추가를 위해 Security 설정에서 공개 경로로 유지됩니다.

## 보호 API

```text
GET /api/admin/auth/me
```

그 외 API는 기본적으로 인증이 필요합니다.

## 로컬 테스트 명령

DB 실행:

```bash
docker compose up -d postgres
```

백엔드 실행:

```bash
./gradlew bootRun --args='--spring.profiles.active=local'
```

Health Check:

```bash
curl http://localhost:8080/api/health
```

로그인:

```bash
curl -s -X POST http://localhost:8080/api/admin/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"admin@gmc.local","password":"admin1234!"}'
```

현재 관리자 프로필:

```bash
curl -s http://localhost:8080/api/admin/auth/me \
  -H 'Authorization: Bearer <accessToken>'
```

IntelliJ HTTP Client를 사용할 경우 로컬에서 http/admin-auth.http 파일을 만들어 테스트할 수 있다.

테스트:

```bash
./gradlew clean test
```

## 보안 메모

- JWT secret은 코드에 하드코딩하지 않습니다.
- 운영 환경에서는 `APP_JWT_SECRET`을 반드시 안전한 값으로 설정해야 합니다.
- 로컬 기본 관리자 비밀번호와 JWT secret은 개발용입니다.
- `INACTIVE` 또는 `LOCKED` 관리자 계정은 로그인할 수 없습니다.
- 로그인 실패 응답은 이메일 존재 여부를 노출하지 않습니다.
- 잘못된 JSON 요청 본문은 내부 파서 예외를 노출하지 않고 `INVALID_REQUEST`로 응답합니다.

## Phase 1에서 의도적으로 제외한 것

- 참가자 등록
- 참가자 로그인
- 참가자 조회
- 조 편성
- 공지
- 일정
- 체크인
- 프론트엔드
- Refresh Token
- OAuth 또는 소셜 로그인
