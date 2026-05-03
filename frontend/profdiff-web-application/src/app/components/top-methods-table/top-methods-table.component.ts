import { Component, computed, input, output } from '@angular/core';
import { MatTableModule } from '@angular/material/table';
import { TopMethod } from '../../models/dto/topMethod';
import { MatTooltip } from '@angular/material/tooltip';
import { ShowIfTruncatedDirective } from '../../directives/show-if-truncated.directive';
import { ColoredTopMethod } from '../../models/top-methods-colors';

/**
 * Displays a table of the top methods (greatest execution percentage).
 * The table allows users to hover and click on rows to trigger events for cross-table highlighting and selection.
 */
@Component({
  selector: 'app-top-methods-table',
  imports: [MatTableModule, MatTooltip, ShowIfTruncatedDirective],
  templateUrl: './top-methods-table.component.html',
  styleUrl: './top-methods-table.component.css'
})
export class TopMethodsTableComponent {
  // Input for the list of colored top methods to display in the table.
  readonly topMethods = input<ColoredTopMethod[] | null>(null);
  // Input to determine if the data is from an AOT profile, which affects the displayed columns.
  readonly isAot = input<boolean>(false);
  // Input for the name of the method currently hovered in a linked twin table.
  readonly highlightedMethodName = input<string | null>(null);
  // Optional input to disable the table click, e.g., while loading tree data.
  readonly disabled = input<boolean>(false);

  // Output emits the selected method when a row is hovered.
  readonly methodSelected = output<TopMethod>();
  // Emits the method being hovered over, or null when the mouse leaves.
  readonly methodHovered = output<TopMethod | null>();

  // Dynamically computes the visible columns based on the profile type.
  protected readonly displayedColumns = computed(() => {
    if (this.isAot()) {
      // hide compilationId for AOT profiles
      return ['executionPercentage', 'cycles', 'name'];
    }
    return ['executionPercentage', 'cycles', 'id', 'name'];
  });

  /**
   * Triggers the hover event for cross-table highlighting.
   */
  protected onRowMouseEnter(row: TopMethod): void {
    this.methodHovered.emit(row);
  }

  /**
   * Clears the hover event when the mouse leaves the row.
   */
  protected onRowMouseLeave(): void {
    this.methodHovered.emit(null);
  }

  /**
   * Triggers the selection event when a row is clicked.
   */
  protected onRowClick(row: TopMethod): void {
    this.methodSelected.emit(row);
  }
}
