# Универсальный Dockerfile для любого сервиса multi-module Maven проекта.
# Кладётся в КОРЕНЬ репозитория (рядом с корневым pom.xml).
#
# Пример использования в docker-compose.yml:
#
#   user-service:
#     build:
#       context: .                      # контекст - КОРЕНЬ репозитория (обязательно, чтобы Maven видел все модули)
#       dockerfile: Dockerfile
#       args:
#         MODULE: core/user-service               # путь к модулю относительно корня
#         ARTIFACT_NAME: user-service              # имя финального jar без версии/расширения (см. artifactId)

# ---------- Stage 1: сборка всего multi-module проекта ----------
FROM maven:3.9-eclipse-temurin-21 AS build
WORKDIR /workspace

# Кэшируем зависимости: сначала копируем только pom.xml всех модулей
COPY pom.xml .
COPY core/pom.xml core/pom.xml
COPY core/common/pom.xml core/common/pom.xml
COPY core/user-service/pom.xml core/user-service/pom.xml
COPY core/category-service/pom.xml core/category-service/pom.xml
COPY core/event-service/pom.xml core/event-service/pom.xml
COPY core/request-service/pom.xml core/request-service/pom.xml
COPY core/comment-service/pom.xml core/comment-service/pom.xml
COPY infra/pom.xml infra/pom.xml
COPY infra/config-server/pom.xml infra/config-server/pom.xml
COPY infra/discovery-server/pom.xml infra/discovery-server/pom.xml
COPY infra/gateway-server/pom.xml infra/gateway-server/pom.xml
COPY stats/pom.xml stats/pom.xml
COPY stats/stats-dto/pom.xml stats/stats-dto/pom.xml
COPY stats/stats-proto/pom.xml stats/stats-proto/pom.xml
COPY stats/stats-client/pom.xml stats/stats-client/pom.xml
COPY stats/stats-server/pom.xml stats/stats-server/pom.xml
COPY stats/collector-service/pom.xml stats/collector-service/pom.xml
COPY stats/aggregator-service/pom.xml stats/aggregator-service/pom.xml
COPY stats/analyzer-service/pom.xml stats/analyzer-service/pom.xml

# Скачиваем зависимости оффлайн (кэшируется отдельным слоем, пока pom.xml не меняются)
RUN mvn -B dependency:go-offline || true

# Теперь копируем весь исходный код
COPY . .

ARG MODULE
# Собираем нужный модуль + все модули, от которых он зависит (-am), пропуская тесты
RUN mvn -B -pl ${MODULE} -am clean package -DskipTests

# ---------- Stage 2: финальный лёгкий образ только с jar ----------
FROM eclipse-temurin:21-jre-alpine
RUN apk add --no-cache curl
WORKDIR /app

ARG MODULE
ARG ARTIFACT_NAME
COPY --from=build /workspace/${MODULE}/target/${ARTIFACT_NAME}-*.jar app.jar

ENTRYPOINT ["java", "-jar", "app.jar"]