const nextJest = require('next/jest')

const createJestConfig = nextJest({
  dir: './',
})

const customJestConfig = {
  setupFilesAfterEnv: ['<rootDir>/jest.setup.ts'],
  testEnvironment: 'jest-environment-jsdom',
  moduleNameMapper: {
    '^@/(.*)$': '<rootDir>/$1',
  },
  collectCoverage: true,
  collectCoverageFrom: [
    'lib/**/*.{ts,tsx}',
    'components/shared/**/*.{ts,tsx}',
    'components/ui/**/*.{ts,tsx}',
    '!lib/mockData.ts',
    '!lib/types.ts',
    '!components/shared/MapView.tsx',
    '!components/shared/SignalerModal.tsx',
    '!components/shared/PageBackdrop.tsx',
    '!components/ui/dialog.tsx',
  ],
  coverageReporters: ['text', 'lcov', 'clover'],
}

module.exports = createJestConfig(customJestConfig)
