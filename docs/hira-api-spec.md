# HIRA 공공 API 명세 및 데이터 가공 정의

> **최종 수정일** 2026-05-19  
> **작성 방법** 실제 API 호출 결과, 공식 docx, 로컬 배치 결과를 대조  
> **샘플 병원** 강북삼성병원 (서울 종로구, `sidoCd=110000`)  
> **API 키** `.env` 파일의 `HIRA_API_KEYS` 또는 `HIRA_API_KEY` 사용

---

## 0. 전체 API 목록

| # | 서비스명 | Base URL | 오퍼레이션 | 용도 |
|---|---|---|---|---|
| 1 | 병원정보서비스 | `apis.data.go.kr/B551182/hospInfoServicev2` | `getHospBasisList` | 배치 — 병원 기본정보 |
| 2 | 비급여진료비정보서비스 | `apis.data.go.kr/B551182/nonPaymentDamtInfoService` | `getNonPaymentItemCodeList2` | 배치 — 비급여 항목 코드 (신버전) |
| 3 | 비급여진료비정보서비스 | `apis.data.go.kr/B551182/nonPaymentDamtInfoService` | `getNonPaymentItemHospDtlList` | 배치 — 병원별 비급여 가격 |
| 3-1 | 비급여진료비정보서비스 | `apis.data.go.kr/B551182/nonPaymentDamtInfoService` | `getNonPaymentItemCodeList` (구버전) | 배치 — 항목 분류별 일반인용 설명 |
| 3-2 | 비급여진료비정보서비스 | `apis.data.go.kr/B551182/nonPaymentDamtInfoService` | `getNonPaymentItemHospList2` | 배치 — 병원×항목 시기별 min/max 가격 |
| 3-3 | 비급여진료비정보서비스 | `apis.data.go.kr/B551182/nonPaymentDamtInfoService` | `getNonPaymentItemClcdList` | 배치 — 항목 × 의료기관 종별 통계 |
| 3-4 | 비급여진료비정보서비스 | `apis.data.go.kr/B551182/nonPaymentDamtInfoService` | `getNonPaymentItemSidoCdList` | 배치 — 항목 × 시도별 통계 |
| 4 | 의료기관별상세정보서비스 | `apis.data.go.kr/B551182/MadmDtlInfoService2.7` | `getDgsbjtInfo2.7` | 실시간 — 진료과목 |
| 5 | 의료기관별상세정보서비스 | `apis.data.go.kr/B551182/MadmDtlInfoService2.7` | `getMedOftInfo2.7` | 실시간 — 의료장비 |
| 6 | 의료기관별상세정보서비스 | `apis.data.go.kr/B551182/MadmDtlInfoService2.7` | `getTrnsprtInfo2.7` | 실시간 — 대중교통 |
| 7 | 의료기관별상세정보서비스 | `apis.data.go.kr/B551182/MadmDtlInfoService2.7` | `getSpclDiagInfo2.7` | 실시간 — 특수진료 |
| 8 | 의료기관별상세정보서비스 | `apis.data.go.kr/B551182/MadmDtlInfoService2.7` | `getDtlInfo2.7` | 실시간 — 주차/진료시간/응급 |

### 인증 키 로테이션

`HiraServiceKeyProvider`가 다중 키를 라운드로빈으로 분배해 일일 호출 한도(키당 10,000건)를 회피.

- `hira.api-keys` — 콤마 구분 다중 키 (우선 사용)
- `hira.api-key` — 단일 키 fallback
- 둘 다 비어 있으면 부팅 실패

공통 응답 구조 (XML):
```xml
<response>
  <header>
    <resultCode>00</resultCode>      <!-- 00 = 정상 -->
    <resultMsg>NORMAL SERVICE.</resultMsg>
  </header>
  <body>
    <items>
      <item>...</item>
    </items>
    <numOfRows>{요청 건수}</numOfRows>
    <pageNo>{페이지 번호}</pageNo>
    <totalCount>{전체 건수}</totalCount>
  </body>
</response>
```

