# Build stage
FROM maven:3.9.9-eclipse-temurin-17-focal AS build
WORKDIR /app
COPY . .
# Build the web portal module specifically
RUN mvn clean package -f tukac-web/pom.xml -DskipTests

# Run stage
FROM eclipse-temurin:17-jre-alpine
WORKDIR /app

# Create a subdirectory for the app so that the relative database path "../tukac.db" 
# (configured in application.properties) points to the /app directory.
RUN mkdir bin
COPY --from=build /app/tukac-web/target/tukac-web-1.0.0.jar bin/app.jar

# Copy the initial database file to the /app directory
COPY tukac.db .

# Expose the port
EXPOSE 8080

# Run the application from the bin directory
WORKDIR /app/bin
CMD ["java", "-jar", "app.jar", "--server.port=${PORT:-8080}"]
