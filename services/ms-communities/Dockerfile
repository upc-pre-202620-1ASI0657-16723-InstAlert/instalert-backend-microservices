FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY services/ms-communities/pom.xml pom.xml
COPY services/ms-communities/src src
RUN --mount=type=cache,target=/root/.m2 mvn -B -DskipTests package

FROM eclipse-temurin:21-jre-jammy
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=build /build/target/ms-communities-*.jar app.jar
USER 10001:10001
EXPOSE 8086
# A protected endpoint must answer 401: verifies HTTP readiness without bypassing auth.
HEALTHCHECK --interval=10s --timeout=5s --start-period=45s --retries=12 \
CMD test "$(curl -s -o /dev/null -w '%{http_code}' http://127.0.0.1:8086/api/v1/communities)" = 401
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
