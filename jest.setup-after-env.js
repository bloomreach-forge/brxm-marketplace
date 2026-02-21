// Runs after Jest installs beforeEach/afterEach — angular-mocks needs these to be global.
// window.jasmine was stubbed in jest.setup.js so angular-mocks will define module()/inject().
require('angular-mocks');
