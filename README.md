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

### Using java -jar

After project build 

```bash
java -jar target/filewatcher-0.0.1-SNAPSHOT.jar
```

## Docker support
Build project and then 

```bash
docker build -t filewatcher-java .
```

You can use docker-compose too. See `docker-compose.yml` for details.