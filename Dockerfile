# ── Build stage ───────────────────────────────────────────────────────────────
FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app

COPY pom.xml .
RUN mvn -B -q dependency:go-offline

COPY src ./src
RUN mvn -B -q clean package -DskipTests -Djavac.executable=javac

# ── Runtime stage ─────────────────────────────────────────────────────────────
FROM eclipse-temurin:17-jre AS runtime
WORKDIR /app

RUN groupadd -r bfrost && useradd -r -g bfrost bfrost

COPY --from=build /app/target/*.jar app.jar

RUN mkdir -p /app/uploads && chown -R bfrost:bfrost /app
USER bfrost

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]