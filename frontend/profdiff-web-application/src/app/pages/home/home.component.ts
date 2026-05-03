import { CommonModule } from '@angular/common';
import { Component, effect, inject, OnInit, signal, viewChild, untracked } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatAutocompleteModule } from '@angular/material/autocomplete';
import { MatAutocompleteTrigger } from '@angular/material/autocomplete';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIcon } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';
import { MatSliderModule } from '@angular/material/slider';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTooltip } from '@angular/material/tooltip';
import { ActivatedRoute, Router } from '@angular/router';
import { catchError, finalize, of, Subscription } from 'rxjs';
import { ErrorMessageComponent } from '../../components/error-message/error-message.component';
import { FloatingDockComponent } from '../../components/floating-dock/floating-dock.component';
import { MetricChartComponent } from '../../components/metric-chart/metric-chart.component';
import { RunsTableComponent } from '../../components/runs-table/runs-table.component';
import { ViewHeaderComponent } from '../../components/view-header/view-header.component';
import { WarningsComponent } from '../../components/warnings/warnings.component';
import { BenchmarkRunMetadata } from '../../models/dto/benchmarkRunMetadata';
import { BenchmarkRuns } from '../../models/dto/benchmarkRuns';
import { ErrorMessage } from '../../models/dto/errorMessage';
import { ChartSeries, ChartFactory } from '../../models/chart-series';
import { RunsService } from '../../services/runs.service';
import { WorkspaceDirectory } from '../../models/dto/workspaceDirectory';

/**
 * HomeComponent is the main entry point.
 * Handles fetching run data, table selection synchronization, and navigation to report or comparison views.
 */
@Component({
  selector: 'app-home',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    RunsTableComponent,
    ErrorMessageComponent,
    ViewHeaderComponent,
    MatAutocompleteModule,
    MatAutocompleteTrigger,
    MatFormFieldModule,
    MatInputModule,
    MatButtonModule,
    MatProgressSpinnerModule,
    MatSliderModule,
    MatSnackBarModule,
    MatTooltip,
    MetricChartComponent,
    MatIcon,
    FloatingDockComponent,
    WarningsComponent,
  ],
  templateUrl: './home.component.html',
  styleUrl: './home.component.css'
})
export class HomeComponent implements OnInit {
  // Services and router dependencies.
  private readonly runsService = inject(RunsService);
  private readonly router = inject(Router);
  private readonly route = inject(ActivatedRoute);
  private readonly snackBar = inject(MatSnackBar);

  // Subscription reference for the active runs loading request, allowing cancellation on new requests or component destruction.
  private runsSubscription?: Subscription;

  // Signal for the root directory path input by the user or from startup config.
  protected readonly path = signal('');
  // Signal to hold the autocomplete suggestions.
  protected readonly directorySuggestions = signal<WorkspaceDirectory[]>([]);
  // Tracks the current Tab-cycle position within directorySuggestions (-1 = no cycle active yet).
  protected readonly tabCycleIndex = signal(-1);
  // Signal holding the loaded runs data, which is set after fetching from the backend.
  protected readonly runs = signal<BenchmarkRuns | null>(null);
  // Signal for any error messages to display in the UI.
  protected readonly errorMessage = signal<ErrorMessage | null>(null);
  // Signal for managing the home page state depending on loading...
  protected readonly isLoading = signal(true);
  // Reference to the runs table component to access its selection.
  protected readonly runsTable = viewChild(RunsTableComponent);
  // Chart data synchronized with table selection.
  protected readonly chartData = signal<ChartSeries[]>([]);

  // Tracks how many warmup iterations to skip based on the slider input.
  protected warmupIterations = 0;
  // Maximum number of iterations available across all runs, used to set the slider's max value.
  protected maxIterations = 0;

  constructor() {
    // automatically syncs chart data when table selection changes (effect due to async table initialization)
    effect((onCleanup) => {
      const table = this.runsTable();
      if (table) {
        const sub = table.selection.changed.subscribe(() => {
          untracked(() => this.updateChartData());
        });
        // initial sync
        untracked(() => this.updateChartData());

        // when the table is destroyed or effect re-runs
        onCleanup(() => {
          sub.unsubscribe();
        });
      }
    });
  }

  /**
   * Fetches startup configuration and loads initial data if a startup path is provided.
   */
  ngOnInit(): void {
    const queryPath = this.route.snapshot.queryParamMap.get('path');

    if (queryPath) {
      this.path.set(queryPath);
      this.loadRuns();
      return;
    }
    this.isLoading.set(false);
  }

