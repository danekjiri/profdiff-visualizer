import { TestBed } from '@angular/core/testing';
import { HttpClientTestingModule, HttpTestingController } from '@angular/common/http/testing';
import { RunsService } from './runs.service';

import { BenchmarkRuns } from '../models/dto/benchmarkRuns';
import { ExperimentProcessingOptions } from '../models/experiment-processing-options';
import { HotPolicyOptions } from '../models/hot-policy-options';
import { JavaMethod } from '../models/dto/javaMethod';
import { RunMetadata } from '../models/dto/runMetadata';
import { TopMethod } from '../models/dto/topMethod';
import { TreeResponse } from '../models/dto/treeResponse';
import { WorkspaceDirectory } from '../models/dto/workspaceDirectory';
import { MethodComparisonPair } from '../models/dto/methodComparisonPair';

describe('RunsService', () => {
  let service: RunsService;
  let httpMock: HttpTestingController;

  const dummyHotOptions: HotPolicyOptions = {
    hotMinLimit: 1,
    hotMaxLimit: 10,
    hotPercentile: 90
  };

  const dummyRenderingOptions: ExperimentProcessingOptions = {
    createFragments: true,
    sortInliningTree: false,
    pruneIdentities: true,
    longBci: true
  };

  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [HttpClientTestingModule],
      providers: [RunsService]
    });
    service = TestBed.inject(RunsService);
    httpMock = TestBed.inject(HttpTestingController);
  });

  afterEach(() => {
    httpMock.verify();
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('getDirectories', () => {
    it('should fetch available subdirectories', () => {
      const dummyResponse: WorkspaceDirectory[] = [
        { path: '/workspace/run1', hasRuns: true },
        { path: '/workspace/run2', hasRuns: false }
      ];
      const path = '/workspace';

      service.getDirectories(path).subscribe(res => {
        expect(res).toEqual(dummyResponse);
      });

      const req = httpMock.expectOne(request => request.url === 'api/workspace/directories');
      expect(req.request.method).toBe('GET');
      expect(req.request.params.get('path')).toBe(path);
      req.flush(dummyResponse);
    });
  });

  describe('getRuns', () => {
    it('should fetch all benchmark runs from the root path', () => {
      const dummyResponse: BenchmarkRuns = { benchmarkRuns: [], generalWarnings: [] };
      const path = '/workspace';

      service.getRuns(path).subscribe(res => {
        expect(res).toEqual(dummyResponse);
      });

      const req = httpMock.expectOne(request => request.url === 'api/runs');
      expect(req.request.method).toBe('GET');
      expect(req.request.params.get('path')).toBe(path);
      req.flush(dummyResponse);
    });
  });

  describe('getRunMetadata', () => {
    it('should fetch report metadata for a specific run', () => {
      const dummyResponse = { executionId: '123', compilationUnitsCount: 10 } as RunMetadata;
      const path = '/workspace';
      const runName = 'run1';

      service.getRunMetadata(path, runName).subscribe(res => {
        expect(res).toEqual(dummyResponse);
      });

      const req = httpMock.expectOne(request => request.url === '/api/report/metadata');
      expect(req.request.method).toBe('GET');
      expect(req.request.params.get('path')).toBe(path);
      expect(req.request.params.get('runName')).toBe(runName);
      req.flush(dummyResponse);
    });
  });

  describe('getTopMethods', () => {
    it('should fetch top methods and append hotness params', () => {
      const dummyResponse: TopMethod[] = [{ executionPercentage: '10%', cycles: '1', level: 4, name: 'java.util.Map' }];
      
      service.getTopMethods('/workspace', 'run1', dummyHotOptions).subscribe(res => {
        expect(res).toEqual(dummyResponse);
      });

      const req = httpMock.expectOne(request => request.url === '/api/report/top-methods');
      expect(req.request.method).toBe('GET');
      expect(req.request.params.get('path')).toBe('/workspace');
      expect(req.request.params.get('runName')).toBe('run1');
      
      expect(req.request.params.get('hot-min-limit')).toBe('1');
      expect(req.request.params.get('hot-max-limit')).toBe('10');
      expect(req.request.params.get('hot-percentile')).toBe('90');
      
      req.flush(dummyResponse);
    });
  });

  describe('getAllCompiledMethods', () => {
    it('should fetch all compiled methods and append hotness/rendering params', () => {
      const dummyResponse: JavaMethod[] = [{ name: 'java.util.Map', compilationUnitMetadata: [] }];
      
      service.getAllCompiledMethods('/workspace', 'run1', dummyHotOptions, dummyRenderingOptions).subscribe(res => {
        expect(res).toEqual(dummyResponse);
      });

      const req = httpMock.expectOne(request => request.url === '/api/report/all-methods');
      expect(req.request.method).toBe('GET');
      expect(req.request.params.get('path')).toBe('/workspace');
      expect(req.request.params.get('runName')).toBe('run1');
      
      expect(req.request.params.get('create-fragments')).toBe('true');
      expect(req.request.params.get('sort-inlining-tree')).toBe('false');
      expect(req.request.params.get('prune-identities')).toBe('true');
      expect(req.request.params.get('long-bci')).toBe('true');
      
      req.flush(dummyResponse);
    });
  });

  describe('getCompiledMethodsUnion', () => {
    it('should fetch the union of comparative methods between two runs', () => {
      const dummyResponse: MethodComparisonPair[] = [{ methodName: 'java.util.Map' }];
      
      service.getCompiledMethodsUnion('/workspace', 'run1', 'run2', dummyHotOptions, dummyRenderingOptions).subscribe(res => {
        expect(res).toEqual(dummyResponse);
      });

      const req = httpMock.expectOne(request => request.url === '/api/compare/methods-union');
      expect(req.request.method).toBe('GET');
      expect(req.request.params.get('path')).toBe('/workspace');
      expect(req.request.params.get('runName1')).toBe('run1');
      expect(req.request.params.get('runName2')).toBe('run2');
      expect(req.request.params.has('hot-percentile')).toBe(true);
      expect(req.request.params.has('create-fragments')).toBe(true);
      
      req.flush(dummyResponse);
    });
  });

  describe('getReportTree', () => {
    it('should fetch the requested report tree (inlining)', () => {
      const dummyResponse = { tree: { label: 'root', marker: 'NEUTRAL', children: [] } } as unknown as TreeResponse;
      
      service.getReportTree('inlining', '/workspace', 'run1', 'java.util.List', 'cid1', dummyHotOptions, dummyRenderingOptions).subscribe(res => {
        expect(res).toEqual(dummyResponse);
      });

      const req = httpMock.expectOne(request => request.url === '/api/report/inlining-tree');
      expect(req.request.method).toBe('GET');
      expect(req.request.params.get('path')).toBe('/workspace');
      expect(req.request.params.get('runName')).toBe('run1');
      expect(req.request.params.get('methodName')).toBe('java.util.List');
      expect(req.request.params.get('compilationId')).toBe('cid1');
      
      req.flush(dummyResponse);
    });
  });

  describe('getComparisonTree', () => {
    it('should fetch the requested comparison tree (optimization-context)', () => {
      const dummyResponse = { tree: { label: 'root', marker: 'MODIFIED', children: [] } } as unknown as TreeResponse;
      
      service.getComparisonTree('optimization-context', '/workspace', 'run1', 'run2', 'java.util.Map', 'cid1', 'cid2', dummyHotOptions, dummyRenderingOptions).subscribe(res => {
        expect(res).toEqual(dummyResponse);
      });

      const req = httpMock.expectOne(request => request.url === '/api/compare/optimization-context-tree');
      expect(req.request.method).toBe('GET');
      expect(req.request.params.get('path')).toBe('/workspace');
      expect(req.request.params.get('runName1')).toBe('run1');
      expect(req.request.params.get('runName2')).toBe('run2');
      expect(req.request.params.get('methodName')).toBe('java.util.Map');
      expect(req.request.params.get('compilationId1')).toBe('cid1');
      expect(req.request.params.get('compilationId2')).toBe('cid2');
      
      req.flush(dummyResponse);
    });
  });
});