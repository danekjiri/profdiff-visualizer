## Profdiff Visualizer API
This project is built using the Micronaut framework and Java 21. It acts as a bridge between the [Angular frontend](../frontend/profdiff-web-application/README.md) and the extracted [GraalVM Profdiff library](profdiff). Its main job is to read GraalVM benchmark log files from local file system, do profdiff logic on them, and send that JSON response back.

## Architecture
The backend is organized into layers:
  * **[Controllers:](profdiff-api/src/main/java/cz/cuni/mff/d3s/profdiffweb/controller)** These handle requests from the frontend. Because reading massive log files takes a lot of computing power, the controllers pass heavy tasks to a separate background thread pool.
  * **[Services:](profdiff-api/src/main/java/cz/cuni/mff/d3s/profdiffweb/service)** These handle the core logic. They search local folders to find benchmark logs and safely extract basic information from them. If an optional file is missing or non-fatal error perceived during parsing, they just attach a warning instead of crashing.
  * **[Models:](profdiff-api/src/main/java/cz/cuni/mff/d3s/profdiffweb/model)** These define the data structures used throughout the backend. They shape the exact format of the JSON responses sent to the frontend and metadata parsing from files. This includes Data Transfer Objects for benchmark metadata, method unions, tree representations (like Inlining and Optimization trees), and standardized error or warning messages. Keeping these strictly typed ensures reliable communication and synchronization with the Angular application.
  * **[The Profdiff Port:](profdiff-api/src/main/java/cz/cuni/mff/d3s/profdiffweb/port/profdiff/)** The original Profdiff tool was built to print text into a command-line terminal. This layer acts as a translator. It converts the tool's internal data into clean JSON format for the web application and catches internal library errors to keep the server running smoothly. The modifications, that this port layer depends on, done to Profdiff tool could be seen [here](https://gitlab.mff.cuni.cz/teaching/nprg045/pecimuth/profdiff-web/graal/-/tree/feature/profdiff-webapp-modification/compiler/src/org.graalvm.profdiff/src/org/graalvm/profdiff).
  * **[Two-Tier Cache:](profdiff-api/src/main/java/cz/cuni/mff/d3s/profdiffweb/port/profdiff/internal/cache)** Processing huge compiler logs takes time. To make pages load instantly and prevent data errors, the server uses a two-level memory cache. It saves completely isolated copies of the raw parsed experiments ([Tier 1](profdiff-api/src/main/java/cz/cuni/mff/d3s/profdiffweb/port/profdiff/internal/cache/ParsedExperimentCache.java)) and the final, hotness-marked and fragments extracted experiments ([Tier 2](profdiff-api/src/main/java/cz/cuni/mff/d3s/profdiffweb/port/profdiff/internal/cache/PreparedExperimentCache.java)).

__NOTE:__ You can visualize this entire architecture by pasting the provided [workspace code](../resources/docs/structurizr-architecture/workspace.dsl) into the [Structurizr Playground](https://playground.structurizr.com/).

## Getting Started and documentation

### Running the server locally

To start the backend, run these commands:

```bash
mvn clean install
cd backend/profdiff-api
mvn mn:run
```

### Viewing the API Endpoints

When the server is running, you can see the whole documented exposed API and test it out [here](https://www.google.com/search?q=http://localhost:8080/swagger-ui)

### JavaDocs

The backend codebase is documented using standard JavaDoc to explain internal methods, caching strategies, and service logic.

Once you have compiled the project, you can view the HTML documentation [here](profdiff-api/target/reports/apidocs/index.html).

### Upgrading Profdiff

If a new version of GraalVM Profdiff comes out and you need to update this application to use it, please read the [CONTRIBUTING.md](../CONTRIBUTING.md) for step-by-step instructions.
