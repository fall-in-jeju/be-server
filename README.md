\# be-server



\# 1. 📁 전체 폴더 구조

> Controller / Service / Model / Infrastructure 계층 분리 가이드 (참고용)





```md

src

└── main

&nbsp;   └── java

&nbsp;       └── com.project

&nbsp;           ├── controller

&nbsp;           │   ├── user

&nbsp;           │   │   └── UserController.java

&nbsp;           │   └── post

&nbsp;           │       └── PostController.java

&nbsp;           │

&nbsp;           ├── service

&nbsp;           │   ├── user

&nbsp;           │   │   ├── UserService.java

&nbsp;           │   │   └── UserServiceImpl.java

&nbsp;           │   └── post

&nbsp;           │       ├── PostService.java

&nbsp;           │       └── PostServiceImpl.java

&nbsp;           │

&nbsp;           ├── model

&nbsp;           │   ├── domain

&nbsp;           │   │   ├── user

&nbsp;           │   │   │   └── User.java

&nbsp;           │   │   └── post

&nbsp;           │   │       └── Post.java

&nbsp;           │   │

&nbsp;           │   ├── dto

&nbsp;           │   │   ├── user

&nbsp;           │   │   │   ├── UserRequestDto.java

&nbsp;           │   │   │   └── UserResponseDto.java

&nbsp;           │   │   └── post

&nbsp;           │   │       ├── PostRequestDto.java

&nbsp;           │   │       └── PostResponseDto.java

&nbsp;           │   │

&nbsp;           │   ├── dao

&nbsp;           │   │   └── PostWithAuthorDao.java

&nbsp;           │   │

&nbsp;           │   ├── jpa

&nbsp;           │   │   ├── user

&nbsp;           │   │   │   └── UserEntity.java

&nbsp;           │   │   └── post

&nbsp;           │   │       └── PostEntity.java

&nbsp;           │   │

&nbsp;           │   └── mapper

&nbsp;           │       ├── user

&nbsp;           │       │   └── UserMapper.java

&nbsp;           │       └── post

&nbsp;           │           └── PostMapper.java

&nbsp;           │

&nbsp;           ├── infrastructure

&nbsp;           │   ├── repository

&nbsp;           │   │   ├── user

&nbsp;           │   │   │   └── UserJpaRepository.java

&nbsp;           │   │   └── post

&nbsp;           │   │       └── PostJpaRepository.java

&nbsp;           │   │

&nbsp;           │   ├── persistence

&nbsp;           │   │   ├── user

&nbsp;           │   │   │   └── UserRepositoryImpl.java

&nbsp;           │   │   └── post

&nbsp;           │   │       └── PostRepositoryImpl.java

&nbsp;           │   │

&nbsp;           │   └── config

&nbsp;           │       └── JpaConfig.java

&nbsp;           │

&nbsp;           └── common

&nbsp;               ├── exception

&nbsp;               ├── util

&nbsp;               └── constants

```



\# 2. 🎯 계층별 역할 정의

\## 2.1 Controller Layer



\#### HTTP 요청을 처리하고 DTO ↔ Service 간 데이터를 전달하는 계층



\### ✔ 역할



\- REST API 엔드포인트 제공



\- RequestDto → Domain 변환 요청



\- Service 호출



\- Domain → ResponseDto 변환



\### ✔ 포함 파일



\- UserController



\- PostController



\### ❌ 포함하면 안 되는 것



\- 비즈니스 로직



\- DB 접근 로직



\## 2.2 Service Layer



\#### 도메인 규칙을 수행하고 비즈니스 로직을 처리하는 계층



\### ✔ 역할



\- 트랜잭션 관리



\- 도메인 로직 실행



\- Repository 호출



\- Domain 객체 조작



\### ✔ 포함 파일



\- UserService, UserServiceImpl



\- PostService, PostServiceImpl



\### ❌ 포함하면 안 되는 것



\- DTO/Entity 직접 변환 (Mapper로 위임)



\- DB 조회 쿼리 작성



\## 2.3 Model Layer



\#### 도메인 중심 객체들과 표현 객체들을 모아둔 계층



\### a) Domain (핵심 비즈니스 모델)



\- 비즈니스 규칙을 담은 순수 객체



\- JPA, Spring, 기술 의존성 없음



```java

public class User {

&nbsp;   private Long id;

&nbsp;   private String nickname;



&nbsp;   public void changeNickname(String nickname) {

&nbsp;       this.nickname = nickname;

&nbsp;   }

}

```



\### b) DTO (계층 간 데이터 전달)



\#### Request/Response 모델



\#### Controller와 직접 연결



\- UserRequestDto, UserResponseDto

\- PostRequestDto, PostResponseDto



\### c) DAO (조회 전용 복합 데이터)



\#### JOIN 결과와 같은 특수 조회용 객체



\- Domain 또는 Entity와 일치할 필요 없음



\- PostWithAuthorDao



\### d) JPA Entity (영속성 모델)



\#### DB 테이블 매핑용 객체



\- Service/Controller로 직접 노출하지 않음



\- UserEntity, PostEntity



\### e) Mapper (MapStruct 기반 매핑)



\#### DTO ↔ Domain ↔ Entity 변환 자동화



\- UserMapper, PostMapper



\## 2.4 Infrastructure Layer



\### 데이터 접근, 외부 시스템 연동, 기술 구현 계층



\### ✔ Repository (JPA 인터페이스)



\- Spring Data JPA 기반 CRUD 인터페이스



\- UserJpaRepository, PostJpaRepository



\### ✔ Persistence (도메인 저장소 구현체)



\- Domain ↔ Entity 매핑



\- QueryDSL, JPA 동작 수행



\- UserRepositoryImpl, PostRepositoryImpl



\### ✔ Config (설정 파일)



\- JPA 설정



\- Redis, Kafka, Security 등 환경 설정



\- JpaConfig



\# 3. 🧩 DTO / DAO / Domain / Entity / Mapper 구분

아래 표는 백엔드 구조에서 사용되는 객체들의 \*\*역할/위치/로직 포함 여부\*\*를 정리한 내용입니다.



| 타입 | 목적 | 위치 | 로직 포함 여부 |

|------|-----------------------------|----------------|----------------|

| \*\*DTO\*\* | 요청·응답 데이터 전달 | `model/dto` | ❌ |

| \*\*DAO\*\* | 복합 조회 결과(조회 전용 구조) | `model/dao` | ❌ |

| \*\*Domain\*\* | 비즈니스 핵심 모델(상태 + 행위) | `model/domain` | ✔ |

| \*\*Entity\*\* | DB 테이블 매핑(JPA) | `model/jpa` | ❌ |

| \*\*Mapper\*\* | DTO ↔ Domain ↔ Entity 변환 | `model/mapper` | ✔ (변환 로직) |



\# 4. 🔥 폴더 구조 설계 원칙

\#### ✔ 1) Controller는 요청만 조정한다 — “Thin Controller”

\#### ✔ 2) Service는 로직만 수행한다 — “Fat Service”

\#### ✔ 3) Domain은 상태와 규칙을 갖는다

\#### ✔ 4) Entity는 영속성만 담당한다

\#### ✔ 5) Repository는 DB 접근에만 집중한다

\#### ✔ 6) Mapper로 변환 책임을 단일화한다

\#### ✔ 7) DAO는 복잡한 조회에만 사용한다