---

## 1. `getHospBasisList` — 병원 기본정보

**호출 방식:** `GET /getHospBasisList?ServiceKey={key}&sidoCd={시도코드}&pageNo={n}&numOfRows={n}`  
**배치 주기:** 월 1회 (또는 수동 트리거)  
**Java 클라이언트:** `HiraHospitalClient.searchHospitals()`

### 실제 응답 필드 전체 목록

| API 필드명 | 타입 | 저장 여부 | DB 컬럼 | 설명 |
|---|---|---|---|---|
| `ykiho` | String | ✓ | `ykiho` (PK) | 암호화된 요양기호 (Base64) |
| `yadmNm` | String | ✓ | `yadm_nm` | 병원명 |
| `clCd` | String | ✓ | `cl_cd` | 의료기관 종류 코드 |
| `clCdNm` | String | ✓ | `cl_cd_nm` | 의료기관 종류명 (예: 상급종합, 의원) |
| `addr` | String | ✓ | `addr` | 도로명 주소 |
| `telno` | String | ✓ | `tel_no` | 전화번호 |
| `hospUrl` | String | ✓ | `hosp_url` | 홈페이지 URL |
| `drTotCnt` | Integer | ✓ | `dr_tot_cnt` | 의사 총 수 |
| `sidoCdNm` | String | ✓ | `sido_cd_nm` | 시도명 (예: 서울) |
| `sgguCdNm` | String | ✓ | `sggu_cd_nm` | 시군구명 (예: 종로구) |
| `XPos` | Double | ✓ | `x_pos` | 경도 — PostGIS `location` 컬럼 생성에 사용 |
| `YPos` | Double | ✓ | `y_pos` | 위도 — PostGIS `location` 컬럼 생성에 사용 |
| `cmdcGdrCnt` | Integer | ✗ | — | 한방의 전문의 수 |
| `cmdcIntnCnt` | Integer | ✗ | — | 한방의 인턴 수 |
| `cmdcResdntCnt` | Integer | ✗ | — | 한방의 레지던트 수 |
| `cmdcSdrCnt` | Integer | ✗ | — | 한방의 일반의 수 |
| `detyGdrCnt` | Integer | ✗ | — | 치과의 전문의 수 |
| `detyIntnCnt` | Integer | ✗ | — | 치과의 인턴 수 |
| `detyResdntCnt` | Integer | ✗ | — | 치과의 레지던트 수 |
| `detySdrCnt` | Integer | ✗ | — | 치과의 일반의 수 |
| `emdongNm` | String | ✗ | — | 읍면동명 |
| `estbDd` | String | ✗ | — | 개원일 (yyyyMMdd) |
| `mdeptGdrCnt` | Integer | ✗ | — | 의과 전문의 수 |
| `mdeptIntnCnt` | Integer | ✗ | — | 의과 인턴 수 |
| `mdeptResdntCnt` | Integer | ✗ | — | 의과 레지던트 수 |
| `mdeptSdrCnt` | Integer | ✗ | — | 의과 일반의 수 |
| `pnursCnt` | Integer | ✗ | — | 간호사 수 |
| `postNo` | String | ✗ | — | 우편번호 |
| `sgguCd` | String | ✗ | — | 시군구 코드 |
| `sidoCd` | String | ✗ | — | 시도 코드 |

### 시도 코드표

| 시도 | sidoCd | 수집 여부 |
|---|---|---|
| 서울 | 110000 | ✓ |
| 부산 | 210000 | ✓ |
| 대구 | 220000 | ✓ |
| 광주 | 290000 | ✓ |
| 대전 | 300000 | ✓ |
| 울산 | 310000 | ✓ |
| 세종 | 360000 | ✓ |
| 경기 | 410000 | ✓ |
| 인천 | 280000 | ✓ |
| 강원 | 420000 | ✓ |
| 충북 | 430000 | ✓ |
| 충남 | 440000 | ✓ |
| 전북 | 450000 | ✓ |
| 전남 | 460000 | ✓ |
| 경북 | 470000 | ✓ |
| 경남 | 480000 | ✓ |
| 제주 | 500000 | ✓ |

