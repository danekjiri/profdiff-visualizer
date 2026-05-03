import { TestBed } from '@angular/core/testing';
import { ErrorMessageComponent } from './error-message.component';

describe('ErrorMessageComponent', () => {
  it('should display the error message text', async () => {
    await TestBed.configureTestingModule({
      imports: [ErrorMessageComponent]
    }).compileComponents();

    const fixture = TestBed.createComponent(ErrorMessageComponent);
    const component = fixture.componentInstance;

    fixture.componentRef.setInput('error', { message: 'Unknown error' });
    fixture.detectChanges();

    const element = fixture.nativeElement as HTMLElement;
    expect(element.textContent).toContain('Unknown error');
  });
});
