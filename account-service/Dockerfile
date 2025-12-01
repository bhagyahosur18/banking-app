FROM eclipse-temurin:17-jdk-slim
WORKDIR /app
COPY target/account-service.jar app.jar
EXPOSE 8082
ENTRYPOINT ["java", "-jar", "app.jar"]
