import { CommonModule } from '@angular/common';
import { Component, computed, inject, OnInit, OnDestroy, signal, ViewChild, ElementRef, WritableSignal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatIcon } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatSlideToggleModule } from '@angular/material/slide-toggle';
import { MatTab, MatTabGroup } from '@angular/material/tabs';
import { MatTooltip } from "@angular/material/tooltip";
import { RouterLink, ActivatedRoute, Router } from '@angular/router';
import { forkJoin, of, Subscription } from 'rxjs';
import { catchError, switchMap, tap, finalize } from 'rxjs/operators';
import { ErrorMessageComponent } from '../../components/error-message/error-message.component';
import { DEFAULT_EXPERIMENT_PROCESSING_OPTIONS, DEFAULT_HOT_POLICY_OPTIONS, ExperimentOptionsComponent } from '../../components/experiment-options/experiment-options.component';
import { CompilationUnitCardComponent } from '../../components/compilation-unit-card/compilation-unit-card.component';
import { FloatingDockComponent } from '../../components/floating-dock/floating-dock.component';
import { GeneralRunMetadataComponent } from '../../components/general-run-metadata/general-run-metadata.component';
import { MethodSelectorComponent, MethodSelectItem } from '../../components/method-selector/method-selector.component';
import { MetricChartComponent } from '../../components/metric-chart/metric-chart.component';
import { RenderedTreeNodeTextComponent, TreeFormatter } from '../../components/rendered-tree-node/rendered-tree-node-text.component';
import { TopMethodsTableComponent } from '../../components/top-methods-table/top-methods-table.component';
import { ViewHeaderComponent } from '../../components/view-header/view-header.component';
import { WarningsComponent } from '../../components/warnings/warnings.component';
import { ErrorMessage } from '../../models/dto/errorMessage';
import { ExperimentProcessingOptions } from '../../models/experiment-processing-options';
import { HotPolicyOptions } from '../../models/hot-policy-options';
import { JavaMethod } from '../../models/dto/javaMethod';
import { MethodComparisonPair } from '../../models/dto/methodComparisonPair';
import { RunMetadata } from '../../models/dto/runMetadata';
import { TopMethod } from '../../models/dto/topMethod';
import { TreeResponse } from '../../models/dto/treeResponse';
import { WarningMessage } from '../../models/dto/warningMessage';
import { ChartFactory, ChartSeries } from '../../models/chart-series';
import { RunsService } from '../../services/runs.service';
import { ColoredTopMethod, generateMethodColorMap, applyColorMap } from '../../models/top-methods-colors';

/**
 * Enhanced with calculated hotness value for sorting.
 */
type DiffPairWithHotness = MethodComparisonPair & {
  hotness: number;
};

/**
 * State per each compared run.
 */
interface RunState {
  name: WritableSignal<string | null>;
  metadata: WritableSignal<RunMetadata | null>;
  topMethods: WritableSignal<TopMethod[]>;
  selectedCompilationId: WritableSignal<string | null>;
}

/**
 * CompareComponent orchestrates the side-by-side comparison of two benchmark runs.
 * It loads metadata for both runs, identifies common compiled methods, and visualizes the differences in compilation trees.
 */
