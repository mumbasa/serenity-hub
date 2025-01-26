FROM eclipse-temurin:17-jdk-focal
 
WORKDIR /serenity-hub
 
COPY .mvn/ .mvn
COPY mvnw pom.xml ./
RUN ./mvnw dependency:go-offline
 
COPY src ./src
 
CMD ["./mvnw", "spring-boot:run -Dspring-boot.run.jvmArguments=\"-Xms2048m -Xmx4096m\""]