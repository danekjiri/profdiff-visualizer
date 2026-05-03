import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { ActivatedRoute, convertToParamMap, Router } from '@angular/router';
import { of } from 'rxjs';
import { provideNoopAnimations } from '@angular/platform-browser/animations';

import { CompareComponent } from './compare.component';
import { RunsService } from '../../services/runs.service';

const makeMetadata = (runName: string) => ({
  executionId: `id-${runName}`,
  totalPeriod: 1_000_000,
  benchmarkRunMetadata: {
    runName,
    profileMetadata: { compilationKind: 'JIT', totalPeriod: 1_000_000 },
    benchResultsMetadata: { benchmarkName: 'scrabble', metricValues: [100, 200] },
    warnings: []
  }
});

const makeMethod = (name: string, compilationId: string = 'cid1', totalPeriod = 50_000) => ({
  name,
  totalPeriod,
  compilationUnitMetadata: [{ id: compilationId, period: 1000, isHot: true, isFragment: false }]
});

const makePairs = () => [
  {
    methodName: 'java.util.List.add',
    methodFromRun1: makeMethod('java.util.List.add', 'cid1', 500_000),
    methodFromRun2: makeMethod('java.util.List.add', 'cid2', 300_000)
  },
  {
    methodName: 'java.util.Map.put',
    methodFromRun1: makeMethod('java.util.Map.put', 'cid3', 100_000),
    methodFromRun2: null
  }
];

const makeTreeResponse = () => ({
  tree: { content: { rawText: 'root' }, marker: 'NEUTRAL', children: [] },
  warnings: []
});

describe('CompareComponent', () => {
  let component: CompareComponent;
  let fixture: ComponentFixture<CompareComponent>;
  let runsServiceSpy: jest.Mocked<RunsService>;

  const createComponent = async (
    queryParams: Record<string, string> = { runName1: 'run-A', runName2: 'run-B', path: 'test-path' }
  ) => {
    runsServiceSpy = {
      getRunMetadata: jest.fn(),
      getTopMethods: jest.fn(),
      getCompiledMethodsUnion: jest.fn(),
      getComparisonTree: jest.fn()
    } as unknown as jest.Mocked<RunsService>;

    runsServiceSpy.getRunMetadata.mockImplementation((path, runName) =>
      of(makeMetadata(runName) as any)
    );
    runsServiceSpy.getTopMethods.mockReturnValue(of([]));
    runsServiceSpy.getCompiledMethodsUnion.mockReturnValue(of(makePairs() as any));
    runsServiceSpy.getComparisonTree.mockReturnValue(of(makeTreeResponse() as any));

    await TestBed.configureTestingModule({
      imports: [CompareComponent],
      providers: [
        provideHttpClient(),
        provideHttpClientTesting(),
        provideNoopAnimations(),
        { provide: RunsService, useValue: runsServiceSpy },
        {
          provide: ActivatedRoute,
          useValue: {
            queryParamMap: of(convertToParamMap(queryParams)),
            snapshot: { queryParamMap: convertToParamMap(queryParams) }
          }
        }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(CompareComponent);
    component = fixture.componentInstance;
  };

  afterEach(() => TestBed.resetTestingModule());

  it('should successfully load data, sort comparison pairs, and auto-select the hottest pair on init', async () => {
    await createComponent();
    fixture.detectChanges();

    const pairs = component['comparisonPairs']();
    
    expect(component['isLoading']()).toBe(false);
    expect(pairs.length).toBe(2);
    expect(pairs[0].hotness).toBeGreaterThanOrEqual(pairs[1].hotness);
    
    expect(component['selectedDiffPair']()).not.toBeNull();
    expect(component['selectedDiffPair']()!.methodName).toBe('java.util.List.add');
  });

  it('should update the selected pair and fetch the corresponding comparison tree when a user selects a method', async () => {
    await createComponent();
    fixture.detectChanges();

    const newSelection = component['comparisonPairs']()[1];
    component['onPairSelected'](newSelection);

    expect(component['selectedDiffPair']()!.methodName).toBe(newSelection.methodName);
    expect(runsServiceSpy.getComparisonTree).toHaveBeenCalledWith(
      'inlining',
      expect.anything(), expect.anything(), expect.anything(),
      expect.anything(), expect.anything(), expect.anything(),
      expect.anything(), expect.anything()
    );
  });

  it('should switch the active tab and reload the specific comparison tree (e.g., optimization) on tab change', async () => {
    await createComponent();
    fixture.detectChanges();

    component['inliningTree'].set(makeTreeResponse() as any);

    component['onTabChange'](1);

    expect(component['activeTabIndex']()).toBe(1);
    expect(runsServiceSpy.getComparisonTree).toHaveBeenCalledWith(
      'optimization',
      expect.anything(), expect.anything(), expect.anything(),
      expect.anything(), expect.anything(), expect.anything(),
      expect.anything(), expect.anything()
    );
  });
});