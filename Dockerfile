# 단일 컨테이너: React SPA를 빌드해 Spring static에 굽고, API+WS까지 한 이미지에서 서빙.
# 빌드는 이미지 안에서 끝나므로 로컬 JDK/Node 없이도 docker build 가능.

# ---- 1) 프론트엔드 빌드 (React 19 + Vite) ----
FROM node:22-alpine AS web
WORKDIR /web
COPY linkly-web/package.json linkly-web/package-lock.json ./
RUN npm ci
COPY linkly-web/ ./
# same-origin 배포: .env.production 의 VITE_API_BASE="" 로 상대경로/현재 오리진 사용
RUN npm run build

# ---- 2) 백엔드 빌드 (Java 21 + Spring Boot) ----
FROM eclipse-temurin:21-jdk-jammy AS build
WORKDIR /app
COPY linkly-api/gradlew linkly-api/settings.gradle linkly-api/build.gradle ./
COPY linkly-api/gradle ./gradle
# Windows에서 커밋되면 실행 비트가 빠질 수 있어 컨테이너에서 보장
RUN chmod +x gradlew && ./gradlew --no-daemon dependencies >/dev/null 2>&1 || true
COPY linkly-api/src ./src
# 프론트 정적 산출물을 Spring static 으로 주입 → 단일 컨테이너 SPA 서빙
COPY --from=web /web/dist ./src/main/resources/static
RUN ./gradlew --no-daemon clean bootJar

# ---- 3) 런타임 ----
FROM eclipse-temurin:21-jre-jammy
WORKDIR /app
COPY --from=build /app/build/libs/linkly-api-0.2.0.jar app.jar
EXPOSE 8080
# DS220+ 메모리(2GB)에 맞춰 힙 제한
ENV JAVA_OPTS="-Xms128m -Xmx320m"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
