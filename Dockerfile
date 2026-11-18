
# Stage 1: Build
FROM eclipse-temurin:21-jdk AS build

WORKDIR /app

# Install Maven 3.8.7
RUN apt-get update && apt-get install -y wget tar \
    && wget https://archive.apache.org/dist/maven/maven-3/3.8.7/binaries/apache-maven-3.8.7-bin.tar.gz \
    && tar -xzf apache-maven-3.8.7-bin.tar.gz -C /opt \
    && ln -s /opt/apache-maven-3.8.7/bin/mvn /usr/local/bin/mvn \
    && rm apache-maven-3.8.7-bin.tar.gz \
    && rm -rf /var/lib/apt/lists/*

COPY pom.xml .
RUN mvn dependency:go-offline

COPY src ./src

RUN mvn clean package -DskipTests


# Stage 2: Runtime
FROM eclipse-temurin:21-jre

WORKDIR /app

COPY --from=build /app/target/*.jar app.jar

EXPOSE 8085

ENTRYPOINT ["java", "-jar", "app.jar"]
 
