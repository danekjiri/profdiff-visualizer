/**
 * Represents a single point on the chart (X/Y coordinates).
 * * name: The X-axis label (e.g., "1", "2").
 * * value: The Y-axis measurement.
 */
export interface ChartDataPoint {
  name: string;
  value: number;
}

/**
 * Represents a complete dataset to be rendered in the chart.
 */
export interface ChartSeries {
  name: string;
  series: ChartDataPoint[];
}

/**
 * Utility factory to transform bench-results.json metric data into the format required by ngx-charts.
 */
export class ChartFactory {
  /**
   * Transforms raw metric values into a structured ChartSeries object.
   */
  static from(runName: string | null, metricValues?: number[]): ChartSeries | null {
    if (!metricValues || metricValues.length === 0) {
      return null;
    }

    return {
      name: runName ?? 'Run',
      series: metricValues.map((value, index) => ({
        name: (index + 1).toString(),
        value: value
      }))
    };
  }
}
