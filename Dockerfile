# -------- Build Stage (Keep this as is) --------
FROM maven:3.9.6-eclipse-temurin-17 AS builder
WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline -B
COPY src ./src
RUN mvn clean package -DskipTests

# -------- Runtime Stage (Updated) --------
FROM eclipse-temurin:17-jre-jammy

WORKDIR /app

# JVM optimizations (Removed the network flag from here to be safe)
ENV JAVA_OPTS="\
-XX:+UseG1GC \
-XX:MaxRAMPercentage=55 \
-XX:InitialRAMPercentage=20 \
-XX:MaxMetaspaceSize=256m \
-XX:+UseStringDeduplication \
-XX:ActiveProcessorCount=2 \
-Djava.security.egd=file:/dev/urandom"

# Copy jar
COPY --from=builder /app/target/*.jar app.jar

EXPOSE 8080

# 👇 THE FIX: Hardcoding the IPv4 flag so it can't be missed
ENTRYPOINT ["java", "-Djava.net.preferIPv4Stack=true", "-jar", "app.jar"]
