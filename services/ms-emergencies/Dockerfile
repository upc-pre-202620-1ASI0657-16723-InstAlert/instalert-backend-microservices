FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /build
COPY services/ms-emergencies/pom.xml pom.xml
COPY services/ms-emergencies/src src
RUN --mount=type=cache,target=/root/.m2 mvn -B -DskipTests package

FROM eclipse-temurin:21-jre-jammy
RUN apt-get update && apt-get install -y --no-install-recommends curl && rm -rf /var/lib/apt/lists/*
WORKDIR /app
COPY --from=build /build/target/ms-emergencies-*.jar app.jar
USER 10001:10001
EXPOSE 8083
# A protected endpoint must answer 401: verifies HTTP readiness without bypassing auth.
HEALTHCHECK --interval=10s --timeout=5s --start-period=45s --retries=12 \
CMD test "$(curl -s -o /dev/null -w '%{http_code}' http://127.0.0.1:8083/api/v1/emergencies)" = 401
ENTRYPOINT ["java", "-jar", "/app/app.jar"]
