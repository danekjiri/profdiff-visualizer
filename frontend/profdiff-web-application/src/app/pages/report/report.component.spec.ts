import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideHttpClientTesting } from '@angular/common/http/testing';
import { provideHttpClient } from '@angular/common/http';
import { ActivatedRoute, convertToParamMap, Router } from '@angular/router';
import { of } from 'rxjs';
import { provideNoopAnimations } from '@angular/platform-browser/animations';

import { ReportComponent } from './report.component';
import { RunsService } from '../../services/runs.service';

const makeMetadata = () => ({
  executionId: '123',
  compilationUnitsCount: 10,
  totalPeriod: 1_000_000,
  benchmarkRunMetadata: {
    runName: 'test-run',
    profileMetadata: { compilationKind: 'JIT', totalPeriod: 1_000_000 },
    benchResultsMetadata: { benchmarkName: 'scrabble', metricValues: [100, 200] },
    warnings: []
  }
});

const makeMethod = (name: string, compilationIds: string[] = ['cid1']) => ({
  name,
  totalPeriod: 50_000,
  compilationUnitMetadata: compilationIds.map(id => ({ id, period: 1000, isHot: true, isFragment: false }))
});

const makeTreeResponse = () => ({
  tree: { content: { rawText: 'root' }, marker: 'NEUTRAL', children: [] },
  warnings: []
});

describe('ReportComponent', () => {
  let component: ReportComponent;
  let fixture: ComponentFixture<ReportComponent>;
  let runsServiceSpy: jest.Mocked<RunsService>;

  const createComponent = async (queryParams: Record<string, string> = { runName: 'test-run', path: 'test-path' }) => {
    runsServiceSpy = {
      getRunMetadata: jest.fn(),
      getTopMethods: jest.fn(),
      getAllCompiledMethods: jest.fn(),
      getReportTree: jest.fn()
    } as unknown as jest.Mocked<RunsService>;

    runsServiceSpy.getRunMetadata.mockReturnValue(of(makeMetadata() as any));
    runsServiceSpy.getTopMethods.mockReturnValue(of([]));
    runsServiceSpy.getAllCompiledMethods.mockReturnValue(of([makeMethod('java.util.Map', ['cid99']) as any]));
    runsServiceSpy.getReportTree.mockReturnValue(of(makeTreeResponse() as any));

    await TestBed.configureTestingModule({
      imports: [ReportComponent],
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

    fixture = TestBed.createComponent(ReportComponent);
    component = fixture.componentInstance;
  };

  afterEach(() => TestBed.resetTestingModule());

  it('should fetch metadata and compiled methods on initialization', async () => {
    await createComponent();
    fixture.detectChanges();

    expect(runsServiceSpy.getRunMetadata).toHaveBeenCalledWith('test-path', 'test-run');
    expect(runsServiceSpy.getAllCompiledMethods).toHaveBeenCalled();

    expect(component['isLoading']()).toBe(false);
    expect(component['metadata']()).not.toBeNull();
    expect(component['metadata']()!.executionId).toBe('123');
  });

  it('should update state and fetch the report tree when a user selects a method/compilation', async () => {
    await createComponent();
    fixture.detectChanges();

    const mockMethod = component['allMethods']()[0];

    component['onMethodSelected'](mockMethod);

    expect(component['selectedMethod']()?.name).toBe('java.util.Map');
    expect(component['selectedCompilationId']()).toBe('cid99');

    component['onCompilationSelected']('cid99');

    expect(runsServiceSpy.getReportTree).toHaveBeenCalledWith(
      'inlining',
      'test-path',
      'test-run',
      'java.util.Map',
      'cid99',
      expect.anything(),
      expect.anything()
    );
  });

  it('should switch the active tab and reload the specific report tree (e.g., optimization)', async () => {
    await createComponent();
    fixture.detectChanges();

    const mockMethod = component['allMethods']()[0];
    component['selectedMethod'].set(mockMethod);
    component['selectedCompilationId'].set('cid99');
    component['inliningTree'].set(makeTreeResponse() as any);
    component['onTabChange'](1);

    expect(component['activeTabIndex']()).toBe(1);
    expect(runsServiceSpy.getReportTree).toHaveBeenCalledWith(
      'optimization',
      expect.anything(), expect.anything(), expect.anything(),
      expect.anything(), expect.anything(), expect.anything()
    );
  });
});
