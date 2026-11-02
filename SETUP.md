# Bakong Adaptor — Setup & Development Guide

This guide walks you through setting up, building, running, and testing the **Bakong Adaptor** service locally and in containerized environments.

---

## 1. Prerequisites

Ensure you have the following installed on your machine:

- **JDK**: Java 21+ or Java 25 (LTS)
- **Maven**: Apache Maven 3.9+
- **Docker**: Docker Desktop or Docker Engine *(optional, for container runs)*

---

## 2. Run Locally (Direct Maven)

You can run the application directly from source without Docker.

### Run Unit Tests
```bash
mvn clean test
```

### Start the Application
```bash
mvn spring-boot:run
```
> The service starts on port **8082** by default using the `dev` profile.

---

## 3. Docker Build & Run

### Build Docker Image
```bash
docker build -t nexus.sbibank.com/sbicore/bakong-adaptor:uat-0.1.0 .
```

### Run with Docker

#### Option A: Single Container (`docker run`)
```bash
docker run --rm -p 8082:8082 --name bakong-adaptor nexus.sbibank.com/sbicore/bakong-adaptor:uat-0.1.0
```

#### Option B: Docker Compose (`docker compose`)
Uses `docker-compose.yml` with health checks and environment configurations:

```bash
# Start in background
docker compose up -d --build

# View live application logs
docker compose logs -f

# Check health endpoint
curl http://localhost:8082/actuator/health

# Stop and remove container
docker compose down
```

---

## 4. SonarQube Code Quality Scan

Run a static code analysis scan before opening a Merge Request.

> **Security Note:** Never commit your SonarQube token to Git. Set it as an environment variable in your terminal session before running.

### Windows (PowerShell)
```powershell
$env:SONAR_TOKEN = "your_sonarqube_token_here"

# Dev scan
mvn clean verify sonar:sonar -P 'sonar-dev' -Dsonar.token=$env:SONAR_TOKEN

# UAT scan
mvn clean verify sonar:sonar -P 'sonar-uat' -Dsonar.token=$env:SONAR_TOKEN
```

### Linux / macOS / Git Bash
```bash
export SONAR_TOKEN="your_sonarqube_token_here"

# Dev scan
mvn clean verify sonar:sonar -P 'sonar-dev' -Dsonar.token=$SONAR_TOKEN

# UAT scan
mvn clean verify sonar:sonar -P 'sonar-uat' -Dsonar.token=$SONAR_TOKEN
```

---

## 5. Multi-Service / Server Deployment

Full server deployment with the **Traefik Gateway** is managed centrally in the `sbi-devops` repository.

To package and prepare this service for server deployment:

```powershell
# Navigate to the sibling DevOps directory
cd ..\sbi-devops

# Run the release packager
.\scripts\package-uat-image.ps1 -ProjectName "bakong-adaptor" -ImageTag "uat-0.1.0"
```
