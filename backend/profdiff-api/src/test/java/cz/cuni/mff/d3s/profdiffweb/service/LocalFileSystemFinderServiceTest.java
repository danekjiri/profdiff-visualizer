package cz.cuni.mff.d3s.profdiffweb.service;

import static org.junit.jupiter.api.Assertions.*;

import cz.cuni.mff.d3s.profdiffweb.model.dto.WorkspaceDirectory;
import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import jakarta.inject.Inject;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@MicronautTest
class LocalFileSystemFinderServiceTest {

    @Inject
    private FileFinderService fileFinderService;

    private Path benchmarksRoot;
    private Path runDirectory;
    private Path malformedRunDirectory;

    @BeforeEach
    void setUp() throws URISyntaxException {
        this.benchmarksRoot = Path.of(
                Objects.requireNonNull(getClass().getResource("/benchmarks")).toURI());
        this.runDirectory = benchmarksRoot.resolve("run_20250506_165951_1");
        this.malformedRunDirectory = benchmarksRoot.resolve("malformed_run");
    }

    @Nested
    class FindProfileAndBenchFilesTests {
        @Test
        void findProfileFile_validDirectory_shouldReturnFile() throws IOException {
            Optional<Path> result = fileFinderService.findProfileFile(runDirectory);

            assertTrue(result.isPresent(), "Expected to find a profiler file, but Optional was empty.");
            assertEquals(
                    "renaissance:scrabble.json", result.get().getFileName().toString(), "File names should match.");
        }

        @Test
        void findProfileFile_invalidDirectory_shouldReturnEmptyOptional() throws IOException {
            Optional<Path> result = fileFinderService.findProfileFile(malformedRunDirectory);

            assertTrue(result.isEmpty(), "Expected an empty Optional, but a file was incorrectly found.");
        }
    }

    @Nested
    class FindLogDirectoryTests {
        @Test
        void findLogDirectory_directoryPresent_shouldReturnLogDirectory() throws IOException {
            Optional<Path> result = fileFinderService.findLogDirectory(runDirectory);

            assertTrue(result.isPresent(), "Expected to find a log directory, but Optional was empty.");
            assertEquals(
                    "renaissance:scrabble_log", result.get().getFileName().toString(), "Directory names should match.");
        }

        @Test
        void findLogDirectory_missingLogDirectory_shouldReturnEmptyOptional() throws IOException {
            Optional<Path> result = fileFinderService.findLogDirectory(malformedRunDirectory);

            assertTrue(result.isEmpty(), "Expected an empty Optional for the missing log directory.");
        }
    }

    @Nested
    class GetMarkedSubdirectoriesTests {
        @Test
        void getMarkedSubdirectories_validParentPath_shouldReturnDirectoriesWithCorrectFlags() {
            Path parentOfBenchmarks = benchmarksRoot.getParent();

            List<WorkspaceDirectory> rootResult =
                    fileFinderService.getMarkedSubdirectories(parentOfBenchmarks.toString());
            assertFalse(rootResult.isEmpty(), "Expected to find subdirectories in the parent root.");

            boolean foundBenchmarksWithRuns =
                    rootResult.stream().anyMatch(dir -> dir.path().endsWith("benchmarks") && dir.hasRuns());
            assertTrue(
                    foundBenchmarksWithRuns,
                    "The 'benchmarks' directory contains run folders, so it should be flagged with hasRuns=true.");

            List<WorkspaceDirectory> childrenResult =
                    fileFinderService.getMarkedSubdirectories(benchmarksRoot.toString());
            assertFalse(childrenResult.isEmpty(), "Expected to find run folders inside benchmarks.");

            long foldersClaimingToHaveRuns =
                    childrenResult.stream().filter(WorkspaceDirectory::hasRuns).count();
            assertEquals(
                    0,
                    foldersClaimingToHaveRuns,
                    "Individual run directories do not contain other runs, so hasRuns should be false.");
        }

        @Test
        void getMarkedSubdirectories_invalidPath_shouldReturnEmptyListAndNotThrow() {
            String invalidPath = "/this/path/absolutely/does/not/exist/12345";

            List<WorkspaceDirectory> result = fileFinderService.getMarkedSubdirectories(invalidPath);

            assertNotNull(result, "Result should not be null.");
            assertTrue(result.isEmpty(), "Expected an empty list for an invalid path.");
        }

        @Test
        void getMarkedSubdirectories_filePath_shouldReturnEmptyList() {
            Path filePath = runDirectory.resolve("renaissance:scrabble.json");

            List<WorkspaceDirectory> result = fileFinderService.getMarkedSubdirectories(filePath.toString());

            assertNotNull(result, "Result should not be null.");
            assertTrue(result.isEmpty(), "Expected an empty list when passing a file path instead of a directory.");
        }
    }
}
