# ================================
# Etapa 1: Build con Gradle Wrapper
# ================================
FROM gradle:8.14-jdk17-alpine AS builder

WORKDIR /home/app

# Copiamos archivos de configuración y wrapper
COPY gradlew .
COPY gradle ./gradle
COPY build.gradle.kts settings.gradle.kts ./
COPY src ./src

# Damos permisos al wrapper y generamos el .jar
RUN chmod +x ./gradlew
RUN ./gradlew bootJar --no-daemon

# ================================
# Etapa 2: Imagen liviana para correr la app
# ================================
FROM eclipse-temurin:17-jre-alpine

# Configuración de zona horaria para registros y schedulers
ENV APP_HOME=/app
ENV TZ=America/Argentina/Buenos_Aires

WORKDIR $APP_HOME

# Copiamos el jar generado en la etapa anterior
COPY --from=builder /home/app/build/libs/*.jar app.jar

# Exponemos el puerto de la app
EXPOSE 8080

# Comando por defecto para ejecutar la app
ENTRYPOINT ["java", "-jar", "app.jar"]
