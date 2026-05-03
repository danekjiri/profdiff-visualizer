import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { By } from '@angular/platform-browser';

import { CompilationUnitCardComponent } from './compilation-unit-card.component';
import { CompilationUnitMetadata } from '../../models/dto/compilationUnitMetadata';

const makeCompilation = (overrides: Partial<CompilationUnitMetadata> = {}): CompilationUnitMetadata => ({
  id: '420',
  isFragment: false,
  period: 100,
  isHot: false,
  ...overrides
});

describe('CompilationUnitCardComponent', () => {
  let component: CompilationUnitCardComponent;
  let fixture: ComponentFixture<CompilationUnitCardComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [CompilationUnitCardComponent, NoopAnimationsModule]
    })
    .compileComponents();

    fixture = TestBed.createComponent(CompilationUnitCardComponent);
    component = fixture.componentInstance;

    fixture.componentRef.setInput('compilation', makeCompilation());
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should display the compilation ID in the subtitle', () => {
    fixture.componentRef.setInput('compilation', makeCompilation({ id: '999' }));
    fixture.detectChanges();

    const subtitle = fixture.debugElement.query(By.css('mat-card-subtitle'));
    expect(subtitle.nativeElement.textContent).toContain('999');
  });

  it('should emit compilationSelected with the compilation ID when clicked', () => {
    const spy = jest.fn();
    component.compilationSelected.subscribe(spy);

    const card = fixture.debugElement.query(By.css('mat-card'));
    card.nativeElement.click();

    expect(spy).toHaveBeenCalledWith('420');
  });

  it('should NOT emit compilationSelected when disabled and card is clicked', () => {
    fixture.componentRef.setInput('disabled', true);
    fixture.detectChanges();

    const spy = jest.fn();
    component.compilationSelected.subscribe(spy);

    const card = fixture.debugElement.query(By.css('mat-card'));
    card.nativeElement.click();

    expect(spy).not.toHaveBeenCalled();
  });

  it('should apply "selected" css class when isSelected is true', () => {
    fixture.componentRef.setInput('isSelected', true);
    fixture.detectChanges();

    const card = fixture.debugElement.query(By.css('mat-card'));
    expect(card.classes['selected']).toBe(true);
  });

  it('should apply "ui-blocked" css class when disabled is true', () => {
    fixture.componentRef.setInput('disabled', true);
    fixture.detectChanges();

    const card = fixture.debugElement.query(By.css('mat-card'));
    expect(card.classes['ui-blocked']).toBe(true);
  });

  it('should show "Hot" chip when compilation is hot and profiler is available', () => {
    fixture.componentRef.setInput('compilation', makeCompilation({ isHot: true }));
    fixture.componentRef.setInput('isProfilerAvailable', true);
    fixture.detectChanges();

    const chips = fixture.debugElement.queryAll(By.css('mat-chip'));
    const hotChip = chips.find(c => c.nativeElement.textContent.trim() === 'Hot');
    expect(hotChip).toBeTruthy();
  });

  it('should NOT show "Hot" chip when profiler is unavailable', () => {
    fixture.componentRef.setInput('compilation', makeCompilation({ isHot: true }));
    fixture.componentRef.setInput('isProfilerAvailable', false);
    fixture.detectChanges();

    const chips = fixture.debugElement.queryAll(By.css('mat-chip'));
    const hotChip = chips.find(c => c.nativeElement.textContent.trim() === 'Hot');
    expect(hotChip).toBeUndefined();
  });

  it('should show "Fragment" chip when compilation is a fragment and profiler is available', () => {
    fixture.componentRef.setInput('compilation', makeCompilation({ id: '1#fragment', isFragment: true }));
    fixture.componentRef.setInput('isProfilerAvailable', true);
    fixture.detectChanges();

    const chips = fixture.debugElement.queryAll(By.css('mat-chip'));
    const fragChip = chips.find(c => c.nativeElement.textContent.includes('Fragment'));
    expect(fragChip).toBeTruthy();
  });

  it('should emit parentCompilationSelected when Fragment chip is clicked', () => {
    fixture.componentRef.setInput('compilation', makeCompilation({ id: '1#fragment', isFragment: true }));
    fixture.componentRef.setInput('isProfilerAvailable', true);
    fixture.detectChanges();

    const spy = jest.fn();
    component.parentCompilationSelected.subscribe(spy);

    const chips = fixture.debugElement.queryAll(By.css('mat-chip'));
    const fragChip = chips.find(c => c.nativeElement.textContent.includes('Fragment'));
    fragChip!.nativeElement.click();

    expect(spy).toHaveBeenCalledWith('1');
  });

  it('should compute parentId from the fragment ID (before #)', () => {
    fixture.componentRef.setInput('compilation', makeCompilation({ id: '7#frag', isFragment: true }));
    fixture.detectChanges();

    expect(component['parentId']()).toBe('7');
  });

  it('should display hotness percentage when profiler and totalPeriod are available', () => {
    fixture.componentRef.setInput('compilation', makeCompilation({ period: 500 }));
    fixture.componentRef.setInput('isProfilerAvailable', true);
    fixture.componentRef.setInput('totalPeriod', 1000);
    fixture.detectChanges();

    const el = fixture.nativeElement as HTMLElement;
    expect(el.textContent).toContain('Hotness:');
    expect(el.textContent).toContain('50.00%');
  });
});
