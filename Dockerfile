FROM eclipse-temurin:21-jdk-alpine AS app-build
WORKDIR /workspace

COPY gradlew gradlew.bat build.gradle settings.gradle ./
COPY gradle ./gradle
RUN chmod +x gradlew

COPY src ./src

RUN ./gradlew --no-daemon clean bootJar -x test

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app

RUN apk add --no-cache graphviz fontconfig

COPY --from=app-build /workspace/build/libs/*.jar app.jar
COPY src/main/resources/data /app/data

RUN mkdir -p /app/generated/graphs

EXPOSE 8080

ENTRYPOINT ["java", "-jar", "app.jar"]
