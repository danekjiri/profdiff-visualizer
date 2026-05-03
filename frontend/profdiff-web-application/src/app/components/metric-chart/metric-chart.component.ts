import { CommonModule } from '@angular/common';
import { Component, computed, effect, input, signal, untracked } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatTooltipModule } from '@angular/material/tooltip';
import { NgxChartsModule } from '@swimlane/ngx-charts';
import { ChartSeries } from '../../models/chart-series';

/**
 * MetricChartComponent displays a line chart of benchmark metrics.
 * It provides controls to filter the data range (slice) by iteration index.
 */
@Component({
  selector: 'app-metric-chart',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    NgxChartsModule,
    MatIconModule,
    MatButtonModule,
    MatTooltipModule,
    MatFormFieldModule,
    MatInputModule
  ],
  templateUrl: './metric-chart.component.html',
  styleUrls: ['./metric-chart.component.css']
})
export class MetricChartComponent {
  // Input signal for the formatted chart datasets.
  readonly chartData = input.required<ChartSeries[]>();

  // Signal to control the visibility of the chart panel.
  protected readonly isChartOpen = signal(false);
  // Signals for the iteration range filter (from - to).
  protected readonly sliceStartInput = signal(0);
  protected readonly sliceEndInput = signal(0);
  // Computed signal checking if valid data exists to display.
  protected readonly canOpen = computed(() => (this.chartData() ?? []).length > 0);

  // Fixed dimensions for the chart view [width, height].
  protected readonly chartView: [number, number] = [700, 300];
  // Computed signal that calculates the maximum series length across all runs to determine the upper limit.
  protected readonly maxIterations = computed(() => {
    const data = this.chartData() ?? [];
    if (data.length === 0 || !data[0].series) {
      return 0;
    }
    return Math.max(...data.map((run) => run.series.length));
  });
  // Computed signal that generates the sliced dataset based on current sliceStartInput/sliceEndInput signals.
  protected readonly filteredChartData = computed(() => {
    const originalData = this.chartData() ?? [];

    return originalData.map((run) => ({
      ...run,
      series: run.series.slice(this.sliceStartInput(), this.sliceEndInput() ?? this.maxIterations()),
    }));
  });

  constructor() {
    // effect to automatically reset the 'to' input when the dataset changes
    effect(() => {
      const max = this.maxIterations();
      untracked(() => this.sliceEndInput.set(max));
    });
  }

  /**
   * Updates the lower bound (from) while user is typing with some logical clamping.
   */
  protected setFromByInput(value: number): void {
    let newValue = value < 0 ? 0 : value;
    this.sliceStartInput.set(newValue);
  }

  /**
   * Updates the upper bound (to) while user is typing with some logical clamping.
   */
  protected setToByInput(value: number): void {
    const max = this.maxIterations();
    let newValue = value > max ? max : value;
    this.sliceEndInput.set(newValue);
  }

  /**
   * Validates range consistency (triggered on blur).
   * Ensures 'From' < 'To' by adjusting boundaries if they overlap.
   */
  protected validateRange(): void {
    const start = this.sliceStartInput();
    const end = this.sliceEndInput();
    const max = this.maxIterations();

    if (end <= start) {
      // push 'end' forward, but do not exceed max
      let newEnd = Math.min(start + 1, max);

      // if 'end' hits max and still overlaps, pull 'start' backward
      if (newEnd <= start) {
        const newStart = Math.max(0, newEnd - 1);
        this.sliceStartInput.set(newStart);
      }

      this.sliceEndInput.set(newEnd);
    }
  }

  /**
   * Toggles the chart visibility state.
   */
  protected toggleChart(): void {
    this.isChartOpen.update((isOpen) => !isOpen);
  }
}
