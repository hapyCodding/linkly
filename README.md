# 🔗 Linkly — URL 단축기 + 실시간 클릭 애널리틱스

긴 URL을 짧은 링크로 만들고, 누가·언제·어디서·무엇으로 눌렀는지를 **실시간 대시보드**로 보여주는 포트폴리오용 풀스택 토이 프로젝트. 백엔드는 Java 21 + Spring Boot(DTO는 `record`), 프론트는 React 19 + Vite.

![stack](https://img.shields.io/badge/Java%2021-Spring%20Boot%203.4-6DB33F) ![stack](https://img.shields.io/badge/React%2019-Vite-646CFF) ![stack](https://img.shields.io/badge/DB-MariaDB-003545)

## 무엇을 보여주나
| 축 | 구현 |
|---|---|
| 인코딩 | base62 7자리 단축 코드 + 충돌 재시도 (`ShortCodeGenerator`) |
| 성능 | 리다이렉트 핫패스에서 클릭 카운터 원자적 증가(`@Modifying` update) + 인덱스 |
| 실시간 | STOMP over WebSocket 으로 클릭 이벤트 라이브 푸시 → 차트가 즉시 반응 |
| 데이터 시각화 | Recharts 로 시간대별 추이(Area) · 국가별(Bar) · 디바이스(Pie) |
| 부가기능 | ZXing QR 코드 생성, 링크 만료(TTL), 링크 삭제 |
| 메타 수집 | 링크 생성 시 대상 페이지 og:title/&lt;title&gt; 자동 수집 (SSRF 가드: 내부/사설 IP 차단) |
| 배포 | 멀티스테이지 Docker + DSM 리버스 프록시 + DDNS (Synology NAS) |

## 아키텍처
```
[React 19 + Vite SPA]  ──REST──▶  [Spring Boot API]  ──JPA──▶  [MariaDB]
   대시보드/차트        ◀─STOMP/WS─   클릭 실시간 푸시
        │
   방문자 클릭 ─▶  GET /x/{code}  →  302 리다이렉트 + 클릭 이벤트 적재 + WS 브로드캐스트
```

## 프로젝트 구조
```
linkly/
├─ linkly-api/     # Java 21 + Spring Boot 백엔드
│  ├─ src/main/java/com/linkly/
│  │  ├─ domain/        Link, ClickEvent (JPA 엔티티)
│  │  ├─ repository/    JPA 리포지토리 + 집계 쿼리
│  │  ├─ service/       LinkService, ClickAnalyticsService, QrService …
│  │  ├─ web/           REST/리다이렉트 컨트롤러 + DTO(record) + 예외 핸들러
│  │  └─ config/        CORS, WebSocket(STOMP), 프로퍼티
│  ├─ Dockerfile / docker-compose.yml / .env.example
│  └─ src/main/resources/application*.yml
└─ linkly-web/     # React 19 + Vite + TS 프론트엔드
   └─ src/
      ├─ components/  CreateLinkForm, LinkList, Dashboard
      ├─ api.ts       REST 클라이언트
      └─ useClickStream.ts  STOMP 실시간 구독 훅
```

## 로컬 실행
### 1) 백엔드 (기본 h2, 포트 8080)
```bash
cd linkly-api
./gradlew bootRun
# 8080이 사용 중이면:
./gradlew bootRun --args='--server.port=8081 --linkly.base-url=http://localhost:8081'
```
- Swagger UI: `http://localhost:8080/swagger-ui.html`
- H2 콘솔: `http://localhost:8080/h2-console`

### 2) 프론트 (포트 5173)
```bash
cd linkly-web
npm install
# .env 의 VITE_API_BASE 가 백엔드 주소와 맞는지 확인 (기본 http://localhost:8081)
npm run dev
```

## 주요 API
| 메서드 | 경로 | 설명 |
|---|---|---|
| POST | `/api/links` | 링크 생성 `{ "url": "...", "expiresInDays": 30 }` |
| GET | `/api/links` | 링크 목록 |
| GET | `/api/links/{code}/stats` | 대시보드 통계(국가/디바이스/추이/최근 클릭) |
| GET | `/api/links/{code}/qr` | QR 코드 PNG |
| DELETE | `/api/links/{code}` | 링크 + 클릭 기록 삭제 (204) |
| GET | `/x/{code}` | 단축 링크 리다이렉트(302) + 클릭 적재 |
| WS | `/ws` → `/topic/clicks/{code}` | 클릭 실시간 구독 |

## 배포
NAS(Synology) + MariaDB + DDNS 공개 절차는 [docs/deploy-nas.md](docs/deploy-nas.md) 참고.

## 참고
- 국가는 `Accept-Language` 헤더 기반 간이 추정(데모 범위). 정밀 지오로케이션은 IP DB가 필요.
