# CI/CD — GitHub 커밋 → 자동 배포

```
git push (main) ──▶ GitHub Actions (클라우드에서 이미지 빌드)
                          │  push
                          ▼
                 GHCR: ghcr.io/<owner>/linkly:latest
                          │  자동 pull + 재시작
                          ▼
                 NAS 의 Watchtower (180초마다 폴링)
```

빌드는 **GitHub 클라우드**에서 하므로 2GB NAS에 부담이 없고, NAS는 새 이미지를 **pull만** 한다.
인바운드 포트를 열 필요도 없다(Watchtower가 바깥으로 폴링).

## 1. (repo) 워크플로우
[.github/workflows/deploy.yml](../.github/workflows/deploy.yml) 가 main push 마다:
1. 루트 `Dockerfile` 로 이미지 빌드 (프론트+백엔드 단일 컨테이너)
2. `ghcr.io/<owner>/linkly:latest` + `:sha-xxxx` 태그로 GHCR push
   - 별도 시크릿 불필요 — 기본 `GITHUB_TOKEN` 의 `packages: write` 권한 사용

## 2. (GitHub, 최초 1회) GHCR 패키지를 Public 으로
첫 Action 실행이 끝나면 패키지가 생긴다. 기본은 private 이라 NAS가 익명 pull 하려면 공개로 바꾼다.
- GitHub > 프로필 > **Packages** > `linkly` > **Package settings** > **Change visibility** > **Public**
- (원하면 같은 화면에서 repo 와 연결 "Connect repository" 도)
> private 로 두려면 NAS Watchtower/도커에 GHCR 로그인(PAT)이 필요하다.

## 3. (NAS) Watchtower 로 자동 배포 전환
기존 tar 반입 방식 대신 [docker-compose.ghcr.yml](../docker-compose.ghcr.yml) 을 쓴다.
1. 그 파일 내용을 NAS `/volume1/docker/linkly/docker-compose.yml` 로 교체
   - `image:` 의 owner 를 **소문자**로 맞춘다 (예: `ghcr.io/hapycodding/linkly:latest`)
2. `.env` 는 그대로 (DB_PASSWORD 등)
3. Container Manager > 프로젝트 > 재생성 → `api` + `watchtower` 두 컨테이너가 뜬다
4. 이후 `git push` 하면: Actions 빌드 → GHCR 갱신 → 3분 내 Watchtower가 자동 pull+재시작

## 확인
- Actions 탭에서 워크플로우 초록불
- GHCR 패키지에 `latest` 갱신 시각 확인
- NAS: `docker logs linkly-watchtower` 에 "Found new ... image" → 재시작 로그

## 롤백
GHCR 에 `:sha-xxxx` 태그가 커밋마다 남으므로, compose 의 `image:` 를 특정 sha 태그로 바꾸면 그 버전으로 고정/롤백할 수 있다.
