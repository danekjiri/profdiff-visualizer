import { Component, input, output, computed, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatCardModule } from '@angular/material/card';
import { MatChip, MatChipTrailingIcon } from '@angular/material/chips';
import { CompilationUnitMetadata } from '../../models/dto/compilationUnitMetadata';
import { MatTooltipModule } from "@angular/material/tooltip";
import { MatIconModule } from '@angular/material/icon';
import { MatSnackBar } from '@angular/material/snack-bar';

/**
 * Represents a card component for displaying compilation unit with its metadata.
 * It supports both regular and fragment compilation units (with the navigation to its parents).
 */
@Component({
  selector: 'app-compilation-unit-card',
  standalone: true,
  imports: [
    CommonModule,
    MatCardModule,
    MatChip,
    MatChipTrailingIcon,
    MatTooltipModule,
    MatIconModule
  ],
  templateUrl: './compilation-unit-card.component.html',
  styleUrl: './compilation-unit-card.component.css'
})
export class CompilationUnitCardComponent {
  // Compilation units passed to be displayed in the card.
  public readonly compilation = input.required<CompilationUnitMetadata>();
  // The optional, total period of the run, used for hotness calculation.
  public readonly totalPeriod = input<number | undefined>();
  // Flag indicating whether profiler data is available, used to conditionally show hotness and other profiler-specific info.
  public readonly isProfilerAvailable = input<boolean>(false);
  // Flag for visual selection state of the card controlled by the parent component.
  public readonly isSelected = input<boolean>(false);
  // Flag to disable interaction with the card, used when data is still loading or in an error state.
  public readonly disabled = input<boolean>(false);
  // Parent name method resolver function (for extracted fragment compilation units).
  public readonly parentMethodResolver = input<(parentId: string) => string | undefined>();

  // Emitted when the user clicks the Fragment chip to navigate to the parent compilation unit.
  public readonly parentCompilationSelected = output<string>();
  // Emitted when the user clicks on the card itself to select this specific compilation unit.
  public readonly compilationSelected = output<string>();

  // Parse the parent compilation unit ID from fragments (numbers before '#').
  protected readonly parentId = computed(() => {
    if (!(this.compilation().isFragment && this.compilation().id.includes('#'))) {
      return null;
    }
    return this.compilation().id.split('#')[0];
  });

  /**
   *  Resolves the parent method name using the provided resolver function.
   */
  protected parentMethodName() {
    const resolver = this.parentMethodResolver();
    if (this.parentId() && resolver) {
      return resolver(this.parentId()!);
    }
    return undefined;
  };

  /**
   * Generates a tooltip string based on parent existence.
   */
  protected fragmentTooltip(): string {
    const parentId = this.parentId();
    const parentName = this.parentMethodName();
    
    if (!parentId) {
      return '';
    }
    return parentName ? `Compilation Fragment of '${parentName}'.` : 'Unknown parent method';
  };

  /**
   * Handles clicks on the card, emitting the selected compilation unit ID if not disabled.
   */
  protected onCardClick() {
    if (!this.disabled()) {
      this.compilationSelected.emit(this.compilation().id);
    }
  }

  /**
   * Handles clicks on the Fragment chip, emitting the parent compilation unit ID.
   */
  protected onParentClick(event: Event) {
    event.stopPropagation();
    event.preventDefault();
    
    if (this.disabled() || !this.parentId()) {
      return;
    }
    this.parentCompilationSelected.emit(this.parentId()!);
  }
}