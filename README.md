## Prerequisites

To build the project following software needs to be installed:
* JDK 17
* Sbt 1.9.2
* Docker


## Building a docker image
To build the docker image please run following command
```
sbt clean compile test docker:publishLocal
```

## Running the application
Once docker image is ready please run following command
```
docker run -p 8080:8080 -it collatzmachine:0.1.0-SNAPSHOT
```
The server will be running on port 8080

## Configuration
It is possible to configure port and collatz machine delay interval. 
* Option 1 - change configuration file in `src/resources/application.conf`
* Option 2 - set environment variables for docker container. Please set `COLLATZ_UPDATE_DELAY` and `COLLATZ_PORT`

## Testing
There are several useful shell commands to test the application
* Create machine:
```
curl -X POST http://localhost:8080/create/2/8 
```
* Stream messages from machine:
```
curl -X GET http://localhost:8080/messages/2
```
* Stream all messages from all created machines:
```
curl -X GET http://localhost:8080/messages
```
* Increment machine's state:
```
curl -X POST http://localhost:8080/increment/2/899
```
* Destroy the machine:
```
curl -X POST http://localhost:8080/destroy/3
```

