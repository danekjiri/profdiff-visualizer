import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { By } from '@angular/platform-browser';

import { WarningsComponent } from './warnings.component';
import { WarningMessage } from '../../models/dto/warningMessage';

describe('WarningsComponent', () => {
  let component: WarningsComponent;
  let fixture: ComponentFixture<WarningsComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [WarningsComponent, NoopAnimationsModule],
    }).compileComponents();

    fixture = TestBed.createComponent(WarningsComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should render nothing when warnings input is undefined', () => {
    const panel = fixture.debugElement.query(By.css('mat-expansion-panel'));
    expect(panel).toBeNull();
  });

  it('should render nothing when warnings array is empty', () => {
    fixture.componentRef.setInput('warnings', []);
    fixture.detectChanges();

    const panel = fixture.debugElement.query(By.css('mat-expansion-panel'));
    expect(panel).toBeNull();
  });

  it('should render the expansion panel when warnings are present', () => {
    const warnings: WarningMessage[] = [{ message: 'Profile missing', type: 'PROFILE' }];
    fixture.componentRef.setInput('warnings', warnings);
    fixture.detectChanges();

    const panel = fixture.debugElement.query(By.css('mat-expansion-panel'));
    expect(panel).toBeTruthy();
  });

  it('should render a list item for each warning message', () => {
    const warnings: WarningMessage[] = [
      { message: 'First warning', type: 'GENERAL' },
      { message: 'Second warning', type: 'PROFILE' }
    ];
    fixture.componentRef.setInput('warnings', warnings);
    fixture.detectChanges();

    const items = fixture.debugElement.queryAll(By.css('li'));
    expect(items.length).toBe(2);
    expect(items[0].nativeElement.textContent).toContain('First warning');
    expect(items[1].nativeElement.textContent).toContain('Second warning');
  });

  it('should show the warning type', () => {
    const warnings: WarningMessage[] = [{ message: 'Something bad happened', type: 'PARSING' }];
    fixture.componentRef.setInput('warnings', warnings);
    fixture.detectChanges();

    const typeLabel = fixture.debugElement.query(By.css('.warning-type'));
    expect(typeLabel).toBeTruthy();
    expect(typeLabel.nativeElement.textContent).toContain('PARSING');
  });
});
