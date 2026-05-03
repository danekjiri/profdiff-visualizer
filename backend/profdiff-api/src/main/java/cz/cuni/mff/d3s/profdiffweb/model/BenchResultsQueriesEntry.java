package cz.cuni.mff.d3s.profdiffweb.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.micronaut.core.annotation.Introspected;

/**
 * Represents a single entry in the benchmark results 'queries' in bench-results.json file.
 *
 * @param benchmarkSuite The name of the benchmark suite.
 * @param benchmarkName The name of the benchmark.
 * @param commitHash The commit hash of the compiler used for the benchmark.
 * @param machinePlatform The platform on which the benchmark was run.
 * @param graalVersion The version of the GraalVM used for the benchmark.
 * @param jdkVersion The version of the JDK used for the benchmark.
 * @param metricValue The value of the metric for the benchmark run, a time period in milliseconds.
 */
@Introspected
@JsonIgnoreProperties(ignoreUnknown = true)
public record BenchResultsQueriesEntry(
        @JsonProperty("bench-suite") String benchmarkSuite,
        @JsonProperty("benchmark") String benchmarkName,
        @JsonProperty("compiler.commit.rev") String commitHash,
        @JsonProperty("extra.machine.platform") String machinePlatform,
        @JsonProperty("platform.graalvm-version-string") String graalVersion,
        @JsonProperty("platform.jdk-version-string") String jdkVersion,
        @JsonProperty("metric.value") Double metricValue) {}
