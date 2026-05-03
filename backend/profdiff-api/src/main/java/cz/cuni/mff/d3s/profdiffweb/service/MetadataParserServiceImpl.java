package cz.cuni.mff.d3s.profdiffweb.service;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import cz.cuni.mff.d3s.profdiffweb.model.BenchResultsQueriesEntry;
import cz.cuni.mff.d3s.profdiffweb.model.dto.BenchResultsMetadata;
import cz.cuni.mff.d3s.profdiffweb.model.dto.ParsedBenchResults;
import cz.cuni.mff.d3s.profdiffweb.model.dto.ProfileMetadata;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Service implementation for parsing metadata from benchmark and profiler files.
 *
 * <p>Provides methods to parse benchmark results and profiler metadata from specified file paths.
 */
@Singleton
public class MetadataParserServiceImpl implements MetadataParserService {

    private final ObjectMapper objectMapper;
    private final JsonFactory jsonFactory;

    @Inject
    public MetadataParserServiceImpl(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.jsonFactory = objectMapper.getFactory();
    }

    @Override
    public ParsedBenchResults parseBenchResults(Path filePath) throws RunsFileParsingException {
        try (JsonParser parser = jsonFactory.createParser(filePath.toFile())) {
            while (parser.nextToken() != null) {
                if (JsonToken.FIELD_NAME.equals(parser.currentToken())
                        && BenchResultsMetadata.EXPECTED_FIRST_JSON_KEY.equals(parser.getText())) {
                    parser.nextToken();
                    return extractBenchResultsMetadata(parser, filePath);
                }
            }
            throw new RunsFileParsingException("Unexpected JSON structure in file: " + filePath);
        } catch (IOException e) {
            throw new RunsFileParsingException("Failed to parse benchmark results from file '" + filePath + "'", e);
        }
    }

    @Override
    public ProfileMetadata parseProfilerMetadata(Path filePath) throws RunsFileParsingException {
        var profilerMetadataBuilder = ProfileMetadata.builder();
        int keysFound = 0;

        try (JsonParser parser = jsonFactory.createParser(filePath.toFile())) {
            while (parser.nextToken() != null && keysFound < 2) {
                if (parser.currentToken() == JsonToken.FIELD_NAME) {
                    String fieldName = parser.currentName();
                    parser.nextToken();

                    switch (fieldName) {
                        case "compilationKind":
                            profilerMetadataBuilder.withCompilationKind(parser.getText());
                            keysFound++;
                            break;
                        case "totalPeriod":
                            if (parser.currentToken() != JsonToken.VALUE_NULL) {
                                profilerMetadataBuilder.withTotalPeriod(parser.getLongValue());
                            }
                            keysFound++;
                            break;
                        default:
                            parser.skipChildren();
                            break;
                    }
                }
            }
        } catch (IOException e) {
            throw new RunsFileParsingException("Failed to parse profiler metadata from file '" + filePath + "'", e);
        }
        return profilerMetadataBuilder.build();
    }

    private ParsedBenchResults extractBenchResultsMetadata(JsonParser parser, Path filePath)
            throws IOException, RunsFileParsingException {
        List<BenchResultsQueriesEntry> runs = objectMapper.readValue(
                parser,
                objectMapper.getTypeFactory().constructCollectionType(List.class, BenchResultsQueriesEntry.class));

        var metadataBuilder =
                BenchResultsMetadata.builder(filePath.getFileName().toString());
        for (BenchResultsQueriesEntry run : runs) {
            metadataBuilder
                    .withBenchmarkSuite(run.benchmarkSuite())
                    .withBenchmarkName(run.benchmarkName())
                    .withCommitHash(run.commitHash())
                    .withMachinePlatform(run.machinePlatform())
                    .withGraalVersion(run.graalVersion())
                    .withJdkVersion(run.jdkVersion())
                    .addNextMetricValue(run.metricValue());
        }
        metadataBuilder.validateMissingFields();

        return new ParsedBenchResults(metadataBuilder.build(), metadataBuilder.getValidationErrors());
    }
}
