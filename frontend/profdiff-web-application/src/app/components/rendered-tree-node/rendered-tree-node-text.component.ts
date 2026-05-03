import { Component, input, signal, computed, ChangeDetectionStrategy, output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RenderedTreeNode } from '../../models/dto/renderedTreeNode';
import { MatTooltip } from "@angular/material/tooltip";

/**
 * Component to render a single tree node with text content recursively, including handling the method navigation.
 */
@Component({
  selector: 'app-rendered-tree-node-text',
  standalone: true,
  imports: [
    CommonModule,
    MatTooltip,
  ],
  templateUrl: './rendered-tree-node-text.component.html',
  styleUrls: ['./rendered-tree-node-text.component.css'],
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class RenderedTreeNodeTextComponent {
  // The node data passed from Compare/Report page.
  public readonly node = input.required<RenderedTreeNode>();
  // Map of method hotnesses passed from parent. e.g., {'java.lang.String': { hotness1: 0.15, hotness2: 0.22 }}
  public readonly hotnessMap = input<Record<string, { hotness1?: number, hotness2?: number }>>({});
  // Emit method name up the tree to the parent page component to trigger navigation
  public readonly methodClicked = output<string>();

  // State to track if the node is expanded or collapsed.
  protected readonly isExpanded = signal(true);
  // INFO nodes (like percentage details) are separate from the main tree structure and start collapsed.
  protected readonly isInfoExpanded = signal(false);

  // Separate INFO nodes from the actual tree branches.
  protected readonly infoChildren = computed(() => this.node().children?.filter(c => c.marker === 'INFO') || []);
  protected readonly regularChildren = computed(() => this.node().children?.filter(c => c.marker !== 'INFO') || []);

  // Determine if the node has any non-INFO children to decide if it should be expandable.
  protected readonly hasChildren = computed(() => (this.node().children || []).length > 0);

  /**
   * Toggle the expanded/collapsed state of the node when the main content is clicked.
   */
  protected toggleExpand(event: Event) {
    event.stopPropagation();
    if (this.hasChildren()) {
      this.isExpanded.update(v => !v);
    }
  }

  /**
   * Toggle the expanded/collapsed state of the INFO details when the INFO header is clicked.
   */
  protected toggleInfo(event: Event) {
    event.stopPropagation();
    this.isInfoExpanded.update(v => !v);
  }

  /**
   * Returns a symbol representing the change type.
   */
  protected get markerSymbol(): string {
    switch (this.node().marker) {
      case 'IDENTITY': return '.';
      case 'INSERT': return '+';
      case 'DELETE': return '-';
      case 'RELABEL': return '*';
      default: return '';
    }
  }

  /**
   * Returns a tooltip help message based on the change type.
   */
  protected get markerTooltip(): string {
    switch (this.node().marker) {
      case 'IDENTITY': return 'Remains unchanged.';
      case 'INSERT': return 'Absent in the left but present in the right compilation.';
      case 'DELETE': return 'Present in the left but absent in the right compilation.';
      case 'RELABEL': return 'Present in both compilations but with different labels.';
      default: return '';
    }
  }

  /**
   * Handle click on a method name to trigger navigation.
   */
  protected onMethodClick(methodName: string, event: Event) {
    event.stopPropagation();
    this.methodClicked.emit(methodName);
  }

  /**
   * Handle click on a method name within INFO nodes.
   */
  protected onChildMethodClicked(event: string) {
    this.methodClicked.emit(event);
  }
}

export class TreeFormatter {
  /**
   * Recursively traverses a RenderedTreeNode to build a formatted string matching the UI.
   */
  public static formatTreeAsText(node: RenderedTreeNode, depth: number = 0): string {
    let result = '';
    const indent = '  '.repeat(depth);

    if (node.marker === 'INFO') {
      const text = node.content?.rawText || '';
      const isIndentedInfo = !text.trim().startsWith('|_');
      const infoIndent = isIndentedInfo ? '  ' : '';
      result += `${indent}${infoIndent}${text}\n`;
    } else {
      let line = indent;

      switch (node.marker) {
        case 'IDENTITY': line += '. '; break;
        case 'INSERT': line += '+ '; break;
        case 'DELETE': line += '- '; break;
        case 'RELABEL': line += '* '; break;
      }

      if (node.content) {
        if (node.content.action) line += node.content.action;
        if (node.content.methodName) line += node.content.methodName;
        if (!node.content.methodName && node.content.rawText) line += node.content.rawText;
        if (node.content.bci) line += ` at bci ${node.content.bci}`;
        if (node.content.additionalInfo) line += ` with ${node.content.additionalInfo}`;
      }
      result += `${line}\n`;
    }

    if (node.children && node.children.length > 0) {
      const infoChildren = node.children.filter((c) => c.marker === 'INFO');
      const regularChildren = node.children.filter((c) => c.marker !== 'INFO');

      if (infoChildren.length > 0) {
        result += `${indent}  RECEIVER-TYPE-PROFILER || REASONING\n`;
        for (const child of infoChildren) {
          result += TreeFormatter.formatTreeAsText(child, depth + 1);
        }
      }

      for (const child of regularChildren) {
        result += TreeFormatter.formatTreeAsText(child, depth + 1);
      }
    }

    return result;
  }
}