### 가공 과정 (`HospitalSyncService` → `HospitalBatchWriter`)

1. 시도 코드별 페이징 조회 (`numOfRows=100`)
2. `HospBasisItem` → `Hospital` 엔티티 변환
3. 페이지/청크 단위 writer에서 `ykiho` 기준 upsert
4. 별도 native UPDATE: `ST_MakePoint(xPos, yPos)::geography` → `location` 컬럼
5. 중간 페이지가 비어도 다음 페이지를 계속 시도한다. 첫 페이지가 비어야 해당 시도 종료로 판단한다.

2026-05-17 로컬 배치 기준 `Hospital` 79,674건 적재 완료.

---

## 2. `getNonPaymentItemCodeList2` — 비급여 항목 코드

**호출 방식:** `GET /getNonPaymentItemCodeList2?ServiceKey={key}&pageNo={n}&numOfRows={n}`  
**Java 클라이언트:** `HiraNonPayClient.searchItemCodes()`

### 실제 응답 필드 전체 목록

| API 필드명 | 타입 | 저장 여부 | DB 컬럼 | 설명 |
|---|---|---|---|---|
| `npayCd` | String | ✓ | `npay_cd` (PK) | 비급여 코드 (예: `ABZ010001`) |
| `npayKorNm` | String | ✓ | `npay_kor_nm` | 한글 항목명 (예: `상급병실료/1인실`) |
| `npayMdivCd` | String | ✓ | `npay_mdiv_cd` | 중분류 코드 (예: `1010A`) |
| `npayMdivCdNm` | String | ✓ | `npay_mdiv_cd_nm` | 중분류명 (예: `상급병실료`) |
| `npaySdivCd` | String | ✓ | `npay_sdiv_cd` | 소분류 코드 |
| `npaySdivCdNm` | String | ✓ | `npay_sdiv_cd_nm` | 소분류명 |
| `adtFrDd` | String | ✓ | `adt_fr_dd` | 적용 시작일 (yyyyMMdd) |
| `adtEndDd` | String | ✓ | `adt_end_dd` | 적용 종료일 (`99991231` = 현재 유효) |
| `npayDtlDivCd` | String | ✗ | — | 세부분류 코드 |
| `npayDtlDivCdNm` | String | ✗ | — | 세부분류명 |

> **`npayDtlDivCd` 비저장 이유:** `npaySdivCd`와 동일값이거나 더 세분화된 코드로, MVP에서 그룹핑 기준은 중분류(`npayMdivCdNm`)만 사용하므로 불필요.

### 가공 과정 (`NonPayItemSyncService`)

1. 전체 조회 (페이징, `numOfRows=100`)
2. `NonPayCodeItem` → `NonPayItem` 엔티티 변환
3. `npayCd` 기준 upsert

---

## 3. `getNonPaymentItemHospDtlList` — 병원별 비급여 가격

**호출 방식:** `GET /getNonPaymentItemHospDtlList?ServiceKey={key}&ykiho={암호화ykiho}&pageNo={n}&numOfRows={n}`  
**Java 클라이언트:** `HiraNonPayClient.searchHospPriceDetail()`

### 실제 응답 필드 전체 목록

