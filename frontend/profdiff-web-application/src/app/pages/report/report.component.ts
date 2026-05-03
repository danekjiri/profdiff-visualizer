import { CommonModule } from '@angular/common';
import { Component, computed, inject, OnInit, OnDestroy, signal, ViewChild, ElementRef, WritableSignal } from '@angular/core';
import { MatButtonModule } from '@angular/material/button';
import { MatCardModule } from '@angular/material/card';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatIcon } from '@angular/material/icon';
import { MatSnackBar, MatSnackBarModule } from '@angular/material/snack-bar';
import { MatTab, MatTabGroup } from '@angular/material/tabs';
import { MatTooltip } from "@angular/material/tooltip";
import { RouterLink, ActivatedRoute, Router } from '@angular/router';
import { of, Subscription } from 'rxjs';
import { catchError, switchMap, tap, finalize } from 'rxjs/operators';
import { CompilationUnitCardComponent } from '../../components/compilation-unit-card/compilation-unit-card.component';
import { ErrorMessageComponent } from '../../components/error-message/error-message.component';
import { DEFAULT_EXPERIMENT_PROCESSING_OPTIONS, DEFAULT_HOT_POLICY_OPTIONS, ExperimentOptionsComponent } from '../../components/experiment-options/experiment-options.component';
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
import { RunMetadata } from '../../models/dto/runMetadata';
import { TopMethod } from '../../models/dto/topMethod';
import { TreeResponse } from '../../models/dto/treeResponse';
import { WarningMessage } from '../../models/dto/warningMessage';
import { ChartFactory, ChartSeries } from '../../models/chart-series';
import { RunsService } from '../../services/runs.service';
import { ColoredTopMethod, generateMethodColorMap, applyColorMap } from '../../models/top-methods-colors';

/**
 * ReportComponent displays detailed analysis of a single benchmark run.
 * It orchestrates loading metadata, profile data, and compilation trees,
 * providing both a list overview and detailed tree visualizations.
 */
@Component({
  selector: 'app-report',
  standalone: true,
  imports: [
    CommonModule,
    ViewHeaderComponent,
    CompilationUnitCardComponent,
    ErrorMessageComponent,
    GeneralRunMetadataComponent,
    MatButtonModule,
    MatCardModule,
    TopMethodsTableComponent,
    MatExpansionModule,
    MatIcon,
    MatSnackBarModule,
    MatTab,
    MatTabGroup,
    MatTooltip,
    MethodSelectorComponent,
    MetricChartComponent,
    FloatingDockComponent,
    ExperimentOptionsComponent,
    WarningsComponent,
    RenderedTreeNodeTextComponent,
    RouterLink
  ],
  templateUrl: './report.component.html',
  styleUrls: ['./report.component.css']
})
export class ReportComponent implements OnInit, OnDestroy {
  // Services and route dependencies.
  private readonly runsService = inject(RunsService);
  private readonly route = inject(ActivatedRoute);
  private readonly router = inject(Router);
  private readonly snackBar = inject(MatSnackBar);

  // Access to child component for retrieving user-selected options.
  @ViewChild(ExperimentOptionsComponent) protected optionsComponent!: ExperimentOptionsComponent;
  // Reference to the Methods Detail card for scrolling into view on top-method selection.
  @ViewChild('methodsDetail', { read: ElementRef }) private methodsDetailRef!: ElementRef;

  // Subscription management for request cancellation on component destroy or new selection.
  private dataLoadSubscription?: Subscription;
  private treeLoadSubscription?: Subscription;
  private currentTreeRequest: string | null = null;

