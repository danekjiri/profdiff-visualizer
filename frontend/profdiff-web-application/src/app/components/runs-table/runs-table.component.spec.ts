import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { By } from '@angular/platform-browser';

import { RunsTableComponent } from './runs-table.component';
import { BenchmarkRunMetadata } from '../../models/dto/benchmarkRunMetadata';

const makeRun = (overrides: Partial<BenchmarkRunMetadata> = {}): BenchmarkRunMetadata => ({
  runName: 'run1',
  benchResultsMetadata: {
    benchmarkName: 'scrabble',
    benchmarkSuite: 'renaissance',
    graalVersion: 'GraalVM CE 25.1.0',
    metricValues: [100, 200, 300]
  } as any,
  profileMetadata: {
    compilationKind: 'JIT',
    totalPeriod: 1_000_000
  } as any,
  ...overrides
});

describe('RunsTableComponent', () => {
  let component: RunsTableComponent;
  let fixture: ComponentFixture<RunsTableComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [RunsTableComponent, NoopAnimationsModule],
    }).compileComponents();

    fixture = TestBed.createComponent(RunsTableComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should show "no runs" message when data is empty', () => {
    component.runs = [];
    fixture.detectChanges();

    const msg = fixture.debugElement.query(By.css('.no-runs-message'));
    expect(msg).toBeTruthy();
    expect(msg.nativeElement.textContent).toContain('no runs were found');
  });

  it('should render the table when runs are provided', () => {
    component.runs = [makeRun()];
    fixture.detectChanges();

    const table = fixture.debugElement.query(By.css('table'));
    expect(table).toBeTruthy();
  });

  describe('getBenchmarkName', () => {
    it('should combine suite and name with colon', () => {
      const run = makeRun();
      expect(component['getBenchmarkName'](run)).toBe('renaissance:scrabble');
    });

    it('should return only the name when no suite is provided', () => {
      const run = makeRun({ benchResultsMetadata: { benchmarkName: 'scrabble' } as any });
      expect(component['getBenchmarkName'](run)).toBe('scrabble');
    });

    it('should return empty string when benchResultsMetadata is absent', () => {
      const run = makeRun({ benchResultsMetadata: undefined });
      expect(component['getBenchmarkName'](run)).toBe('');
    });
  });

  describe('calculateAverageTime', () => {
    it('should return average of all metric values when warmup is 0', () => {
      const run = makeRun({ benchResultsMetadata: { metricValues: [100, 200, 300] } as any });
      component.warmupIterations = 0;
      expect(component['calculateAverageTime'](run)).toBeCloseTo(200);
    });

    it('should skip warmup iterations', () => {
      const run = makeRun({ benchResultsMetadata: { metricValues: [10, 100, 200, 300] } as any });
      component.warmupIterations = 1;
      expect(component['calculateAverageTime'](run)).toBeCloseTo(200);
    });

    it('should return null when metrics are missing', () => {
      const run = makeRun({ benchResultsMetadata: undefined });
      expect(component['calculateAverageTime'](run)).toBeNull();
    });

    it('should always keep at least one iteration even if warmup exceeds length', () => {
      const run = makeRun({ benchResultsMetadata: { metricValues: [42] } as any });
      component.warmupIterations = 99;
      expect(component['calculateAverageTime'](run)).toBeCloseTo(42);
    });
  });

  describe('hasProfilerInfo, hasBenchResultsInfo', () => {
    it('should return true when profileMetadata is present', () => {
      const run = makeRun();
      expect(component['hasProfilerInfo'](run)).toBe(true);
    });

    it('should return false when profileMetadata is absent', () => {
      const run = makeRun({ profileMetadata: undefined });
      expect(component['hasProfilerInfo'](run)).toBe(false);
    });

    it('should return true when benchResultsMetadata is present', () => {
      const run = makeRun();
      expect(component['hasBenchResultsInfo'](run)).toBe(true);
    });

    it('should return false when benchResultsMetadata is absent', () => {
      const run = makeRun({ benchResultsMetadata: undefined });
      expect(component['hasBenchResultsInfo'](run)).toBe(false);
    });
  });

  describe('selection logic', () => {
    beforeEach(() => {
      component.runs = [makeRun({ runName: 'run1' }), makeRun({ runName: 'run2' })];
      fixture.detectChanges();
    });

    it('isAllSelected should return false when nothing is selected', () => {
      expect(component['isAllSelected']()).toBe(false);
    });

    it('toggleAllRows should select all filtered rows', () => {
      component['toggleAllRows']();
      expect(component.selection.selected.length).toBe(2);
    });

    it('toggleAllRows should deselect all when all are already selected', () => {
      component['toggleAllRows']();
      component['toggleAllRows']();
      expect(component.selection.selected.length).toBe(0);
    });

    it('isSomeSelected should return true when at least one row is selected', () => {
      const firstRow = component['dataSource'].data[0];
      component.selection.select(firstRow);
      expect(component['isSomeSelected']()).toBe(true);
    });
  });

  describe('filterPredicate', () => {
    it('should filter out rows not matching benchmark name', () => {
      component.runs = [
        makeRun({ runName: 'run1', benchResultsMetadata: { benchmarkName: 'b1', benchmarkSuite: 's' } as any }),
        makeRun({ runName: 'run2', benchResultsMetadata: { benchmarkName: 'b2', benchmarkSuite: 's' } as any })
      ];
      fixture.detectChanges();

      component['columnFilters'].benchmark = ['s:b1'];
      component['applyFilters']();
      fixture.detectChanges();

      expect(component['dataSource'].filteredData.length).toBe(1);
      expect(component['dataSource'].filteredData[0].runName).toBe('run1');
    });

    it('should show all rows when no filters are active', () => {
      component.runs = [makeRun({ runName: 'run1' }), makeRun({ runName: 'run2' })];
      fixture.detectChanges();

      component['columnFilters'].benchmark = [];
      component['columnFilters'].compilationKind = [];
      component['columnFilters'].graalVersion = [];
      component['columnFilters'].status = [];
      component['applyFilters']();

      expect(component['dataSource'].filteredData.length).toBe(2);
    });
  });

  describe('generateReport', () => {
    it('should emit the report event and stop propagation', () => {
      const run = makeRun();
      const spy = jest.fn();
      component.report.subscribe(spy);

      const mockEvent = { stopPropagation: jest.fn() } as unknown as MouseEvent;
      component['generateReport'](run, mockEvent);

      expect(mockEvent.stopPropagation).toHaveBeenCalled();
      expect(spy).toHaveBeenCalledWith(run);
    });
  });

  describe('extractUniqueFilterValues', () => {
    it('should populate uniqueBenchmarks, uniqueCompilations, uniqueGraalVersions from run data', () => {
      component.runs = [
        makeRun({ benchResultsMetadata: { benchmarkName: 'a', benchmarkSuite: 's', graalVersion: 'v21' } as any, profileMetadata: { compilationKind: 'JIT' } as any }),
        makeRun({ benchResultsMetadata: { benchmarkName: 'b', benchmarkSuite: 's', graalVersion: 'v25' } as any, profileMetadata: { compilationKind: 'AOT' } as any })
      ];
      fixture.detectChanges();

      expect(component['uniqueBenchmarks']).toContain('s:a');
      expect(component['uniqueBenchmarks']).toContain('s:b');
      expect(component['uniqueCompilations']).toContain('JIT');
      expect(component['uniqueCompilations']).toContain('AOT');
      expect(component['uniqueGraalVersions']).toContain('v21');
      expect(component['uniqueGraalVersions']).toContain('v25');
    });

    it('should use EMPTY_LABEL for missing metadata fields', () => {
      component.runs = [makeRun({ benchResultsMetadata: undefined, profileMetadata: undefined })];
      fixture.detectChanges();

      expect(component['uniqueBenchmarks']).toContain(component['EMPTY_LABEL']);
      expect(component['uniqueCompilations']).toContain(component['EMPTY_LABEL']);
      expect(component['uniqueGraalVersions']).toContain(component['EMPTY_LABEL']);
    });
  });
});
