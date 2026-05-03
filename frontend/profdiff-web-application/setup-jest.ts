// setup-jest.ts
import 'jest-preset-angular/setup-env/zone';

// Keep your mocks below the import
Object.defineProperty(Element.prototype, 'scrollTo', {
  value: jest.fn(),
  writable: true,
  configurable: true,
});