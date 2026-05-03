#!/bin/bash

# Creates a temporary environment to clone the Graal and mx repositories, builds the compiler, and extracts the
# dependency JARs for Profdiff and Profdiff source code itself if specified.
#
# Prerequisites: git and a valid JDK from the labs-openjdk repository
# (Download: https://github.com/graalvm/labs-openjdk/releases)
#
# Usage: ./profdiff_extract.sh <JAVA_HOME> [--extract-source]
#
# Arguments:
#   $1 <JAVA_HOME>      : (Required) absolute path to the labs-openjdk directory
#   $2 --extract-source : (Optional) extracts the Profdiff source code and test files into a local directory
#
# Outputs:
#   - profdiff-dependency-jars/ : contains the extracted classpath JARs
#   - profdiff-source/          : (Optional) contains the extracted 'src' and 'test' directories if the flag provided

set -euo pipefail

GRAAL_BRANCH="master"
SCRIPT_ROOT_DIR=$(pwd)
PROFDIFF_DEPENDENCY_JARS_DIR="profdiff-dependency-jars"
TEMP_GRAAL_DIR="$SCRIPT_ROOT_DIR/temp-graal"
EXTRACT_SRC=false

# define a cleanup function and set a trap to catch unexpected failures
cleanup() {
    echo ">running cleanup..."
    if [ -d "$TEMP_GRAAL_DIR" ]; then
        echo ">removing temporary directory $TEMP_GRAAL_DIR..."
        rm -rf "$TEMP_GRAAL_DIR"
    fi
}
trap cleanup EXIT

# check for JAVA_HOME path from the first script parameter at the beginning
if [ "$#" -lt 1 ]; then
    echo ">error: the path to JAVA_HOME must be provided as the first script parameter."
    echo ">the JDK has to be latest from repo https://github.com/graalvm/labs-openjdk/releases."
    echo ">usage: $0 absolute/path/to/your/java_home [--extract-source]"
    exit 1
fi

export JAVA_HOME="$1"

# check for the optional second parameter to extract source
if [ "$#" -ge 2 ]; then
    if [[ "$2" == "--extract-source" || "$2" == "--extract-src" ]]; then
        EXTRACT_SRC=true
        echo ">source extraction flag detected."
    else
        echo ">warning: unknown parameter $2 ignored. Usage: $0 <JAVA_HOME> [--extract-source]"
    fi
fi

# verify that the JAVA_HOME/bin directory exists
if [ ! -d "$JAVA_HOME/bin" ]; then
    echo ">please ensure that the provided JAVA_HOME path is correct and points to a valid JDK installation."
    echo ">valid JDK is the latest from repo https://github.com/graalvm/labs-openjdk/releases."
    exit 1
fi

echo ">JAVA_HOME has been set to: $JAVA_HOME"
echo ">script root directory: $SCRIPT_ROOT_DIR"

# create a temporary directory for building graalvm
mkdir -p "$TEMP_GRAAL_DIR"
cd "$TEMP_GRAAL_DIR"
echo ">temporary graal directory: $TEMP_GRAAL_DIR"

# clone the graal repository
git clone -b "$GRAAL_BRANCH" https://github.com/oracle/graal.git

# clone the mx repository
git clone https://github.com/graalvm/mx.git

export PATH="$PWD/mx:$PATH"
echo ">mx has been added to PATH: $PWD/mx"

# navigate into the cloned graal directory for building
cd graal/compiler
echo ">current directory for building: $(pwd)"
mx build

# extract the JARs
echo ">attempting to extract JARs using mx profdiff..."
MX_VERBOSE_PROFDIFF_OUTPUT=$(mx -v profdiff || true) # true to avoid exit on failure
CLASSPATH_STRING=$(echo "$MX_VERBOSE_PROFDIFF_OUTPUT" | \
                   grep -- '-cp .* org\.graalvm\.profdiff\.Profdiff' | \
                   sed -n 's/.*-cp \([^ ]*\).*/\1/p' | \
                   head -n 1)

# initialize an empty array for the JARs
jar_array=()

if [ -n "$CLASSPATH_STRING" ]; then
    echo ">found classpath string: $CLASSPATH_STRING"
    # split the classpath string by the colon ':' delimiter
    IFS=':' read -r -a jar_array <<< "$CLASSPATH_STRING"
else
    echo ">classpath string for org.graalvm.profdiff.Profdiff was not found in the output."
fi

# print the found JARs
echo ">number of JARs found: ${#jar_array[@]}"

if [ "${#jar_array[@]}" -eq 0 ]; then
    echo ">no JARs were extracted, so no JARs to copy."
    exit 1
fi

# create directory and copy JARs
cd "$SCRIPT_ROOT_DIR"
echo ">changed directory to $SCRIPT_ROOT_DIR for creating libs folder."

mkdir -p "$PROFDIFF_DEPENDENCY_JARS_DIR"
echo ">created directory for JARs: $SCRIPT_ROOT_DIR/$PROFDIFF_DEPENDENCY_JARS_DIR"

echo ">copying JARs to $PROFDIFF_DEPENDENCY_JARS_DIR..."
for jar_file_path in "${jar_array[@]}"; do
    if [ -f "$jar_file_path" ]; then
        cp "$jar_file_path" "$PROFDIFF_DEPENDENCY_JARS_DIR/"
        echo "  >copied $(basename "$jar_file_path") to $PROFDIFF_DEPENDENCY_JARS_DIR/"
    else
        echo "  >warning: JAR file not found at $jar_file_path. Skipping."
    fi
done

# final messages
echo ">JAR copying process complete."
echo ">you can find the JARs in: $SCRIPT_ROOT_DIR/$PROFDIFF_DEPENDENCY_JARS_DIR"

# conditional profdiff source code extraction
if [ "$EXTRACT_SRC" = true ]; then
    EXTRACT_TARGET_DIR="profdiff-source"
    SRC_TARGET_DIR="$SCRIPT_ROOT_DIR/$EXTRACT_TARGET_DIR/src"
    TEST_TARGET_DIR="$SCRIPT_ROOT_DIR/$EXTRACT_TARGET_DIR/test"

    echo ">extracting source code to $SCRIPT_ROOT_DIR/$EXTRACT_TARGET_DIR..."

    mkdir -p "$SRC_TARGET_DIR"
    mkdir -p "$TEST_TARGET_DIR"

    # copy source code
    SOURCE_CODE_DIR="$TEMP_GRAAL_DIR/graal/compiler/src/org.graalvm.profdiff/src"
    if [ -d "$SOURCE_CODE_DIR" ]; then
        cp -r "$SOURCE_CODE_DIR"/* "$SRC_TARGET_DIR/"
    fi

    # copy tests
    TEST_SOURCE_CODE_DIR="$TEMP_GRAAL_DIR/graal/compiler/src/org.graalvm.profdiff.test/src"
    if [ -d "$TEST_SOURCE_CODE_DIR" ]; then
        cp -r "$TEST_SOURCE_CODE_DIR"/* "$TEST_TARGET_DIR/"
    fi

    echo ">source code and tests have been extracted to $SCRIPT_ROOT_DIR/$EXTRACT_TARGET_DIR"
else
    echo ">skipping source code extraction (use --extract-source to enable)."
fi

echo ">script completed successfully."