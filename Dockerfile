# ================================
# Build Stage
# ================================
FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app

# Copy only pom first (cache dependencies)
COPY pom.xml .
RUN mvn dependency:go-offline

# Copy source code
COPY src ./src

# Build JAR
RUN mvn clean package -DskipTests

# ================================
# Run Stage
# ================================
FROM eclipse-temurin:17-jre
WORKDIR /app

# Copy jar from build stage
COPY --from=build /app/target/*.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]