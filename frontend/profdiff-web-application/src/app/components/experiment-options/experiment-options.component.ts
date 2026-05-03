import { CommonModule } from '@angular/common';
import { Component, input, output, signal, effect } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { MatButtonModule } from '@angular/material/button';
import { MatCheckboxModule } from '@angular/material/checkbox';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatIconModule } from '@angular/material/icon';
import { MatInputModule } from '@angular/material/input';
import { MatTooltip } from '@angular/material/tooltip';
import { ExperimentProcessingOptions } from '../../models/experiment-processing-options';
import { HotPolicyOptions } from '../../models/hot-policy-options';

// Default configuration constants.
export const DEFAULT_HOT_POLICY_OPTIONS: HotPolicyOptions = {
  hotMinLimit: 1,
  hotMaxLimit: 10,
  hotPercentile: 0.9
};
export const DEFAULT_EXPERIMENT_PROCESSING_OPTIONS: ExperimentProcessingOptions = {
  createFragments: true,
  sortInliningTree: true,
  sortUnorderedPhases: true,
  removeDetailedPhases: true,
  pruneIdentities: true,
  longBci: false,
  inlinerReasoning: false
};

/**
 * ExperimentOptionsComponent provides a floating panel to configure
 * hotness thresholds and tree rendering options.
 */
@Component({
  selector: 'app-experiment-options',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    MatButtonModule,
    MatIconModule,
    MatCheckboxModule,
    MatFormFieldModule,
    MatInputModule,
    MatTooltip
  ],
  templateUrl: './experiment-options.component.html',
  styleUrl: './experiment-options.component.css'
})
export class ExperimentOptionsComponent {
  // Optional override to lock the 'Prune Identities' option, that is useful in Report view.
  public readonly forcedPruneIdentities = input<boolean | undefined>(undefined);
  // Optional input to disable all hot policy options, when no profile data available.
  public readonly disabledHotPolicyOptions = input<boolean>(false);
  // Emits when the user clicks 'Apply', signaling the parent component could fetch corresponding data.
  readonly optionsApplied = output<void>();
  // Signal to control the visibility of the options panel.
  protected readonly areOptionsOpen = signal(false);

  // Hot Policy Signals.
  protected readonly tempHotMinLimit = signal(DEFAULT_HOT_POLICY_OPTIONS.hotMinLimit);
  protected readonly tempHotMaxLimit = signal(DEFAULT_HOT_POLICY_OPTIONS.hotMaxLimit);
  protected readonly tempHotPercentile = signal(DEFAULT_HOT_POLICY_OPTIONS.hotPercentile);
  // Processing Option Signals.
  protected readonly tempCreateFragments = signal(DEFAULT_EXPERIMENT_PROCESSING_OPTIONS.createFragments);
  protected readonly tempLongBci = signal(DEFAULT_EXPERIMENT_PROCESSING_OPTIONS.longBci);
  protected readonly tempSortInliningTree = signal(DEFAULT_EXPERIMENT_PROCESSING_OPTIONS.sortInliningTree);
  protected readonly tempSortUnorderedPhases = signal(DEFAULT_EXPERIMENT_PROCESSING_OPTIONS.sortUnorderedPhases);
  protected readonly tempRemoveDetailedPhases = signal(DEFAULT_EXPERIMENT_PROCESSING_OPTIONS.removeDetailedPhases);
  protected readonly tempPruneIdentities = signal(DEFAULT_EXPERIMENT_PROCESSING_OPTIONS.pruneIdentities);
  protected readonly tempInlinerReasoning = signal(DEFAULT_EXPERIMENT_PROCESSING_OPTIONS.inlinerReasoning);

  constructor() {
    // sync forced values into internal state whenever they change
    effect(() => {
      const forced = this.forcedPruneIdentities();
      if (forced !== undefined) {
        this.tempPruneIdentities.set(forced);
      }
    });
  }

  /**
   * Toggles the visibility of the settings pop-up.
   */
  protected toggleOptions(): void {
    this.areOptionsOpen.update((v) => !v);
  }

  /**
   * Closes the panel and emits an event to the parent to reload data with new settings.
   */
  protected applyChanges(): void {
    this.areOptionsOpen.set(false);
    this.optionsApplied.emit();
  }

  /**
   * Validates and normalizes numeric inputs to prevent malformed serialization.
   * Reverts to the default value if the input is empty, null, or non-finite.
   */
  private normalizeNumeric(value: any, fallback: number | undefined): number | undefined {
    if (value === null || value === undefined || value === '') {
      return fallback;
    }
    const parsed = Number(value);
    return Number.isFinite(parsed) ? parsed : fallback;
  }

  /**
   * Constructs the HotPolicyOptions object from current signal values used for the API request.
   */
  public getHotnessOptions(): HotPolicyOptions {
    return {
      hotMinLimit: this.normalizeNumeric(this.tempHotMinLimit(), DEFAULT_HOT_POLICY_OPTIONS.hotMinLimit),
      hotMaxLimit: this.normalizeNumeric(this.tempHotMaxLimit(), DEFAULT_HOT_POLICY_OPTIONS.hotMaxLimit),
      hotPercentile: this.normalizeNumeric(this.tempHotPercentile(), DEFAULT_HOT_POLICY_OPTIONS.hotPercentile)
    };
  }

  /**
   * Constructs the ExperimentProcessingOptions object from current signal values used for the API request.
   */
  public getRenderingOptions(): ExperimentProcessingOptions {
    return {
      createFragments: this.tempCreateFragments(),
      longBci: this.tempLongBci(),
      sortInliningTree: this.tempSortInliningTree(),
      sortUnorderedPhases: this.tempSortUnorderedPhases(),
      removeDetailedPhases: this.tempRemoveDetailedPhases(),
      pruneIdentities: this.forcedPruneIdentities() ?? this.tempPruneIdentities(),
      inlinerReasoning: this.tempInlinerReasoning()
    };
  }
}