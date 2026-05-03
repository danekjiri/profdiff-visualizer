# Profdiff Visualizer web frontend

This is the Angular frontend for the [Profdiff Visualizer API](../../backend/README.md). It transforms processed JSONized GraalVM compiler logs parsed by the backend into an interactive, color-coded web interface. It helps compiler engineers to easily read, track, and analyze optimization decisions without dealing with raw textual output of [`mx profdiff`](https://github.com/oracle/graal/blob/master/compiler/docs/Profdiff.md).

## Web-application architecture

The application is built using reusable components (e.g., floating-dock, rendered-tree-node, and compilation-unit-card...). It is divided into 3 main pages:

* **Home** A discovery page to search for benchmark workspaces, check run statuses, and view performance charts.
* **Report** A single-run analysis view. It shows the most active methods and lets user explore deeply nested optimization trees.
* **Compare** A side-by-side view to compare two different runs. It highlights exactly what changed in the compiler decisions using specialized delta trees with color-coding.

The frontend gets all its data from the [Micronaut backend](../../backend/). All communication with this API is wrapped inside the [RunsService](app/services/runs.service.ts). This service safely handles fetching all experiments, metadata, and compilation trees.

## Local development

### Running the frontend locally

Use `ng serve` to start the local development server, then access it via [http://localhost:4200](http://localhost:4200)

### Generate docs

Use `npm run docs` to generate the frontend documentation.

### Synchronize with backend DTOs

Use the `npm run generate-api` command. This updates the TypeScript models to make sure they match the backend data structures.
