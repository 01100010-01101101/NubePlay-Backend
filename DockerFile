# Imagen del backend NUBEPLAY para Render (o cualquier servicio con Docker).
# Etapa 1: compilar el .jar con Maven
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /app
COPY pom.xml .
RUN mvn -B -ntp dependency:go-offline
COPY src ./src
RUN mvn -B -ntp package -DskipTests


# Etapa 2: imagen liviana solo con Java para ejecutar
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/nubeplay-backend-1.0.0.jar app.jar
# El plan gratis de Render tiene 512 MB: Java usa como máximo el 75 % de esa memoria.
ENV JAVA_OPTS="-XX:MaxRAMPercentage=75 -XX:+UseSerialGC -Xss512k"
# Render entrega el puerto en la variable PORT (application.properties ya la lee).
EXPOSE 8080
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]



