FROM maven AS build

WORKDIR /app
COPY . /app
RUN mvn clean package
RUN cd /app/target

FROM eclipse-temurin:21-jre
WORKDIR /app
COPY --from=build /app/target/*.jar /app/app.jar

EXPOSE 8080
CMD ["java", "-jar", "/app/app.jar"]