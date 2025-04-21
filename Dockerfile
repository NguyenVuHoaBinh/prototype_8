FROM eclipse-temurin:17-jre-alpine

WORKDIR /app

# Add a volume pointing to /tmp for temporary files
VOLUME /tmp

# Make port 8080 available to the world outside this container
EXPOSE 8080

# Set the JAR file name as an argument
ARG JAR_FILE=target/*.jar

# Add the application's JAR to the container
COPY ${JAR_FILE} app.jar

# Run the jar file
ENTRYPOINT ["java","-Djava.security.egd=file:/dev/./urandom","-jar","/app/app.jar"]