| API 필드명 | 타입 | 저장 여부 | DB 컬럼 | 설명 |
|---|---|---|---|---|
| `ykiho` | String | ✓ | `ykiho` (복합PK) | 병원 식별자 |
| `npayCd` | String | ✓ | `npay_cd` (복합PK) | 비급여 코드 |
| `curAmt` | Long | ✓ | `cur_amt` | 현재 신고 가격 (원 단위) |
| `adtFrDd` | String | ✓ | `adt_fr_dd` | 적용 시작일 |
| `adtEndDd` | String | ✓ | `adt_end_dd` | 적용 종료일 |
| `npayKorNm` | String | ✓ (파싱만) | — | 표준 항목명 — DB 미저장, NonPayItem 조인으로 대체 |
| `yadmNpayCdNm` | String | ✗ | — | 병원 자체 공시명 (표준명과 다를 수 있음) |
| `clCd` | String | ✗ | — | 의료기관 종류 코드 |
| `clCdNm` | String | ✗ | — | 의료기관 종류명 |
| `sgguCd` | String | ✗ | — | 시군구 코드 |
| `sgguCdNm` | String | ✗ | — | 시군구명 |
| `sidoCd` | String | ✗ | — | 시도 코드 |
| `sidoCdNm` | String | ✗ | — | 시도명 |
| `sno` | Integer | ✗ | — | 순번 |
| `urlAddr` | String | ✗ | — | 병원 자체 비급여 공시 페이지 URL |
| `yadmNm` | String | ✗ | — | 병원명 |

### 가공 과정 (`PriceSyncService`)

1. DB의 전체 `ykiho` 목록 조회
2. ykiho별 가격 페이징 조회
3. `adtEndDd = '99991231'` (현재 유효)인 항목만 필터
4. `(ykiho, npayCd)` 기준 upsert

> **`yadmNpayCdNm` 비저장 이유:** 병원마다 표기 방식이 달라 비교 불가. 표준 항목명(`npayKorNm`)을 `NonPayItem` 테이블에서 조인해 사용.

---

## 4. `getDgsbjtInfo2.7` — 진료과목

**호출 방식:** `GET /getDgsbjtInfo2.7?ServiceKey={key}&ykiho={ykiho}&pageNo=1&numOfRows=100`  
**Java 클라이언트:** `HiraDetailClient.fetchDgsbjt()`  
**Java 모델:** `DgsbjtItem`

### 실제 응답 필드

| API 필드명 | 타입 | 사용 | Java 필드 | 설명 |
|---|---|---|---|---|
| `dgsbjtCd` | String | ✓ | `dgsbjtCd` | 진료과목 코드 (예: `01`=내과) |
| `dgsbjtCdNm` | String | ✓ | `dgsbjtCdNm` | 진료과목명 (예: `내과`) |
| `dgsbjtPrSdrCnt` | Integer | ✓ | `dgsbjtPrSdrCnt` | 해당 과목 전문의 수 |
| `cdiagDrCnt` | Integer | ✗ | — | 협진의 수 |

**상태:** 필드 매핑 정상

---

## 5. `getMedOftInfo2.7` — 의료장비

**호출 방식:** `GET /getMedOftInfo2.7?ServiceKey={key}&ykiho={ykiho}&pageNo=1&numOfRows=100`  
**Java 클라이언트:** `HiraDetailClient.fetchMedOft()`  
**Java 모델:** `MedOftItem`

### 실제 응답 필드

| API 필드명 | 타입 | 사용 | Java 필드 | 매핑 상태 |
|---|---|---|---|---|
| `oftCd` | String | ✗ | `oftCd` | ✅ |
| `oftCdNm` | String | ✓ | `oftCdNm` | ✅ |
| `oftCnt` | Integer | ✓ | `oftCnt` | ✅ |

**수정 이력:** 기존 모델이 `medOftCd`/`medOftCdNm`/`medOftCnt`로 선언되어 있어 모두 `null`이던 문제를 2026-05-17 수정했다.

**가공:** `oftCdNm`과 `oftCnt`를 병원 상세 의료장비 목록으로 내려준다.

---

## 6. `getTrnsprtInfo2.7` — 대중교통 정보

**호출 방식:** `GET /getTrnsprtInfo2.7?ServiceKey={key}&ykiho={ykiho}&pageNo=1&numOfRows=100`  
**Java 클라이언트:** `HiraDetailClient.fetchTrnsprt()`  
**Java 모델:** `TrnsprtItem`

### 실제 응답 필드

