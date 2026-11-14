const { createCjsPreset } = require('jest-preset-angular/presets');
module.exports = {
  ...createCjsPreset({ tsconfig: '<rootDir>/tsconfig.spec.json' }),
  setupFilesAfterEnv: ['<rootDir>/tests/setup-jest.ts'],
  testMatch: ['<rootDir>/src/**/*.spec.ts'],
  collectCoverageFrom: ['src/app/**/*.ts', '!src/app/**/*.spec.ts', '!src/app/**/*.model.ts'],
  coverageDirectory: '../coverage/front',
  coverageReporters: ['text', 'html', 'lcov'],
  clearMocks: true,
  watchman: false,
};
