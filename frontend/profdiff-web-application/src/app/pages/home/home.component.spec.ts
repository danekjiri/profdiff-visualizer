import { ComponentFixture, TestBed } from '@angular/core/testing';
import { HomeComponent } from './home.component';
import { RunsService } from '../../services/runs.service';
import { provideRouter, Router } from '@angular/router';
import { of } from 'rxjs';
import { provideNoopAnimations } from '@angular/platform-browser/animations';
import { ActivatedRoute, convertToParamMap } from '@angular/router';

const makeBenchmarkRuns = (runNames: string[] = ['run1', 'run2']) => ({
  benchmarkRuns: runNames.map(name => ({
    runName: name,
    benchResultsMetadata: {
      benchmarkName: 'scrabble',
      benchmarkSuite: 'renaissance',
      metricValues: [100, 200, 300]
    },
    profileMetadata: { compilationKind: 'JIT', totalPeriod: 1_000_000 }
  })),
  generalWarnings: []
});

describe('HomeComponent', () => {
  let component: HomeComponent;
  let fixture: ComponentFixture<HomeComponent>;
  let runsServiceSpy: jest.Mocked<RunsService>;
  let router: Router;

  const createComponent = async (queryPath: string | null = '/workspace') => {
    runsServiceSpy = {
      getRuns: jest.fn(),
      getDirectories: jest.fn(),
      refreshRunsMetadata: jest.fn()
    } as unknown as jest.Mocked<RunsService>;

    runsServiceSpy.getRuns.mockReturnValue(of(makeBenchmarkRuns() as any));
    runsServiceSpy.getDirectories.mockReturnValue(of([]));
    runsServiceSpy.refreshRunsMetadata.mockReturnValue(of(undefined as any));

    await TestBed.configureTestingModule({
      imports: [HomeComponent],
      providers: [
        { provide: RunsService, useValue: runsServiceSpy },
        provideRouter([]),
        provideNoopAnimations(),
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              queryParamMap: convertToParamMap(queryPath ? { path: queryPath } : {})
            }
          }
        }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(HomeComponent);
    component = fixture.componentInstance;
    router = TestBed.inject(Router);
  };

  afterEach(() => TestBed.resetTestingModule());

  it('should initialize path from query params, successfully load runs, and compute max iterations', async () => {
    await createComponent('/workspace');
    fixture.detectChanges();

    expect(runsServiceSpy.getRuns).toHaveBeenCalledWith('/workspace');
    expect(component['isLoading']()).toBe(false);

    expect(component['runs']()).toBeTruthy();
    expect(component['runs']()?.benchmarkRuns.length).toBe(2);

    expect(component['maxIterations']).toBe(2);
  });

  it('should navigate to /report with the correct query parameters when doReport is triggered', async () => {
    await createComponent('/workspace');
    fixture.detectChanges();

    const navigateSpy = jest.spyOn(router, 'navigate');
    const mockRun = { runName: 'run1' } as any;

    component['doReport'](mockRun);

    expect(navigateSpy).toHaveBeenCalledWith(['/report'], expect.objectContaining({
      queryParams: expect.objectContaining({ runName: 'run1', path: '/workspace' })
    }));
  });

  it('should update the active path and reset tab states when a user changes the directory path', async () => {
    await createComponent('/workspace');
    fixture.detectChanges();

    runsServiceSpy.getDirectories.mockReturnValue(of([
      { path: '/workspace/new-folder', hasRuns: true }
    ]));

    component['onPathChange']('/workspace/new-folder');

    expect(component['path']()).toBe('/workspace/new-folder');
    expect(component['tabCycleIndex']()).toBe(-1);
  });

   it('should NOT navigate to /compare if fewer than 2 runs are selected', async () => {
    await createComponent();
    const mockRuns = makeBenchmarkRuns(['run1']) as any;
    runsServiceSpy.getRuns.mockReturnValue(of(mockRuns));
    component['path'].set('/workspace');
    component['loadRuns']();
    fixture.detectChanges();

    const navigateSpy = jest.spyOn(router, 'navigate');
    component['doCompare']();
    expect(navigateSpy).not.toHaveBeenCalled();
  });
});
