FROM eclipse-temurin:17-jdk-slim
VOLUME /tmp
COPY target/user-service.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