  // Signals for managing UI state.
  protected readonly isLoading: WritableSignal<boolean> = signal(true);
  protected readonly errorMessage: WritableSignal<ErrorMessage | null> = signal(null);
  protected readonly isProfilerAvailable = signal<boolean>(false);
  protected readonly isBenchResultsAvailable = signal<boolean>(false);
  protected readonly isLoadingTrees = signal<boolean>(false);
  // Signal to track the current path parameter for data loading.
  protected readonly path = signal<string | null>(null);
  // Signal to track the current runName parameter for data loading.
  protected readonly runName = signal<string | null>(null);
  // Signal to track the number of warmup iterations.
  protected readonly warmupIterations = signal<number>(0);
  // Signals for loaded data.
  protected readonly metadata: WritableSignal<RunMetadata | null> = signal(null);
  protected readonly topMethods: WritableSignal<TopMethod[]> = signal([]);
  protected readonly allMethods = signal<JavaMethod[]>([]);
  // Selection state.
  protected readonly selectedMethod = signal<JavaMethod | null>(null);
  protected readonly selectedCompilationId = signal<string | null>(null);
  // Signals for the report trees of the selected method.
  protected readonly inliningTree = signal<TreeResponse | null>(null);
  protected readonly optimizationTree = signal<TreeResponse | null>(null);
  protected readonly optimizationContextTree = signal<TreeResponse | null>(null);
  // Signal to track the active tab index for tree view switching. (0 = Inlining, 1 = Optimization, 2 = Optimization Context)
  protected readonly activeTabIndex = signal<number>(0);
  // Computed chart data based on runs metadata.
  protected readonly chartData = computed<ChartSeries[]>(() => {
    const meta = this.metadata();
    const metrics = meta?.benchmarkRunMetadata.benchResultsMetadata?.metricValues;

    const series = ChartFactory.from(this.runName(), metrics);
    return series ? [series] : [];
  });
  // Computed signal to adapt JavaMethod[] -> MethodSelectItem[] for the selector component.
  protected readonly methodSelectorItems = computed<MethodSelectItem[]>(() => {
    const totalPeriod = this.metadata()?.totalPeriod ?? 1;
    return this.allMethods().map((method) => ({
      name: method.name,
      hotness: (method.totalPeriod ?? 0) / totalPeriod,
      value: method
    }));
  });
  // Computed signal to derive the currently selected item in the method selector based on the selectedMethod signal.
  protected readonly selectedSelectorItem = computed<MethodSelectItem | null>(() => {
    const selected = this.selectedMethod();
    if (!selected) return null;
    const totalPeriod = this.metadata()?.totalPeriod ?? 1;
    return {
      name: selected.name,
      hotness: (selected.totalPeriod ?? 0) / totalPeriod,
      value: selected
    };
  });
  // Computed signal that generates a list of ColoredTopMethod by applying a color map to the top methods.
  protected readonly coloredTopMethods = computed<ColoredTopMethod[]>(() => {
    const methods = this.topMethods();
    const colorMap = generateMethodColorMap(methods.map(m => m.name));
    return applyColorMap(methods, colorMap);
  });
  // Computed property to determine if the current route is the home page.
  protected get returnPath(): string | null {
    return this.route.snapshot.queryParamMap.get('path');
  }
  // Computed signal to create a map of method hotness values for quick lookup in tree nodes.
  protected readonly treeHotnessMap = computed(() => {
    const map: Record<string, { hotness1?: number, hotness2?: number }> = {};
    const totalPeriod = this.metadata()?.totalPeriod || 1;

    for (const method of this.allMethods()) {
      map[method.name] = {
        hotness1: method.totalPeriod !== undefined ? method.totalPeriod / totalPeriod : undefined
      };
    }
    return map;
  });

