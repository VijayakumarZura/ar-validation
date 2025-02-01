# Use a Gradle image to build the application
FROM gradle:jdk11 AS build
WORKDIR /app

# Copy only necessary files to reduce build context size
COPY gradlew gradlew
COPY gradle gradle
COPY build.gradle settings.gradle ./
COPY src src

RUN chmod +x gradlew

RUN ./gradlew --version

# Clean and build the project, skipping tests to speed up the build process
RUN ./gradlew clean build -x test

# Debugging: List the contents of the build/libs directory
RUN echo "Contents of /app/build/libs:" && ls -l /app/build/libs


#RUN gradle build
#RUN ls -l /mail/build/libs

# Use the OpenJDK image to run the application
FROM openjdk:11-jre-slim

WORKDIR /app

COPY --from=build /app/build/libs/*.jar app.jar

EXPOSE 8084
# Debugging: List the contents of the /app directory in the runtime image
RUN echo "Contents of /app:" && ls -l /app

CMD ["java", "-jar", "app.jar"]
