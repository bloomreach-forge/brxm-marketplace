module.exports = {
    testEnvironment: 'jest-environment-jsdom',
    // setupFiles runs before the test framework — use it to load angular (requires window)
    // and stub window.jasmine so angular-mocks will define module()/inject() globals.
    setupFiles: ['./jest.setup.js'],
    // setupFilesAfterEnv runs after Jest installs beforeEach/afterEach — required by angular-mocks.
    setupFilesAfterEnv: ['./jest.setup-after-env.js'],
    testMatch: ['**/src/test/js/**/*.spec.js']
};
