# Troubleshooting

2026-05-16 ~ 2026-05-17 심평원 배치/문서 검증 중 확인한 문제와 조치 이력.

## 1. 상세 API 모델 필드 불일치

### 증상

병원 상세에서 의료장비, 교통/주차, 특수진료 섹션이 비어 있었다. API 호출은 성공했지만 Jackson XML 역직렬화 결과가 `null`이었다.

### 원인

의료기관별 상세정보서비스의 실제 XML 필드와 Java item 모델명이 달랐다.

| API | 실제 응답 | 기존 모델 문제 | 조치 |
|---|---|---|---|
| `getMedOftInfo2.7` | `oftCd`, `oftCdNm`, `oftCnt` | `medOftCd`, `medOftCdNm`, `medOftCnt`로 선언 | 모델 필드명 수정 |
| `getTrnsprtInfo2.7` | `trafNm`, `lineNo`, `arivPlc`, `dir`, `dist` | 주차 필드(`parkYn` 등)를 들고 있었음 | 교통 모델로 재정의 |
| `getDtlInfo2.7` | `parkEtc`, `parkQty`, 진료시간, 응급실 정보 | 호출 자체가 없었음 | `DtlInfoItem`과 호출 추가 |
| `getSpclDiagInfo2.7` | `srchCd`, `srchCdNm` | `srvTpCd`, `srvTpCdNm`로 선언 | 모델 필드명 수정 |

### 조치

- `HiraDetailClient.fetchAll()`이 상세 API 5개를 병렬 호출하도록 확장했다.
- `HospitalDetailDto`는 `transitList`, `parkingInfo`, `operatingInfo`, `spclDiagList`, `medOftList`를 분리해 내려준다.
- 실제 API 샘플 XML 기반 역직렬화 테스트와 WireMock 클라이언트 테스트를 추가했다.

## 2. HIRA API key 인코딩 문제

### 증상

일부 키에서 `401 Unauthorized`가 반복 발생했다. 특히 base64 계열 키는 같은 endpoint에서도 간헐적으로 실패했다.

### 원인

공공데이터포털 service key는 `+`, `/`, `=` 같은 문자를 포함할 수 있다. URI query에 raw value로 들어가면 서버가 다른 값으로 해석한다.

`DefaultUriBuilderFactory.EncodingMode.VALUES_ONLY`만으로는 현재 호출 방식에서 안전하지 않았다.

### 조치

- HIRA client의 `RestClient` URI builder encoding mode를 `URI_COMPONENT`로 변경했다.
- 문제가 있던 `key3`는 진행 중인 로컬 배치에서 제외했다.
- 추후 key3를 다시 사용할 때는 단건 smoke test로 `getHospBasisList`, `getNonPaymentItemCodeList2`, `getNonPaymentItemHospDtlList`를 각각 검증한다.

## 3. Hospital 배치가 서울 900건에서 끝난 문제

### 증상

서울 병원 producer가 900건만 생산하고 완료 처리됐다. 실제 서울 병원 수와 맞지 않았다.

### 원인

중간 페이지가 비거나 일시 오류가 발생했을 때 전체 시도 수집을 끝난 것으로 판단했다. 심평원 API는 페이지 단위로 일시적인 빈 응답/오류가 발생할 수 있다.

### 조치

- 첫 페이지가 비면 해당 시도 데이터 없음으로 판단하고 break한다.
- 중간 페이지가 비면 누락으로 보고 다음 페이지를 계속 시도한다.
- 페이지별 재시도와 실패 격리를 유지한다.

### 검증

2026-05-17 로컬 배치에서 `Hospital` 79,674건 적재 완료.

## 4. SidoStat 지역 약어 불일치

### 증상

`NonPayItemSidoStat`에 일부 지역이 들어오지 않았다. `sido_key`가 기대한 값과 달라 매핑되지 않았다.

### 원인

`getNonPaymentItemSidoCdList` 응답의 지역 약어가 기존 추정과 달랐다.

| 지역 | 잘못 추정한 key | 실제 확인 key |
|---|---|---|
| 광주 | `Kj` | `Kw` |
| 울산 | `Us` | `Usn` |
| 경북 | `Ks` | `Ksb` |
| 경남 | `Kn` | `Ksn` |

### 조치

- `SidoCode`와 `NonPaySidoStatItem` 필드 매핑을 실제 응답 기준으로 수정했다.
- `NonPayItemSidoStat`를 truncate 후 재실행했다.

### 검증

2026-05-17 로컬 DB 기준 `NonPayItemSidoStat` 6,988건 적재.

확인된 key: `All`, `Sl`, `Tg`, `Ich`, `Kw`, `Dj`, `Usn`, `Sj`, `Kyg`, `Kaw`, `Ccn`, `Ksb`, `Ksn`.

아직 API/docx 기준으로 확정하지 못한 key: 부산(`Bs`), 충북(`Cb`), 전북(`Jb`), 전남(`Jn`), 제주(`Jj`).

## 5. Price API quota 초과

### 증상

가격 상세 배치 진행 중 `429 Too Many Requests: API token quota exceeded`가 발생했다.

### 영향

- `Price`와 `PriceSummary`는 중간까지 DB에 저장됐다.
- 배치가 중단돼 전체 데이터 완전성은 보장되지 않는다.
- Hospital, NonPayItem, NonPayItemDesc, ClcdStat, SidoStat 적재분은 별도로 완료된 상태다.

### 로컬 DB 스냅샷

| 테이블 | 건수 | 상태 |
|---|---:|---|
| `Hospital` | 79,674 | 완료 |
| `NonPayItem` | 875 | 완료 |
| `NonPayItemDesc` | 54 | 완료 |
| `NonPayItemClcdStat` | 2,459 | 완료 |
| `NonPayItemSidoStat` | 6,988 | 완료 |
| `Price` | 79,428 | 부분 적재 |
| `PriceSummary` | 122,097 | 부분 적재 |

### 다음 조치

- 심평원 운영계정 신청 또는 quota 회복 후 재시도한다.
- 재시도 전 truncate 전체 재적재와 PK upsert 이어받기 중 하나를 결정한다.
- 이어받기를 선택하면 ykiho/endpoint별 성공 상태를 기록하는 checkpoint 테이블이 필요하다.

## 6. Price만 Hospital에 의존하는 이유

`PriceSyncService`는 `getNonPaymentItemHospDtlList`를 호출한다. 이 API는 병원 식별자 `ykiho`가 있어야 호출할 수 있다.

따라서 Price는 NonPayItem, Desc, Summary, ClcdStat, SidoStat처럼 독립적으로 전체 페이지를 순회할 수 없다. 먼저 `Hospital`에서 `ykiho` 목록을 확보해야 한다.

현재 구현은 Hospital 완료 후 Price를 시작한다. 개선안은 Hospital producer가 `ykiho`를 찾는 즉시 queue에 넣고, Price worker가 그 queue를 병렬 소비하는 방식이다.

## 7. 배치 병렬화 상태

현재 전체 배치는 `BatchService.syncAll()`에서 7개 sync 작업을 `hiraBatchExecutor`로 동시에 dispatch한다.

예외: Price는 Hospital의 `CompletableFuture`에 chained 되어 있다. 이유는 6번과 같다.

각 sync 내부도 page/ykiho 단위 writer 트랜잭션으로 분리되어 긴 외부 API 호출이 하나의 대형 트랜잭션을 물고 있지 않게 했다.