| API 필드명 | 타입 | 설명 | 예시 |
|---|---|---|---|
| `trafNm` | String | 교통수단명 | `지하철`, `시내버스` |
| `lineNo` | String | 노선번호 | `5호선`, `710, 273, 270` |
| `arivPlc` | String | 도착지명 | `서대문역` |
| `dir` | String | 방향/출구 | `4번 출구` |
| `dist` | String | 거리 | `도보 5분`, `50m` |

### 현재 `TrnsprtItem` 모델 필드

| Java 필드 | API 필드 | 실제 출처 | 상태 |
|---|---|---|---|
| `trafNm` | `trafNm` | `getTrnsprtInfo2.7` | ✅ |
| `lineNo` | `lineNo` | `getTrnsprtInfo2.7` | ✅ |
| `arivPlc` | `arivPlc` | `getTrnsprtInfo2.7` | ✅ |
| `dir` | `dir` | `getTrnsprtInfo2.7` | ✅ |
| `dist` | `dist` | `getTrnsprtInfo2.7` | ✅ |

**수정 이력:** 기존 `TrnsprtItem`이 주차 필드를 들고 있어 모두 `null`이던 문제를 2026-05-17 수정했다.

**가공:** 대중교통 row는 `transitList`로 내려준다. 주차/진료시간/응급 정보는 `getDtlInfo2.7`에서 별도로 가져와 `parkingInfo`와 `operatingInfo`로 분리한다.


---

## 7. `getSpclDiagInfo2.7` — 특수진료

**호출 방식:** `GET /getSpclDiagInfo2.7?ServiceKey={key}&ykiho={ykiho}&pageNo=1&numOfRows=100`  
**Java 클라이언트:** `HiraDetailClient.fetchSpclDiag()`  
**Java 모델:** `SpclDiagItem`

### 실제 응답 필드

| API 필드명 | 타입 | Java 필드 | 매핑 상태 |
|---|---|---|---|
| `srchCd` | String | `srchCd` | ✅ |
| `srchCdNm` | String | `srchCdNm` | ✅ |

**수정 이력:** 기존 모델이 `srvTpCd`/`srvTpCdNm`으로 선언되어 있어 모두 `null`이던 문제를 2026-05-17 수정했다.

---

## 8. `getDtlInfo2.7` — 세부정보

**호출 방식:** `GET /getDtlInfo2.7?ServiceKey={key}&ykiho={ykiho}&pageNo=1&numOfRows=1`  
**Java 클라이언트:** `HiraDetailClient.fetchDtlInfo()`  
**Java 모델:** `DtlInfoItem`

### 실제 응답 필드 (강북삼성병원 기준)

| API 필드명 | 타입 | 설명 | 예시 |
|---|---|---|---|
| `parkEtc` | String | 주차 기타 조건 | `외래-당일 최대 8시간` |
| `parkQty` | String | 주차 가능 대수 | `298` |
| `parkXpnsYn` | String | 유료주차 여부 | `Y` / `N` |
| `trmtMonStart`/`trmtMonEnd` | String | 월요일 진료시간 | `0830` / `1700` |
| `trmtTueStart`/`trmtTueEnd` | String | 화요일 진료시간 | (동일 형식) |
| `trmtWedStart`/`trmtWedEnd` | String | 수요일 | |
| `trmtThuStart`/`trmtThuEnd` | String | 목요일 | |
| `trmtFriStart`/`trmtFriEnd` | String | 금요일 | |
| `trmtSatStart`/`trmtSatEnd` | String | 토요일 | `0830` / `1200` |
| `rcvWeek` | String | 평일 접수시간 | `08:00 ~ 17:00` |
| `rcvSat` | String | 토요일 접수시간 | `08:00 ~ 12:00` |
| `lunchWeek` | String | 점심시간 | `12:30 ~ 13:30` |
| `noTrmtSun` | String | 일요일 비진료 | `휴진` |
| `noTrmtHoli` | String | 공휴일 비진료 | `휴진` |
| `emyDayYn` | String | 낮 응급실 여부 | `Y` / `N` |
| `emyNgtYn` | String | 야간 응급실 여부 | `Y` / `N` |
| `emyDayTelNo1` / `emyDayTelNo2` | String | 낮 응급 전화 | |
| `emyNgtTelNo1` / `emyNgtTelNo2` | String | 야간 응급 전화 | |
| `plcNm` | String | 인근 랜드마크명 | `서울역사박물관` |
| `plcDir` | String | 랜드마크 방향 | `서대문 방면` |
| `plcDist` | String | 랜드마크 거리 | `30m` |

