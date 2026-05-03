import { SelectionModel } from '@angular/cdk/collections';
import { CommonModule } from '@angular/common';
import { Component, Input, ViewChild, output, OnInit } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatChip, MatChipsModule } from '@angular/material/chips';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatPaginator, MatPaginatorModule } from '@angular/material/paginator';
import { MatSelectModule } from '@angular/material/select';
import { MatSort, MatSortModule } from '@angular/material/sort';
import { MatOptionModule } from '@angular/material/core';
import { MatTableDataSource, MatTableModule } from '@angular/material/table';
import { MatTooltipModule } from '@angular/material/tooltip';
import { BenchmarkRunMetadata } from '../../models/dto/benchmarkRunMetadata';

/**
 * RunsTableComponent displays a sortable, paginated list of benchmark runs.
 * It allows users to select runs for comparison/charting and navigate to detailed reports.
 */
@Component({
  selector: 'app-runs-table',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatSelectModule,
    MatTableModule,
    MatPaginatorModule,
    MatCheckboxModule,
    MatSortModule,
    MatFormFieldModule,
    MatOptionModule,
    MatInputModule,
    MatIconModule,
    MatTooltipModule,
    MatChip,
    MatChipsModule,
    MatButtonModule
  ],
  templateUrl: './runs-table.component.html',
  styleUrls: ['./runs-table.component.css']
})
export class RunsTableComponent implements OnInit {
  private _warmupIterations: number = 0;

  // Output event to signal when a user clicks on 'Report' for a specific run, navigating to Report view.
  readonly report = output<BenchmarkRunMetadata>();

  // Accessed by HomeComponent via @ViewChild to sync charts.
  public readonly selection = new SelectionModel<BenchmarkRunMetadata>(true, []);

  // Table configuration.
  protected readonly displayedColumns: string[] = [
    'select',
    'report',
    'status',
    'runName',
    'benchmark',
    'compilationKind',
    'graalVersion',
    'totalPeriod',
    'averageTime'
  ];
  // Data source for the table, set via the 'runs' input setter.
  protected readonly dataSource = new MatTableDataSource<BenchmarkRunMetadata>();

  // Constants for filter options and display.
  protected readonly EMPTY_LABEL = '(Empty)';
  protected readonly statusOptions = ['Healthy', 'Missing profile', 'No bench-results.json'];

  // Unique values for filters, extracted from the input data to populate filter dropdowns.
  protected uniqueBenchmarks: string[] = [];
  protected uniqueCompilations: string[] = [];
  protected uniqueGraalVersions: string[] = [];

  // State object to track currently selected filters
  protected columnFilters = {
    benchmark: [] as string[],
    compilationKind: [] as string[],
    graalVersion: [] as string[],
    status: [...this.statusOptions]
  };

  // Internal references to MatTable; features supported by Angular Materials.
  private _paginator!: MatPaginator;
  @ViewChild(MatPaginator) set paginator(paginator: MatPaginator) {
    this._paginator = paginator;
    this.dataSource.paginator = this._paginator;
  }

  // Sorting setup with custom sortingDataAccessor to handle nested properties.
  private _sort!: MatSort;
  @ViewChild(MatSort) set sort(sort: MatSort) {
    this._sort = sort;
    this.dataSource.sort = this._sort;
  }

  /**
   * Configure sorting and filtering logic for the table.
   * Sorting: Custom sortingDataAccessor to sort by nested properties alphabetically, but totalPeriod numerically.
   * Filtering: Custom filterPredicate that checks if each row matches the selected filter criteria extracted from actual values.
   */
  ngOnInit(): void {
    // sorting
    this.dataSource.sortingDataAccessor = (item, property) => {
      switch (property) {
        case 'runName': return item.runName;
        case 'benchmark': return this.getBenchmarkName(item);
        case 'compilationKind': return item.profileMetadata?.compilationKind || '';
        case 'graalVersion': return item.benchResultsMetadata?.graalVersion || '';
        case 'totalPeriod': return item.profileMetadata?.totalPeriod || 0;
        case 'averageTime': return this.calculateAverageTime(item) || 0;
        default: return (item as any)[property];
      }
    };

    // filter
    this.dataSource.filterPredicate = (data: BenchmarkRunMetadata, filterString: string) => {
      const filters = JSON.parse(filterString);

      const benchmarkName = this.getBenchmarkName(data) || this.EMPTY_LABEL;
      const compilationKind = data.profileMetadata?.compilationKind || this.EMPTY_LABEL;
      const graalVersion = data.benchResultsMetadata?.graalVersion || this.EMPTY_LABEL;

      const matchBenchmark = filters.benchmark.length === 0 || filters.benchmark.includes(benchmarkName);
      const matchCompilation = filters.compilationKind.length === 0 || filters.compilationKind.includes(compilationKind);
      const matchGraal = filters.graalVersion.length === 0 || filters.graalVersion.includes(graalVersion);

      const isMissingProfile = !this.hasProfilerInfo(data);
      const isMissingBenchResults = !this.hasBenchResultsInfo(data);
      const isHealthyRun = !isMissingProfile && !isMissingBenchResults;

      const wantsHealthy = filters.status.includes(this.statusOptions[0]);
      const wantsMissingProfile = filters.status.includes(this.statusOptions[1]);
      const wantsMissingBenchResults = filters.status.includes(this.statusOptions[2]);

      // enforce strict matching logic across all status toggles
      const matchStatus = filters.status.length === 0 ||
                          (isHealthyRun && wantsHealthy) ||
                          (isMissingProfile && wantsMissingProfile) ||
                          (isMissingBenchResults && wantsMissingBenchResults);

      return matchBenchmark && matchCompilation && matchGraal && matchStatus;
    };
  }

