FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /build
COPY . .
RUN mvn clean package -pl deployment -am -DskipTests -Dgpg.skip

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=builder /build/deployment/target/mapcode-rest-service.war .
CMD ["java", "-jar", "mapcode-rest-service.war"]