> 주차 정보(`parkEtc`, `parkQty`, `parkXpnsYn`)와 진료시간/응급 정보는 이 API에서 가져온다. `getTrnsprtInfo2.7`는 대중교통 목록 전용으로 사용한다.

---

## 9. 버그 요약 및 수정 이력

### 발견된 버그 (수정 완료, 2026-05-17)

| # | 파일 | 버그 | 영향 | 상태 |
|---|---|---|---|---|
| B1 | `MedOftItem.java` | `medOftCd`/`medOftCdNm`/`medOftCnt` → 실제 API는 `oftCd`/`oftCdNm`/`oftCnt` | 의료장비 섹션 항상 빈 목록 | ✅ 수정 |
| B2 | `TrnsprtItem.java` + `HiraDetailClient.java` | `TrnsprtItem`이 주차 필드 보유하나 실제 API는 교통수단 정보 반환 | 교통/주차 섹션 항상 빈 상태 | ✅ 수정 (교통+주차 분리) |
| B3 | `SpclDiagItem.java` | `srvTpCd`/`srvTpCdNm` → 실제 API는 `srchCd`/`srchCdNm` | 특수진료 섹션 항상 빈 목록 | ✅ 수정 |

### 수정 내용

**B1 — `MedOftItem`:** 필드명을 실제 API 응답에 맞춰 `oftCd`/`oftCdNm`/`oftCnt`로 변경.

**B2 — `TrnsprtItem` 재구성 + `DtlInfoItem` 신규:**
- `TrnsprtItem`을 실제 `getTrnsprtInfo2.7` 응답 필드(`trafNm`/`lineNo`/`arivPlc`/`dir`/`dist`)로 교체. 복수 row 지원.
- 신규 `DtlInfoItem` + `HiraDetailClient.fetchDtlInfo()` 추가 — `getDtlInfo2.7` 호출. 주차+진료시간+응급 정보 제공.
- `HospitalDetailBundle`이 5개 결과를 반환하도록 확장 (`hiraDetailExecutor` 풀 크기 4→5).
- `HospitalDetailDto`에 `transitList`/`parkingInfo`/`operatingInfo` 필드 추가, 기존 `trnsprtInfo` 제거.

**B3 — `SpclDiagItem`:** 필드명을 `srchCd`/`srchCdNm`으로 변경.

### 회귀 방지 테스트

- `src/test/java/com/khm1102/mediprice/client/hira/{DgsbjtItem,MedOftItem,SpclDiagItem,TrnsprtItem,DtlInfoItem}Test.java` — 5개 모델 모두 실제 API 응답 샘플 XML로 역직렬화 검증.
- `src/test/java/com/khm1102/mediprice/client/HiraDetailClientTest.java` — WireMock으로 5개 API stub 후 `fetchAll` 결과 검증 + 1개 API 실패 시 격리 검증.

---

## 9-A. 추가 적재 API 4개

### `getNonPaymentItemCodeList` (구버전 항목 + 설명)
| 필드 | 타입 | 비고 |
|---|---|---|
| `divCd1`/`divCd1Nm`/`divCd1Dsc` | String | 1차 분류 코드 + 명 + 설명(TEXT) |
| `divCd2`/`divCd2Nm`/`divCd2Dsc` | String | 2차 분류 |
| `divCd3`/`divCd3Nm`/`divCd3Dsc` | String | 3차 분류 (대부분 null) |

