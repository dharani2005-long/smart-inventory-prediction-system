# Step 1: Download a mini operating system that already has Java 17 installed
FROM eclipse-temurin:21-jdk-jammy

# Step 2: Create a workspace folder inside the cloud server named /app
WORKDIR /app

# Step 3: Copy your packaged JAR file from your local target folder into the cloud folder
COPY target/*.jar app.jar

# Step 4: Tell the cloud network to open port 8080 (the default port for Spring Boot Tomcat)
EXPOSE 8080

# Step 5: Tell the cloud server the exact terminal command to run your app
ENTRYPOINT ["java", "-jar", "app.jar"]
