# Contributing to Profdiff Visualizer
 
## Table of Contents
 
- [Contributing to Profdiff Visualizer](#contributing-to-profdiff-visualizer)
  - [Table of Contents](#table-of-contents)
  - [Updating the Profdiff Dependency](#updating-the-profdiff-dependency)
    - [Prerequisites](#prerequisites)
    - [Step 1: Extract the new JAR dependencies](#step-1-extract-the-new-jar-dependencies)
    - [Step 2: Sync the Profdiff source code](#step-2-sync-the-profdiff-source-code)
    - [Step 3: Verify the update](#step-3-verify-the-update)

---
 
## Updating the Profdiff Dependency
 
The `backend/profdiff/` module contains source code extracted from the GraalVM repository and modified for use in a concurrent web environment. When a new version of GraalVM Profdiff is released, you need to re-extract and re-sync these files.
 
> **Why is this manual?** Profdiff is not published as a standalone Maven artifact — it lives deep inside the monolithic GraalVM repository and relies on internal GraalVM JARs. Until the visualizer's modifications are merged upstream, this extraction process is the only way to stay in sync.
 
### Prerequisites
 
- [Git](https://git-scm.com/)
- [mx build tool](https://github.com/graalvm/mx) — GraalVM's custom build system, required by the extraction script
- Bash shell (Linux/macOS native; on Windows use WSL or Git Bash)
---
 
### Step 1: Extract the new JAR dependencies
 
The extraction script downloads the GraalVM repository, builds Profdiff using `mx`, and packages the required JAR dependencies.
 
**1. Download the latest Labs OpenJDK** from the [GraalVM releases page](https://github.com/graalvm/labs-openjdk/releases) and note its absolute path.
 
**2. Make the extraction script executable:**
```bash
chmod +x resources/scripts_benchmarks/profdiff_extract.sh
```
 
**3. Run the script:**
```bash
./resources/scripts_benchmarks/profdiff_extract.sh <absolute_path_to_labs-openjdk>
```
 
This produces two outputs in the current directory:
- `profdiff-dependency-jars/` — the required GraalVM JAR dependencies
- the extracted Profdiff source code
**4. Replace the old JAR dependencies in the repository:**
```bash
rm -rf resources/profdiff-dependency-jars
mv profdiff-dependency-jars resources/
```
 
> **Do not modify `backend/profdiff/pom.xml`** unless absolutely necessary. It references these local JARs by filename. They are installed into the local Maven repository automatically by `resources/scripts_benchmarks/mvn_install_file.sh`, which the CI pipeline and `mvn clean install` both invoke.
 
---
 
### Step 2: Sync the Profdiff source code
 
All visualizer-specific modifications to the Profdiff source are captured in a single patch file:
```
resources/profdiff_visualizer.patch
```
 
**1. Get the latest Profdiff source from GraalVM:**
```bash
git clone https://github.com/oracle/graal.git
cd graal
```
If you already have it cloned:
```bash
git fetch origin && git checkout master && git pull
```
 
**2. Apply the visualizer patch:**
```bash
git apply --3way <path_to_this_repo>/resources/profdiff-restapi-modification.patch
```
 
**3. Copy the updated source into this repository:**
```bash
# From inside the graal/ directory — replace <repo> with the path to this repo
 
# Source code
cp -r compiler/src/org.graalvm.profdiff/src/org/graalvm/profdiff/ \
      <repo>/backend/profdiff/src/main/java/org/graalvm/
 
# Tests
cp -r compiler/src/org.graalvm.profdiff.test/src/org/graalvm/profdiff/test/ \
      <repo>/backend/profdiff/src/test/java/org/graalvm/
```
 
**4. Regenerate the patch file** so it stays current for the next update:
```bash
cd graal
git diff > <repo>/resources/profdiff_visualizer.patch
```
 
---
 
### Step 3: Verify the update
 
```bash
# From the backend directory:
mvn clean install
```