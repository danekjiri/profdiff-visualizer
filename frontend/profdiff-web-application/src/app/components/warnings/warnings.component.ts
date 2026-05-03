import { CommonModule } from '@angular/common';
import { Component, input } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatExpansionModule } from '@angular/material/expansion';
import { MatIconModule } from '@angular/material/icon';
import { WarningMessage } from '../../models/dto/warningMessage';

/**
 * WarningsComponent displays a collapsible list of system warnings.
 * It is typically shown at the top of report pages to alert users
 * about data issues (e.g., missing profiles, incomplete parsing).
 */
@Component({
  selector: 'app-warnings',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatIconModule,
    MatExpansionModule
  ],
  templateUrl: './warnings.component.html',
  styleUrl: './warnings.component.css'
})
export class WarningsComponent {
  // Input list of warning messages to display.
  readonly warnings = input<WarningMessage[] | undefined>();
}
