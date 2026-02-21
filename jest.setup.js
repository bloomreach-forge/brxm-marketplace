// Runs before the test framework — window (jsdom) is available but beforeEach is not yet.
require('angular');
// Stub window.jasmine so angular-mocks (loaded in setupFilesAfterEnv) will define module()/inject().
window.jasmine = { isSpy: function() { return false; } };
// Declare the host module that forgeMarketplace.js attaches its service and controller to.
window.angular.module('hippo.essentials', []);
require('./marketplace-essentials/src/main/resources/META-INF/resources/tool/forgeMarketplace/forgeMarketplace.js');
