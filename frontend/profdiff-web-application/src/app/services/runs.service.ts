import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';
import { BenchmarkRuns } from '../models/dto/benchmarkRuns';
import { ExperimentProcessingOptions } from '../models/experiment-processing-options';
import { HotPolicyOptions } from '../models/hot-policy-options';
import { JavaMethod } from '../models/dto/javaMethod';
import { RunMetadata } from '../models/dto/runMetadata';
import { TopMethod } from '../models/dto/topMethod';
import { TreeResponse } from '../models/dto/treeResponse';
import { WorkspaceDirectory } from '../models/dto/workspaceDirectory';
import { MethodComparisonPair } from '../models/dto/methodComparisonPair';

/**
 * RunsService is responsible for all HTTP interactions with the backend API related to benchmark runs.
 */
@Injectable({
  providedIn: 'root',
})
export class RunsService {
  private http = inject(HttpClient);

  /** 
   * Fetches available subdirectories for the autocomplete browser. 
   */
  public getDirectories(path: string): Observable<WorkspaceDirectory[]> {
    const params = new HttpParams().set('path', path);
    return this.http.get<WorkspaceDirectory[]>('api/workspace/directories', { params });
  }

  /**
   * Fetches the all benchmark runs results from the specified root path.
   */
  public getRuns(path: string): Observable<BenchmarkRuns> {
    const params = new HttpParams().set('path', path);
    return this.http.get<BenchmarkRuns>('api/runs', { params });
  }

  /**
   * Invalidates the backend cache for the specified directory path.
   */
  public refreshRunsMetadata(path: string): Observable<void> {
    const params = new HttpParams().set('path', path);
    return this.http.put<void>('api/runs/refresh', {}, { params });
  }

  /**
   * Fetches metadata for a specific benchmark run.
   */
  public getRunMetadata(path: string, runName: string): Observable<RunMetadata> {
    const params = new HttpParams()
      .set('path', path)
      .set('runName', runName);
    return this.http.get<RunMetadata>('/api/report/metadata', { params });
  }

  /**
   * Fetches the list of top 10 methods for a specific run, based on the provided hotness options.
   */
  public getTopMethods(path: string, runName: string, hotnessOptions: HotPolicyOptions): Observable<TopMethod[]> {
    let params = new HttpParams()
      .set('path', path)
      .set('runName', runName);
    params = this.setHotnessParams(params, hotnessOptions);

    return this.http.get<TopMethod[]>('/api/report/top-methods', { params });
  }

  /**
   * Fetches the list of all compiled methods for a specific run, based on the provided hotness and createFragments options.
   */
  public getAllCompiledMethods(
    path: string,
    runName: string,
    hotnessOptions: HotPolicyOptions,
    renderingOptions: ExperimentProcessingOptions,
  ): Observable<JavaMethod[]> {
    let params = new HttpParams().set('path', path).set('runName', runName);
    params = this.setHotnessParams(params, hotnessOptions);
    params = this.setRenderingParams(params, renderingOptions);

    return this.http.get<JavaMethod[]>('/api/report/all-methods', { params });
  }

  /**
   * Fetches the union of comparative Java methods across two different profiling runs, based on the provided hotness and rendering options.
   */
  public getCompiledMethodsUnion(
    path: string,
    runName1: string,
    runName2: string,
    hotnessOptions: HotPolicyOptions,
    processingOptions: ExperimentProcessingOptions,
  ): Observable<MethodComparisonPair[]> {
    let params = new HttpParams()
      .set('path', path)
      .set('runName1', runName1)
      .set('runName2', runName2);
    params = this.setHotnessParams(params, hotnessOptions);
    params = this.setRenderingParams(params, processingOptions);

    return this.http.get<MethodComparisonPair[]>('/api/compare/methods-union', { params });
  }

