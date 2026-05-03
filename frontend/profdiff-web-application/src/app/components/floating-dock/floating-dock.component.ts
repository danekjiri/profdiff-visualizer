import { CommonModule } from '@angular/common';
import { Component } from '@angular/core';

/**
 * FloatingDockComponent creates a sticky bottom bar for global actions and content.
 * It uses content projection (slots) to allow parent components to inject
 * specific buttons (left) or widgets (right).
 */
@Component({
  selector: 'app-floating-dock',
  standalone: true,
  imports: [
    CommonModule
  ],
  templateUrl: './floating-dock.component.html',
  styleUrls: ['./floating-dock.component.css']
})
export class FloatingDockComponent {}