- DB: `NonPayItemDesc` 테이블 — UNIQUE (`div_cd_1`, `div_cd_2`, `div_cd_3`)
- 적재량: ~54건
- 활용: 항목 설명 툴팁

### `getNonPaymentItemHospList2` (병원×항목 가격 요약)
| 필드 | 비고 |
|---|---|
| `ykiho`, `npayCd`, `adtFrDd` | 복합 PK |
| `minPrc`, `maxPrc` | 시기별 가격 범위 |
| `clCd`/`clCdNm`, `sidoCd`/`sidoCdNm`, `sgguCd`/`sgguCdNm` | 종별/지역 메타 |
| `npayKorNm`, `npayMdivCd`, `npayMdivCdNm`, `npaySdivCd`, `npaySdivCdNm`, `npayDtlDivCd`, `npayDtlDivCdNm` | 항목 메타 |
| `yadmNm`, `urlAddr` | 병원명, 비급여 공시 URL |

- DB: `PriceSummary` 테이블 (188,700+ row)
- Producer-Consumer 패턴 적재
- 활용: 시기별/옵션별 가격 변동 표시

### `getNonPaymentItemClcdList` (종별 통계)
| 필드 | 비고 |
|---|---|
| `npayCd`, `stdDate` | 통계 식별 |
| `prcAvg{All,Usgh,Hosp,Gnhp}` | 종별 평균 |
| `prcMin/Max/middAvg{...}` | 종별 최저/최고/중간 |

- DB: `NonPayItemClcdStat` — 정규화 long table `(npay_cd, clcd_key, std_date)` PK
- 종별 key: `All`(전체), `Usgh`(상급종합), `Hosp`(종합·병원), `Gnhp`(의원)
- 활용: "이 병원이 동종 대비 ±X% 비쌈"

### `getNonPaymentItemSidoCdList` (지역별 통계)
| 필드 | 비고 |
|---|---|
| `npayCd`, `stdDate` | 통계 식별 |
| `prcAvg{All + 17 시도}` | 시도별 평균 |
| `prcMin/Max/middAvg{...}` | 시도별 최저/최고/중간 |

- DB: `NonPayItemSidoStat` — 정규화 long table `(npay_cd, sido_key, std_date)` PK
- 실제 확인 key: `All`, `Sl`(서울), `Tg`(대구), `Ich`(인천), `Kw`(광주), `Dj`(대전), `Usn`(울산), `Sj`(세종), `Kyg`(경기), `Kaw`(강원), `Ccn`(충남), `Ksb`(경북), `Ksn`(경남)
- 미확정 key: `Bs`(부산), `Cb`(충북), `Jb`(전북), `Jn`(전남), `Jj`(제주). 현재 문서/응답에서 필드 존재를 확정하지 못했다.
- 활용: "서울 평균 vs 이 병원" 비교

---

## 10. 공식 API 가이드 참조 (hira-docs/ docx)

> `hira-docs/` 폴더의 docx 2개 (`OpenAPI활용가이드_건강보험심사평가원(병원정보서비스).docx`, `OpenAPI활용가이드_건강보험심사평가원(비급여진료비정보서비스).docx`)에서 확인한 공식 명세와 실제 호출 결과를 대조.

### `getHospBasisList` 요청 파라미터 (공식)

| 파라미터 | 국문 | 필수 | 사용 여부 |
|---|---|---|---|
| `ServiceKey` | 인증키 | 필수 | ✓ |
| `pageNo` | 페이지 번호 | 선택 | ✓ |
| `numOfRows` | 결과 수 | 선택 | ✓ |
| `sidoCd` | 시도코드 | 선택 | ✓ |
| `sgguCd` | 시군구코드 | 선택 | ✓ (일부 시도) |
| `emdongNm` | 읍면동명 | 선택 | ✗ |
| `yadmNm` | 병원명 (검색용) | 선택 | ✗ |
| `zipCd` | 분류코드 | 선택 | ✗ |
| `clCd` | 종별코드 | 선택 | ✗ |
| `dgsbjtCd` | 진료과목코드 | 선택 | ✗ |
| `xPos` / `yPos` / `radius` | 좌표 기반 검색 | 선택 | ✗ (PostGIS 대체) |

