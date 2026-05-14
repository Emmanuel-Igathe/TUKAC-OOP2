# TUKAC Portal - Deployment Script
# This script builds the project and runs the Spring Boot application.

Write-Host "--- TUKAC Portal Deployment Wizard ---" -ForegroundColor Cyan

# 1. Build the project
Write-Host "[1/2] Building the JAR file using Maven..." -ForegroundColor Yellow
mvn clean package -DskipTests

if ($LASTEXITCODE -ne 0) {
    Write-Host "Error: Maven build failed." -ForegroundColor Red
    exit $LASTEXITCODE
}

# 2. Run the JAR
$jarFile = Get-ChildItem -Path "tukac-web\target\*.jar" | Select-Object -First 1
if ($null -eq $jarFile) {
    Write-Host "Error: JAR file not found in target directory." -ForegroundColor Red
    exit 1
}

Write-Host "[2/2] Launching the application: $($jarFile.Name)" -ForegroundColor Yellow
Write-Host "The portal will be available at: http://localhost:8080" -ForegroundColor Green
java -jar $jarFile.FullName
