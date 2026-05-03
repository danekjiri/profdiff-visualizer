import { ComponentFixture, TestBed } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { GeneralRunMetadataComponent } from './general-run-metadata.component';
import { RunMetadata } from '../../models/dto/runMetadata';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { MatTooltip } from '@angular/material/tooltip';

describe('GeneralRunMetadataComponent', () => {
  let component: GeneralRunMetadataComponent;
  let fixture: ComponentFixture<GeneralRunMetadataComponent>;

  const mockMetadata = {
    executionId: '857632',
    totalPeriod: 235519192180,
    graalPeriod: 147962720818,
    compilationUnitsCount: 358,
    proftoolMethodsCount: 321,
    benchmarkRunMetadata: {
      profileMetadata: {
        compilationKind: 'JIT'
      },
      benchResultsMetadata: {
        benchmarkName: 'scrabble',
        benchmarkSuite: 'renaissance',
        commitHash: '65df3ec6',
        graalVersion: 'GraalVM CE 25.1.0',
        jdkVersion: 'OpenJDK 64-Bit Server VM GraalVM CE 25.1.0-dev+8.1',
        machinePlatform: 'Linux-x86_64'
      },
      runName: '32'
    }
  } as unknown as RunMetadata;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [GeneralRunMetadataComponent, NoopAnimationsModule] 
    }).compileComponents();

    fixture = TestBed.createComponent(GeneralRunMetadataComponent);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    fixture.detectChanges();
    expect(component).toBeTruthy();
  });

  it('should display "No metadata available" when metadata input is null', () => {
    fixture.componentRef.setInput('metadata', null);
    fixture.detectChanges();

    const fallbackDiv = fixture.debugElement.query(By.css('.general-run-metadata'));
    expect(fallbackDiv).toBeTruthy();
    expect(fallbackDiv.nativeElement.textContent.trim()).toBe('No metadata available.');
  });

  it('should render numeric metadata as strings', () => {
    fixture.componentRef.setInput('metadata', mockMetadata);
    fixture.detectChanges();

    const labels = fixture.debugElement.queryAll(By.css('.tile-label b'));
    const values = fixture.debugElement.queryAll(By.css('.tile-value'));

    const totalPeriodIndex = labels.findIndex(l => l.nativeElement.textContent.includes('Total Periods:'));
    expect(totalPeriodIndex).toBeGreaterThan(-1);
    expect(values[totalPeriodIndex].nativeElement.textContent.trim()).toBe('235,519,192,180');
  });

  it('should apply matTooltip with the full value for all metadata items', () => {
    fixture.componentRef.setInput('metadata', mockMetadata);
    fixture.detectChanges();

    const valueElements = fixture.debugElement.queryAll(By.directive(MatTooltip));
    
    const jdkTooltip = valueElements.find(el => el.nativeElement.textContent.includes('OpenJDK'));
    const tooltipInstance = jdkTooltip?.injector.get(MatTooltip);
    
    expect(tooltipInstance?.message).toBe(mockMetadata.benchmarkRunMetadata.benchResultsMetadata?.jdkVersion);
  });

  it('should compute and render General Metadata and Benchmark Metadata headers', () => {
    fixture.componentRef.setInput('metadata', mockMetadata);
    fixture.detectChanges();

    const headers = fixture.debugElement.queryAll(By.css('h3'));
    expect(headers.length).toBe(2);
    expect(headers[0].nativeElement.textContent.trim()).toBe('General Metadata');
    expect(headers[1].nativeElement.textContent.trim()).toBe('Benchmark Metadata');
  });

  it('should map the "cols" signal input to grid layout and title spanning', () => {
    fixture.componentRef.setInput('metadata', mockMetadata);
    const testCols = 4;
    fixture.componentRef.setInput('cols', testCols);
    fixture.detectChanges();

    const gridLists = fixture.debugElement.queryAll(By.css('mat-grid-list'));
    expect(gridLists.length).toBeGreaterThan(0);
    
    gridLists.forEach(grid => {
      expect(grid.componentInstance.cols).toBe(testCols);
    });
  });
});