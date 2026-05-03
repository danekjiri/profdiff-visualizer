package cz.cuni.mff.d3s.profdiffweb.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import cz.cuni.mff.d3s.profdiffweb.model.dto.*;
import cz.cuni.mff.d3s.profdiffweb.port.profdiff.ProfdiffPort;
import cz.cuni.mff.d3s.profdiffweb.port.profdiff.ProfdiffProcessingException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RunsServiceImplTest {

    @Mock
    private FileFinderService fileFinder;

    @Mock
    private MetadataParserService metadataParser;

    @Mock
    private ProfdiffPort profdiffPort;

    @InjectMocks
    private RunsServiceImpl runsService;

    private Path mockRootPath;
    private Path mockRunDirPath;
    private String mockRootPathString;
    private HotPolicyOptions mockHotPolicy;
    private ExperimentProcessingOptions mockRenderingOptions;
    private final String runName = "run1";

    @BeforeEach
    void setUp() {
        mockRootPath = Paths.get("/tmp/benchmarks");
        mockRunDirPath = mockRootPath.resolve(runName);
        mockRootPathString = mockRootPath.toString();
        mockHotPolicy = mock(HotPolicyOptions.class);
        mockRenderingOptions = mock(ExperimentProcessingOptions.class);
    }

    private void setupValidPath(MockedStatic<Files> mockedFiles, Path runPath) {
        mockedFiles.when(() -> Files.isDirectory(mockRootPath)).thenReturn(true);
        mockedFiles.when(() -> Files.isDirectory(runPath)).thenReturn(true);
    }

    @Nested
    class ValidationTests {
        @Test
        void getRunsMetadata_nullOrEmptyPath_shouldThrowIllegalArgumentException() {
            assertThrows(
                    IllegalArgumentException.class,
                    () -> runsService.getAllBenchmarksRunsMetadata(null),
                    "Passing a null path should throw IllegalArgumentException.");

            assertThrows(
                    IllegalArgumentException.class,
                    () -> runsService.getAllBenchmarksRunsMetadata(" "),
                    "Passing a blank path should throw IllegalArgumentException.");
        }

        @Test
        void getRunsMetadata_invalidRootDirectory_shouldThrowResourceNotFoundException() {
            try (MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
                mockedFiles.when(() -> Files.isDirectory(mockRootPath)).thenReturn(false);

                assertThrows(
                        ResourceNotFoundException.class,
                        () -> runsService.getAllBenchmarksRunsMetadata(mockRootPathString),
                        "Passing a non-existent directory should throw ResourceNotFoundException.");
            }
        }

        @Test
        void getRunMetadata_invalidRunDirectory_shouldThrowResourceNotFoundException() {
            try (MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
                mockedFiles.when(() -> Files.isDirectory(mockRootPath)).thenReturn(true);
                mockedFiles.when(() -> Files.isDirectory(mockRunDirPath)).thenReturn(false);

                assertThrows(
                        ResourceNotFoundException.class,
                        () -> runsService.getRunMetadata(mockRootPathString, runName),
                        "Passing a non-existent run name should throw ResourceNotFoundException.");
            }
        }

        @Test
        void getRunsMetadata_corruptProfilerJson_shouldKeepRunButAddWarning() throws Exception {
            try (MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
                setupValidPath(mockedFiles, mockRunDirPath);
                mockedFiles.when(() -> Files.list(mockRootPath)).thenReturn(Stream.of(mockRunDirPath));

                var logDir = mockRunDirPath.resolve("optimization_log");
                var profilerFile = mockRunDirPath.resolve("scrabble_prof.json");

                when(fileFinder.findLogDirectory(mockRunDirPath)).thenReturn(Optional.of(logDir));
                when(fileFinder.findProfileFile(mockRunDirPath)).thenReturn(Optional.of(profilerFile));

                when(metadataParser.parseProfilerMetadata(profilerFile))
                        .thenThrow(new RunsFileParsingException("Invalid JSON format"));

                BenchmarkRuns result = runsService.getAllBenchmarksRunsMetadata(mockRootPathString);

                assertEquals(
                        1,
                        result.benchmarkRuns().size(),
                        "Run should not be skipped if only profiler data is corrupt.");
                var runMeta = result.benchmarkRuns().getFirst();
                assertNull(runMeta.profileMetadata(), "Profile metadata should be null due to parsing error.");
                assertTrue(
                        runMeta.warnings().stream().anyMatch(w -> w.type().equals("PROFILER_ERROR")),
                        "Should contain PROFILER_ERROR warning.");
            }
        }
    }

    @Nested
    class GetRunsMetadataTests {

        private void setupMockListFileSystem(MockedStatic<Files> mockedFiles) {
            setupValidPath(mockedFiles, mockRunDirPath);
            mockedFiles.when(() -> Files.list(mockRootPath)).thenReturn(Stream.of(mockRunDirPath));
        }

        @Test
        void getRunsMetadata_withValidRun_shouldReturnCompleteDTO() throws Exception {
            try (MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
                setupMockListFileSystem(mockedFiles);

                var benchFile = mockRunDirPath.resolve(BenchResultsMetadata.EXPECTED_FILE_NAME);
                var profilerFile = mockRunDirPath.resolve("scrabble_prof.json");
                var logDir = mockRunDirPath.resolve("optimization_log");
                mockedFiles.when(() -> Files.exists(benchFile)).thenReturn(true);

                var benchMeta = BenchResultsMetadata.builder(
                                benchFile.getFileName().toString())
                        .withBenchmarkName("bench")
                        .build();
                var parsedBenchResults = new ParsedBenchResults(benchMeta, Collections.emptyList());
                var profilerMeta =
                        ProfileMetadata.builder().withCompilationKind("JIT").build();

                when(fileFinder.findProfileFile(mockRunDirPath)).thenReturn(Optional.of(profilerFile));
                when(fileFinder.findBenchResultsFile(mockRunDirPath)).thenReturn(Optional.of(benchFile));
                when(fileFinder.findLogDirectory(mockRunDirPath)).thenReturn(Optional.of(logDir));

                when(metadataParser.parseBenchResults(benchFile)).thenReturn(parsedBenchResults);
                when(metadataParser.parseProfilerMetadata(profilerFile)).thenReturn(profilerMeta);

                BenchmarkRuns result = runsService.getAllBenchmarksRunsMetadata(mockRootPathString);

                assertEquals(1, result.benchmarkRuns().size(), "Result should contain exactly one run.");
                var dto = result.benchmarkRuns().getFirst();
                assertEquals(runName, dto.runName(), "Run name should match the parsed directory name.");
                assertNotNull(dto.benchResultsMetadata(), "Bench results metadata should be populated.");
                assertNotNull(dto.profileMetadata(), "Profiler metadata should be populated.");
            }
        }

        @Test
        void getRunsMetadata_missingLogDirectory_shouldSkipRun() throws Exception {
            try (MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
                setupMockListFileSystem(mockedFiles);
                when(fileFinder.findLogDirectory(mockRunDirPath)).thenReturn(Optional.empty());

                BenchmarkRuns result = runsService.getAllBenchmarksRunsMetadata(mockRootPathString);

                assertTrue(
                        result.benchmarkRuns().isEmpty(),
                        "Run should be entirely skipped if optimization log directory is missing.");
                assertEquals(
                        1,
                        result.generalWarnings().size(),
                        "A general warning should be recorded for the skipped run.");
            }
        }
    }

    @Nested
    class PortDelegationTests {

        @Test
        void getReportMetadata_validRun_shouldDelegateToPort() {
            try (MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
                setupValidPath(mockedFiles, mockRunDirPath);
                when(fileFinder.findLogDirectory(mockRunDirPath)).thenReturn(Optional.of(mockRunDirPath));

                var mockMetadata = mock(RunMetadata.class);
                when(profdiffPort.getRunMetadata(eq(mockRunDirPath), any())).thenReturn(mockMetadata);

                RunMetadata result = runsService.getRunMetadata(mockRootPathString, runName);
                assertNotNull(result);
                verify(profdiffPort).getRunMetadata(eq(mockRunDirPath), any());
            } catch (IOException | ProfdiffProcessingException | RunsFileParsingException e) {
                fail(e);
            }
        }

        @Test
        void getTopHotMethods_validRun_shouldDelegateToPort() {
            try (MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
                setupValidPath(mockedFiles, mockRunDirPath);

                List<TopMethod> expectedList = List.of(mock(TopMethod.class));
                when(profdiffPort.getTopHotMethods(mockRunDirPath, mockHotPolicy))
                        .thenReturn(expectedList);

                List<TopMethod> result = runsService.getTopHotMethods(mockRootPathString, runName, mockHotPolicy);
                assertEquals(expectedList, result);
                verify(profdiffPort).getTopHotMethods(mockRunDirPath, mockHotPolicy);
            }
        }

        @Test
        void getCompiledMethods_validRun_shouldDelegateToPort() {
            try (MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
                setupValidPath(mockedFiles, mockRunDirPath);

                Collection<JavaMethod> expectedCollection = List.of(mock(JavaMethod.class));
                when(profdiffPort.getCompiledMethods(mockRunDirPath, mockRenderingOptions, mockHotPolicy))
                        .thenReturn(expectedCollection);

                Collection<JavaMethod> result = runsService.getCompiledMethods(
                        mockRootPathString, runName, mockRenderingOptions, mockHotPolicy);

                assertEquals(expectedCollection, result);
                verify(profdiffPort).getCompiledMethods(mockRunDirPath, mockRenderingOptions, mockHotPolicy);
            }
        }

        @Test
        void getCompiledMethodsUnionPairs_validRuns_shouldDelegateToPort() {
            String run2Name = "run2";
            Path mockRun2Path = mockRootPath.resolve(run2Name);

            try (MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
                setupValidPath(mockedFiles, mockRunDirPath);
                setupValidPath(mockedFiles, mockRun2Path);

                Collection<MethodComparisonPair> expectedCollection = List.of(mock(MethodComparisonPair.class));
                when(profdiffPort.getCompiledMethodsUnionPairs(
                                mockRunDirPath, mockRun2Path, mockRenderingOptions, mockHotPolicy))
                        .thenReturn(expectedCollection);

                Collection<MethodComparisonPair> result = runsService.getCompiledMethodsUnionPairs(
                        mockRootPathString, runName, run2Name, mockRenderingOptions, mockHotPolicy);

                assertEquals(expectedCollection, result);
                verify(profdiffPort)
                        .getCompiledMethodsUnionPairs(
                                mockRunDirPath, mockRun2Path, mockRenderingOptions, mockHotPolicy);
            }
        }

        @Test
        void getReportInliningTree_validRun_shouldDelegateToPort() {
            try (MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
                setupValidPath(mockedFiles, mockRunDirPath);

                var expectedTree = mock(TreeResponse.class);
                when(profdiffPort.getReportInliningTree(
                                mockRunDirPath, "methodName", "comp1", mockRenderingOptions, mockHotPolicy))
                        .thenReturn(expectedTree);

                TreeResponse result = runsService.getReportInliningTree(
                        mockRootPathString, runName, "methodName", "comp1", mockRenderingOptions, mockHotPolicy);

                assertEquals(expectedTree, result);
                verify(profdiffPort)
                        .getReportInliningTree(
                                mockRunDirPath, "methodName", "comp1", mockRenderingOptions, mockHotPolicy);
            }
        }

        @Test
        void getReportOptimizationTree_validRun_shouldDelegateToPort() {
            try (MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
                setupValidPath(mockedFiles, mockRunDirPath);

                var expectedTree = mock(TreeResponse.class);
                when(profdiffPort.getReportOptimizationTree(
                                mockRunDirPath, "methodName", "comp1", mockRenderingOptions, mockHotPolicy))
                        .thenReturn(expectedTree);

                TreeResponse result = runsService.getReportOptimizationTree(
                        mockRootPathString, runName, "methodName", "comp1", mockRenderingOptions, mockHotPolicy);

                assertEquals(expectedTree, result);
                verify(profdiffPort)
                        .getReportOptimizationTree(
                                mockRunDirPath, "methodName", "comp1", mockRenderingOptions, mockHotPolicy);
            }
        }

        @Test
        void getComparedInliningTree_bothIdsPresent_shouldDelegateToDiffingPort() {
            String run2Name = "run2";
            Path mockRun2Path = mockRootPath.resolve(run2Name);

            try (MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
                setupValidPath(mockedFiles, mockRunDirPath);
                setupValidPath(mockedFiles, mockRun2Path);

                var expectedTree = mock(TreeResponse.class);
                when(profdiffPort.getComparedInliningTree(
                                mockRunDirPath,
                                mockRun2Path,
                                "methodName",
                                "comp1",
                                "comp2",
                                mockRenderingOptions,
                                mockHotPolicy))
                        .thenReturn(expectedTree);

                TreeResponse result = runsService.getComparedInliningTree(
                        mockRootPathString,
                        runName,
                        run2Name,
                        "methodName",
                        "comp1",
                        "comp2",
                        mockRenderingOptions,
                        mockHotPolicy);

                assertEquals(expectedTree, result);
                verify(profdiffPort)
                        .getComparedInliningTree(
                                mockRunDirPath,
                                mockRun2Path,
                                "methodName",
                                "comp1",
                                "comp2",
                                mockRenderingOptions,
                                mockHotPolicy);
            }
        }

        @Test
        void getComparedInliningTree_missingSecondId_shouldFallbackToSingleReportTree() {
            try (MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
                setupValidPath(mockedFiles, mockRunDirPath);

                var expectedTree = mock(TreeResponse.class);
                when(profdiffPort.getReportInliningTree(
                                mockRunDirPath, "methodName", "comp1", mockRenderingOptions, mockHotPolicy))
                        .thenReturn(expectedTree);

                TreeResponse result = runsService.getComparedInliningTree(
                        mockRootPathString,
                        runName,
                        "run2",
                        "methodName",
                        "comp1",
                        "",
                        mockRenderingOptions,
                        mockHotPolicy);

                assertEquals(expectedTree, result);
                verify(profdiffPort)
                        .getReportInliningTree(
                                mockRunDirPath, "methodName", "comp1", mockRenderingOptions, mockHotPolicy);
                verify(profdiffPort, never()).getComparedInliningTree(any(), any(), any(), any(), any(), any(), any());
            }
        }

        @Test
        void getComparedOptimizationTree_bothIdsPresent_shouldDelegateToDiffingPort() {
            String run2Name = "run2";
            Path mockRun2Path = mockRootPath.resolve(run2Name);

            try (MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
                setupValidPath(mockedFiles, mockRunDirPath);
                setupValidPath(mockedFiles, mockRun2Path);

                var expectedTree = mock(TreeResponse.class);
                when(profdiffPort.getComparedOptimizationTree(
                                mockRunDirPath,
                                mockRun2Path,
                                "methodName",
                                "comp1",
                                "comp2",
                                mockRenderingOptions,
                                mockHotPolicy))
                        .thenReturn(expectedTree);

                TreeResponse result = runsService.getComparedOptimizationTree(
                        mockRootPathString,
                        runName,
                        run2Name,
                        "methodName",
                        "comp1",
                        "comp2",
                        mockRenderingOptions,
                        mockHotPolicy);

                assertEquals(expectedTree, result);
                verify(profdiffPort)
                        .getComparedOptimizationTree(
                                mockRunDirPath,
                                mockRun2Path,
                                "methodName",
                                "comp1",
                                "comp2",
                                mockRenderingOptions,
                                mockHotPolicy);
            }
        }

        @Test
        void getComparedOptimizationTree_missingSecondId_shouldFallbackToSingleReportTree() {
            try (MockedStatic<Files> mockedFiles = mockStatic(Files.class)) {
                setupValidPath(mockedFiles, mockRunDirPath);

                var expectedTree = mock(TreeResponse.class);
                when(profdiffPort.getReportOptimizationTree(
                                mockRunDirPath, "methodName", "comp1", mockRenderingOptions, mockHotPolicy))
                        .thenReturn(expectedTree);

                TreeResponse result = runsService.getComparedOptimizationTree(
                        mockRootPathString,
                        runName,
                        "run2",
                        "methodName",
                        "comp1",
                        null,
                        mockRenderingOptions,
                        mockHotPolicy);

                assertEquals(expectedTree, result);
                verify(profdiffPort)
                        .getReportOptimizationTree(
                                mockRunDirPath, "methodName", "comp1", mockRenderingOptions, mockHotPolicy);
                verify(profdiffPort, never())
                        .getComparedOptimizationTree(any(), any(), any(), any(), any(), any(), any());
            }
        }
    }
}
