# Spring Boot CI/CD Automation with Jenkins, Docker, and AWS EC2

A Java 21 Spring Boot application packaged as a Docker image and continuously built, tested, pushed to Docker Hub, and deployed to an AWS EC2 instance using a Jenkins Declarative Pipeline.

**Repository:** https://github.com/saiabbhiram/springboot-demo

## Project overview

This project demonstrates an end-to-end CI/CD workflow:

1. Jenkins checks out the application source from the `main` branch.
2. Maven builds the Spring Boot application.
3. Maven runs the available tests.
4. Docker builds a container image and pushes it to Docker Hub.
5. Jenkins connects to an Ubuntu EC2 instance over SSH.
6. EC2 pulls the newly published image and replaces the running application container.

## Technology stack

| Technology | Role |
|---|---|
| Java 21 | Application runtime and compilation |
| Spring Boot 3.2.0 | REST/web application framework |
| Maven | Dependency management, build, and tests |
| Jenkins | CI/CD orchestration |
| Docker | Container image build and runtime |
| Docker Hub | Container image registry |
| AWS EC2 (Ubuntu) | Deployment host |
| SSH | Secure remote deployment connection |

## Repository structure

```text
springboot-demo/
├── src/
│   └── main/
│       ├── java/                 # Spring Boot application source
│       └── resources/
│           └── application.properties
├── Dockerfile                    # Multi-stage Docker build
├── Jenkinsfile                   # Declarative CI/CD pipeline
└── pom.xml                       # Maven project configuration
```

## Application configuration

The application configuration in `src/main/resources/application.properties` sets:

```properties
server.port=8085
spring.application.name=spring-boot-basic-api
logging.level.com.example.basicapi=DEBUG
```

Spring Boot therefore listens on port **8085 inside the container**.

## Build and run locally

### Prerequisites

Install Java 21 and Maven, then confirm:

```bash
java -version
mvn -version
```

### Build

From the repository root:

```bash
git clone https://github.com/saiabbhiram/springboot-demo.git
cd springboot-demo
mvn clean package
```

The executable JAR is created under `target/`.

### Run without Docker

```bash
java -jar target/*.jar
```

The application is configured to listen on:

```text
http://localhost:8085
```

The root path may not be implemented by the application. Use an endpoint that exists in the source code when testing; an HTTP 404 from the root path can still confirm that the server is reachable.

## Docker

### Dockerfile design

The Dockerfile uses a two-stage build:

- **Build stage:** starts from `eclipse-temurin:21-jdk`, installs Maven 3.8.7, copies the Maven project, downloads dependencies, and packages the application.
- **Runtime stage:** starts from `eclipse-temurin:21-jre`, copies the packaged JAR, exposes container port 8085, and starts the JAR with Java.

This keeps the JDK and Maven build tooling out of the final runtime image.

### Build the image

```bash
docker build -t springboot-app:local .
```

### Run the container

The application listens on port 8085 inside the container. Publish host port 8085 to container port 8085:

```bash
docker run -d \
  --name springboot-app \
  --restart unless-stopped \
  -p 8085:8085 \
  springboot-app:local
```

### Verify

```bash
docker ps
docker logs --tail 100 springboot-app
curl -i http://localhost:8085
```

To stop and remove the local container:

```bash
docker stop springboot-app
docker rm springboot-app
```

## Jenkins CI/CD pipeline

The repository's `Jenkinsfile` defines these stages:

| Stage | What it does |
|---|---|
| Checkout | Checks out branch `main` from this GitHub repository |
| Build | Runs `mvn clean package -DskipTests` |
| Test | Runs `mvn test` |
| Docker Build and Push | Builds `DOCKER_USER/springboot-app:BUILD_NUMBER` and pushes it to Docker Hub |
| Deploy | Uses SSH to access EC2, logs into Docker Hub, pulls the image, replaces the old container, and starts the new one |

Each Jenkins build uses its build number as the Docker image tag. For example, build 12 publishes:

```text
<DOCKER_HUB_USERNAME>/springboot-app:12
```

### Jenkins prerequisites

The Jenkins machine or agent that runs the pipeline needs:

- Git
- Java 21
- Maven
- Docker Engine/CLI with permission to access the Docker daemon
- Jenkins Pipeline support
- Jenkins Credentials Binding support
- Jenkins SSH Agent plugin (provides the `sshagent` pipeline step)

Configure Java and Maven in Jenkins or ensure the executables are available on the agent's `PATH`.

### Configure Jenkins credentials

Create these credentials under **Manage Jenkins → Credentials → System → Global credentials**.

| Credentials ID | Kind | Required values |
|---|---|---|
| `dockerhub-creds` | Username with password | Docker Hub username and password or access token |
| `deploy-ssh-key` | SSH Username with private key | Username `ubuntu` and the private key corresponding to the EC2 instance key pair |

Use the exact credential IDs because the Jenkinsfile references them directly. Do not put registry passwords, private keys, or other secrets in source control.

### Create a Jenkins Pipeline job

1. Open the Jenkins dashboard.
2. Select **New Item** and create a Pipeline job.
3. Under Pipeline, select **Pipeline script from SCM**.
4. Select **Git** as SCM.
5. Set the repository URL to `https://github.com/saiabbhiram/springboot-demo.git`.
6. Set the branch specifier to `*/main`.
7. Set the script path to `Jenkinsfile`.
8. Save and select **Build Now**.

Review **Console Output** to see each stage and diagnose failures.

## AWS EC2 deployment configuration

The Jenkinsfile currently targets an Ubuntu EC2 host using:

