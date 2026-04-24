<div align="center">

# MediPrice

**비급여 진료비 비교 플랫폼**

[![Java](https://img.shields.io/badge/Java%2021-007396?style=flat-square&logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/21/)
[![Spring](https://img.shields.io/badge/Spring%207.0.6-6DB33F?style=flat-square&logo=spring&logoColor=white)](https://spring.io/projects/spring-framework)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-4169E1?style=flat-square&logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![PostGIS](https://img.shields.io/badge/PostGIS-4EAA25?style=flat-square&logo=postgresql&logoColor=white)](https://postgis.net/)
[![Tomcat](https://img.shields.io/badge/Tomcat%2011-F8DC75?style=flat-square&logo=apachetomcat&logoColor=black)](https://tomcat.apache.org/)
[![Docker](https://img.shields.io/badge/Docker-2496ED?style=flat-square&logo=docker&logoColor=white)](https://www.docker.com/)

</div>

---

## 목차

- [프로젝트 소개](#프로젝트-소개)
- [주요 기능](#주요-기능)
- [기술 스택](#기술-스택)
- [아키텍처](#아키텍처)
- [패키지 구조](#패키지-구조)
- [시작하기](#시작하기)
- [환경변수 설정](#환경변수-설정)
- [API 명세](#api-명세)
- [인증 방식](#인증-방식)
- [팀원](#팀원)

---

## 프로젝트 소개

도수치료, 영양주사, 추나요법처럼 건강보험이 적용되지 않는 비급여 항목은 병원마다 가격이 천차만별이지만, 환자가 진료 전에 가격을 확인할 공식적인 수단이 없다.

MediPrice는 건강보험심사평가원(심평원) 공공 API를 기반으로 주변 병원의 비급여 진료비를 지도 위에 시각화하는 웹 서비스다. 원하는 항목을 검색하면 현재 위치 기준으로 가까운 병원들의 가격이 핀으로 표시되고, 지역 평균·종별 평균과 비교한 가격 수준도 함께 제공한다. 전화하거나 직접 찾아가지 않아도 주변 병원의 비급여 가격을 한눈에 비교할 수 있다.

---

## 주요 기능

- **지도 기반 가격 시각화** — 네이버맵 API 기반. 가격대에 따라 핀 색상을 달리해 주변 병원의 가격 분포를 한눈에 파악. 지도 이동 시 해당 영역 병원 정보 자동 업데이트
- **비급여 항목 검색 및 필터** — 키워드 검색, 거리순·가격순 정렬, 병원 종별·진료과·가격 범위 필터 제공
- **병원 상세 페이지** — 전체 비급여 항목 가격 목록, 연락처, 진료과목, 운영 시간 제공. 지역 평균 및 종별 평균 대비 가격 수준 표시
- **지역·종별 평균가 비교** — 시·군·구별 평균가, 의원·병원·종합병원 종별 평균가 비교 제공
- **즐겨찾기 및 가격 비교** — 관심 병원 즐겨찾기 저장, 최대 3개 병원 가격 비교 테이블

---

## 기술 스택

### Backend
[![Java](https://img.shields.io/badge/Java%2021-007396?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/21/)
[![Spring](https://img.shields.io/badge/Spring%207.0.6-6DB33F?style=for-the-badge&logo=spring&logoColor=white)](https://spring.io/projects/spring-framework)
[![Spring Security](https://img.shields.io/badge/Spring%20Security-6DB33F?style=for-the-badge&logo=springsecurity&logoColor=white)](https://spring.io/projects/spring-security)
[![Gradle](https://img.shields.io/badge/Gradle-02303A?style=for-the-badge&logo=gradle&logoColor=white)](https://gradle.org/)

### Frontend
[![JSP](https://img.shields.io/badge/JSP%20+%20JSTL-D22128?style=for-the-badge&logo=apache&logoColor=white)]()
[![Tailwind CSS](https://img.shields.io/badge/Tailwind%20CSS-06B6D4?style=for-the-badge&logo=tailwindcss&logoColor=white)](https://tailwindcss.com/)
[![JavaScript](https://img.shields.io/badge/Vanilla%20JS-F7DF1E?style=for-the-badge&logo=javascript&logoColor=black)]()
[![Naver Map](https://img.shields.io/badge/Naver%20Map%20API-03C75A?style=for-the-badge&logo=naver&logoColor=white)](https://navermaps.github.io/maps.js.ncp/)

### Infra & DB
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-4169E1?style=for-the-badge&logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![PostGIS](https://img.shields.io/badge/PostGIS-4EAA25?style=for-the-badge&logo=postgresql&logoColor=white)](https://postgis.net/)
[![Docker](https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white)](https://www.docker.com/)
[![Tomcat](https://img.shields.io/badge/Tomcat%2011-F8DC75?style=for-the-badge&logo=apachetomcat&logoColor=black)](https://tomcat.apache.org/)
[![NGINX](https://img.shields.io/badge/NGINX-009639?style=for-the-badge&logo=nginx&logoColor=white)](https://nginx.org/)
[![Cloudflare Tunnel](https://img.shields.io/badge/Cloudflare%20Tunnel-F38020?style=for-the-badge&logo=cloudflare&logoColor=white)](https://www.cloudflare.com/products/tunnel/)

---

## 아키텍처

> 추후 작성 예정

---

## 패키지 구조

> 추후 작성 예정

---

## 시작하기

### 사전 요구사항

- Docker & Docker Compose
- 심평원 공공 API 키 ([공공데이터포털](https://www.data.go.kr) 발급)
- 네이버맵 API 키 ([네이버 클라우드 플랫폼](https://www.ncloud.com) 발급)

### 실행

```bash
git clone https://github.com/YOUR_REPO/mediprice.git
cd mediprice

# 환경변수 파일 설정 (아래 환경변수 설정 참고)
cp .env.example .env

# 실행
docker-compose up -d
```

서버가 정상적으로 올라오면 `http://localhost:8080` 으로 접속한다.

---

## 환경변수 설정

프로젝트 루트에 `.env` 파일을 생성하고 아래 항목을 채운다.

```env
# 심평원 공공 API
HIRA_API_KEY=your_hira_api_key_here

# 네이버맵 API
NAVER_MAP_CLIENT_ID=your_naver_client_id_here
NAVER_MAP_CLIENT_SECRET=your_naver_client_secret_here

# 데이터베이스
DB_HOST=localhost
DB_PORT=5432
DB_NAME=mediprice
DB_USERNAME=your_db_username
DB_PASSWORD=your_db_password
```

---

## API 명세

> 추후 작성 예정

---

## 인증 방식

> 추후 작성 예정

---

## 팀원

<table align="center">
  <tr>
    <td align="center"><img src="https://github.com/kmj228.png" width="100px;"/><br /><sub><b>김민재</b></sub><br /><a href="https://github.com/kmj228">kmj228</a></td>
    <td align="center"><img src="https://github.com/khm1102.png" width="100px;"/><br /><sub><b>김현민</b></sub><br /><a href="https://github.com/khm1102">khm1102</a></td>
    <td align="center"><img src="https://github.com/identicons/placeholder2.png" width="100px;"/><br /><sub><b>이상건</b></sub><br /><a href="https://github.com/">GitHub</a></td>
    <td align="center"><img src="https://github.com/identicons/placeholder3.png" width="100px;"/><br /><sub><b>정재운</b></sub><br /><a href="https://github.com/">GitHub</a></td>
  </tr>
  <tr>
    <td align="center">PM</td>
    <td align="center">PL</td>
    <td align="center">QA</td>
    <td align="center">Docs</td>
  </tr>
</table>

<div align="center">
  <sub>한국공학대학교 AI소프트웨어학과 · 김민재(kmj228) · 김현민(khm1102)</sub>
</div>