### `getNonPaymentItemCodeList2` vs 구버전 `getNonPaymentItemCodeList`

docx에 따르면 구버전('16.2월 이전)은 `divCd1`, `divCd2`, `divCd3` 필드 체계를 사용.  
현재 사용 중인 `getNonPaymentItemCodeList2`('16.3월 이후)는 `npayCd`, `npayMdivCd` 체계로 변경됨.  
우리 코드는 정확히 최신 버전을 사용 중이며 필드 매핑도 올바름.

### 비급여 오퍼레이션 목록 (docx 기준, 전체)

| # | 오퍼레이션명 | 국문명 | 사용 여부 |
|---|---|---|---|
| 1 | `getNonPaymentItemCodeList` | 비급여항목코드조회 (구버전) | ✓ |
| 2 | `getNonPaymentItemHospList` | 비급여항목병원목록 (구버전) | ✗ |
| 3 | `getNonPaymentItemCodeList2` | 비급여항목코드조회 (현행) | ✓ |
| 4 | `getNonPaymentItemHospList2` | 비급여항목병원목록요약 | ✓ |
| 5 | `getNonPaymentItemHospDtlList` | 비급여항목병원목록상세 (현행) | ✓ |
| 6 | `getNonPaymentItemClcdList` | 비급여진료비용종별정보 | ✓ |
| 7 | `getNonPaymentItemSidoCdList` | 비급여진료비용지역별정보 | ✓ |

> `getNonPaymentItemHospDtlList`와 `getNonPaymentItemHospList2`는 가격 계열 데이터라 호출량이 크다. 2026-05-17 로컬 배치에서는 quota 초과로 부분 적재 상태다.

---

## 11. DTO 매핑 요약 (API 응답 → 프론트 응답)

### `GET /api/hospitals` → `HospitalSummaryDto`

| DTO 필드 | 출처 | DB 컬럼 / 계산 |
|---|---|---|
| `ykiho` | Hospital | `ykiho` |
| `yadmNm` | Hospital | `yadm_nm` |
| `addr` | Hospital | `addr` |
| `clCdNm` | Hospital | `cl_cd_nm` |
| `telNo` | Hospital | `tel_no` |
| `curAmt` | Price | `cur_amt` |
| `distance` | PostGIS | `ST_Distance(location, 검색좌표)` |
| `lat` | Hospital | `y_pos` |
| `lng` | Hospital | `x_pos` |

### `GET /api/hospitals/{ykiho}` → `HospitalDetailDto`

| DTO 필드 | 출처 | 비고 |
|---|---|---|
| `yadmNm`, `clCdNm`, `addr`, `telNo`, `hospUrl`, `drTotCnt` | Hospital (DB) | |
| `prices` | Price + NonPayItem (DB 조인) | `(ykiho, npayCd)` → `(cur_amt, npay_kor_nm)` |
| `dgsbjtList` | `getDgsbjtInfo2.7` (실시간) | 진료과목명 리스트 |
| `medOftList` | `getMedOftInfo2.7` (실시간) | 의료장비명 리스트 |
| `transitList` | `getTrnsprtInfo2.7` (실시간) | 대중교통 노선 리스트 (지하철/버스) |
| `parkingInfo` | `getDtlInfo2.7` (실시간) | 주차 정보 (필드 전부 null이면 DTO 자체 null) |
| `operatingInfo` | `getDtlInfo2.7` (실시간) | 진료시간+응급 (필드 전부 null이면 DTO 자체 null) |
| `spclDiagList` | `getSpclDiagInfo2.7` (실시간) | 특수진료명 리스트 |
