ARG JAVA_BUILD_IMAGE=maven:3.9-eclipse-temurin-17
ARG JAVA_RUNTIME_IMAGE=eclipse-temurin:17-jre-alpine

FROM ${JAVA_BUILD_IMAGE} AS builder
WORKDIR /app

COPY pom.xml .
COPY src ./src

RUN --mount=type=cache,target=/root/.m2,sharing=locked \
    mvn clean package -DskipTests -q

FROM mwader/static-ffmpeg:7.1@sha256:a8090df5f5608daef387e1b2e93b98aaacb4d92153ad904e7d715c725724fca4 AS ffmpeg

FROM ${JAVA_RUNTIME_IMAGE}
WORKDIR /app

COPY --from=ffmpeg /ffmpeg /usr/local/bin/ffmpeg

COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8093

ENTRYPOINT ["java", "-jar", "app.jar"]
