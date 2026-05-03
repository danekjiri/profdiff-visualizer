import { TopMethod } from './dto/topMethod';

/**
 * Extends the TopMethod type by adding a color property, which is a string representing the assigned color for that method.
 */
export type ColoredTopMethod = TopMethod & { color: string };

/**
 * Generates a color map for the given method names, assigning a unique HSL color to each method name.
 */
export function generateMethodColorMap(methodNames: Iterable<string>): Map<string, string> {
  const colorMap = new Map<string, string>();
  let colorIndex = 0;
  for (const name of methodNames) {
    if (!colorMap.has(name)) {
      const hue = Math.round((colorIndex * 137.5) % 360);
      colorMap.set(name, `hsl(${hue}, 70%, 85%)`);
      colorIndex++;
    }
  }
  return colorMap;
}

/**
 * Applies the provided color map to the list of top methods, returning a new list of ColoredTopMethod with the assigned colors.
 * If a method name does not exist in the color map, it defaults to 'transparent'.
 */
export function applyColorMap(
  methods: TopMethod[],
  colorMap: Map<string, string>
): ColoredTopMethod[] {
  return methods.map(m => ({ ...m, color: colorMap.get(m.name) ?? 'transparent' }));
}