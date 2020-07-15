# Beorn SSR

Beorn SSR: a new server-side HTML templating 

## Getting Started

These instructions will get you a copy of the project up and running on your local machine for development and testing purposes.

### Installing

####Asked to use Java 8
##### Ubuntu 20.04 Env (as of July 2020)
* `sudo apt-get install openjdk-8-jdk`
* `JAVA_HOME=/usr/lib/jvm/java-8-openjdk-amd64 ./mvnw etc.`


Install dependencies

```
$ ./mvnw clean install
```

Run the jetty server

```
$ ./mvnw jetty:run
```
Server will be available at: http://localhost:8080


Tests

```
$ ./mvnw test
```


## Documentation

TBD
