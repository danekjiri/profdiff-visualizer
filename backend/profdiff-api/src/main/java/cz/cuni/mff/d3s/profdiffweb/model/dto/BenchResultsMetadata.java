package cz.cuni.mff.d3s.profdiffweb.model.dto;

import io.micronaut.core.annotation.Introspected;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Selected considerable metadata parsed from the bench-results.json file.
 *
 * @param benchmarkSuite Name of the benchmark suite.
 * @param benchmarkName Name of the benchmark.
 * @param commitHash Commit hash in GraalVM git repo used for compilations in current the benchmark.
 * @param machinePlatform Platform (OS) on which the benchmark was run.
 * @param graalVersion Version of the GraalVM used for the benchmark.
 * @param jdkVersion Version of the JDK used for the benchmark.
 * @param metricValues Time sampling periods (_NOTE_: not 1-1 relation with cpu time nor
 *     milliseconds) of the whole benchmark run, one for each metric round.
 */
@Introspected
public record BenchResultsMetadata(
        @Schema(
                        description = "Name of the benchmark suite.",
                        example = "renaissance",
                        requiredMode = Schema.RequiredMode.NOT_REQUIRED)
                String benchmarkSuite,
        @Schema(
                        description = "Name of the benchmark.",
                        example = "scrabble",
                        requiredMode = Schema.RequiredMode.NOT_REQUIRED)
                String benchmarkName,
        @Schema(
                        description = "Commit hash in GraalVM git repo used for compilations in current the benchmark.",
                        example = "5d94c2ee4e65e6a0ad564a588d47d76ed003bb7b",
                        requiredMode = Schema.RequiredMode.NOT_REQUIRED)
                String commitHash,
        @Schema(
                        description = "Platform (OS) on which the benchmark was run.",
                        example = "Linux-5.16.11-200.fc35.x86_64-x86_64-with-glibc2.34",
                        requiredMode = Schema.RequiredMode.NOT_REQUIRED)
                String machinePlatform,
        @Schema(
                        description = "Version of the GraalVM used for the benchmark.",
                        example = "GraalVM CE 21.0.2",
                        requiredMode = Schema.RequiredMode.NOT_REQUIRED)
                String graalVersion,
        @Schema(
                        description = "Version of the JDK used for the benchmark.",
                        example =
                                "OpenJDK 64-Bit Server VM GraalVM CE 21.0.2+13.1 (build 21.0.2+13-jvmci-23.1-b33, mixed mode, sharing)",
                        requiredMode = Schema.RequiredMode.NOT_REQUIRED)
                String jdkVersion,
        @Schema(
                        description =
                                "Time sampling periods (_NOTE_: not 1-1 relation with cpu time nor milliseconds) of the whole benchmark run, one for each metric round.",
                        example = "[420.0, 500.0, 600.0]")
                List<Double> metricValues) {
    /// Expected file name for the bench-results JSON file.
    public static final String EXPECTED_FILE_NAME = "bench-results.json";
    /// Key for the list of metadata objects in the bench-results JSON file.
    public static final String EXPECTED_FIRST_JSON_KEY = "queries";
    public static final Logger LOGGER = LoggerFactory.getLogger(BenchResultsMetadata.class);

    public static Builder builder(String fileName) {
        return new Builder(fileName);
    }

    /**
     * Builder for creating instances of {@link BenchResultsMetadata}.
     *
     * <p>It ensures that the metadata fields are consistent across multiple entries, i.e., they do
     * not differ from previous entries and `metric.value` is a list of values for each metric
     * recorded in the benchmark results.
     */
    public static class Builder {
        private static final String WARNING_LABEL = "METADATA_WARNING";
        private final String fileName;
        private String benchmarkSuite;
        private String benchmarkName;
        private String commitHash;
        private String machinePlatform;
        private String graalVersion;
        private String jdkVersion;
        private List<Double> metricValue;

        private final List<WarningMessage> validationErrors = new ArrayList<>();

        private Builder(String fileName) {
            this.fileName = fileName;
        }

        public List<WarningMessage> getValidationErrors() {
            return validationErrors;
        }

        public Builder withBenchmarkSuite(String benchmarkSuite) {
            if (this.benchmarkSuite == null) {
                this.benchmarkSuite = benchmarkSuite;
            } else if (!this.benchmarkSuite.equals(benchmarkSuite)) {
                String msg = "BenchmarkSuite should not differ from previous entry for file '" + fileName + "'.";
                LOGGER.warn(msg);
                validationErrors.add(WarningMessage.of(msg, WARNING_LABEL));
            }
            return this;
        }

        public Builder withBenchmarkName(String benchmarkName) {
            if (this.benchmarkName == null) {
                this.benchmarkName = benchmarkName;
            } else if (!this.benchmarkName.equals(benchmarkName)) {
                String msg = "BenchmarkName should not differ from previous entry for file '" + fileName + "'.";
                LOGGER.warn(msg);
                validationErrors.add(WarningMessage.of(msg, WARNING_LABEL));
            }
            return this;
        }

        public Builder withCommitHash(String commitHash) {
            if (this.commitHash == null) {
                this.commitHash = commitHash;
            } else if (!this.commitHash.equals(commitHash)) {
                String msg = "CommitHash should not differ from previous entry for file '" + fileName + "'.";
                LOGGER.warn(msg);
                validationErrors.add(WarningMessage.of(msg, WARNING_LABEL));
            }
            return this;
        }

        public Builder withMachinePlatform(String machinePlatform) {
            if (this.machinePlatform == null) {
                this.machinePlatform = machinePlatform;
            } else if (!this.machinePlatform.equals(machinePlatform)) {
                String msg = "MachinePlatform should not differ from previous entry for file '" + fileName + "'.";
                LOGGER.warn(msg);
                validationErrors.add(WarningMessage.of(msg, WARNING_LABEL));
            }
            return this;
        }

        public Builder withGraalVersion(String graalVersion) {
            if (this.graalVersion == null) {
                this.graalVersion = graalVersion;
            } else if (!this.graalVersion.equals(graalVersion)) {
                String msg = "GraalVersion should not differ from previous entry for file '" + fileName + "'.";
                LOGGER.warn(msg);
                validationErrors.add(WarningMessage.of(msg, WARNING_LABEL));
            }
            return this;
        }

        public Builder withJdkVersion(String jdkVersion) {
            if (this.jdkVersion == null) {
                this.jdkVersion = jdkVersion;
            } else if (!this.jdkVersion.equals(jdkVersion)) {
                String msg = "JdkVersion should not differ from previous entry for file '" + fileName + "'.";
                LOGGER.warn(msg);
                validationErrors.add(WarningMessage.of(msg, WARNING_LABEL));
            }
            return this;
        }

        public Builder addNextMetricValue(Double metricValue) {
            if (this.metricValue == null) {
                this.metricValue = new ArrayList<>();
            }

            if (metricValue == null) {
                int iteration = this.metricValue.size() + 1;
                String msg = "Metric value is missing for iteration " + iteration + " in file '" + fileName
                        + "'. Defaulting to 0.0.";
                LOGGER.warn(msg);
                validationErrors.add(WarningMessage.of(msg, WARNING_LABEL));
                metricValue = 0d;
            }

            this.metricValue.add(metricValue);
            return this;
        }

        /**
         * Validates if any of the metadata fields were never populate across all queried entries.
         */
        public void validateMissingFields() {
            checkMissing("BenchmarkSuite", benchmarkSuite);
            checkMissing("BenchmarkName", benchmarkName);
            checkMissing("CommitHash", commitHash);
            checkMissing("MachinePlatform", machinePlatform);
            checkMissing("GraalVersion", graalVersion);
            checkMissing("JdkVersion", jdkVersion);
        }

        private void checkMissing(String fieldName, String value) {
            if (value == null) {
                String msg = fieldName + " key is missing entirely for file '" + fileName + "'.";
                LOGGER.warn(msg);
                validationErrors.add(WarningMessage.of(msg, WARNING_LABEL));
            }
        }

        public BenchResultsMetadata build() {
            return new BenchResultsMetadata(
                    benchmarkSuite, benchmarkName, commitHash, machinePlatform, graalVersion, jdkVersion, metricValue);
        }
    }
}