@Component({
  selector: 'app-compare',
  standalone: true,
  imports: [
    CommonModule,
    ViewHeaderComponent,
    CompilationUnitCardComponent,
    ErrorMessageComponent,
    GeneralRunMetadataComponent,
    MatCardModule,
    TopMethodsTableComponent,
    MatExpansionModule,
    MethodSelectorComponent,
    MatTab,
    MatTabGroup,
    MatSnackBarModule,
    MatSlideToggleModule,
    MetricChartComponent,
    FloatingDockComponent,
    ViewHeaderComponent,
    ExperimentOptionsComponent,
    MatButtonModule,
    MatIcon,
    WarningsComponent,
    RenderedTreeNodeTextComponent,
    RouterLink,
    MatTooltip
  ],
  templateUrl: './compare.component.html',
  styleUrls: ['./compare.component.css']
})
export class CompareComponent implements OnInit, OnDestroy {
  // Services and route injection.
  private readonly runsService = inject(RunsService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly snackBar = inject(MatSnackBar);

  // Access to child component for retrieving user-selected options.
  @ViewChild(ExperimentOptionsComponent) protected optionsComponent!: ExperimentOptionsComponent;
  @ViewChild('methodsDetail', { read: ElementRef }) private methodsDetailRef!: ElementRef;

  // Subscription management for request cancellation on component destroy or new selection.
  private dataLoadSubscription?: Subscription;
  private treeLoadSubscription?: Subscription;

  // Signals for managing UI state.
  protected readonly isLoading = signal(true);
  protected readonly errorMessage = signal<ErrorMessage | null>(null);
  protected readonly isProfilerAvailable = signal<boolean>(false);
  protected readonly isLoadingTrees = signal<boolean>(false);
  // Signal to track the current path parameter for data loading.
  protected readonly path = signal<string | null>(null);
  // Signal to track the number of warmup iterations.
  protected readonly warmupIterations = signal<number>(0);
  // Signal to track the currently hovered method for UI top-methods table highlighting.
  protected readonly hoveredMethodName = signal<string | null>(null);
  // Pairs of methods from both runs with calculated hotness for sorting and display in the selector.
  protected readonly comparisonPairs = signal<DiffPairWithHotness[]>([]);
  // Signal for the currently selected method pair, which drives the displayed trees and details.
  protected readonly selectedDiffPair = signal<DiffPairWithHotness | null>(null);
  // Signals for the compared trees of the selected method pair.
  protected readonly inliningTree = signal<TreeResponse | null>(null);
  protected readonly optimizationTree = signal<TreeResponse | null>(null);
  protected readonly optimizationContextTree = signal<TreeResponse | null>(null);
  // Signal to track the active tab index for tree view switching. (0 = Inlining, 1 = Optimization, 2 = Optimization Context)
  protected readonly activeTabIndex = signal<number>(0);
  // State for both runs that simplifies and unifies methods access in the template and logic.
  protected readonly runs: RunState[] = [
    {
      name: signal(null),
      metadata: signal(null),
      topMethods: signal([]),
      selectedCompilationId: signal(null)
    },
    {
      name: signal(null),
      metadata: signal(null),
      topMethods: signal([]),
      selectedCompilationId: signal(null)
    }
  ];
  // Computed chart data based on metadata from both runs.
  protected readonly chartData = computed<ChartSeries[]>(() => {
    return this.runs
      .map((run) => {
        const meta = run.metadata();
        const metrics = meta?.benchmarkRunMetadata.benchResultsMetadata?.metricValues;

        const fallbackName = `Run ${this.runs.indexOf(run) + 1}`;
        return ChartFactory.from(run.name() ?? fallbackName, metrics);
      })
      .filter((item): item is ChartSeries => item !== null);
  });
  // Computed signal to adapt JavaMethod[] -> MethodSelectItem[] for the method-selector component.
  protected readonly diffSelectorItems = computed<MethodSelectItem<DiffPairWithHotness>[]>(() => {
    return this.comparisonPairs().map((pair) => ({
      name: pair.methodName,
      hotness: pair.hotness,
      isUnpaired: !pair.methodFromRun1 || !pair.methodFromRun2,
      value: pair
    }));
  });
  // Computed signal to derive the currently selected item in the method selector based on the selectedDiffPair.
  protected readonly selectedSelectorItem = computed<MethodSelectItem<DiffPairWithHotness> | null>(() => {
    const selected = this.selectedDiffPair();
    if (!selected) return null;
    return {
      name: selected.methodName,
      hotness: selected.hotness,
      value: selected
    };
  });
  // Computed signal to generate a shared color map for top methods across both runs, ensuring consistent coloring in the UI.
  private readonly sharedColorMap = computed(() => {
    const allNames = [
      ...this.runs[0].topMethods().map(m => m.name),
      ...this.runs[1].topMethods().map(m => m.name),
    ];
  return generateMethodColorMap(allNames); // union for consistent indices
  });
  // Computed signals to apply the shared color map to the top methods of each run.
  protected readonly coloredTopMethods0 = computed<ColoredTopMethod[]>(() =>
    applyColorMap(this.runs[0].topMethods(), this.sharedColorMap())
  );
  protected readonly coloredTopMethods1 = computed<ColoredTopMethod[]>(() =>
    applyColorMap(this.runs[1].topMethods(), this.sharedColorMap())
  );
  // Computed signal to create a map of method hotness values for quick lookup in tree nodes.
  protected readonly treeHotnessMap = computed(() => {
    const map: Record<string, { hotness1?: number, hotness2?: number }> = {};
    const total1 = this.runs[0].metadata()?.totalPeriod || 1;
    const total2 = this.runs[1].metadata()?.totalPeriod || 1;

    for (const pair of this.comparisonPairs()) {
      map[pair.methodName] = {
        hotness1: pair.methodFromRun1?.totalPeriod !== undefined
            ? pair.methodFromRun1.totalPeriod / total1
            : undefined,

        hotness2: pair.methodFromRun2?.totalPeriod !== undefined
            ? pair.methodFromRun2.totalPeriod / total2
            : undefined,
      };
    }
    return map;
  });

  // Computed property to determine if the current route is the home page.
  protected get returnPath(): string | null {
    return this.route.snapshot.queryParamMap.get('path');
  }

  /**
   * Aggregates warnings from metadata and tree parsing for display.
   */
  protected get mergedWarnings(): WarningMessage[] {
    return [
      ...this.runs.flatMap((run) => run.metadata()?.benchmarkRunMetadata?.warnings ?? []),
      ...(this.inliningTree()?.warnings ?? []),
      ...(this.optimizationTree()?.warnings ?? []),
      ...(this.optimizationContextTree()?.warnings ?? [])
    ];
  }

  /**
   * Subscribes to route parameters and initiates data loading on component initialization.
   */
  ngOnInit(): void {
    this.route.queryParamMap.subscribe((params) => {
      const path = params.get('path');
      const runName1 = params.get('runName1');
      const runName2 = params.get('runName2');
      const warmup = params.get('warmup');
      const method = params.get('method');
      const cu1 = params.get('cu1');
      const cu2 = params.get('cu2');

      // trigger initial load/reload if the core experiment parameters changed
      if (this.path() !== path || this.runs[0].name() !== runName1 || this.runs[1].name() !== runName2) {
        this.path.set(path);
        this.runs[0].name.set(runName1);
        this.runs[1].name.set(runName2);
        this.warmupIterations.set(warmup ? parseInt(warmup, 10) : 0);

        if (!path || !runName1 || !runName2) {
          this.errorMessage.set({ message: 'Missing path, runName1, or runName2 parameter in URL.' });
          this.isLoading.set(false);
          return;
        }

        this.loadAllData();
      }
      // if data is already loaded and the user clicks back/forward in the browser to change methods
      else if (!this.isLoading() && method && this.comparisonPairs().length > 0) {
        const currentMethod = this.selectedDiffPair()?.methodName;
        const currentCu1 = this.runs[0].selectedCompilationId();
        const currentCu2 = this.runs[1].selectedCompilationId();

        if (method !== currentMethod || cu1 !== currentCu1 || cu2 !== currentCu2) {
          const foundPair = this.comparisonPairs().find(p => p.methodName === method);
          if (foundPair) {
            this.onPairSelected(foundPair, undefined, cu1, cu2);
          }
        }
      }
    });
  }

  /**
   * Cancels any ongoing data requests when the component is destroyed.
   */
  ngOnDestroy(): void {
    if (this.dataLoadSubscription) {
      this.dataLoadSubscription.unsubscribe();
    }
    if (this.treeLoadSubscription) {
      this.treeLoadSubscription.unsubscribe();
    }
  }

  /**
   * Build the observable for fetching a single run's metadata and top methods.
   */
  private createRunRequest(runIndex: number) {
    const runName = this.runs[runIndex].name()!;

    return this.runsService.getRunMetadata(this.path()!, runName).pipe(
      tap((m) => this.runs[runIndex].metadata.set(m)),
      switchMap((m) =>
        m.benchmarkRunMetadata.profileMetadata
          ? this.runsService.getTopMethods(this.path()!, runName, this.getHotnessOptions())
          : of([])
      ),
      tap((top) => this.runs[runIndex].topMethods.set(top))
    );
  }

  /**
   * Loads metadata and top methods for both runs, then fetches the list of method pairs for comparison.
   */
  protected loadAllData(): void {
    // cancel previous request if active
    if (this.dataLoadSubscription) {
      this.dataLoadSubscription.unsubscribe();
    }

    this.isLoading.set(true);
    this.errorMessage.set(null);

    this.dataLoadSubscription = forkJoin({
      run0: this.createRunRequest(0),
      run1: this.createRunRequest(1),
      pairs: this.runsService.getCompiledMethodsUnion(
        this.path()!,
        this.runs[0].name()!,
        this.runs[1].name()!,
        this.getHotnessOptions(),
        this.getRenderingOptions()
      )
    })
      .pipe(
        catchError((err) => {
          this.errorMessage.set({ message: err.error?.message || 'Error loading data' });
          this.isLoading.set(false);
          return of(null);
        }),
        finalize(() => this.isLoading.set(false))
      )
      .subscribe((result) => {
        if (result) {
          this.processDiffPairs(result.pairs);
        }
      });
  }

  /**
   * Calculates hotness for each method pair, enriches the pairs with this information, sorts them, and updates the state.
   */
  private processDiffPairs(rawPairs: MethodComparisonPair[]): void {
    const meta0 = this.runs[0].metadata();
    const meta1 = this.runs[1].metadata();

    // determine if profiler is available for both runs
    const hasProfile0 = !!meta0?.benchmarkRunMetadata.profileMetadata;
    const hasProfile1 = !!meta1?.benchmarkRunMetadata.profileMetadata;
    this.isProfilerAvailable.set(hasProfile0 && hasProfile1);

    // total periods for normalization (avoiding division by zero)
    const totalPeriod0 = meta0?.totalPeriod || 1;
    const totalPeriod1 = meta1?.totalPeriod || 1;

    // calculate hotness
    const enrichedPairs: DiffPairWithHotness[] = rawPairs.map((pair) => {
      const period0 = pair.methodFromRun1?.totalPeriod ?? 0;
      const period1 = pair.methodFromRun2?.totalPeriod ?? 0;
      const hotness0 = period0 / totalPeriod0;
      const hotness1 = period1 / totalPeriod1;

      return {
        ...pair,
        hotness: this.isProfilerAvailable() ? Math.max(hotness0, hotness1) : 0
      };
    });

    // sort descending (hottest first)
    enrichedPairs.sort((a, b) => b.hotness - a.hotness);

    // update state
    this.comparisonPairs.set(enrichedPairs);

    // try resore from signals first (apply button) and by uri as fallback
    const currentMethod = this.selectedDiffPair()?.methodName;
    const urlMethod = this.route.snapshot.queryParamMap.get('method');
    const targetMethod = currentMethod || urlMethod;

    let foundPair = targetMethod ? enrichedPairs.find(p => p.methodName === targetMethod) : undefined;

    if (foundPair) {
      const currentCu1 = this.runs[0].selectedCompilationId();
      const urlCu1 = this.route.snapshot.queryParamMap.get('cu1');

      const currentCu2 = this.runs[1].selectedCompilationId();
      const urlCu2 = this.route.snapshot.queryParamMap.get('cu2');

      this.onPairSelected(foundPair, undefined, currentCu1 || urlCu1, currentCu2 || urlCu2);
    } else {
      this.selectHottestPair();
    }
  }

  /**
   * Auto-selects the first pair in the list if available.
   */
  private selectHottestPair(): void {
    if (this.comparisonPairs().length === 0) return;
    this.onPairSelected(this.comparisonPairs()[0]);
  }

  /**
   * Updates the URL parameters silently to sync state without reloading.
   */
  private updateUrlWithSelection(method: string, cu1: string | null, cu2: string | null) {
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { method, cu1, cu2 },
      queryParamsHandling: 'merge',
    });
  }

  /**
   * Resets selection state and picks the hottest compilation units for the new pair.
   */
  protected onPairSelected(
    pair: DiffPairWithHotness,
    target?: { id: string; runIndex: number },
    fallbackCu1?: string | null,
    fallbackCu2?: string | null
  ): void {
    if (this.treeLoadSubscription) {
      this.treeLoadSubscription.unsubscribe();
      this.isLoadingTrees.set(false);
    }
    this.selectedDiffPair.set(pair);

    // reset
    this.runs[0].selectedCompilationId.set(null);
    this.runs[1].selectedCompilationId.set(null);

    this.inliningTree.set(null);
    this.optimizationTree.set(null);
    this.optimizationContextTree.set(null);

    if (pair.methodFromRun1) this.sortCompilationUnits(pair.methodFromRun1);
    if (pair.methodFromRun2) this.sortCompilationUnits(pair.methodFromRun2);

    // default to the hottest compilation units (first in the sorted list)
    let id1 = pair.methodFromRun1?.compilationUnitMetadata?.[0]?.id;
    let id2 = pair.methodFromRun2?.compilationUnitMetadata?.[0]?.id;

    // if in uri or previous state and still exists in the new pair, use it
    if (fallbackCu1 && pair.methodFromRun1?.compilationUnitMetadata?.find(cu => cu.id === fallbackCu1)) {
      id1 = fallbackCu1;
    }
    if (fallbackCu2 && pair.methodFromRun2?.compilationUnitMetadata?.find(cu => cu.id === fallbackCu2)) {
      id2 = fallbackCu2;
    }

    // try to select specified compilation unit by id (target overrides everything)
    if (target) {
      if (target.runIndex === 0) {
        const match1 = pair.methodFromRun1?.compilationUnitMetadata?.find(cu => cu.id === target.id);
        if (match1) id1 = match1.id;
      } else if (target.runIndex === 1) {
        const match2 = pair.methodFromRun2?.compilationUnitMetadata?.find(cu => cu.id === target.id);
        if (match2) id2 = match2.id;
      }
    }

    if (id1 || id2) {
      this.onCompilationSelected(id1 ?? '', id2 ?? '');
    } else {
      // fallback to select hottest from both
      this.updateUrlWithSelection(pair.methodName, null, null);
    }
  }

  /**
   * Sorts compilation units by period (descending) to show hottest compilations first.
   */
  private sortCompilationUnits(method: JavaMethod) {
    if (method.compilationUnitMetadata && this.isProfilerAvailable()) {
      method.compilationUnitMetadata.sort((a, b) => (b.period ?? 0) - (a.period ?? 0));
    }
  }

  /**
   * Updates selected compilation IDs and fetches the comparison tree.
   * If one run is missing compilation units, the empty string is passed and the `report` view is fetched.
   */
  protected onCompilationSelected(id1: string | null, id2: string | null): void {
    if (id1 !== null) this.runs[0].selectedCompilationId.set(id1);
    if (id2 !== null) this.runs[1].selectedCompilationId.set(id2);

    const currentMethod = this.selectedDiffPair()?.methodName;
    if (currentMethod) {
      this.updateUrlWithSelection(currentMethod, this.runs[0].selectedCompilationId(), this.runs[1].selectedCompilationId());
    }

    this.inliningTree.set(null);
    this.optimizationTree.set(null);
    this.optimizationContextTree.set(null);

    this.loadActiveTree();
  }

  /**
   * Loads the active tab's tree data for the currently selected method pair and compilation units.
   * If one of the compilation IDs is missing (null), it triggers loading of the `report` view for the available run.
   */
  private loadActiveTree(): void {
    const cid1 = this.runs[0].selectedCompilationId();
    const cid2 = this.runs[1].selectedCompilationId();
    const currentPair = this.selectedDiffPair();

    if (!currentPair || (!cid1 && !cid2)) return;

    const treeTypes: ('inlining' | 'optimization' | 'optimization-context')[] = [
      'inlining',
      'optimization',
      'optimization-context'
    ];
    const activeType = treeTypes[this.activeTabIndex()];

    // do not reload if already fetched
    if (activeType === 'inlining' && this.inliningTree()) return;
    if (activeType === 'optimization' && this.optimizationTree()) return;
    if (activeType === 'optimization-context' && this.optimizationContextTree()) return;

    if (this.treeLoadSubscription) {
      this.treeLoadSubscription.unsubscribe();
      this.isLoadingTrees.set(false);
    }

    this.isLoadingTrees.set(true);

    // pass empty string if null, backend handles blank strings as "missing" -> report view
    this.treeLoadSubscription = this.runsService
      .getComparisonTree(
        activeType,
        this.path()!,
        this.runs[0].name()!,
        this.runs[1].name()!,
        currentPair.methodName,
        cid1 ?? '',
        cid2 ?? '',
        this.getHotnessOptions(),
        this.getRenderingOptions()
      )
      .pipe(
        finalize(() => this.isLoadingTrees.set(false))
      )
      .subscribe({
        next: (res) => {
          if (activeType === 'inlining') this.inliningTree.set(res);
          if (activeType === 'optimization') this.optimizationTree.set(res);
          if (activeType === 'optimization-context') this.optimizationContextTree.set(res);
        },
        error: (e) => {
          this.snackBar.open(`Failed to load ${activeType} tree, consult the backend logs.`, 'Retry', {
            duration: 10_000,
            horizontalPosition: 'center',
            verticalPosition: 'bottom',
            panelClass: ['snackbar-error']
          }).onAction().subscribe(() => this.loadActiveTree());
        }
      });
  }

  /**
   * Handles tab changes to switch between trees; triggering tree loading.
   */
  protected onTabChange(index: number): void {
    this.activeTabIndex.set(index);
    this.loadActiveTree();
  }

  /**
   * Handles clicks on the Fragment chip to find the corresponding parent compilation units pair and select it.
   */
  public onCompilationFragmentParentClicked(parentId: string, runIndex: number): void {
    const foundPair = CompareComponent.findMethodPairByOneCompilationId(this.comparisonPairs(), parentId, runIndex);
    if (foundPair) {
      this.onPairSelected(foundPair, { id: parentId, runIndex });
    } else {
      this.snackBar.open(
        `Fragments parent compilation unit not found in comparison pairs.`,
        'Dismiss',
        {
          duration: 10_000,
          horizontalPosition: 'center',
          verticalPosition: 'bottom',
          panelClass: ['snackbar-error']
        }
      );
    }
  }

  /**
   * Method resolver function to find the parent method name based on the parent compilation unit ID.
   */
  resolveParentMethodName = (parentId: string, runIndex?: number): string | undefined => {
    const foundPair = CompareComponent.findMethodPairByOneCompilationId(this.comparisonPairs(), parentId, runIndex);
    return foundPair?.methodName;
  };

  /**
   * Handles clicks on top methods in the child component, finds the corresponding pair, and selects it.
   */
  public onTopMethodClicked(topMethod: TopMethod, runIndex: number): void {
    if (topMethod.id) {
      const foundPair = CompareComponent.findMethodPairByOneCompilationId(this.comparisonPairs(), topMethod.id, runIndex);
      if (foundPair) {
        this.onPairSelected(foundPair, { id: topMethod.id, runIndex });
        this.scrollToMethodsDetail();
        return;
      }
    }

    const foundPair = CompareComponent.findMethodPairByName(this.comparisonPairs(), topMethod.name);
    if (foundPair) {
      if (topMethod.id) {
        this.onPairSelected(foundPair, { id: topMethod.id, runIndex });
      } else {
        this.onPairSelected(foundPair);
      }
      this.scrollToMethodsDetail();
      return;
    }

    // method with its compilation units was not found by id or name
    this.snackBar.open(
      `Method not found in comparison pairs. It may be missing from Optimization Log or there was a C++ profiler parsing issue.`,
      'Dismiss',
      {
        duration: 10_000,
        horizontalPosition: 'center',
        verticalPosition: 'bottom',
        panelClass: ['snackbar-error']
      }
    );
  }

  /**
   * Finds a DiffPair by method name, strictly enforcing it exists in the requested runIndex.
   */
  private static findMethodPairByName(pairs: DiffPairWithHotness[], methodName: string, runIndex?: number): DiffPairWithHotness | undefined {
    return pairs.find((pair) => {
      if (pair.methodName !== methodName) return false;

      if (runIndex === 0) return !!pair.methodFromRun1;
      if (runIndex === 1) return !!pair.methodFromRun2;
      return true;
    });
  }

  /**
   * Finds a DiffPair that contains a compilation unit with the given ID in either run.
   * If runIndex is specified, it restricts the search to that run (0 or 1).
   */
  private static findMethodPairByOneCompilationId(pairs: DiffPairWithHotness[], compilationId: string, runIndex?: number): DiffPairWithHotness | undefined {
    if (runIndex === 0) {
      return pairs.find(
        (pair) =>
          pair.methodFromRun1?.compilationUnitMetadata.find((cu) => cu.id === compilationId)
      );
    } else if (runIndex === 1) {
      return pairs.find(
        (pair) =>
          pair.methodFromRun2?.compilationUnitMetadata.find((cu) => cu.id === compilationId)
      );
    }

    return pairs.find(
      (pair) =>
        pair.methodFromRun1?.compilationUnitMetadata.find((cu) => cu.id === compilationId) ||
        pair.methodFromRun2?.compilationUnitMetadata.find((cu) => cu.id === compilationId),
    );
  }

  /**
   * Retrieves hotness options from the child component.
   */
  private getHotnessOptions(): HotPolicyOptions {
    return this.optionsComponent?.getHotnessOptions() ?? DEFAULT_HOT_POLICY_OPTIONS;
  }

  /**
   * Retrieves rendering options from the child component.
   */
  private getRenderingOptions(): ExperimentProcessingOptions {
    return this.optionsComponent?.getRenderingOptions() ?? DEFAULT_EXPERIMENT_PROCESSING_OPTIONS;
  }

  /**
   * Determines if either of the runs is an AOT profile, which affects the displayed columns in top-methods table.
   */
  protected isAnyAot(): boolean {
    return (
      this.runs[0].metadata()?.benchmarkRunMetadata?.profileMetadata?.compilationKind === 'AOT' ||
      this.runs[1].metadata()?.benchmarkRunMetadata?.profileMetadata?.compilationKind === 'AOT'
    );
  }



  /**
   * Handles hover events from the top methods table to set the hovered method name for UI highlighting in both tables.
   */
  protected onTopMethodHovered(method: TopMethod | null): void {
    this.hoveredMethodName.set(method ? method.name : null);
  }

  /**
   * Handles clicks on method names within tree nodes to find the corresponding method pair and select it.
   */
  protected onTreeMethodClicked(methodName: string): void {
    const foundPair = CompareComponent.findMethodPairByName(this.comparisonPairs(), methodName);

    if (foundPair) {
      this.onPairSelected(foundPair);
    } else {
      this.snackBar.open(`Target method not found in comparison pairs.`, 'Dismiss', { duration: 5000 });
    }
  }

  /**
   * Triggers the copy action for whichever tree is currently visible in the active tab.
   */
  public async copyActiveTree(): Promise<void> {
    let activeNode;
    const index = this.activeTabIndex();

    if (index === 0) activeNode = this.inliningTree()?.tree;
    else if (index === 1) activeNode = this.optimizationTree()?.tree;
    else if (index === 2) activeNode = this.optimizationContextTree()?.tree;

    if (!activeNode) {
      this.snackBar.open('No tree available to copy.', 'Dismiss', { duration: 3000 });
      return;
    }

    const rawText = TreeFormatter.formatTreeAsText(activeNode);

    try {
      await navigator.clipboard.writeText(rawText);
      this.snackBar.open('Tree copied to clipboard!', 'Dismiss', { duration: 3000 });
    } catch (err) {
      console.error('Failed to copy text: ', err);
      this.snackBar.open('Failed to copy tree.', 'Dismiss', {
        duration: 5000,
        panelClass: ['snackbar-error']
      });
    }
  }

  /**
   * Scrolls the Methods Union Detail card into view after a top-method row click.
   */
  private scrollToMethodsDetail(): void {
    setTimeout(() => {
      this.methodsDetailRef?.nativeElement.scrollIntoView({
        behavior: 'smooth',
        block: 'start'
      });
    }, 0);
  }
}
