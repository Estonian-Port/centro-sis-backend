# Etapa 1: Build con Gradle Wrapper
FROM gradle:8.14-jdk17-alpine AS builder

WORKDIR /home/app

# Copiar wrapper y configuración antes que el código fuente para aprovechar el caché
COPY gradlew .
COPY gradle ./gradle
COPY build.gradle.kts settings.gradle.kts gradle.properties* ./

RUN chmod +x ./gradlew

# Descargar dependencias en una capa cacheable
RUN ./gradlew dependencies --no-daemon || true

COPY src ./src

# Generar el JAR excluyendo tests
RUN ./gradlew bootJar -x test --no-daemon

# Etapa 2: Imagen liviana para correr la app
FROM eclipse-temurin:17-jre-alpine

RUN apk add --no-cache tzdata

ENV APP_HOME=/app \
    SPRING_PROFILES_ACTIVE=prod \
    TZ=America/Argentina/Buenos_Aires

WORKDIR $APP_HOME

# Usuario sin privilegios de root
RUN addgroup -S appgroup && adduser -S appuser -G appgroup

# Copiamos el jar generado en la etapa anterior
COPY --from=builder /home/app/build/libs/*.jar app.jar
RUN chown -R appuser:appgroup $APP_HOME

USER appuser

# Exponemos el puerto de la app
EXPOSE 8080

# Comando por defecto para ejecutar la app
ENTRYPOINT ["java", "-jar", "app.jar"]
