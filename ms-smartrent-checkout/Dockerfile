# Etapa de construcción
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /workspace/app

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .
COPY src src

# En entornos Windows a veces los scripts pierden los permisos o traen CRLF, usamos sh
RUN sed -i 's/\r$//' mvnw
RUN chmod +x mvnw
RUN ./mvnw install -DskipTests

# Etapa de ejecución
FROM eclipse-temurin:17-jre-alpine
VOLUME /tmp
WORKDIR /app

COPY --from=build /workspace/app/target/*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]
