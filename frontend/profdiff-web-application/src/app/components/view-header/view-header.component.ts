import { Component, input, inject } from '@angular/core';
import { MatButton } from '@angular/material/button';
import { MatToolbarModule } from '@angular/material/toolbar';
import { RouterLink, ActivatedRoute } from '@angular/router';

/**
 * ViewHeaderComponent renders the top navigation bar.
 * * It displays the current page title and a 'HOME' button on non-home pages.
 */
@Component({
  selector: 'app-view-header',
  standalone: true,
  imports: [
    MatToolbarModule,
    MatButton,
    RouterLink,
  ],
  templateUrl: './view-header.component.html',
  styleUrl: './view-header.component.css'
})
export class ViewHeaderComponent {
  // Signal input for the title displayed in the center of the toolbar.
  readonly pageTitle = input.required<string>();
  // Injected to determine if the 'HOME' button should be displayed.
  private readonly route = inject(ActivatedRoute);

  // Computed property to determine if the current route is the home page.
  protected get returnPath(): string | null {
    return this.route.snapshot.queryParamMap.get('path');
  }
}
