# FileWatcher Java

Server implementation using Spring Boot and WatchService API to monitor filesystem changes and send notifications via WebSockets.
For client use https://github.com/ShumilovStas1/filewatcher-client

## Installation

```bash
git clone https://github.com/yourusername/filewatcher-java.git
cd filewatcher-java
```

## Build 

```bash
./mnvw package
```

## Run

### Using maven

```bash
./mvnw spring-boot:run
```

## Docker support
Build docker image:

```bash
./mvnw clean package spring-boot:build-image -Dspring-boot.build-image.imageName=filewatcher-java
```

You can use docker-compose too. See `docker-compose.yml` for details.