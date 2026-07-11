# NAS 배포 가이드 (Synology DS220+ · MariaDB · DDNS 공개)

**단일 컨테이너**로 SPA + API + WebSocket 을 한 번에 서빙하고, **포트 기반**으로 HTTPS 공개한다.
DB 포트(3307)는 절대 인터넷에 열지 않는다.

```
인터넷 ──HTTPS──▶ DSM 리버스 프록시(8444) ──▶ linkly 컨테이너(호스트 18081→8080) ──▶ MariaDB(내부 3307)
                                                  └ React SPA + REST API + STOMP WS 전부 한 컨테이너
```

## 0. DB 외부 포워딩 닫기 (먼저)
공유기/`제어판 > 외부 액세스 > 라우터 구성`에서 `3307` 포워딩 규칙이 있으면 삭제.
컨테이너는 LAN IP `192.168.35.2:3307` 로만 접속하므로 외부 노출이 필요 없다.

## 1. MariaDB에 전용 DB/계정 생성 (한 번만)
phpMyAdmin(root) 또는 SSH에서 [linkly-api/db/init.sql](../linkly-api/db/init.sql) 실행.
스크립트의 `CHANGE_ME` 를 쓸 비밀번호로 바꾼다.
```sql
CREATE DATABASE IF NOT EXISTS linkly CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER IF NOT EXISTS 'linkly'@'%' IDENTIFIED BY 'CHANGE_ME';
GRANT ALL PRIVILEGES ON linkly.* TO 'linkly'@'%';
FLUSH PRIVILEGES;
```
> 테이블은 첫 기동 시 JPA(`ddl-auto: update`)가 자동 생성한다.
> PC에서 포트 확인: `Test-NetConnection 192.168.35.2 -Port 3307`

## 2. 이미지 빌드 (PC에서 — 2GB NAS 부담 회피)
```powershell
# linkly 리포 루트에서
docker build -t linkly-api:0.2.0 .
docker save linkly-api:0.2.0 -o linkly-api.tar
```
`linkly-api.tar` 를 `\\192.168.35.2\docker\linkly\` 로 복사 →
**Container Manager > 이미지 > 가져오기** 로 등록.

## 3. Container Manager 프로젝트 배포
> ⚠️ **NAS에서 빌드하지 말 것.** 루트 `docker-compose.yml` 의 `build:` 블록을 그대로 두면
> Container Manager가 NAS에서 이미지를 빌드하려 하는데, 프로젝트 폴더에 `linkly-web/` 소스가
> 없으면 `COPY failed: ... linkly-web/package.json` 로 실패한다. 2단계에서 가져온 이미지를
> 쓰도록 **`build:` 없는 [docker-compose.nas.yml](../docker-compose.nas.yml)** 을 사용한다.

NAS `/volume1/docker/linkly/` 에 두 파일을 둔다:
- `docker-compose.yml` ← 리포의 **`docker-compose.nas.yml`** 내용(빌드 없이 `image:` 만 사용)
- `.env` (아래, `.env.example` 복사)

`.env`:
```
DB_PASSWORD=<1단계에서 정한 linkly 비밀번호>
LINKLY_BASE_URL=https://your-nas.synology.me:8444
LINKLY_CORS_ALLOWED_ORIGINS=https://your-nas.synology.me:8444
```
> 이미지를 이미 가져왔다면 compose의 `build:` 블록은 지워도 된다(그러면 `image:` 만 사용).
> NAS에서 직접 빌드하려면 리포 전체를 이 폴더에 두고 그대로 실행(느림).

**Container Manager > 프로젝트 > 생성** → 경로 `/volume1/docker/linkly` → 실행.
컨테이너가 호스트 포트 **18081** 로 노출된다.

**LAN 확인** (공개 전):
- 대시보드: `http://192.168.35.2:18081/`
- API: `http://192.168.35.2:18081/api/links`

## 4. 인증서 + 리버스 프록시 (DSM)
1. **제어판 > 보안 > 인증서**: `your-nas.synology.me` 인증서 확인(Synology DDNS 자동 인증서 또는 Let's Encrypt).
2. **제어판 > 로그인 포털 > 고급 > 리버스 프록시 > 생성**:
   - 소스: `HTTPS` / `your-nas.synology.me` / `8444`
   - 대상: `HTTP` / `localhost` / `18081`
   - **사용자 지정 헤더 > WebSocket** 추가 (Upgrade/Connection 전달) — 실시간 클릭 피드에 필수.
3. 공유기에서 `8444` 포워딩. 리버스 프록시에 위 인증서 지정.

**공개 확인**:
```
https://your-nas.synology.me:8444/            (대시보드)
https://your-nas.synology.me:8444/api/links   (API)
```
단축 링크는 `https://your-nas.synology.me:8444/x/{code}` 형태로 생성된다.

## 5. 공개 주의 (인증 없음)
이 데모 API는 인증이 없다 → 누구나 링크를 만들 수 있다. 둘 중 하나를 택한다.
- 데모로 공개하고 데이터를 주기적으로 리셋(가장 단순).
- 리버스 프록시에 basic auth (단, SPA도 같은 인증 뒤에 있어야 fetch가 자격증명 전달).

## 보안 체크리스트
- [ ] DB 포트 3307 은 공유기 포워딩에 **없음** (LAN 전용)
- [ ] 컨테이너는 `192.168.35.2:3307` (LAN IP)로만 DB 접속
- [ ] 공개 repo에 올릴 땐 DDNS 실주소를 `your-nas.synology.me` 로 치환
