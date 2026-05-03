import { ComponentFixture, TestBed } from '@angular/core/testing';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';
import { By } from '@angular/platform-browser';

import { TopMethodsTableComponent } from './top-methods-table.component';
import { ColoredTopMethod } from '../../models/top-methods-colors';

const makeMethods = (): ColoredTopMethod[] => [
  { executionPercentage: '30%', cycles: '24000', level: 4, name: 'a.b.c.add', id: '1', color: 'hsl(0,70%,85%)' },
  { executionPercentage: '10%', cycles: '16000', level: 4, name: 'a.d.f.remove', id: '2', color: 'hsl(137,70%,85%)' },
  { executionPercentage: '5%', cycles: '8000', level: 3, name: 'a.d.f.put', id: '3', color: 'hsl(275,70%,85%)' }
];

describe('TopMethodsTableComponent', () => {
  let component: TopMethodsTableComponent;
  let fixture: ComponentFixture<TopMethodsTableComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TopMethodsTableComponent, NoopAnimationsModule]
    }).compileComponents();

    fixture = TestBed.createComponent(TopMethodsTableComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should display "Data is not available." when topMethods is null', () => {
    fixture.detectChanges();
    const el = fixture.nativeElement as HTMLElement;
    expect(el.textContent).toContain('Data is not available.');
  });

  it('should render a table row for each method', () => {
    fixture.componentRef.setInput('topMethods', makeMethods());
    fixture.detectChanges();

    const rows = fixture.debugElement.queryAll(By.css('tr.clickable-row'));
    expect(rows.length).toBe(3);
  });

  it('should exclude the "id" column for AOT profiles', () => {
    fixture.componentRef.setInput('isAot', true);
    fixture.componentRef.setInput('topMethods', makeMethods());
    fixture.detectChanges();

    expect(component['displayedColumns']()).not.toContain('id');
  });

  it('should emit methodSelected when a row is clicked', () => {
    const methods = makeMethods();
    fixture.componentRef.setInput('topMethods', methods);
    fixture.detectChanges();

    const spy = jest.fn();
    component.methodSelected.subscribe(spy);

    const rows = fixture.debugElement.queryAll(By.css('tr.clickable-row'));
    rows[1].nativeElement.click();

    expect(spy).toHaveBeenCalledWith(methods[1]);
  });

  it('should emit methodHovered with a method on mouseenter', () => {
    const methods = makeMethods();
    fixture.componentRef.setInput('topMethods', methods);
    fixture.detectChanges();

    const spy = jest.fn();
    component.methodHovered.subscribe(spy);

    const rows = fixture.debugElement.queryAll(By.css('tr.clickable-row'));
    rows[0].nativeElement.dispatchEvent(new MouseEvent('mouseenter'));

    expect(spy).toHaveBeenCalledWith(methods[0]);
  });

  it('should apply "twin-highlight" class to the row matching highlightedMethodName', () => {
    fixture.componentRef.setInput('topMethods', makeMethods());
    fixture.componentRef.setInput('highlightedMethodName', 'a.d.f.remove');
    fixture.detectChanges();

    const rows = fixture.debugElement.queryAll(By.css('tr.clickable-row'));
    expect(rows[1].classes['twin-highlight']).toBe(true);
    expect(rows[0].classes['twin-highlight']).toBeFalsy();
  });

  it('should show "No methods found." message for an empty methods array', () => {
    fixture.componentRef.setInput('topMethods', []);
    fixture.detectChanges();

    const el = fixture.nativeElement as HTMLElement;
    expect(el.textContent).toContain('No methods found.');
  });
});
