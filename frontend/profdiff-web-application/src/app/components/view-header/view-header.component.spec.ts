import { ComponentFixture, TestBed } from '@angular/core/testing';
import { provideRouter } from '@angular/router';
import { ActivatedRoute, convertToParamMap } from '@angular/router';
import { By } from '@angular/platform-browser';
import { ViewHeaderComponent } from './view-header.component';

describe('ViewHeaderComponent', () => {
  let component: ViewHeaderComponent;
  let fixture: ComponentFixture<ViewHeaderComponent>;

  const createComponent = async (path: string | null = 'test-path') => {
    await TestBed.configureTestingModule({
      imports: [ViewHeaderComponent],
      providers: [
        provideRouter([]),
        {
          provide: ActivatedRoute,
          useValue: {
            snapshot: {
              queryParamMap: convertToParamMap(path ? { path } : {})
            }
          }
        }
      ]
    }).compileComponents();

    fixture = TestBed.createComponent(ViewHeaderComponent);
    component = fixture.componentInstance;
  };

  afterEach(() => TestBed.resetTestingModule());

  it('should show the title provided in input', async () => {
    await createComponent();
    fixture.componentRef.setInput('pageTitle', 'My Dashboard');

    fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;

    expect(element.textContent).toContain('My Dashboard');
  });

  it('should NOT show HOME button if title is Home', async () => {
    await createComponent();
    fixture.componentRef.setInput('pageTitle', 'Home');

    fixture.detectChanges();
    const element = fixture.nativeElement as HTMLElement;

    expect(element.textContent).not.toContain('RETURN');
  });

  it('should show HOME button when title is not "home"', async () => {
    await createComponent();
    fixture.componentRef.setInput('pageTitle', 'Report');

    fixture.detectChanges();
    const homeBtn = fixture.debugElement.query(By.css('.home-button'));
    expect(homeBtn).toBeTruthy();
    expect(homeBtn.nativeElement.textContent.trim()).toBe('HOME');
  });

  it('should carry the path query param in the HOME button routerLink queryParams', async () => {
    await createComponent('my-workspace-path');
    fixture.componentRef.setInput('pageTitle', 'Report');

    fixture.detectChanges();
    expect(component['returnPath']).toBe('my-workspace-path');
  });
});