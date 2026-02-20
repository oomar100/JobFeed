FROM eclipse-temurin:21-jdk-alpine

RUN apk add --no-cache maven bash

WORKDIR /app
COPY pom.xml .
RUN mvn dependency:go-offline
COPY . .
EXPOSE 8080
CMD ["mvn", "spring-boot:run"]