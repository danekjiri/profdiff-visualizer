package cz.cuni.mff.d3s.profdiffweb.model.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.micronaut.core.annotation.Introspected;
import io.swagger.v3.oas.annotations.media.Schema;

/**
 * Metadata for profiler file - a file starting with {@link #EXPECTED_FIRST_JSON_KEY}.
 *
 * <p>It is used to parse {@code compilationKind} and {@code totalPeriod} values from given profiler
 * file.
 *
 * @param compilationKind Kind of compilation for Java source code used by GraalVM compiler.
 * @param totalPeriod Total time sampling period of the profiler run.
 */
@Introspected
@JsonIgnoreProperties(ignoreUnknown = true)
public record ProfileMetadata(
        @JsonProperty("compilationKind")
                @Schema(
                        description = "Kind of compilation for Java source code used by GraalVM compiler.",
                        example = "JIT",
                        requiredMode = Schema.RequiredMode.NOT_REQUIRED)
                String compilationKind,
        @JsonProperty("totalPeriod")
                @Schema(
                        description = "Total time sampling period of the profiler run.",
                        example = "355173222694",
                        requiredMode = Schema.RequiredMode.NOT_REQUIRED)
                Long totalPeriod) {

    /// Expected firsts field name of the profiler JSON file.
    public static final String EXPECTED_FIRST_JSON_KEY = "compilationKind";
    public static final String EXPECTED_SECOND_JSON_KEY = "totalPeriod";
    public static final String EXPECTED_THIRD_OR_FORTH_JSON_KEY = "code";

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private String compilationKind;
        private long totalPeriod;

        private Builder() {}

        public Builder withCompilationKind(String compilationKind) {
            this.compilationKind = compilationKind;
            return this;
        }

        public Builder withTotalPeriod(long totalPeriod) {
            this.totalPeriod = totalPeriod;
            return this;
        }

        public ProfileMetadata build() {
            return new ProfileMetadata(this.compilationKind, this.totalPeriod);
        }
    }
}
