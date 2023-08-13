FROM maven:3.9.3-amazoncorretto-20 AS build
WORKDIR /opt/htmx
COPY .idea .idea
COPY pom.xml .
RUN mvn dependency:go-offline

RUN ls ~/.m2
COPY src src
RUN mvn package

FROM amazoncorretto:20-alpine3.18
ENV PORT 8080
EXPOSE $PORT

COPY --from=build /opt/htmx/target/htmx-1.0-SNAPSHOT.jar htmx.jar
CMD ["java", "-jar", "htmx.jar"]
