import { Component, input, output, ViewChild, ChangeDetectionStrategy, signal, computed } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatSelectModule } from '@angular/material/select';
import { MatChipsModule } from '@angular/material/chips';
import { MatInputModule } from '@angular/material/input';
import { MatIconModule } from '@angular/material/icon';
import { MatButtonModule } from '@angular/material/button';
import { ScrollingModule, CdkVirtualScrollViewport } from '@angular/cdk/scrolling';

/**
 * Interface representing a selectable method in the dropdown.
 */
export interface MethodSelectItem<T = any> {
  name: string;
  hotness?: number;
  isUnpaired?: boolean;
  value: T;
}

/**
 * An optimized method selector component designed to handle AOT methods dataset.
 */
@Component({
  selector: 'app-method-selector',
  standalone: true,
  imports: [
    CommonModule,
    MatFormFieldModule,
    MatSelectModule,
    ScrollingModule,
    MatInputModule,
    MatIconModule,
    MatButtonModule,
    MatChipsModule
  ],
  templateUrl: './method-selector.component.html',
  styleUrls: ['./method-selector.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class MethodSelectorComponent<T> {
  // UI label for currently selected method name.
  readonly label = input<string>('Select Method');
  // The complete list of items to display in the virtual scroller.
  readonly items = input.required<MethodSelectItem<T>[]>();
  // The currently selected item wrapper (emits underlying value on change).
  readonly selectedItem = input<MethodSelectItem<T> | null>(null);
  // Emits the underlying value of the selected item.
  readonly selectionChange = output<T>();
  // Optional input to disable the selector, e.g., while loading data.
  readonly disabled = input<boolean>(false);

  // Reference to the virtual scroller to control scroll position.
  @ViewChild(CdkVirtualScrollViewport) protected viewport!: CdkVirtualScrollViewport;
  // Holds the current search text for filtering the items in the dropdown.
  protected readonly searchQuery = signal<string>('');

  // Reactively filters the items list based on the search query.
  protected readonly filteredItems = computed(() => {
    const query = this.searchQuery().toLowerCase();
    const allItems = this.items();
    if (!query) {
      return allItems;
    }
    return allItems.filter((item) => item.name.toLowerCase().includes(query));
  });

  /**
   * Helper to extract the name from the selected item signal.
   */
  protected get selectedName(): string | null {
    return this.selectedItem()?.name ?? null;
  }

  /**
   * Handles selection by finding the full object based on the selected name string.
   */
  protected onSelectionChange(name: string) {
    const selectedWrapper = this.items().find((i) => i.name === name);
    if (selectedWrapper) {
      this.selectionChange.emit(selectedWrapper.value);
    }
  }

  /**
   * Clears the search input and prevents the dropdown from closing.
   */
  protected clearSearch(event: Event) {
    event.stopPropagation();
    this.searchQuery.set('');

    if (this.viewport) {
      this.viewport.scrollToIndex(0);
    }
  }

  /**
   * Updates the search query as the user types.
   */
  protected onSearchInput(event: Event) {
    const inputElement = event.target as HTMLInputElement;
    this.updateSearch(inputElement.value);
  }

  /**
   * Updates the search query.
   */
  public updateSearch(query: string) {
    this.searchQuery.set(query);

    // reset to top
    if (this.viewport) {
      this.viewport.scrollToIndex(0);
    }
  }

  /**
   * Automatically scrolls the virtual viewport to the currently selected item when opened.
   */
  protected onOpenedChange(isOpen: boolean) {
    if (!isOpen) {
      this.searchQuery.set('');
      return;
    }

    const currentName = this.selectedName;
    if (isOpen && currentName) {
      // find index by name
      const index = this.items().findIndex((i) => i.name === currentName);
      if (index > -1) {
        setTimeout(() => {
          if (this.viewport) {
            this.viewport.scrollToIndex(index);
            this.viewport.checkViewportSize();
          }
        }, 0);
      }
    }
  }
}
