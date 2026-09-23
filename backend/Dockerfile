FROM maven:3.9-eclipse-temurin-17 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B -ntp dependency:go-offline
COPY src ./src
RUN mvn -B -ntp -DskipTests package
FROM eclipse-temurin:17-jre
WORKDIR /app
RUN useradd --system --uid 10001 school
COPY --from=build /app/target/school-management-api-0.1.0-SNAPSHOT.jar app.jar
USER school
EXPOSE 8080
ENTRYPOINT ["java","-jar","app.jar"]
