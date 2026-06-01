FROM eclipse-temurin:11-jdk-alpine AS builder
WORKDIR /workspace
COPY pom.xml .
COPY src src
RUN apk add --no-cache maven && mvn -B -q clean package -DskipTests

FROM eclipse-temurin:11-jre-alpine
RUN addgroup -S cleardocs && adduser -S cleardocs -G cleardocs
WORKDIR /app
COPY --from=builder /workspace/target/cleardocs-1.0.0.jar app.jar
RUN mkdir -p /tmp/cleardocs/documents && chown -R cleardocs:cleardocs /tmp/cleardocs /app
USER cleardocs
EXPOSE 8080
ENTRYPOINT ["java", \
  "-XX:+UseContainerSupport", \
  "-XX:MaxRAMPercentage=75.0", \
  "-Djava.security.egd=file:/dev/./urandom", \
  "-jar", "app.jar"]
