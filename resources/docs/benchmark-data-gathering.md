# Benchmark data gathering

This documentation provides the complete commands for collecting benchmark diagnostic data (for demonstration `renaissance:scrabble`) in the Linux environment, due to the dependency of `proftool`. Data collected in `benchmark_run` the directory, such as profile data, optimization logs, and execution metadata, are used as input for analysis with Profdiff Visualizer.

### Prerequisites

Before running any benchmarks, the GraalVM compiler and `mx` build tool must be set up. The following commands fetch the required JDK, clone the necessary repositories, and build the GraalVM compiler.

```
mx fetch-jdk -A labsjdk-ce-latest
export JAVA_HOME=<path-printed-by-the-command-above>

git clone https://github.com/oracle/graal.git
git clone https://github.com/graalvm/mx.git
export PATH="$PWD/mx:$PATH"

cd graal/compiler
mx build
```

### JIT benchmark run with libgraal

With the environment ready, the benchmark is executed in JIT mode using `libgraal` as the AOT pre-build compiler. The `proftool` is attached as a profiler, and the optimization log is written to `scrabble_log` for later analysis.

```
mx -p ../vm --env libgraal build

rm -rf proftool_scrabble_*
mx -p ../vm --env libgraal \
  benchmark renaissance:scrabble \
  --tracker none -- \
  --jvm=graalvm-libgraal \
  --jvm-config=native \
  --profiler proftool \
  -Djdk.graal.TrackNodeSourcePosition=true \
  -Djdk.graal.OptimizationLog=Directory \
  -Djdk.graal.OptimizationLogPath=$PWD/scrabble_log
```

### AOT benchmark run with native-image

The same benchmark is also compiled and run in AOT `native-image`. Optimization logging is enabled at image build time, producing an AOT counterpart to the JIT log in `aot\_scrabble_log`.

```
mx -p ../vm --env ni-ce build

rm -rf proftool_scrabble_* aot_scrabble_log
mx -p ../vm --env ni-ce \
  benchmark renaissance-native-image:scrabble -- \
  --jvm=native-image \
  --jvm-config=default-ce \
  -Dnative-image.benchmark.extra-image-build-argument=-H:+TrackNodeSourcePosition \
  -Dnative-image.benchmark.extra-image-build-argument=-H:OptimizationLog=Directory \
  -Dnative-image.benchmark.extra-image-build-argument=-H:OptimizationLogPath=$PWD/aot_scrabble_log
```

### Collecting data for visualization

Once the run is complete, the raw profiling output is converted to JSON and all diagnostic artifacts are consolidated `benchmark_run` into the directory, which serves as the input for Profdiff Visualizer.

```
mx profjson \
  -E proftool_scrabble_* \
  -o scrabble_prof.json

mkdir benchmark_run
mv bench-results.json scrabble_prof.json *scrabble_log \
  benchmark_run
```
