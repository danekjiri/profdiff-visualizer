# Web application for Profdiff

A local client-server web application for visualizing and analyzing GraalVM compiler optimization differences between runs. Built on top of [Profdiff](https://github.com/oracle/graal/blob/master/compiler/docs/Profdiff.md). This application replaces its raw text output with an interactive, color-coded web interface. Supports both JIT and AOT compilation kind analysis.

## Table of contents

- [Web application for Profdiff](#web-application-for-profdiff)
  - [Table of contents](#table-of-contents)
  - [Key features](#key-features)
  - [Project structure](#project-structure)
  - [Quick start (Docker)](#quick-start-docker)
  - [Usage Guide](#usage-guide)
  - [Benchmark runs directory structure](#benchmark-runs-directory-structure)
  - [Architecture](#architecture)
  - [Local Development (without Docker)](#local-development-without-docker)
- [License](#license)

---

## Key features

- **Workspace browser:** navigate your mounted benchmark directory (`BENCHMARKS_DIR`) or the built-in `/default-benchmarks` demo data
- **Report page:** (`mx profdiff report`) interactive top-methods table, on-demand trees loading, clickable method navigation
- **Compare page:** (`mx profdiff jit-vs-aot`) side-by-side diff of two runs with delta trees
- **Compilation fragments:** hot methods inlined into a parent unit are extracted as fragments with a chip linking back to the parent compilation
- **Execution charts:** plot iteration times across selected runs to confirm regressions and filter out warmup
- **Settings dock:** adjust hotness thresholds, tree verbosity

---
 
## Project structure
 
```
root/
+-- .github/workflows/ci.yml        # GitHub Actions CI pipeline
+-- backend/
|   +-- profdiff/                   # Extracted & modified GraalVM Profdiff source (GPL v2 + Classpath Exception license)
|   +-- profdiff-api/               # Micronaut REST API server (MIT license)
|   +-- README.md                   # Backend-specific documentation
|   +-- Dockerfile                  # Backend-specific dockerfile
+-- frontend/
|   +-- profdiff-web-application/   # Angular SPA frontend (MIT license)
|       +-- README.md               # Frontend-specific documentation
|   +-- Dockerfile                  # Frontend-specific dockerfile
|   +-- nginx.conf.template          # Frontend-specific nginx file template
+-- resources/
|   +-- profdiff-dependency-jars/   # Extracted GraalVM JAR dependencies for Profdiff
|   +-- scripts_benchmarks/         # Helper scripts (e.g., profdiff_extract.sh)
|   +-- docs/structurizr/           # C4 architecture model (workspace.dsl)
+-- docker-compose.yml
+-- CONTRIBUTING.md             # How to update the Profdiff dependency
+-- LICENSE                     # GPL v2 + Classpath Exception (covers backend/profdiff/)
```

## Quick start (Docker)

The recommended way to run the application is via Docker. No Java or Node.js installation required.

**Prerequisites:** [Docker](https://www.docker.com/products/docker-desktop/) running locally.

### Option A — Pull from Docker Hub (no clone required)

**1. Create a `docker-compose.yml`** anywhere on your machine:
```yaml
services:
  backend:
    image: danekjiri/profdiff-visualizer-backend:latest
    ports:
      - "${BACKEND_PORT:-8080}:8080"
    volumes:
      - "${BENCHMARKS_DIR:?Error: BENCHMARKS_DIR must be set!}:/workspace:ro,z"
    environment:
      - MICRONAUT_ENVIRONMENTS=docker 

  frontend:
    image: danekjiri/profdiff-visualizer-frontend:latest
    ports:
      - "${FRONTEND_PORT:-4200}:80"
    environment:
      - BACKEND_URL=http://backend:8080
    depends_on:
      - backend
```

**2. Create a `.env` file** next to it, with corresponding content:
```
BENCHMARKS_DIR=/absolute/path/to/your/benchmark/directory
```

**3. Pull and run:**
```bash
docker compose up
```

### Option B — Clone and build

**1. Clone the repository:**
```bash
git clone git@github.com:danekjiri/profdiff-visualizer.git
cd profdiff-visualizer
```

**2. Point the app at your benchmark data:**

Create a `.env` file in the project root (next to `docker-compose.yml`), with corresponding content:
```
BENCHMARKS_DIR=/absolute/path/to/your/benchmark/directory
```
 
Alternatively, export it in your shell:
```bash
# Linux/macOS
export BENCHMARKS_DIR="/absolute/path/to/your/benchmark/directory"
 
# Windows (PowerShell)
$env:BENCHMARKS_DIR="C:\absolute\path\to\your\benchmark\directory"
```

**3. Build and run:**

```bash
docker compose up --build
```

> The `--build` flag is only needed the first time, or after pulling new changes. After that, `docker compose up` is enough.

---

> By default the frontend runs on port `4200` and the backend on `8080`. To change either, add `FRONTEND_PORT=<port>` or `BACKEND_PORT=<port>` to your `.env` file.

**4. Open the app:** http://localhost:4200

---
 
## Usage Guide
 
Once the app is open in your browser:
 
1. **Enter a workspace path** in the search input at the top.
   - Type `/workspace` for your configured `BENCHMARKS_DIR`, mapping your local folder into the container.
   - Type `/default-benchmarks` to explore the built-in demo data and try the app without any diagnostic data setup.
2. **Press Enter or click *Show Runs*.** The runs table will populate with discovered benchmark runs and their metadata. Any warnings (e.g., missing profiler files) appears in orange container; if error a red container appears instead of runs table.
3. **Explore a single run** — click the *REPORT* button on any row to open the Report page for that run.
   - The top methods table highlights the hottest compilation units automatically (if profiler data available).
   - Choose a method in all methods dropdown. Switch tabs to load the optimization or optimization-context tree on demand.
   - Method names within trees are clickable links — navigate directly to any method's independent profile.
   - Use the floating settings dock (bottom-left) to adjust hotness thresholds and tree verbosity interactively.
4. **Compare two runs** — check the checkboxes on exactly two rows, then click *COMPARE*.
   - The side-by-side view pairs matching compilation units between both runs.
   - The delta tree uses `+`, `-`, `*` icons to show exactly what changed in compiler decisions.
5. **Spot regressions** — select multiple rows to plot their execution times in the interactive chart (bottom-right corner). Use the iteration range filter in the settings dock to trim warmup overhead.

For a full walkthrough of a real regression investigation using the included
demo data, see the **[User docs walkthrough](resources/docs/user-docs/USER-DOCS-WALKTHROUGH.md)**.

---

## Benchmark runs directory structure

The application expects benchmark run directories to follow this layout:

```
root_workspace_directory/
+-- sd1-graal25-valid-fast/          (unique run directory)
|   +-- bench-results.json           (optional) benchmark metadata
|   +-- renaissance/scala-doku.json  (optional) profiler data;
|   +-- renaissance/scala-doku_log/  (mandatory) optimization logs
|       +-- 0_14
|       +-- ...
+-- sd2-graal25-valid-slow/
    +-- ...
```

- The **optimization log directory** is the only mandatory item per run. 
- `bench-results.json` and profiler JSON (result of `mx profjson` command) are optional
- Directories can be deeply nested, the workspace browser navigates them interactively.

> See the **[Benchmark Data Gathering Guide](resources/docs/benchmark-data-gathering.md)** for how to generate runs using `mx benchmark` and `proftool`.

---
 
## Architecture

Three components, see the linked READMEs for details.

| Component | Description | Docs |
|---|---|---|
| **Frontend** | Angular v20 SPA — interactive trees, charts, settings dock | [README](frontend/profdiff-web-application/README.md) |
| **Backend API** | Java 21 + Micronaut REST server with two-tier cache | [README](backend/README.md) |
| **Profdiff library** | Extracted & modified GraalVM Profdiff source — see [CONTRIBUTING.md](CONTRIBUTING.md) to update | — |

> Want to visualize the full C4 architecture? Paste [`resources/docs/structurizr/workspace.dsl`](resources/docs/structurizr-architecture/workspace.dsl) into the [Structurizr Playground](https://playground.structurizr.com/).
---
 
## Local Development (without Docker)
 
Not recommended for regular use, but useful for active development.
 
**Backend:**
```bash
mvn clean install
cd backend/profdiff-api
mvn mn:run
```
The API server starts on http://localhost:8080.
 
**Frontend:**
```bash
cd frontend/profdiff-web-application
npm install
ng serve
```
The frontend starts on http://localhost:4200.

---

# License
This project is a modified fork of GraalVM's profdiff tool.
 
- `backend/profdiff/` — GPL v2 with the Classpath Exception (see [LICENSE](LICENSE))
- `backend/profdiff-api/` — MIT (see [LICENSE](backend/profdiff-api/LICENSE))
- `frontend/profdiff-web-application/` — MIT (see [LICENSE](frontend/profdiff-web-application/LICENSE))