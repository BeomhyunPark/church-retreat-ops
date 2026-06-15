# GMC Retreat App

`church-retreat-ops`는 교회 수련회 준비와 현장 운영을 위한 백엔드 프로젝트입니다.

현재는 백엔드 기반과 관리자 인증 기반만 구현되어 있습니다. 참가자 등록, 조 편성, 공지, 일정, 체크인, 프론트엔드는 아직 구현하지 않았습니다.

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

## 테스트

```bash
./gradlew test
```

전체 클린 테스트:

```bash
./gradlew clean test
```

테스트는 Testcontainers PostgreSQL을 사용하므로 Docker Desktop이 실행 중이어야 합니다.

## Health Check

```bash
curl http://localhost:8080/api/health
```

## 관리자 로그인 테스트

로컬 개발 기본 관리자 계정:

- Email: `admin@gmc.local`
- Password: `admin1234!`

```bash
curl -s -X POST http://localhost:8080/api/admin/auth/login \
  -H 'Content-Type: application/json' \
  -d '{"email":"admin@gmc.local","password":"admin1234!"}'
```

응답의 `data.accessToken` 값을 `/me` API 호출에 사용합니다.

## 현재 관리자 프로필 테스트

```bash
curl -s http://localhost:8080/api/admin/auth/me \
  -H 'Authorization: Bearer <accessToken>'
```

## 환경 변수

운영 환경에서는 아래 값을 환경 변수로 주입해야 합니다.

```text
APP_JWT_SECRET
APP_SYSTEM_ADMIN_EMAIL
APP_SYSTEM_ADMIN_PASSWORD
APP_SYSTEM_ADMIN_NAME
```

로컬 기본값은 개발 편의를 위한 값입니다. 운영 환경에서 기본 관리자 계정 정보와 JWT secret을 그대로 사용하면 안 됩니다.

## 공개 API와 보호 API

현재 공개 API:

- `GET /api/health`
- `POST /api/admin/auth/login`
- Swagger/OpenAPI 경로가 추가될 경우를 대비한 기본 공개 경로

JWT가 필요한 API:

- `GET /api/admin/auth/me`
- 그 외 API는 기본적으로 인증 필요

## 아직 구현하지 않은 범위

아래 기능은 현재 단계에서 의도적으로 제외되어 있습니다.

- 참가자 등록
- 참가자 조회 또는 참가자 로그인
- 조 편성
- 공지
- 일정
- 체크인
- 프론트엔드

참가자는 관리자 계정이 아닙니다. 관리자 로그인 계정은 수련회 운영진과 시스템 관리자를 위한 `admin_users`에만 저장됩니다.
