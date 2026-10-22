# The goal is one image that builds React, jar with React in it, and runs on slim Java runtime.
# We'll be working with a 512 MB memory limit from Render's free instance.
# It's built this way so the final image contains only the jre & jar making it smaller in size MB.

# Step 1: React/frontend
FROM node:22-alpine AS frontend
WORKDIR /app/frontend
COPY frontend/package.json frontend/package-lock.json ./
RUN npm ci
COPY frontend/ ./
RUN npm run build

# Step 2: Spring Boot jar w/ frontend
FROM eclipse-temurin:21-jdk AS backend
WORKDIR /app
COPY .mvn/ .mvn/
COPY mvnw pom.xml ./
RUN chmod +x mvnw && ./mvnw -B -q dependency:go-offline
COPY src/ src/
COPY --from=frontend /app/frontend/dist/ src/main/resources/static/
RUN ./mvnw -B -q -DskipTests package

# Step 3: runtime
FROM eclipse-temurin:21-jre
WORKDIR /app
RUN useradd --system --uid 1001 opslog # runs as a user for security measure.
COPY --from=backend /app/target/*.jar app.jar
USER opslog
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70 -XX:+UseSerialGC -XX:TieredStopAtLevel=1"
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]