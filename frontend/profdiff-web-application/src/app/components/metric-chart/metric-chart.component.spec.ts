import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { By } from '@angular/platform-browser';
import { MetricChartComponent } from './metric-chart.component';
import { ChartSeries } from '../../models/chart-series';

describe('MetricChartComponent', () => {
  let component: MetricChartComponent;
  let fixture: ComponentFixture<MetricChartComponent>;

  const mockChartData: ChartSeries[] = [
    {
      name: 'RunA',
      series: [
        { name: '0', value: 100 },
        { name: '1', value: 110 },
        { name: '2', value: 120 }
      ]
    },
    {
      name: 'RunB',
      series: [
        { name: '0', value: 90 },
        { name: '1', value: 95 }
      ]
    }
  ];

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [
        MetricChartComponent,
        NoopAnimationsModule
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(MetricChartComponent);
    component = fixture.componentInstance;

    fixture.componentRef.setInput('chartData', mockChartData);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should compute maxIterations based on the longest series', () => {
    expect(component['maxIterations']()).toBe(3);
    expect(component['canOpen']()).toBe(true);
  });

  it('should update sliceEndInput to maxIterations automatically when data changes', () => {
    expect(component['sliceEndInput']()).toBe(3);

    const extendedData = [{
      name: 'RunC',
      series: [ {name: '0', value: 1}, {name: '1', value: 2}, {name: '2', value: 3}, {name: '3', value: 4}, {name: '4', value: 5} ]
    }];
    
    fixture.componentRef.setInput('chartData', extendedData);
    fixture.detectChanges();

    expect(component['maxIterations']()).toBe(5);
    expect(component['sliceEndInput']()).toBe(5);
  });

  it('should filter chart data accurately based on slice inputs', () => {
    component['sliceStartInput'].set(1);
    component['sliceEndInput'].set(2);
    fixture.detectChanges();

    const filtered = component['filteredChartData']();
    
    expect(filtered[0].series.length).toBe(1);
    expect(filtered[0].series[0].value).toBe(110);
  });

  it('should clamp the "from" input so it cannot go below 0', () => {
    component['setFromByInput'](-5);
    expect(component['sliceStartInput']()).toBe(0);

    component['setFromByInput'](2);
    expect(component['sliceStartInput']()).toBe(2);
  });

  it('should clamp the "to" input so it cannot exceed maxIterations', () => {
    component['setToByInput'](10);
    expect(component['sliceEndInput']()).toBe(3);

    component['setToByInput'](2);
    expect(component['sliceEndInput']()).toBe(2);
  });

  it('should validate and correct overlapping ranges', () => {
    component['sliceStartInput'].set(2);
    component['sliceEndInput'].set(1);
    
    component['validateRange']();
    
    expect(component['sliceStartInput']()).toBe(2);
    expect(component['sliceEndInput']()).toBe(3);
  });

  it('should validate and pull "start" backwards if overlap occurs at the upper boundary', () => {
    component['sliceStartInput'].set(3);
    component['sliceEndInput'].set(3);
    
    component['validateRange']();
    
    expect(component['sliceStartInput']()).toBe(2);
    expect(component['sliceEndInput']()).toBe(3);
  });

  it('should toggle chart visibility when the button is clicked', () => {
    const button = fixture.debugElement.query(By.css('button[mat-fab]'));
    
    expect(component['isChartOpen']()).toBe(false);
    expect(fixture.debugElement.query(By.css('.chart-panel'))).toBeNull();

    button.nativeElement.click();
    fixture.detectChanges();

    expect(component['isChartOpen']()).toBe(true);
    expect(fixture.debugElement.query(By.css('.chart-panel'))).toBeTruthy();

    button.nativeElement.click();
    fixture.detectChanges();

    expect(component['isChartOpen']()).toBe(false);
  });

  it('should disable the toggle button if there is no valid chart data', () => {
    fixture.componentRef.setInput('chartData', []);
    fixture.detectChanges();

    const fabButton = fixture.debugElement.query(By.css('button[mat-fab]'));
    
    expect(component['canOpen']()).toBe(false);
    expect(fabButton.nativeElement.disabled).toBe(true);
  });
});