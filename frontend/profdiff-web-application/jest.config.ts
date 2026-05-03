import type { Config } from 'jest';

const config: Config = {
  preset: 'jest-preset-angular',
  setupFilesAfterEnv: ['<rootDir>/setup-jest.ts'],
  testPathIgnorePatterns: [
    '<rootDir>/node_modules/',
    '<rootDir>/dist/',
  ],
  transformIgnorePatterns: [
    'node_modules/(?!.*\\.mjs$|@swimlane|d3-.*|internmap)'
  ],
};
export default config;