  /**
   * Fetches the tree data for a specific method and compilation unit, based on the active tab (Inlining, Optimization, OptimizationContext)
   *  and the provided hotness and rendering options.
   */
  public getReportTree(
    treeType: 'inlining' | 'optimization' | 'optimization-context',
    path: string,
    runName: string,
    methodSignature: string,
    compilationId: string,
    hotnessOptions: HotPolicyOptions,
    renderingOptions: ExperimentProcessingOptions,
  ): Observable<TreeResponse> {
    let params = new HttpParams()
      .set('path', path)
      .set('runName', runName)
      .set('methodName', methodSignature)
      .set('compilationId', compilationId);
    params = this.setHotnessParams(params, hotnessOptions);
    params = this.setRenderingParams(params, renderingOptions);
    return this.http.get<TreeResponse>(`/api/report/${treeType}-tree`, { params });
  }

  /**
   * Fetches the compared tree data for a specific method and compilation unit across two runs, based on
   *  the active tab (Inlining, Optimization, OptimizationContext)
   */
  public getComparisonTree(
    treeType: 'inlining' | 'optimization' | 'optimization-context',
    path: string,
    runName1: string,
    runName2: string,
    methodSignature: string,
    compilationId1: string,
    compilationId2: string,
    hotnessOptions: HotPolicyOptions,
    renderingOptions: ExperimentProcessingOptions,
  ): Observable<TreeResponse> {
    let params = new HttpParams()
      .set('path', path)
      .set('runName1', runName1)
      .set('runName2', runName2)
      .set('methodName', methodSignature)
      .set('compilationId1', compilationId1)
      .set('compilationId2', compilationId2);
    params = this.setHotnessParams(params, hotnessOptions);
    params = this.setRenderingParams(params, renderingOptions);
    return this.http.get<TreeResponse>(`/api/compare/${treeType}-tree`, { params });
  }  

  /**
   * Set hotness-related query parameters based on the provided hotness options.
   */
  private setHotnessParams(params: HttpParams, hotnessOptions: HotPolicyOptions): HttpParams {
    if (hotnessOptions.hotMinLimit !== undefined && hotnessOptions.hotMinLimit !== null) {
      const minLimit = Number(hotnessOptions.hotMinLimit);
      if (Number.isFinite(minLimit)) {
        params = params.set('hot-min-limit', minLimit.toString());
      }
    }
    if (hotnessOptions.hotMaxLimit !== undefined && hotnessOptions.hotMaxLimit !== null) {
      const maxLimit = Number(hotnessOptions.hotMaxLimit);
      if (Number.isFinite(maxLimit)) {
        params = params.set('hot-max-limit', maxLimit.toString());
      }
    }
    if (hotnessOptions.hotPercentile !== undefined && hotnessOptions.hotPercentile !== null) {
      const percentile = Number(hotnessOptions.hotPercentile);
      if (Number.isFinite(percentile)) {
        params = params.set('hot-percentile', percentile.toString());
      }
    }
    return params;
  }

  /**
   * Set rendering-related query parameters based on the provided rendering options.
   */
  private setRenderingParams(params: HttpParams, renderingOptions: ExperimentProcessingOptions): HttpParams {
    if (renderingOptions.createFragments !== undefined && renderingOptions.createFragments !== null) {
      params = params.set('create-fragments', String(renderingOptions.createFragments));
    }
    if (renderingOptions.sortInliningTree !== undefined && renderingOptions.sortInliningTree !== null) {
      params = params.set('sort-inlining-tree', String(renderingOptions.sortInliningTree));
    }
    if (renderingOptions.sortUnorderedPhases !== undefined && renderingOptions.sortUnorderedPhases !== null) {
      params = params.set('sort-unordered-phases', String(renderingOptions.sortUnorderedPhases));
    }
    if (renderingOptions.removeDetailedPhases !== undefined && renderingOptions.removeDetailedPhases !== null) {
      params = params.set('remove-detailed-phases', String(renderingOptions.removeDetailedPhases));
    }
    if (renderingOptions.pruneIdentities !== undefined && renderingOptions.pruneIdentities !== null) {
      params = params.set('prune-identities', String(renderingOptions.pruneIdentities));
    }
    if (renderingOptions.longBci !== undefined && renderingOptions.longBci !== null) {
      params = params.set('long-bci', String(renderingOptions.longBci));
    }
    if (renderingOptions.inlinerReasoning !== undefined && renderingOptions.inlinerReasoning !== null) {
      params = params.set('inliner-reasoning', String(renderingOptions.inlinerReasoning));
    }
    return params;
  }
}