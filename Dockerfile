# -------- Build Stage --------
FROM maven:3.9.6-eclipse-temurin-17 AS builder

WORKDIR /app

# Copy pom first (better layer caching)
COPY pom.xml .
RUN mvn dependency:go-offline -B

# Copy source and build
COPY src ./src
RUN mvn clean package -DskipTests


# -------- Runtime Stage --------
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# JVM container tuning
ENV JAVA_OPTS="-XX:+UseG1GC \
-XX:MaxRAMPercentage=75 \
-XX:InitialRAMPercentage=25 \
-XX:+UseStringDeduplication \
-Djava.security.egd=file:/dev/urandom"

# Copy jar from build stage
COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

ENTRYPOINT ["sh","-c","java $JAVA_OPTS -jar app.jar"]
