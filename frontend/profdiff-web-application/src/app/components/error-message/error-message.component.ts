import { Component, input } from '@angular/core';
import { MatCardModule } from '@angular/material/card';
import { MatIconModule } from '@angular/material/icon';
import { ErrorMessage } from '../../models/dto/errorMessage';

/**
 * ErrorMessageComponent displays a standardized error card.
 * * It visualizes error messages with a warning icon and distinctive styling.
 */
@Component({
  selector: 'app-error-message',
  standalone: true,
  imports: [
    MatCardModule,
    MatIconModule
  ],
  templateUrl: './error-message.component.html',
  styleUrls: ['./error-message.component.css'],
})
export class ErrorMessageComponent {
  // The error message to display.
  readonly error = input.required<ErrorMessage>();
}