  /**
   * Input setter to update table data.
   */
  @Input()
  set runs(data: BenchmarkRunMetadata[]) {
    this.dataSource.data = data;
    this.extractUniqueFilterValues(data);
    this.applyFilters();
  }

  @Input()
  set warmupIterations(val: number) {
    this._warmupIterations = val;
    // force a re-sort if the user is actively sorting by Average Time
    if (this._sort?.active === 'averageTime') {
      this.dataSource.data = this.dataSource.data;
    }
  }

  get warmupIterations(): number {
    return this._warmupIterations;
  }

  /**
   * Calculates the average run time, dropping the first 'n' warmup iterations.
   */
  protected calculateAverageTime(run: BenchmarkRunMetadata): number | null {
    const metrics = run.benchResultsMetadata?.metricValues;
    if (!metrics || metrics.length === 0) {
      return null;
    }

    // ensure to maintain at least one iteration
    const startIndex = Math.min(this.warmupIterations, metrics.length - 1);
    const validMetrics = metrics.slice(startIndex);

    const sum = validMetrics.reduce((acc, val) => acc + val, 0);
    return sum / validMetrics.length;
  }

  /**
   * Forces the table data source to re-sort when the slider changes
   */
  protected onWarmupChange(): void {
    if (this._sort?.active === 'averageTime') {
        this.dataSource.data = this.dataSource.data;
    }
  }

  /**
   * Extracts unique values for benchmarks, compilation kinds, and Graal versions from the input data to populate filter dropdowns.
   */
  private extractUniqueFilterValues(data: BenchmarkRunMetadata[]): void {
    const benchmarks = new Set<string>();
    const compilations = new Set<string>();
    const graals = new Set<string>();

    data.forEach(run => {
      benchmarks.add(this.getBenchmarkName(run) || this.EMPTY_LABEL);
      compilations.add(run.profileMetadata?.compilationKind || this.EMPTY_LABEL);
      graals.add(run.benchResultsMetadata?.graalVersion || this.EMPTY_LABEL);
    });

    this.uniqueBenchmarks = Array.from(benchmarks).sort();
    this.uniqueCompilations = Array.from(compilations).sort();
    this.uniqueGraalVersions = Array.from(graals).sort();
  }

  protected applyFilters(): void {
    this.dataSource.filter = JSON.stringify(this.columnFilters);
    if (this.dataSource.paginator) {
      this.dataSource.paginator.firstPage();
    }
  }

  /**
   * Emits the report event for a selected run, preventing row selection click propagation.
   */
  protected generateReport(run: BenchmarkRunMetadata, event: MouseEvent): void {
    event.stopPropagation();
    this.report.emit(run);
  }

  /**
   * Formats the benchmark name, prepending the suite if available.
   */
  protected getBenchmarkName(run: BenchmarkRunMetadata): string {
    if (!run?.benchResultsMetadata) {
      return '';
    }

    const { benchmarkName, benchmarkSuite } = run.benchResultsMetadata;
    if (benchmarkName) {
      return benchmarkSuite ? `${benchmarkSuite}:${benchmarkName}` : benchmarkName;
    }
    return benchmarkSuite || '';
  }

  /**
   * Checks if profile metadata exists (for showing 'No profile' warning).
   */
  protected hasProfilerInfo(run: BenchmarkRunMetadata): boolean {
    return !!run?.profileMetadata;
  }

  /**
   * Checks if benchmark results exist (for showing 'No bench-results' warning).
   */
  protected hasBenchResultsInfo(run: BenchmarkRunMetadata): boolean {
    return !!run?.benchResultsMetadata;
  }

  /**
   * Check if all currently filtered (visible) rows are selected.
   */
  protected isAllSelected(): boolean {
    const filteredData = this.dataSource.filteredData;
    if (filteredData.length === 0) return false;

    return filteredData.every(row => this.selection.isSelected(row));
  }

  /**
   * Check if any currently filtered (visible) rows are selected.
   */
  protected isSomeSelected(): boolean {
    return this.dataSource.filteredData.some(row => this.selection.isSelected(row));
  }

  /**
   * Selects all filtered rows if they are not all selected; otherwise deselects them.
   */
  protected toggleAllRows(): void {
    const filteredData = this.dataSource.filteredData;

    if (this.isAllSelected()) {
      filteredData.forEach(row => this.selection.deselect(row));
    } else {
      filteredData.forEach(row => this.selection.select(row));
    }
  }
}
