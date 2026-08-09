FROM eclipse-temurin:21-jre-jammy
VOLUME /tmp
COPY target/aggregator-service-*.jar app.jar
ENTRYPOINT ["java", "-jar", "/app.jar"]