  /**
   * Subscribes to route parameters and initiates data loading on component initialization.
   */
  ngOnInit(): void {
    this.route.queryParamMap.subscribe((params) => {
      const path = params.get('path');
      const runName = params.get('runName');
      const warmup = params.get('warmup');
      const method = params.get('method');
      const cu = params.get('cu');

      // trigger initial load/reload if the core experiment parameters changed
      if (this.path() !== path || this.runName() !== runName) {
        this.path.set(path);
        this.runName.set(runName);
        this.warmupIterations.set(warmup ? parseInt(warmup, 10) : 0);

        if (!path || !runName) {
          this.errorMessage.set({ message: 'Missing path or runName parameter in URL.' });
          this.isLoading.set(false);
          return;
        }

        this.loadReportData();
      }

      // restore from uri if navigating history
      else if (!this.isLoading() && method && this.allMethods().length > 0) {
        const currentMethod = this.selectedMethod()?.name;
        const currentCu = this.selectedCompilationId();

        if (method !== currentMethod || cu !== currentCu) {
          const foundMethod = this.allMethods().find(m => m.name === method);
          if (foundMethod) {
            this.onMethodSelected(foundMethod, cu);
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
   * Loads the report metadata, top methods, and all compiled methods for the selected run.
   */
  protected loadReportData(): void {
    if (this.dataLoadSubscription) {
      this.dataLoadSubscription.unsubscribe();
    }

    this.isLoading.set(true);
    this.errorMessage.set(null);

    const path = this.path()!;
    const runName = this.runName()!;

    this.dataLoadSubscription = this.runsService
      .getRunMetadata(path, runName)
      .pipe(
        tap((metadata) => {
          this.metadata.set(metadata);
          this.isProfilerAvailable.set(!!metadata.benchmarkRunMetadata.profileMetadata);
          this.isBenchResultsAvailable.set(!!metadata.benchmarkRunMetadata.benchResultsMetadata);
        }),
        switchMap(() => {
          if (this.isProfilerAvailable()) {
            return this.runsService.getTopMethods(path, runName, this.getHotnessOptions());
          } else {
            return of([]);
          }
        }),
        tap((methods) => {
          this.topMethods.set(methods);
        }),
        switchMap(() => {
          return this.runsService.getAllCompiledMethods(
            path,
            runName,
            this.getHotnessOptions(),
            this.getRenderingOptions()
          );
        }),
        tap((methods) => {
          const enrichedMethods: JavaMethod[] = methods;
          const totalPeriod = this.metadata()?.totalPeriod ?? 0;

          this.allMethods.set(this.getSortedMethodsByPeriodIfAvailable(enrichedMethods, totalPeriod));

          // try restore from signals first (apply button) and by uri as fallback
          const currentMethod = this.selectedMethod()?.name;
          const urlMethod = this.route.snapshot.queryParamMap.get('method');
          const targetMethod = currentMethod || urlMethod;

          let foundMethod = targetMethod ? this.allMethods().find(m => m.name === targetMethod) : undefined;

          if (foundMethod) {
            const currentCu = this.selectedCompilationId();
            const urlCu = this.route.snapshot.queryParamMap.get('cu');
            const targetCu = currentCu || urlCu;

            this.onMethodSelected(foundMethod, targetCu);
          } else {
            this.selectHottestMethodAndCompilations();
          }
        }),
        catchError((err) => {
          const message = err.error?.message || 'An unknown error occurred while loading the report.';
          this.errorMessage.set({ message });
          return of(null);
        }),
        finalize(() => {
          this.isLoading.set(false);
        })
      )
      .subscribe();
  }

  /**
   * Sort methods descending by total period (hotness).
   */
  private getSortedMethodsByPeriodIfAvailable(methods: JavaMethod[], methodsTotalPeriod: number): JavaMethod[] {
    if (!this.isProfilerAvailable()) {
      return methods;
    }

    return [...methods].sort((a, b) => ((b?.totalPeriod ?? 0) / methodsTotalPeriod) - ((a?.totalPeriod ?? 0) / methodsTotalPeriod));
  }

  /**
   * Automatically selects the hottest method and its first compilation unit.
   */
  private selectHottestMethodAndCompilations(): void {
    if (!this.isProfilerAvailable() || this.allMethods().length === 0) {
      return;
    }

    // the hottest method is the first (sorted list)
    const hottestMethod = this.allMethods()[0];
    if (!hottestMethod) {
      return;
    }

    this.onMethodSelected(hottestMethod);
  }

  /**
   * Updates the URL parameters silently to sync state without reloading.
   */
  private updateUrlWithSelection(method: string, cu: string | null) {
    this.router.navigate([], {
      relativeTo: this.route,
      queryParams: { method, cu },
      queryParamsHandling: 'merge',
    });
  }

  /**
   * Handles user selection of a method from the dropdown, optionally restoring a specific compilation ID.
   */
  protected onMethodSelected(method: JavaMethod, targetCu?: string | null): void {
    const sortedCompilationUnits = this.sortMethodsCompilationUnitsByPeriod(method);
    this.selectedMethod.set(sortedCompilationUnits);
    this.selectedCompilationId.set(null);

    let idToSelect = targetCu;

    // verify the requested compilation unit still exists for this method
    if (idToSelect && !sortedCompilationUnits.compilationUnitMetadata?.some(cu => cu.id === idToSelect)) {
      idToSelect = null;
    }

    // auto-select the hottest as fallback
    if (!idToSelect) {
      idToSelect = sortedCompilationUnits.compilationUnitMetadata?.[0]?.id;
    }

    if (idToSelect) {
      this.onCompilationSelected(idToSelect);
    } else {
      this.updateUrlWithSelection(method.name, null);
      this.inliningTree.set(null);
      this.optimizationTree.set(null);
      this.optimizationContextTree.set(null);
    }
  }

  /**
   * Handles user selection of a specific compilation unit within a method.
   */
  protected onMethodCompilationSelected(method: JavaMethod, compilationId: string): void {
    this.onMethodSelected(method, compilationId);
  }

  /**
   * Sorts the compilation units within a method by period (descending).
   */
  private sortMethodsCompilationUnitsByPeriod(method: JavaMethod): JavaMethod {
    if (method.compilationUnitMetadata) {
      method.compilationUnitMetadata.sort((a, b) => b.period! - a.period!);
    }
    return method;
  }

  /**
   * Handles selection of a compilation unit, triggering the loading of the corresponding tree data.
   */
  protected onCompilationSelected(compilationId: string): void {
    if (!this.selectedMethod()) return;

    if (this.treeLoadSubscription) {
      this.treeLoadSubscription.unsubscribe();
      this.isLoadingTrees.set(false);
    }

    this.selectedCompilationId.set(compilationId);

    this.updateUrlWithSelection(this.selectedMethod()!.name, compilationId);
    this.inliningTree.set(null);
    this.optimizationTree.set(null);
    this.optimizationContextTree.set(null);

    this.loadActiveTree();
  }

  /**
   * Loads the active tab's tree data for the currently selected method and compilation unit.
   */
  private loadActiveTree(): void {
    const compId = this.selectedCompilationId();
    const methodName = this.selectedMethod()?.name;
    if (compId === null || compId === undefined || !methodName) return;

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

    const requestKey = `${methodName}-${compId}-${activeType}`;
    if (this.isLoadingTrees() && this.currentTreeRequest === requestKey) {
      return;
    }

    if (this.treeLoadSubscription) {
      this.treeLoadSubscription.unsubscribe();
      this.isLoadingTrees.set(false);
    }
    this.isLoadingTrees.set(true);
    this.currentTreeRequest = requestKey;

    this.treeLoadSubscription = this.runsService
      .getReportTree(
        activeType,
        this.path()!,
        this.runName()!,
        methodName,
        compId,
        this.getHotnessOptions(),
        this.getRenderingOptions()
      )
      .pipe(
        finalize(() => {
          this.isLoadingTrees.set(false);
          if (this.currentTreeRequest === requestKey) {
            this.currentTreeRequest = null;
          }
        })
      )
      .subscribe({
        next: (res) => {
          if (activeType === 'inlining') this.inliningTree.set(res);
          if (activeType === 'optimization') this.optimizationTree.set(res);
          if (activeType === 'optimization-context') this.optimizationContextTree.set(res);
        },
        error: (err) => {
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
   * Handle tab switching.
   */
  protected onTabChange(index: number): void {
    if (this.activeTabIndex() === index) {
      return;
    }
    this.activeTabIndex.set(index);
    this.loadActiveTree();
  }

  /**
   * Handles clicks on the Fragment chip to find the corresponding parent compilation unit and select it.
   */
  protected onCompilationFragmentClicked(parentId: string): void {
    const foundMethod = ReportComponent.findMethodByCompilationId(this.allMethods(), parentId);
    if (foundMethod) {
      this.onMethodCompilationSelected(foundMethod, parentId);
    } else {
      this.snackBar.open(
        `Fragments parent compilation unit not found in report methods.`,
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
  protected resolveParentMethodName = (parentId: string): string | undefined => {
    const foundMethod = ReportComponent.findMethodByCompilationId(this.allMethods(), parentId);
    return foundMethod?.name;
  };

  /**
   * Handles click events from the Top Methods table.
   */
  protected onTopMethodClicked(topMethod: TopMethod): void {
    if (topMethod.id) {
      const foundMethod = ReportComponent.findMethodByCompilationId(this.allMethods(), topMethod.id);
      if (foundMethod) {
        this.onMethodCompilationSelected(foundMethod, topMethod.id!);
        this.scrollToMethodsDetail();
        return;
      }
    }

    const foundMethod = this.allMethods().find(method => method.name === topMethod.name);
    if (foundMethod) {
      this.onMethodSelected(foundMethod);
      this.scrollToMethodsDetail();
      return;
    }

    // method with its compilation units was not found by id or name
    this.snackBar.open(
      `Method not found in report methods. It may be missing from Optimization Log or there was a C++ profiler parsing issue.`,
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
   * Utility function to find a JavaMethod by matching compilation ID in its metadata.
   */
  private static findMethodByCompilationId(allMethods: JavaMethod[], targetCompilationId: string): JavaMethod | undefined {
    return allMethods.find(method =>
      method.compilationUnitMetadata.some(unit => unit.id === targetCompilationId)
    );
  }

  /**
   * Retrieve hotness options from the child component.
   */
  private getHotnessOptions(): HotPolicyOptions {
    return this.optionsComponent?.getHotnessOptions() ?? DEFAULT_HOT_POLICY_OPTIONS;
  }

  /**
   * Retrieve rendering options from the child component.
   */
  private getRenderingOptions(): ExperimentProcessingOptions {
    return this.optionsComponent?.getRenderingOptions() ?? DEFAULT_EXPERIMENT_PROCESSING_OPTIONS;
  }

  /**
   * Aggregates warnings from metadata and tree parsing for display.
   */
  protected get mergedWarnings(): WarningMessage[] {
    return [
      ...(this.metadata()?.benchmarkRunMetadata?.warnings ?? []),
      ...(this.inliningTree()?.warnings ?? []),
      ...(this.optimizationTree()?.warnings ?? []),
      ...(this.optimizationContextTree()?.warnings ?? [])
    ];
  }

  /**
   * Handles clicks on method links inside the tree nodes, triggering navigation.
   */
  protected onTreeMethodClicked(methodName: string): void {
    const foundMethod = this.allMethods().find(m => m.name === methodName);

    if (foundMethod) {
      this.onMethodSelected(foundMethod);
    } else {
      this.snackBar.open('Target method not found in compiled methods.', 'Dismiss', { duration: 5000 });
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
   * Scrolls the Methods Detail card into view after a top-method row click.
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
