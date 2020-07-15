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

### Run the jetty server

```
$ ./mvnw jetty:run
```
Server will be available at: http://localhost:8080


### Tests

```
$ ./mvnw test
```


## Documentation

This application is composed of three elements: 
- the Servlet ([SSRServlet](src/main/kotlin/com/beorntech/SSRServlet.kt))
- the Html Parser ([HtmlParser#parseHtml](src/main/kotlin/com/beorntech/HtmlParser.kt))
- the JS Interpreter ([RhinoJSInterpreter](src/main/kotlin/com/beorntech/RhinoJSInterpreter.kt))

The Html Parser shouldn't be aware of the implementation details of the JS Interpreter.

Sorely lacking error handling.

### Servlet
A simple servlet, currently only statically serves useless html code

### The Html Parser
Powered by Jsoup.    
Runs down the Html DOM tree and parses each attribute and each element in order to identify the various templating language elements.

### RhinoJSInterpreter
Powered by Mozilla's Rhino Javascript engine.  
Evaluates javascript expressions and manages the java to javascript integration.


