#!/bin/bash
set -euo pipefail

BENCHMARK=""
USE_PROFILE=false
RUN_PROFHOT=false
COMPILATION_MODE="JIT"

if [ $# -lt 1 ]; then
    echo "Usage: $0 <ROOT_DIR> [options]"
    exit 1
fi

ROOT_DIR=$1
RUN_NAME="run_$(date +%Y%m%d_%H%M%S)"
RUN_DIR="$ROOT_DIR/$RUN_NAME"
OPT_LOG_PATH=""
PROFILE_DIR=""

cleanup_on_failure() {
    if [ $? -ne 0 ]; then
        echo "Run failed. Cleaning up..."
        rm -rf "$RUN_DIR"
        if [ -z "$(ls -A "$ROOT_DIR")" ]; then
            rmdir "$ROOT_DIR"
        fi
        echo "Run failed and directories cleaned up."
    fi
}

cleanup_temporary_files() {
    echo "Cleaning up temporary files..."
    if [[ -n "${PROFILE_DIR:-}" ]]; then
        rm -rf "${PROFILE_DIR}"*
    fi
    rm -rf perf*
    [ -f bench-results.json ] && mv bench-results.json "$RUN_DIR/"
}

exit_on_non_provided_benchmarks() {
    echo "Error: --benchmarks=<name> is required"
    exit 1
}

exit_on_profhot_without_profile() {
    echo "Error: --profhot requires --profile to be used as well."
    exit 1
}

check_compilation_kind() {
    if [[ "$COMPILATION_MODE" != "JIT" && "$COMPILATION_MODE" != "AOT" ]]; then
        echo "Error: --compilation must be JIT or AOT"
        exit 1
    fi
}

set_default_opt_log_path() {
    if [[ -z "$OPT_LOG_PATH" ]]; then
        OPT_LOG_PATH="${RUN_DIR}"
    fi
}

parse() {
    for arg in "$@"; do
        case "$arg" in
        --benchmarks=*)
            BENCHMARK="${arg#*=}"
            ;;
        --profile)
            USE_PROFILE=true
            ;;
        --profhot)
            RUN_PROFHOT=true
            ;;
        --opt_dir=*)
            OPT_LOG_PATH="${arg#*=}"
            ;;
        --compilation=*)
            COMPILATION_MODE="${arg#*=}"
            ;;
        esac
    done

    if [[ -z "$BENCHMARK" ]]; then
        exit_on_non_provided_benchmarks
    fi

    if $RUN_PROFHOT && ! $USE_PROFILE; then
        exit_on_profhot_without_profile
    fi

    check_compilation_kind
    set_default_opt_log_path
}

prepare_env() {
    mkdir -p "$ROOT_DIR"
    mkdir "$RUN_DIR"

    if [[ -n "$OPT_LOG_PATH" ]]; then
        mkdir -p "$OPT_LOG_PATH"
    fi

    trap cleanup_on_failure EXIT
}

run_benchmarks() {
    echo "Compiling/Running in $COMPILATION_MODE mode..."

    if [[ "$COMPILATION_MODE" == "JIT" ]]; then
        # libgraal build first??
        # mx -p ../vm --env libgraal build

        if $USE_PROFILE; then
            echo "Running JIT benchmark with profiler..."
            mx -p ../vm --env libgraal benchmark "$BENCHMARK" --tracker none \
                -- \
                --jvm=graalvm-libgraal \
                --jvm-config=native \
                --profiler proftool \
                -Djdk.graal.TrackNodeSourcePosition=true \
                -Djdk.graal.OptimizationLog=Directory \
                -Djdk.graal.OptimizationLogPath="$(realpath ${OPT_LOG_PATH}/${BENCHMARK}_log)"

            PROFILE_DIR=$(ls -td proftool_* 2>/dev/null | head -n 1 || true)
            if [[ -z "$PROFILE_DIR" ]]; then
                echo "Profiler output not found."
                exit 1
            fi

            mx profjson -E "$PROFILE_DIR" -o "${OPT_LOG_PATH}/${BENCHMARK}.json"
        else
            echo "Running JIT benchmark without profiler..."
            mx -p ../vm --env libgraal benchmark "$BENCHMARK" --tracker none \
                -- \
                --jvm=graalvm-libgraal \
                --jvm-config=native \
                -Djdk.graal.OptimizationLog=Directory \
                -Djdk.graal.OptimizationLogPath="$(realpath ${OPT_LOG_PATH}/${BENCHMARK}_log)"
        fi

    elif [[ "$COMPILATION_MODE" == "AOT" ]]; then
        if $USE_PROFILE; then
            mx -p ../vm --env ni-ce benchmark "$BENCHMARK" --tracker none \
                -- \
                --profiler proftool \
                --jvm=native-image \
                --jvm-config=default-ce \
                -Dnative-image.benchmark.extra-image-build-argument=-H:+TrackNodeSourcePosition \
                -Dnative-image.benchmark.extra-image-build-argument=-H:OptimizationLog=Directory \
                -Dnative-image.benchmark.extra-image-build-argument=-H:OptimizationLogPath="$(realpath ${OPT_LOG_PATH}/${BENCHMARK}_log)"

            PROFILE_DIR=$(ls -td proftool_* 2>/dev/null | head -n 1 || true)
            if [[ -z "$PROFILE_DIR" ]]; then
                echo "Profiler output not found."
                exit 1
            fi

            mx profjson -E "$PROFILE_DIR" -o "${OPT_LOG_PATH}/${BENCHMARK}.json"
        else 

            # ni-ce build first??
            # mx -p ../vm --env ni-ce build

            echo "Running AOT benchmark: $BENCHMARK"
            mx -p ../vm --env ni-ce benchmark "$BENCHMARK" \
                -- \
                --jvm=native-image \
                --jvm-config=default-ce \
                -Dnative-image.benchmark.extra-image-build-argument=-H:+TrackNodeSourcePosition \
                -Dnative-image.benchmark.extra-image-build-argument=-H:OptimizationLog=Directory \
                -Dnative-image.benchmark.extra-image-build-argument=-H:OptimizationLogPath="$(realpath ${OPT_LOG_PATH}/${BENCHMARK}_log)"
        fi
   fi
}

capture_profiles() {
    if $RUN_PROFHOT; then
        echo "Running mx profpackage on ${PROFILE_DIR}..."
        mx profpackage "$PROFILE_DIR"

        echo "Running mx profhot on ${PROFILE_DIR}.zip..."
        mx profhot "$PROFILE_DIR" > "${RUN_DIR}/${RUN_NAME}_profhot.log"
    else
        echo "Skipping profhot"
    fi
}

main() {
    parse "$@"
    prepare_env

    echo "Benchmark: $BENCHMARK"
    echo "Use profiler: $USE_PROFILE"
    echo "Use profpackage: $RUN_PROFHOT"

    run_benchmarks
    capture_profiles

    cleanup_temporary_files
}

main "$@"