# Deployment and Release Management

Backend component: **ScotlandYard-Server**
Course: [621.252] Software Engineering 2 - Group 2
Sprint: 3

---

## Table of Contents

1. [Purpose](#1-purpose)
2. [Scope](#2-scope)
3. [Deployment Overview](#3-deployment-overview)
4. [Local Deployment](#4-local-deployment)
5. [Docker Compose Deployment](#5-docker-compose-deployment)
6. [Port Configuration](#6-port-configuration)
7. [Resource Limits](#7-resource-limits)
8. [Release Management Overview](#8-release-management-overview)
9. [Branching and Pull Request Workflow](#9-branching-and-pull-request-workflow)
10. [Quality Gates Before Release](#10-quality-gates-before-release)
11. [Release Process](#11-release-process)
12. [Rollback and Recovery Considerations](#12-rollback-and-recovery-considerations)
13. [Relation to Other Sprint 3 Requirements](#13-relation-to-other-sprint-3-requirements)
14. [Open Points and Assumptions](#14-open-points-and-assumptions)

---

## 1. Purpose

This document describes the deployment procedures and release management practices for the
ScotlandYard-Server backend, written as part of the Sprint 3 documentation requirements for
[621.252] Software Engineering 2.

---

## 2. Scope

This document covers local and Docker-based deployment, the CI/CD pipeline, branch and release
workflow, and quality gates. It does not cover Android client deployment or infrastructure
outside this repository.

---

## 3. Deployment Overview

ScotlandYard-Server is a Spring Boot application that exposes a STOMP-over-WebSocket interface.
Client applications connect to this interface to participate in game sessions.

The server supports two deployment modes:

- **Local** — built and run directly from source using Maven and Java 21. Used during
  development and for automated testing.
- **Docker Compose** — uses a pre-built image from the GitHub Container Registry (`ghcr.io`).
  Intended for integration testing against the Android client or shared use within the team.

The Android client connects via a URI that must be configured on the client side. Supported
connection contexts include a local emulator URI, a physical device URI (local network), and
a global URI if a shared deployment is available.

---

## 4. Local Deployment

Local deployment uses the Maven wrapper included in the repository.

**Build and test:**

```bash
./mvnw clean package
```

Compiles all sources, runs the unit test suite, and produces a runnable JAR in `target/`.

**Start the server:**

```bash
java -jar target/<generated-jar-name>.jar
```

The JAR filename is determined by the `artifactId` and `version` in `pom.xml`. The server
starts on **port 8080** by default (`http://localhost:8080`).

**Skip tests** (development only, not recommended before merging):

```bash
./mvnw clean package -DskipTests
```

**Run tests only:**

```bash
./mvnw test
```

A JaCoCo HTML coverage report is written to `target/site/jacoco/index.html` after the test run.

---

## 5. Docker Compose Deployment

The `docker-compose.yml` defines a single service (`server`) and pulls the pre-built image from
the GitHub Container Registry.

**Start:**

```bash
docker compose up -d
```

The server is reachable at `http://localhost:53206`.

**Stop:**

```bash
docker compose down
```

**Key compose behaviour:**

- `pull_policy: always` — Docker checks for an updated image on every `docker compose up`.
- `restart: unless-stopped` — the container restarts automatically after unexpected exits.

**Build image locally** (if custom changes are needed):

```bash
docker build -t scotlandyard-server .
docker run -p 8080:8080 scotlandyard-server
```

The `Dockerfile` uses a multi-stage build: Stage 1 (`maven:3.9-eclipse-temurin-21`) builds the
JAR; Stage 2 (`eclipse-temurin:21`) produces a minimal runtime image.

---

## 6. Port Configuration

| Context              | Port  |
|----------------------|-------|
| Internal (container) | 8080  |
| External (host)      | 53206 |

The application listens on port 8080 inside the container. Docker Compose maps this to port
53206 on the host. Clients using Docker Compose must connect on port 53206; local (non-Docker)
deployments use port 8080 directly.

Any change to the external port in `docker-compose.yml` must be reflected in the Android client
configuration.

---

## 7. Resource Limits

The `docker-compose.yml` configures the following resource constraints on the `server` service:

| Setting             | Value     |
|---------------------|-----------|
| CPU limit           | 1 core    |
| Memory limit        | 1 GB      |
| CPU reservation     | 0.5 cores |
| Memory reservation  | 512 MB    |

Limits define the maximum resources the container may use before being terminated by the runtime.
Reservations indicate the minimum that should be available on the host. These values should be
reviewed if the server's resource usage changes significantly during development.

---

## 8. Release Management Overview

Release management in this project controls which code enters `main` and what constitutes a
versioned release. The primary goals are:

- Keeping `main` stable and buildable at all times.
- Ensuring every change has been reviewed, built, and tested before integration.
- Producing identifiable release artefacts (Git tags, Docker images) tied to reviewed states
  of the codebase.

Release management is intentionally lightweight given the university project context.

---

## 9. Branching and Pull Request Workflow

**Branch naming:**

```
<type>/<short-description>
```

Example: `feature/player-movement`, `fix/websocket-disconnect`

**Commit convention:**

```
[#IssueNumber] <type>: <description>
```

The issue number is omitted when there is no associated issue.

**Pull Requests:**

All changes to `main` must go through a Pull Request. Direct pushes are not permitted. Each PR
must pass the CI pipeline before it can be merged and is expected to be reviewed by at least one
other team member.

**Merge strategy:** Only merge commits are used. Squash and rebase merges are disabled.

---

## 10. Quality Gates Before Release

The following automated checks run on every push to `main` and on every pull request targeting
`main`:

1. **Build** — the project compiles successfully using Maven.
2. **Unit tests** — all JUnit tests pass.
3. **Coverage report** — JaCoCo generates a coverage report. Thresholds must be met if
   configured.
4. **SonarCloud analysis** — results are published to
   `SE2-S26-Gruppe2-ScotlandYard_ScotlandYard-Server`. Any configured blocking quality gate
   must pass.

---

## 11. Release Process

A typical release follows these steps:

1. Development work is completed on a feature or fix branch.
2. A Pull Request is opened targeting `main`.
3. GitHub Actions runs the build, test, and analysis pipeline.
4. The PR is reviewed and approved by at least one team member.
5. The branch is merged into `main` using a merge commit.
6. A Git tag (e.g. `v1.0.0`) is applied to the relevant commit on `main` to mark the release.

Whether the CI pipeline automatically builds and publishes a Docker image to `ghcr.io` on merge
has not been independently confirmed in this document and should be verified against the actual
GitHub Actions workflow files. If image publication is automated, any deployment using
`docker compose up` will pick up the updated image automatically due to `pull_policy: always`.

---

## 12. Rollback and Recovery Considerations

If a defective change is merged, the following options are available:

- **Revert the merge commit** via a new commit and a follow-up Pull Request, restoring `main`
  without rewriting history.
- **Re-deploy a previous image** by temporarily updating `docker-compose.yml` to reference a
  specific versioned tag instead of `latest`, then running `docker compose up -d`.
- **Confirm stability** by running the full test suite against the restored state before
  considering the rollback complete.

Rollback procedures are expected to be executed manually. No automated rollback mechanism is
assumed to be in place.

---

## 13. Relation to Other Sprint 3 Requirements

This document is one part of the Sprint 3 documentation set:

- **API Security Considerations** (`readme.md`, Section 6): security-relevant configuration
  should be reviewed before any shared or hosted deployment.
- **Error Handling Considerations** (`readme.md`, Section 7): stable error handling is a
  prerequisite for a reliable release and should be confirmed during pre-release testing.
- **Resource Limits** (Section 7 above): the configured constraints are directly relevant to
  deployment planning and should be kept up to date.

---

## 14. Open Points and Assumptions

| # | Topic | Note |
|---|-------|------|
| 1 | **CI image publication** | It is assumed that the GitHub Actions workflow publishes a Docker image to `ghcr.io` on merge to `main`. This should be verified against the workflow YAML files. |
| 2 | **Versioned image tags** | It is assumed that versioned Docker tags are applied manually or via the release workflow. The exact mechanism should be confirmed. |
| 3 | **SonarCloud quality gate rules** | Specific thresholds (coverage percentage, allowed issues) are not documented here and should be added if known. |
| 4 | **JaCoCo coverage thresholds** | Whether a minimum threshold is enforced in Maven or CI configuration has not been confirmed. |
| 5 | **Hosted deployment** | No shared hosting environment is documented. If one is used for integration testing, connection details should be recorded here. |
| 6 | **PR reviewer requirements** | At least one reviewer is assumed based on team agreement. This should be confirmed against the GitHub branch protection settings. |
