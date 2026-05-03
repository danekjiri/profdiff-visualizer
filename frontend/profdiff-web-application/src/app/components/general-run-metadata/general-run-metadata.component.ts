import { Component, computed, input } from '@angular/core';
import { MatGridListModule } from '@angular/material/grid-list';
import { MatTooltipModule } from '@angular/material/tooltip';
import { RunMetadata } from '../../models/dto/runMetadata';
import { ShowIfTruncatedDirective } from '../../directives/show-if-truncated.directive';
import { BenchResultsMetadata } from '../../models/dto/benchResultsMetadata';

// Helper interface for key-value display.
interface DisplayMetadata {
  label: string;
  value: string;
  url?: string;
}

// Helper interface for grouping metadata in the grid.
interface DisplayMetadataGroup {
  title: string;
  items: DisplayMetadata[];
}

/**
 * GeneralRunMetadataComponent displays a categorized grid of key-value pairs
 * describing the benchmark run (environment, git commit, profiler stats, ...).
 */
@Component({
  selector: 'app-general-run-metadata',
  standalone: true,
  imports: [
    MatGridListModule,
    MatTooltipModule,
    ShowIfTruncatedDirective
  ],
  templateUrl: './general-run-metadata.component.html',
  styleUrl: './general-run-metadata.component.css'
})
export class GeneralRunMetadataComponent {
  // Input for the raw metadata object, which will be transformed into display groups.
  readonly metadata = input<RunMetadata | null>();
  // Input for the number of columns in the grid, allowing parent components to adjust layout.
  readonly cols = input<number>(3);
  readonly warmupIterations = input<number>(0);

  /**
   * Transforms the raw RunMetadata into grouped display objects.
   */
  protected readonly metadataGroups = computed<DisplayMetadataGroup[]>(() => {
    const data = this.metadata();
    if (!data) {
      return [];
    }

    const generalItems: DisplayMetadata[] = [];
    const benchItems: DisplayMetadata[] = [];

    // general stats
    if (data.executionId) generalItems.push({ label: 'Execution ID', value: data.executionId });
    if (data.totalPeriod) generalItems.push({ label: 'Total Periods', value: data.totalPeriod.toLocaleString() });
    if (data.graalPeriod) generalItems.push({ label: 'Graal Period', value: data.graalPeriod.toLocaleString() });
    if (data.compilationUnitsCount) generalItems.push({ label: 'Compilation Units Count', value: data.compilationUnitsCount.toLocaleString() });
    if (data.proftoolMethodsCount) generalItems.push({ label: 'Proftool Methods Count', value: data.proftoolMethodsCount.toLocaleString() });

    // profiler specifics
    if (data.benchmarkRunMetadata?.profileMetadata?.compilationKind) {
      generalItems.push({ label: 'Compilation Kind', value: data.benchmarkRunMetadata.profileMetadata.compilationKind });
    }

    // benchmark environment & configuration
    const benchData = data.benchmarkRunMetadata?.benchResultsMetadata;
    if (benchData) {
      const formattedName = this.formattedBenchmarkName(benchData);
      if (formattedName) {
        benchItems.push({ label: 'Name', value: formattedName });
      }

      if (benchData.metricValues && benchData.metricValues.length > 0) {
        const metrics: number[] = benchData.metricValues;
        const warmupCount = this.warmupIterations();

        if (warmupCount < metrics.length) {
          const peakSlice = metrics.slice(warmupCount);
          const avgPeak = peakSlice.reduce((a: number, b: number) => a + b, 0) / peakSlice.length;
          benchItems.push({
            label: 'Avg Run Time',
            value: `${avgPeak.toFixed(2)}`
          });
        }
      }

      if (benchData.commitHash) {
        const hash = benchData.commitHash;
        const shortHash = hash.substring(0, 7);
        const repoUrl = 'https://github.com/oracle/graal';

        benchItems.push({
          label: 'Commit',
          value: shortHash,
          url: `${repoUrl}/commit/${hash}`
        });
      }

      if (benchData.graalVersion) benchItems.push({ label: 'Graal Version', value: benchData.graalVersion });
      if (benchData.jdkVersion) benchItems.push({ label: 'JDK', value: benchData.jdkVersion });
      if (benchData.machinePlatform) benchItems.push({ label: 'Platform', value: benchData.machinePlatform });
    }

    // construct the final groups
    const groups: DisplayMetadataGroup[] = [];
    if (generalItems.length > 0) {
      groups.push({ title: 'General Metadata', items: generalItems });
    }
    if (benchItems.length > 0) {
      groups.push({ title: 'Benchmark Metadata', items: benchItems });
    }
    return groups;
  });

  private formattedBenchmarkName(metadata: BenchResultsMetadata): string | null {
    if (metadata.benchmarkSuite && metadata.benchmarkName) {
      return `${metadata.benchmarkSuite}:${metadata.benchmarkName}`;
    }
    if (metadata.benchmarkName) {
      return metadata.benchmarkName;
    }
    if (metadata.benchmarkSuite) {
      return metadata.benchmarkSuite;
    }
    return null;
  }

}
