import { Directive, ElementRef, HostListener, inject } from '@angular/core';
import { MatTooltip } from '@angular/material/tooltip';

/**
 * Conditionally enables a tooltip based on content overflow.
 */
@Directive({
  selector: '[matTooltip][appShowIfTruncated]',
  standalone: true
})
export class ShowIfTruncatedDirective {
  private readonly elementRef = inject(ElementRef<HTMLElement>);
  private readonly tooltip = inject(MatTooltip);

  @HostListener('mouseenter')
  checkOverflow(): void {
    const el = this.elementRef.nativeElement;
    // If the content is wider than the visible area, enable the tooltip.
    this.tooltip.disabled = el.scrollWidth <= el.clientWidth;
  }
}
