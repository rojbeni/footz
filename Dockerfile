FROM eclipse-temurin:17-jdk-focal
WORKDIR /workspace/app
COPY .mvn .mvn
COPY mvnw pom.xml ./
COPY footz-api footz-api 
COPY footz-core footz-core
COPY footz-dao footz-dao
COPY footz-db footz-db
RUN ./mvnw clean install

WORKDIR /workspace/app/footz-api/target
CMD ["java","-jar" ,"footz-api-0.0.1-SNAPSHOT.jar"]