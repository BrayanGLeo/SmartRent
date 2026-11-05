# Etapa de construcción
FROM eclipse-temurin:17-jdk-alpine AS build
WORKDIR /workspace/app

COPY mvnw .
COPY .mvn .mvn
COPY pom.xml .

# En entornos Windows a veces los scripts pierden los permisos o traen CRLF; cacheamos dependencias antes del código fuente
RUN sed -i 's/\r$//' mvnw && chmod +x mvnw && ./mvnw dependency:go-offline -B

COPY src src
RUN ./mvnw package -DskipTests -B

# Etapa de ejecución
FROM eclipse-temurin:17-jre-alpine
VOLUME /tmp
WORKDIR /app

RUN addgroup -S spring && adduser -S spring -G spring
COPY --from=build /workspace/app/target/*.jar app.jar
RUN chown -R spring:spring /app
USER spring:spring

ENV JAVA_TOOL_OPTIONS="-XX:+UseContainerSupport -XX:MaxRAMPercentage=75.0"
ENTRYPOINT ["java", "-jar", "app.jar"]
