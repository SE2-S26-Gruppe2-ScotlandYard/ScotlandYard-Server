# Scotland Yard Server

Backend server for the Scotland Yard board game, developed as part of the university course
[621.252] Software Engineering 2 - Group 2.
The project is based on the provided
[WebSocketDemo-Server](https://github.com/AAU-SE2/WebSocketDemo-Server) template.

---

## Table of Contents

1. [Project Overview](#1-project-overview)
2. [Technology Stack](#2-technology-stack)
3. [Prerequisites](#3-prerequisites)
4. [Build and Run Locally](#4-build-and-run-locally)
5. [Running with Docker Compose](#5-running-with-docker-compose)
6. [API Security Considerations](#6-api-security-considerations)
7. [Error Handling Considerations](#7-error-handling-considerations)
8. [CI/CD and Quality Assurance](#8-cicd-and-quality-assurance)
9. [Further Documentation](#9-further-documentation)

---

## 1. Project Overview

ScotlandYard-Server is the backend component of a digital implementation of the Scotland Yard
board game. It manages game state, player sessions, and real-time communication between connected
clients via WebSockets.

The server exposes a STOMP-over-WebSocket interface that client applications (such as the
Android client) connect to. Game events are propagated to connected clients through the
message broker.

The current project version is **v1.0.0**.

---

## 2. Technology Stack

| Component           | Technology / Library                          |
|---------------------|-----------------------------------------------|
| Language            | Java 21                                       |
| Secondary Language  | Kotlin 2.3 (compiled alongside Java sources)  |
| Framework           | Spring Boot 4.0.3                             |
| Real-time Messaging | Spring WebSocket (STOMP)                      |
| Monitoring          | Spring Boot Actuator                          |
| JSON Serialization  | Jackson Databind                              |
| Boilerplate         | Lombok 1.18                                   |
| Build Tool          | Maven (wrapper included)                      |
| Testing             | JUnit Jupiter, Mockito                        |
| Coverage            | JaCoCo 0.8.14                                 |
| Static Analysis     | SonarCloud                                    |
| Containerization    | Docker (multi-stage build), Docker Compose    |

---

## 3. Prerequisites

The following tools are needed before building or running the project locally:

- **Java 21** (e.g. Eclipse Temurin 21)
- **Maven 3.9+** (a Maven wrapper script `mvnw` is provided in the repository)
- **Docker** and **Docker Compose** (required only for the containerized setup)

No external database is configured by default. Any additional runtime dependencies should be
confirmed in `application.properties` before deployment.

---

## 4. Build and Run Locally

### Clone the repository

```bash
git clone https://github.com/SE2-S26-Gruppe2-ScotlandYard/ScotlandYard-Server.git
cd ScotlandYard-Server
```

### Build the project

```bash
./mvnw clean package
```

This compiles the source code, runs all unit tests, and produces a runnable JAR in the `target/`
directory.

To skip tests during the build:

```bash
./mvnw clean package -DskipTests
```

### Run the application

```bash
java -jar target/<generated-jar-name>.jar
```

Replace `<generated-jar-name>` with the actual JAR filename produced in `target/` after the
build. The server starts on **port 8080** by default.

### Run only the tests

```bash
./mvnw test
```

After the test run, a JaCoCo HTML coverage report is available at:

```
target/site/jacoco/index.html
```

---

## 5. Running with Docker Compose

The repository includes a `docker-compose.yml` that pulls the pre-built image from the GitHub
Container Registry and starts the server.

```bash
docker compose up -d
```

The server will be reachable at `http://localhost:53206` (mapped from container port 8080).

To stop the server:

```bash
docker compose down
```

### Building the Docker image locally

The `Dockerfile` uses a multi-stage build:

- **Stage 1** (`maven:3.9-eclipse-temurin-21`): compiles the project and packages the JAR.
- **Stage 2** (`eclipse-temurin:21`): creates a minimal runtime image exposing port 8080.

To build and run the image manually:

```bash
docker build -t scotlandyard-server .
docker run -p 8080:8080 scotlandyard-server
```

---

## 6. API Security Considerations

This project is developed in a university context. The following security aspects should be
addressed and verified as the project matures:

- **WebSocket access control**: It should be confirmed whether the WebSocket endpoint restricts
  access to authorised clients only, for example through session validation or a handshake
  interceptor.
- **Actuator exposure**: Spring Boot Actuator is included. The set of exposed endpoints should
  be reviewed and restricted as appropriate for the deployment environment.
- **Input validation**: Incoming STOMP message payloads should be validated to guard against
  malformed or unexpected data.

Any security measures that have been implemented should be documented here as the project
develops.

---

## 7. Error Handling Considerations

The following error handling aspects are relevant for this type of WebSocket-based backend and
should be considered during development and review:

- **Client disconnects**: The server should handle unexpected client disconnections gracefully
  without affecting ongoing game sessions for other players.
- **Invalid payloads**: Messages that do not conform to the expected format should be rejected
  or ignored in a controlled manner.
- **Error responses**: Where errors are communicated back to clients, the information exposed
  should be appropriate for the deployment context and avoid leaking internal details.

---

## 8. CI/CD and Quality Assurance

GitHub Actions runs automatically on every push to `main` and on all pull requests targeting
`main`. The pipeline performs the following steps:

1. **Build** - compiles the project using Maven.
2. **Unit Tests and Coverage Report** - runs all JUnit tests and generates a JaCoCo XML report.
3. **SonarCloud Scan** - uploads the coverage report and performs static code analysis.
   Results are published to the SonarCloud project
   `SE2-S26-Gruppe2-ScotlandYard_ScotlandYard-Server`.

### Branch Workflow

- Branch naming: `<type>/<short-description>` (e.g. `feature/player-movement`)
- Commit convention: `[#IssueNumber] <type>: <description>` (issue number is omitted if not applicable)
- All changes to `main` must go through a **Pull Request**. Direct pushes are not permitted.
- Squash and rebase merges are disabled; only merge commits are used.
- The `main` branch is protected and must remain in a buildable and working state at all times.

---

## 9. Further Documentation

- Deployment procedures and release management are documented in
  [`docs/deployment-and-release.md`](docs/deployment-and-release.md).
- The upstream WebSocket demo project used as a starting point can be found at
  [AAU-SE2/WebSocketDemo-Server](https://github.com/AAU-SE2/WebSocketDemo-Server).
