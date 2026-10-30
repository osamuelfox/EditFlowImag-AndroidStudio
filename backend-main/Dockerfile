FROM maven:3.9.6-eclipse-temurin-17 AS build
WORKDIR /app
COPY backend-main/pom.xml .
# Try to download dependencies to cache them
RUN mvn dependency:go-offline -B
COPY backend-main/src ./src
RUN mvn clean package -DskipTests -B

FROM eclipse-temurin:17-jre-jammy
WORKDIR /app
COPY --from=build /app/target/*.jar app.jar

# Create uploads directory
# Note: This folder must be mounted as a volume in docker-compose.yml
RUN mkdir uploads

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
