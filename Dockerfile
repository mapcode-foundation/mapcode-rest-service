FROM maven:3.9-eclipse-temurin-17 AS builder
WORKDIR /build
COPY . .
RUN mvn clean package -pl deployment -am -DskipTests -Dgpg.skip

FROM eclipse-temurin:17-jre
WORKDIR /app
COPY --from=builder /build/deployment/target/mapcode-rest-service.war .

# Memory-footprint flags for small (≤ 1 GB) hosts.
# MaxRAMPercentage caps heap at ~70 % of container RAM, leaving room for native
# (mmap), metaspace, code cache and threads. SerialGC has the smallest native
# overhead on small heaps. ExitOnOutOfMemoryError lets the platform restart the
# container cleanly instead of leaving a wedged JVM.
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70 -XX:InitialRAMPercentage=30 -XX:+UseSerialGC -XX:MaxMetaspaceSize=128m -XX:ReservedCodeCacheSize=64m -Xss256k -XX:+ExitOnOutOfMemoryError"

CMD ["java", "-jar", "mapcode-rest-service.war"]
