import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Component } from '@angular/core';
import { By } from '@angular/platform-browser';
import { FloatingDockComponent } from './floating-dock.component';

@Component({
  standalone: true,
  imports: [FloatingDockComponent],
  template: `
    <app-floating-dock>
      <button dock-actions id="test-action-btn">Action Button</button>
      <div dock-content id="test-widget">Chart Widget</div>
    </app-floating-dock>
  `
})
class TestHostComponent {}

describe('FloatingDockComponent', () => {
  let hostFixture: ComponentFixture<TestHostComponent>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TestHostComponent, FloatingDockComponent] 
    }).compileComponents();

    hostFixture = TestBed.createComponent(TestHostComponent);
    hostFixture.detectChanges();
  });

  it('should create the floating dock inside the host', () => {
    const dockComponent = hostFixture.debugElement.query(By.directive(FloatingDockComponent));
    expect(dockComponent).toBeTruthy();
  });

  it('should project dock-actions elements into the .dock-actions container', () => {
    const actionsContainer = hostFixture.debugElement.query(By.css('.dock-actions'));
    expect(actionsContainer).toBeTruthy();

    const projectedButton = actionsContainer.query(By.css('#test-action-btn'));
    expect(projectedButton).toBeTruthy();
    expect(projectedButton.nativeElement.textContent.trim()).toBe('Action Button');
  });

  it('should project dock-content elements into the .dock-content container', () => {
    const contentContainer = hostFixture.debugElement.query(By.css('.dock-content'));
    expect(contentContainer).toBeTruthy();

    const projectedWidget = contentContainer.query(By.css('#test-widget'));
    expect(projectedWidget).toBeTruthy();
    expect(projectedWidget.nativeElement.textContent.trim()).toBe('Chart Widget');
  });
});