  /**
   * Loads runs from the specified path.
   * If the path is not set or an error occurs, it shows an error message.
   */
  protected loadRuns(): void {
    if (!this.path()) {
      this.errorMessage.set({ message: 'Specify the path to the root directory.' });
      this.isLoading.set(false);
      return;
    }

    // cancel previous request if active
    if (this.runsSubscription) {
      this.runsSubscription.unsubscribe();
    }

    // reset UI state before fetching
    this.isLoading.set(true);
    this.errorMessage.set(null);
    this.runs.set(null);
    this.warmupIterations = 0;

    this.runsSubscription = this.runsService
      .getRuns(this.path())
      .pipe(
        catchError((err) => {
          const message = err.error?.message || 'Error loading runs.';
          this.errorMessage.set({ message });
          return of(null);
        }),
        finalize(() => this.isLoading.set(false))
      )
      .subscribe((data) => {
        this.runs.set(data);

        // determine max iterations for slider based on the longest metric values array among the runs
        if (data?.benchmarkRuns) {
          this.maxIterations = data.benchmarkRuns.reduce((max, run) => {
            const length = run.benchResultsMetadata?.metricValues?.length || 0;
            return Math.max(max, Math.max(0, length - 1));
          }, 0);
        } else {
          this.maxIterations = 0;
        }
      });
  }

  /**
   * Called whenever the user types in the path input.
   * Extracts the base directory, asks the backend for its contents, and updates the autocomplete.
   */
  protected onPathChange(newPath: string): void {
    this.path.set(newPath);
    this.tabCycleIndex.set(-1);

    // find the last slash to isolate the parent directory the user is currently browsing
    const lastSlashIndex = Math.max(newPath.lastIndexOf('/'), newPath.lastIndexOf('\\'));
    
    if (lastSlashIndex >= 0) {
      const parentDir = newPath.substring(0, lastSlashIndex + 1);
      
      this.runsService.getDirectories(parentDir).subscribe((dirs) => {
        // filter the results so it only shows folders matching what they typed so far
        const filtered = dirs.filter(dir => dir.path.toLowerCase().includes(newPath.toLowerCase()));
        this.directorySuggestions.set(filtered);
      });
    } else {
      this.directorySuggestions.set([]);
    }
  }

  /**
   * Navigate to the single run report page.
   */
  protected doReport(run: BenchmarkRunMetadata) {
    if (!run) return;

    this.router.navigate(['/report'], {
      queryParams: {
        path: this.path(),
        runName: run.runName,
        warmup: this.warmupIterations
      }
    });
  }

  /**
   * Navigate to the comparison page if exactly two runs are selected.
   */
  protected doCompare() {
    const selected = this.runsTable()!.selection.selected;
    if (selected.length !== 2) {
      return;
    }

    this.router.navigate(['/compare'], {
      queryParams: {
        path: this.path(),
        runName1: selected[0].runName,
        runName2: selected[1].runName,
        warmup: this.warmupIterations
      }
    });
  }

  /**
   * Handles the tab key to provide path auto-completion in path input.
   */
  protected onTab(event: Event, trigger: MatAutocompleteTrigger): void {
    const suggestions = this.directorySuggestions();
    if (suggestions.length === 0) return;

    event.preventDefault();
    const nextIndex = (this.tabCycleIndex() + 1) % suggestions.length;
    this.tabCycleIndex.set(nextIndex);
    this.path.set(suggestions[nextIndex].path);

    // keep the suggestion panel open
    setTimeout(() => trigger.openPanel());
  }

  /**
   * Handles the enter key in the path input hint.
   */
  protected onEnter(event: Event, trigger: MatAutocompleteTrigger): void {
    event.preventDefault();
    trigger.closePanel();
    this.loadRuns();
  }

  /**
   * Called on every slider change to update the table data. 
   */
  protected onWarmupChange(): void {
    this.updateChartData();
  }

  /**
   * Clears the backend cache for the current path, then triggers a fresh load.
   */
  protected refreshRuns(): void {
    if (!this.path()) return;
    
    this.isLoading.set(true);
    
    this.runsService.refreshRunsMetadata(this.path()).subscribe({
      next: () => this.loadRuns(),
      error: () => {
        this.snackBar.open(
          `Failed to force refresh. Please wait 1 minute for the cache to expire, or restart the backend to see changes.`,
          'Dismiss',
          {
            duration: 10_000,
            horizontalPosition: 'center',
            verticalPosition: 'bottom',
            panelClass: ['snackbar-error']
          }
        );
        this.loadRuns();
      }
    });
  }

  /**
   * Transform selected table rows into chart-compatible series data.
   */
  private updateChartData(): void {
    const selected = this.runsTable()?.selection.selected ?? [];

    const newChartData = selected
      .map(run => ChartFactory.from(
        run.runName,
        run.benchResultsMetadata?.metricValues
      ))
      // filter out nulls (runs that had no valid metric data)
      .filter((item): item is ChartSeries => item !== null);

    this.chartData.set(newChartData);
  }
}
