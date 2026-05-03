#!/bin/bash
set -euo pipefail

# check for directory with JAR libs from the first script parameter
if [ "$#" -lt 1 ]; then
    echo "error: the path to the directory with JAR libs must be provided as the first script parameter."
    echo "usage: $0 /path/to/your/maven/jar_libs"
    exit 1
fi

MAVEN_LIBS_DIR="$1"

# verify that the provided directory exists
if [ ! -d "$MAVEN_LIBS_DIR" ]; then
    echo "please ensure that the provided path is correct and points to a valid directory with JAR libs."
    exit 1
fi

for jar_file in "$MAVEN_LIBS_DIR"/*.jar; do
    if [ -f "$jar_file" ]; then
        echo "Installing JAR file: $jar_file"
        mvn install:install-file \
            -Dfile="$jar_file" \
            -DgroupId=org.graalvm.local \
            -DartifactId=$(basename "$jar_file" .jar) \
            -Dversion=1.0-SNAPSHOT \
            -Dpackaging=jar
    else
        echo "Skipping non-JAR file: $jar_file"
    fi
done