FROM openjdk:26-jdk-slim
LABEL authors="David Tooke"
EXPOSE 8080
VOLUME /tmp
ARG JAR_FILE=build/libs/*.jar
COPY ${JAR_FILE} word-verifier.jar
ENTRYPOINT ["java","-jar","/word-verifier.jar"]