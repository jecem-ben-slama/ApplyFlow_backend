# --- STAGE 1: Build the application ---
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app

# Copy pom.xml and download dependencies (caches dependencies layer)
COPY pom.xml .
RUN mvn dependency:go-offline

# Copy the source code and build the application
COPY src ./src
RUN mvn clean package -DskipTests

# --- STAGE 2: Run the application ---
FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

# Create a non-root user to run the app (security best practice)
RUN addgroup -S spring && adduser -S spring -G spring
USER spring:spring

# Copy the built jar file from the build stage and rename it app.jar
COPY --from=build /app/target/*.jar app.jar

# Expose the port your Spring Boot app runs on (usually 8080)
EXPOSE 8080

# Let Docker/orchestrators know when the app is actually ready
# (requires spring-boot-starter-actuator on the classpath)
HEALTHCHECK --interval=30s --timeout=3s --start-period=40s \
  CMD wget -qO- http://localhost:8080/actuator/health || exit 1

# Run the jar file, capping heap to a safe % of container memory
ENTRYPOINT ["java", "-XX:MaxRAMPercentage=75.0", "-jar", "app.jar"]