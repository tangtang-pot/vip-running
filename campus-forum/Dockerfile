FROM maven:3.9.9-eclipse-temurin-21 AS build
WORKDIR /workspace
COPY pom.xml ./
COPY forum-service ./forum-service
COPY gateway ./gateway
COPY agent-service ./agent-service
RUN mvn -B -Pcloud,ai -DskipTests package
FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /workspace/forum-service/target/forum-service-1.0.0.jar /app/forum.jar
COPY --from=build /workspace/gateway/target/gateway-1.0.0.jar /app/gateway.jar
COPY --from=build /workspace/agent-service/target/agent-service-1.0.0.jar /app/agent.jar
USER 10001
ENV JAVA_TOOL_OPTIONS="-XX:MaxRAMPercentage=70"
ENTRYPOINT ["java","-jar"]
CMD ["/app/forum.jar"]
