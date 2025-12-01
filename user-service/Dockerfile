FROM eclipse-temurin:17-jdk
VOLUME /tmp
COPY user-service/target/users-service.jar app.jar
ENTRYPOINT ["java", "-jar", "app.jar"]
