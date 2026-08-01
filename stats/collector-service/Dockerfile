FROM eclipse-temurin:21-jre-jammy
VOLUME /tmp
COPY target/collector-service-*.jar app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]