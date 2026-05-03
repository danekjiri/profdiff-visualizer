import { ComponentFixture, TestBed } from '@angular/core/testing';
import { ExperimentOptionsComponent, DEFAULT_HOT_POLICY_OPTIONS } from './experiment-options.component';

describe('ExperimentOptionsComponent', () => {
  let component: ExperimentOptionsComponent;
  let fixture: ComponentFixture<ExperimentOptionsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ExperimentOptionsComponent]
    }).compileComponents();

    fixture = TestBed.createComponent(ExperimentOptionsComponent);
    component = fixture.componentInstance;

    fixture.detectChanges();
  });

  it('should return default configuration values', () => {
    const hotOptions = component.getHotnessOptions();

    expect(hotOptions.hotMinLimit).toBe(DEFAULT_HOT_POLICY_OPTIONS.hotMinLimit);
    expect(hotOptions.hotPercentile).toBe(DEFAULT_HOT_POLICY_OPTIONS.hotPercentile);
  });

  it('should toggle options visibility boolean', () => {
    expect(component['areOptionsOpen']()).toBe(false);

    component['toggleOptions']();
    expect(component['areOptionsOpen']()).toBe(true);
  });
});
