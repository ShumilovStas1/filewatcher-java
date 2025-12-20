# FileWatcher Java

Server implementation using Spring Boot and WatchService API to monitor filesystem changes and send notifications via WebSockets.
For client use https://github.com/ShumilovStas1/filewatcher-client

## Installation

```bash
git clone https://github.com/yourusername/filewatcher-java.git
cd filewatcher-java
```

## Run

### Using maven
Port by default is 5001

```bash
./mvnw spring-boot:run
```

## Docker support
Build docker image:

```bash
./mvnw clean package spring-boot:build-image -Dspring-boot.build-image.imageName=filewatcher-java
```

You can use docker-compose too. See `docker-compose.yml` for details.

## Configuration

### Set watcher type

application.properties:
```properties
fw.watcher-type=nio # or spring, or apache_commons
```
evn variable:
```bash
export FW_WATCHER_TYPE=nio
```

Options: 
- nio - Java NIO WatchService (Default)
- spring - Spring FileSystemWatcher (only files supported)
- apache_commons - Apache Commons FileAlterationMonitor 

### Set directories to watch

application.properties:
```properties
fw.watch-dirs[0]=dir
fw.watch-dirs[1]=/home/user/dir2
...
```
evn variable:
```bash
export FW_WATCH_DIRS_0=dir
export FW_WATCH_DIRS_1=/home/user/dir2
```