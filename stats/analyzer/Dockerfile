FROM eclipse-temurin:21-jre-jammy
VOLUME /tmp
COPY target/analyzer-service-*.jar app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]