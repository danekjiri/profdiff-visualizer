package cz.cuni.mff.d3s.profdiffweb.port.profdiff.internal;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import cz.cuni.mff.d3s.profdiffweb.model.dto.ProfileMetadata;
import cz.cuni.mff.d3s.profdiffweb.port.profdiff.internal.cache.ParsedExperimentCache;
import cz.cuni.mff.d3s.profdiffweb.port.profdiff.model.ExperimentResult;
import cz.cuni.mff.d3s.profdiffweb.service.*;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Optional;
import org.graalvm.profdiff.core.Experiment;
import org.graalvm.profdiff.core.OptionValues;
import org.graalvm.profdiff.parser.ExperimentParser;
import org.graalvm.profdiff.parser.ExperimentParserError;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedConstruction;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ExperimentParserServiceTest {

    @Mock
    private FileFinderService fileFinder;

    @Mock
    private MetadataParserService metadataParser;

    @Mock
    private ParsedExperimentCache cache;

    @InjectMocks
    private ExperimentParserService experimentParserService;

    private final Path runPath = Path.of("/mock/run/path");
    private OptionValues options;

    @BeforeEach
    void setUp() {
        options = mock(OptionValues.class);
    }

    @Nested
    class CacheInteractionTests {
        @Test
        void getParsedExperimentResult_whenInCache_shouldReturnCachedImmediately() throws IOException {
            var cachedResult = mock(ExperimentResult.class);
            when(cache.get(runPath.toString())).thenReturn(Optional.of(cachedResult));

            ExperimentResult result = experimentParserService.getParsedExperimentResult(runPath, options);

            assertEquals(cachedResult, result, "Should return the exact object from the cache.");
            verify(fileFinder, never()).findLogDirectory(any());
        }
    }

    @Nested
    class ParsingTests {
        private final Path logDirPath = runPath.resolve("optimization_log");

        @Test
        void getParsedExperimentResult_missingLogDirectory_shouldThrowException() throws IOException {
            when(cache.get(runPath.toString())).thenReturn(Optional.empty());
            when(fileFinder.findLogDirectory(runPath)).thenReturn(Optional.empty());

            ResourceNotFoundException ex = assertThrows(
                    ResourceNotFoundException.class,
                    () -> experimentParserService.getParsedExperimentResult(runPath, options),
                    "Missing log directory must throw ResourceNotFoundException.");

            assertTrue(
                    ex.getMessage().contains("Optimization log directory not found"),
                    "Error message should mention missing log.");
        }

        @Test
        void getParsedExperimentResult_withValidFiles_shouldParseAndCache() throws Exception {
            Path profilerPath = runPath.resolve("prof.json");
            var mockExperiment = mock(Experiment.class);

            when(cache.get(runPath.toString())).thenReturn(Optional.empty());
            when(fileFinder.findLogDirectory(runPath)).thenReturn(Optional.of(logDirPath));
            when(fileFinder.findProfileFile(runPath)).thenReturn(Optional.of(profilerPath));

            var profilerMeta = ProfileMetadata.builder()
                    .withCompilationKind("JIT")
                    .withTotalPeriod(1000L)
                    .build();
            when(metadataParser.parseProfilerMetadata(profilerPath)).thenReturn(profilerMeta);

            try (MockedConstruction<ExperimentParser> mockedParser =
                    mockConstruction(ExperimentParser.class, (mock, context) -> {
                        when(mock.parse()).thenReturn(mockExperiment);
                    })) {
                ExperimentResult result = experimentParserService.getParsedExperimentResult(runPath, options);

                assertNotNull(result, "Result should not be null.");
                assertEquals(
                        mockExperiment, result.experiment(), "The returned experiment should match the parsed one.");
                verify(cache).put(eq(runPath.toString()), eq(result));
            }
        }

        @Test
        void getParsedExperimentResult_corruptProfilerFile_shouldGracefullyFallbackToNullKind() throws Exception {
            Path profilerPath = runPath.resolve("prof.json");
            var mockExperiment = mock(Experiment.class);

            when(cache.get(runPath.toString())).thenReturn(Optional.empty());
            when(fileFinder.findLogDirectory(runPath)).thenReturn(Optional.of(logDirPath));
            when(fileFinder.findProfileFile(runPath)).thenReturn(Optional.of(profilerPath));

            when(metadataParser.parseProfilerMetadata(profilerPath))
                    .thenThrow(new RunsFileParsingException("Corrupt file"));

            try (MockedConstruction<ExperimentParser> mockedParser =
                    mockConstruction(ExperimentParser.class, (mock, context) -> {
                        when(mock.parse()).thenReturn(mockExperiment);
                    })) {
                assertDoesNotThrow(
                        () -> experimentParserService.getParsedExperimentResult(runPath, options),
                        "Corrupt profiler metadata should not crash the entire experiment parsing.");
            }
        }

        @Test
        void getParsedExperimentResult_parserThrowsFatalError_shouldWrapInRunsDirectoryException() throws Exception {
            when(cache.get(runPath.toString())).thenReturn(Optional.empty());
            when(fileFinder.findLogDirectory(runPath)).thenReturn(Optional.of(logDirPath));
            when(fileFinder.findProfileFile(runPath)).thenReturn(Optional.empty());

            try (MockedConstruction<ExperimentParser> mockedParser =
                         mockConstruction(ExperimentParser.class, (mock, context) -> {
                             ExperimentParserError parserError = mock(ExperimentParserError.class);
                             when(parserError.getMessage()).thenReturn("Simulated internal parser crash");

                             when(mock.parse()).thenThrow(parserError);
                         })) {
                RunsFileParsingException ex = assertThrows(
                        RunsFileParsingException.class,
                        () -> experimentParserService.getParsedExperimentResult(runPath, options),
                        "Fatal Profdiff parser error should be wrapped in RunsFileParsingException.");

                assertNotNull(ex.getMessage(), "Exception should contain a message.");
                assertTrue(ex.getMessage().contains("Simulated internal parser crash"));
            }
        }
    }
}