- Host address: `15.206.147.184`
- SSH user: `ubuntu`
- Application host port: `8085`
- Container port: `8085`
- Container name: `springboot-app`

The deployment command uses this port mapping:

```bash
-p 8085:8085
```

This is consistent with `server.port=8085` and the Dockerfile's `EXPOSE 8085`.

### EC2 requirements

On the EC2 instance:

1. Install Docker Engine.
2. Start and enable the Docker service.
3. Add the `ubuntu` user to the `docker` group, then log out and back in so the group membership applies.
4. Confirm the instance can pull the image from Docker Hub. For a private Docker Hub repository, the pipeline performs a Docker login on the EC2 host before pulling.
5. Ensure the EC2 security group allows inbound TCP 8085 from the intended clients.
6. Ensure network ACLs and host firewall rules do not block the traffic.

Example Docker installation using Ubuntu's distribution package:

```bash
sudo apt update
sudo apt install -y docker.io
sudo systemctl enable --now docker
sudo usermod -aG docker ubuntu
```

Reconnect after adding the user to the Docker group, then verify:

```bash
docker --version
docker ps
```

### Security group guidance

Allow only the ports and source addresses you need:

| Port | Purpose | Suggested source |
|---|---|---|
| 22/TCP | SSH deployment | Jenkins server's public IP or network |
| 8085/TCP | Spring Boot application | Your IP or intended client network |
| 8080/TCP | Jenkins UI, only if Jenkins is hosted on this EC2 instance | Trusted administrator IPs |

Avoid exposing Jenkins or SSH to the entire internet. The Jenkins UI port is separate from the Spring Boot application's port.

## Verify deployment

After a successful Jenkins run, connect to the EC2 instance and execute:

```bash
docker ps -a
docker logs --tail 100 springboot-app
curl -i http://localhost:8085
```

Expected container port mapping:

```text
0.0.0.0:8085->8085/tcp
```

From a browser or another machine allowed by the EC2 security group, open:

```text
http://15.206.147.184:8085
```

The application may return 404 at `/` if no root controller is defined. That does not necessarily indicate a network or deployment failure; test a valid application route from the controller source code.

## Troubleshooting

### 1. Browser reports that the server cannot be reached

Check each layer:

```bash
# On EC2: container status
docker ps -a

# On EC2: application startup logs
docker logs --tail 100 springboot-app

# On EC2: test published port locally
curl -v http://localhost:8085

# On EC2: check listening sockets
sudo ss -lntp | grep 8085
```

If local EC2 access works but remote access fails, check the EC2 security group, network ACL, and host firewall for TCP 8085.

### 2. Port mapping does not match the application port

The current application listens on 8085. These settings should agree:

- `application.properties`: `server.port=8085`
- Dockerfile: `EXPOSE 8085`
- Jenkinsfile: `APP_PORT=8085` and `-p 8085:8085`

If the application port is changed, update the related configuration consistently and rebuild the image.

### 3. Jenkins reports that `sshagent` is not a pipeline step

Install and enable the **SSH Agent** plugin in Jenkins. It supplies the `sshagent` step used by the Deploy stage. Verify that the Jenkins credential ID `deploy-ssh-key` exists and contains the correct EC2 private key.

### 4. Jenkins cannot run Docker commands

Ensure Docker is installed on the Jenkins agent and that the Jenkins service account has permission to access the Docker daemon. After changing group membership, restart Jenkins and verify Docker access as the Jenkins user. Docker group membership grants powerful, effectively root-level access, so restrict it to trusted accounts.

### 5. SSH deployment fails

Check that:

- The EC2 instance is running and reachable.
- Inbound SSH (TCP 22) permits connections from the Jenkins host.
- The private key in `deploy-ssh-key` matches the EC2 key pair.
- The SSH credential username is `ubuntu`.
- The EC2 host key is verified and trusted.

The pipeline currently uses `StrictHostKeyChecking=accept-new`, which trusts a host key on first connection. For production, verify the EC2 host key through a trusted channel and pin it in Jenkins' known-hosts configuration.

### 6. Docker Hub authentication or pull fails

Verify the `dockerhub-creds` username and token, confirm the image repository and tag exist, and ensure EC2 has outbound network access to Docker Hub.

## Operational and security notes

- The pipeline uses Jenkins build numbers as image tags. Consider also publishing an immutable Git commit SHA tag for traceability.
- The current pipeline runs `mvn clean package -DskipTests` and then `mvn test`. The build stage skips tests; the separate Test stage runs them afterward. For stricter gating, run tests before publishing the image.
- The current pipeline logs into Docker Hub on both Jenkins and EC2. Prefer a short-lived or least-privilege access token and a secure credential-handling approach. Avoid interpolating secrets into remote shell command strings; use a safer transfer or registry authentication design.
- The current pipeline replaces the running container. For production, consider health checks and a rollback strategy before removing the previous working release.
- Use HTTPS and a reverse proxy or load balancer for public production access rather than exposing an application port directly.
- Do not commit EC2 private keys, Docker Hub credentials, or other secrets to the repository.

## Useful commands

```bash
# Maven
mvn clean package
mvn test

# Docker image
docker build -t springboot-app:local .
docker images

# Docker container
docker ps -a
docker logs -f springboot-app
docker restart springboot-app
docker stop springboot-app
docker rm springboot-app

# Port and HTTP checks
sudo ss -lntp | grep 8085
curl -i http://localhost:8085
```

## Author

**Sai Abbhiram Valupadasu**

GitHub: https://github.com/saiabbhiram

## License

No license file was present in the repository when this documentation was prepared. Add a `LICENSE` file if you intend to specify reuse and distribution terms.
