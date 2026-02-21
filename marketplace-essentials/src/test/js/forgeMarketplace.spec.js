'use strict';

describe('forgeMarketplaceCtrl', function () {

    var $scope, ctrl, svcMock, $q, $rootScope;

    function makeAddon(overrides) {
        return Object.assign({
            id: 'test-addon',
            name: 'Test Addon',
            description: 'A test addon',
            version: '2.0.0',
            artifacts: [{ type: 'maven-lib', maven: { groupId: 'org.example', artifactId: 'test-addon' }, target: 'cms' }]
        }, overrides);
    }

    function makeEpoch(min, max, inferredMax, version) {
        return {
            version: version || '2.0.0',
            compatibility: { brxm: Object.assign({}, min ? { min: min } : {}, max ? { max: max } : {}) },
            inferredMax: inferredMax || null,
            artifacts: [{ type: 'maven-lib', maven: { groupId: 'org.example', artifactId: 'test-addon' }, target: 'cms' }]
        };
    }

    function resolvedPromise(value) {
        var deferred = $q.defer();
        deferred.resolve(value);
        return deferred.promise;
    }

    function rejectedPromise(value) {
        var deferred = $q.defer();
        deferred.reject(value);
        return deferred.promise;
    }

    function buildController() {
        ctrl = $injector.get('$controller')('forgeMarketplaceCtrl', {
            $scope: $scope,
            marketplaceService: svcMock
        });
        $rootScope.$digest(); // flush init calls
    }

    var $injector;

    beforeEach(angular.mock.module('hippo.essentials'));

    beforeEach(inject(function (_$rootScope_, _$q_, _$injector_) {
        $rootScope = _$rootScope_;
        $q = _$q_;
        $injector = _$injector_;
        $scope = $rootScope.$new();

        svcMock = {
            getAddons: jest.fn(() => resolvedPromise([])),
            getProjectContext: jest.fn(() => resolvedPromise({
                brxmVersion: null,
                javaVersion: null,
                installedAddons: {},
                misconfiguredAddons: {}
            })),
            refreshAddons: jest.fn(() => resolvedPromise({})),
            refreshProjectContext: jest.fn(() => resolvedPromise({})),
            installAddon: jest.fn(() => resolvedPromise({ success: true })),
            uninstallAddon: jest.fn(() => resolvedPromise({ success: true })),
            fixAddon: jest.fn(() => resolvedPromise({ success: true }))
        };
    }));

    describe('getCompatibilityStatus()', function () {

        it('returns unknown when brxmVersion is null', function () {
            buildController();
            var addon = makeAddon({ versions: [makeEpoch('14.0.0', '14.9.9')] });
            expect($scope.getCompatibilityStatus(addon).status).toBe('unknown');
        });

        it('returns compatible when an epoch covers the project version', function () {
            buildController();
            $scope.projectContext.brxmVersion = '14.5.0';
            var addon = makeAddon({ versions: [makeEpoch('14.0.0', '14.9.9')] });
            expect($scope.getCompatibilityStatus(addon).status).toBe('compatible');
        });

        it('returns incompatible when no epoch covers the project version', function () {
            buildController();
            $scope.projectContext.brxmVersion = '15.0.0';
            var addon = makeAddon({ versions: [makeEpoch('14.0.0', '14.9.9')] });
            expect($scope.getCompatibilityStatus(addon).status).toBe('incompatible');
        });

        it('returns compatible via legacy compatibility.brxm min/max when in range', function () {
            buildController();
            $scope.projectContext.brxmVersion = '14.5.0';
            var addon = makeAddon({ versions: [], compatibility: { brxm: { min: '14.0.0', max: '14.9.9' } } });
            expect($scope.getCompatibilityStatus(addon).status).toBe('compatible');
        });

        it('returns incompatible via legacy fallback when below min', function () {
            buildController();
            $scope.projectContext.brxmVersion = '13.0.0';
            var addon = makeAddon({ versions: [], compatibility: { brxm: { min: '14.0.0', max: '14.9.9' } } });
            expect($scope.getCompatibilityStatus(addon).status).toBe('incompatible');
        });

        it('returns incompatible via legacy fallback when above max', function () {
            buildController();
            $scope.projectContext.brxmVersion = '15.0.0';
            var addon = makeAddon({ versions: [], compatibility: { brxm: { min: '14.0.0', max: '14.9.9' } } });
            expect($scope.getCompatibilityStatus(addon).status).toBe('incompatible');
        });

        it('returns unknown when addon has no compatibility info at all', function () {
            buildController();
            $scope.projectContext.brxmVersion = '14.5.0';
            var addon = makeAddon({ versions: [], compatibility: null });
            expect($scope.getCompatibilityStatus(addon).status).toBe('unknown');
        });

        it('returns incompatible when inferredMax equals the project version (exclusive upper bound)', function () {
            buildController();
            $scope.projectContext.brxmVersion = '15.0.0';
            var addon = makeAddon({ versions: [makeEpoch('14.0.0', null, '15.0.0')] });
            expect($scope.getCompatibilityStatus(addon).status).toBe('incompatible');
        });
    });

    describe('hasProjectContext()', function () {

        it('returns false when brxmVersion is null (initial state)', function () {
            buildController();
            expect($scope.hasProjectContext()).toBe(false);
        });

        it('returns true after brxmVersion is set', function () {
            buildController();
            $scope.projectContext.brxmVersion = '14.5.0';
            expect($scope.hasProjectContext()).toBe(true);
        });
    });

    describe('installAddon()', function () {

        beforeEach(function () {
            buildController();
            $scope.projectContext.brxmVersion = '14.5.0';
        });

        it('shows compat warning dialog when addon is incompatible and project context is present', function () {
            var addon = makeAddon({ versions: [makeEpoch('13.0.0', '13.9.9')] });
            $scope.installAddon(addon);
            expect($scope.showCompatWarning).toBe(true);
            expect($scope.addonToInstall).toBe(addon);
        });

        it('does NOT call marketplaceService.installAddon when intercepted by dialog', function () {
            var addon = makeAddon({ versions: [makeEpoch('13.0.0', '13.9.9')] });
            $scope.installAddon(addon);
            expect(svcMock.installAddon).not.toHaveBeenCalled();
        });

        it('calls marketplaceService.installAddon directly when no project context', function () {
            $scope.projectContext.brxmVersion = null;
            var addon = makeAddon({ versions: [makeEpoch('13.0.0', '13.9.9')] });
            $scope.installAddon(addon);
            $rootScope.$digest();
            expect(svcMock.installAddon).toHaveBeenCalledWith('test-addon', false);
        });

        it('calls marketplaceService.installAddon directly when addon is compatible', function () {
            var addon = makeAddon({ versions: [makeEpoch('14.0.0', '14.9.9')] });
            $scope.installAddon(addon);
            $rootScope.$digest();
            expect(svcMock.installAddon).toHaveBeenCalledWith('test-addon', false);
        });
    });

    describe('cancelCompatWarning()', function () {

        it('clears showCompatWarning and addonToInstall', function () {
            buildController();
            $scope.showCompatWarning = true;
            $scope.addonToInstall = makeAddon();
            $scope.cancelCompatWarning();
            expect($scope.showCompatWarning).toBe(false);
            expect($scope.addonToInstall).toBeNull();
        });
    });

    describe('proceedInstall()', function () {

        it('closes dialog, clears addonToInstall, calls installAddon, sets installing=true', function () {
            buildController();
            var addon = makeAddon();
            $scope.showCompatWarning = true;
            $scope.addonToInstall = addon;

            $scope.proceedInstall();

            expect($scope.showCompatWarning).toBe(false);
            expect($scope.addonToInstall).toBeNull();
            expect($scope.installing).toBe(true);
            expect(svcMock.installAddon).toHaveBeenCalledWith('test-addon', false);
        });

        it('sets installing = false after promise resolves', function () {
            buildController();
            $scope.addonToInstall = makeAddon();
            $scope.proceedInstall();
            $rootScope.$digest();
            expect($scope.installing).toBe(false);
        });
    });

    describe('uninstall dialog', function () {

        beforeEach(buildController);

        it('confirmUninstall sets showUninstallConfirm=true and stages addon', function () {
            var addon = makeAddon();
            $scope.confirmUninstall(addon);
            expect($scope.showUninstallConfirm).toBe(true);
            expect($scope.addonToUninstall).toBe(addon);
        });

        it('cancelUninstall clears showUninstallConfirm and staged addon', function () {
            $scope.showUninstallConfirm = true;
            $scope.addonToUninstall = makeAddon();
            $scope.cancelUninstall();
            expect($scope.showUninstallConfirm).toBe(false);
            expect($scope.addonToUninstall).toBeNull();
        });

        it('proceedUninstall closes dialog and calls uninstallAddon', function () {
            var addon = makeAddon();
            $scope.addonToUninstall = addon;
            $scope.showUninstallConfirm = true;
            $scope.proceedUninstall();
            expect($scope.showUninstallConfirm).toBe(false);
            expect($scope.addonToUninstall).toBeNull();
            expect(svcMock.uninstallAddon).toHaveBeenCalledWith('test-addon');
        });
    });

    describe('installation guards', function () {

        var addon;

        beforeEach(function () {
            buildController();
            addon = makeAddon();
        });

        describe('canInstall()', function () {
            it('returns false when installing=true', function () {
                $scope.installing = true;
                expect($scope.canInstall(addon)).toBe(false);
            });

            it('returns false when addon is already installed', function () {
                $scope.projectContext.installedAddons['test-addon'] = '1.0.0';
                expect($scope.canInstall(addon)).toBe(false);
            });

            it('returns true when not installed, not installing, with installable artifacts', function () {
                expect($scope.canInstall(addon)).toBe(true);
            });

            it('returns false when addon has no installable artifacts', function () {
                addon.artifacts = [];
                expect($scope.canInstall(addon)).toBe(false);
            });
        });

        describe('canUpdate()', function () {
            it('returns true when installed, newer version exists, not installing', function () {
                $scope.projectContext.installedAddons['test-addon'] = '1.0.0';
                // addon.version = '2.0.0' → update available
                expect($scope.canUpdate(addon)).toBe(true);
            });

            it('returns false when not installed', function () {
                expect($scope.canUpdate(addon)).toBe(false);
            });

            it('returns false when installing=true', function () {
                $scope.projectContext.installedAddons['test-addon'] = '1.0.0';
                $scope.installing = true;
                expect($scope.canUpdate(addon)).toBe(false);
            });

            it('returns false when installed version equals display version (no update)', function () {
                $scope.projectContext.installedAddons['test-addon'] = '2.0.0';
                expect($scope.canUpdate(addon)).toBe(false);
            });
        });

        describe('canUninstall()', function () {
            it('returns false when not installed', function () {
                expect($scope.canUninstall(addon)).toBe(false);
            });

            it('returns true when installed and not installing', function () {
                $scope.projectContext.installedAddons['test-addon'] = '2.0.0';
                expect($scope.canUninstall(addon)).toBe(true);
            });

            it('returns false when installed but installing=true', function () {
                $scope.projectContext.installedAddons['test-addon'] = '2.0.0';
                $scope.installing = true;
                expect($scope.canUninstall(addon)).toBe(false);
            });
        });

        describe('canFix()', function () {
            it('returns true when misconfigured and not installing', function () {
                $scope.projectContext.misconfiguredAddons['test-addon'] = [{ type: 'WRONG_POM' }];
                expect($scope.canFix(addon)).toBe(true);
            });

            it('returns falsy when not misconfigured', function () {
                // isMisconfigured() short-circuits via &&, returning undefined (not false).
                expect($scope.canFix(addon)).toBeFalsy();
            });

            it('returns false when misconfigured but installing=true', function () {
                $scope.projectContext.misconfiguredAddons['test-addon'] = [{ type: 'WRONG_POM' }];
                $scope.installing = true;
                expect($scope.canFix(addon)).toBe(false);
            });
        });
    });

    describe('applyFilters()', function () {

        var addons;

        beforeEach(function () {
            buildController();
            addons = [
                makeAddon({ id: 'a1', name: 'Alpha Widget', description: 'First addon', category: 'security' }),
                makeAddon({ id: 'a2', name: 'Beta Tool', description: 'Second addon with widget keyword', category: 'seo' }),
                makeAddon({ id: 'a3', name: 'Gamma Utility', description: 'Third addon', category: 'security' })
            ];
            $scope.allAddons = addons;
        });

        it('text search matches on name (case-insensitive)', function () {
            $scope.filter.query = 'alpha';
            $scope.applyFilters();
            expect($scope.addons.length).toBe(1);
            expect($scope.addons[0].id).toBe('a1');
        });

        it('text search matches on description', function () {
            $scope.filter.query = 'widget';
            $scope.applyFilters();
            // 'Alpha Widget' (name) and 'Beta Tool' (description contains 'widget keyword')
            expect($scope.addons.length).toBe(2);
        });

        it('category filter narrows results', function () {
            $scope.filter.category = 'security';
            $scope.applyFilters();
            expect($scope.addons.length).toBe(2);
            expect($scope.addons.map(function (a) { return a.id; })).toEqual(expect.arrayContaining(['a1', 'a3']));
        });

        it("quick filter 'installed' keeps only installed addons", function () {
            $scope.projectContext.installedAddons['a2'] = '1.0.0';
            $scope.quickFilter = 'installed';
            $scope.applyFilters();
            expect($scope.addons.length).toBe(1);
            expect($scope.addons[0].id).toBe('a2');
        });

        it("quick filter 'compatible' keeps only compatible addons", function () {
            $scope.projectContext.brxmVersion = '14.5.0';
            addons[0].versions = [makeEpoch('14.0.0', '14.9.9')];
            addons[1].versions = [makeEpoch('13.0.0', '13.9.9')]; // incompatible
            addons[2].versions = [makeEpoch('14.0.0', '14.9.9')];
            $scope.allAddons = addons;
            $scope.quickFilter = 'compatible';
            $scope.applyFilters();
            expect($scope.addons.length).toBe(2);
            expect($scope.addons.map(function (a) { return a.id; })).toEqual(expect.arrayContaining(['a1', 'a3']));
        });

        it("quick filter 'all' returns everything", function () {
            $scope.quickFilter = 'all';
            $scope.applyFilters();
            expect($scope.addons.length).toBe(3);
        });

        it('updates resultCounts.filtered', function () {
            $scope.filter.query = 'alpha';
            $scope.applyFilters();
            expect($scope.resultCounts.filtered).toBe(1);
        });
    });

    describe('loadProjectContext()', function () {

        it('sets projectContextLoading = true on start, false after resolve', function () {
            var deferred = $q.defer();
            svcMock.getProjectContext = jest.fn(() => deferred.promise);
            buildController();

            $scope.loadProjectContext();
            expect($scope.projectContextLoading).toBe(true);

            deferred.resolve({ brxmVersion: '14.5.0', javaVersion: null, installedAddons: {}, misconfiguredAddons: {} });
            $rootScope.$digest();
            expect($scope.projectContextLoading).toBe(false);
        });

        it('maps successful response to $scope.projectContext', function () {
            var context = { brxmVersion: '14.5.0', javaVersion: '11', installedAddons: { 'addon-a': '1.0' }, misconfiguredAddons: {} };
            svcMock.getProjectContext = jest.fn(() => resolvedPromise(context));
            buildController();

            $scope.loadProjectContext();
            $rootScope.$digest();
            expect($scope.projectContext.brxmVersion).toBe('14.5.0');
            expect($scope.projectContext.installedAddons).toEqual({ 'addon-a': '1.0' });
        });

        it('calls applyFilters() after successful load', function () {
            svcMock.getProjectContext = jest.fn(() => resolvedPromise({
                brxmVersion: '14.5.0', javaVersion: null, installedAddons: {}, misconfiguredAddons: {}
            }));
            buildController();
            var spy = jest.spyOn($scope, 'applyFilters');

            $scope.loadProjectContext();
            $rootScope.$digest();
            expect(spy).toHaveBeenCalled();
        });

        it('resets projectContextLoading to false on error', function () {
            svcMock.getProjectContext = jest.fn(() => rejectedPromise(new Error('network error')));
            buildController();

            $scope.loadProjectContext();
            $rootScope.$digest();
            expect($scope.projectContextLoading).toBe(false);
        });
    });

    describe('getMatchingEpoch()', function () {

        beforeEach(buildController);

        it('returns null when addon has no versions array', function () {
            expect($scope.getMatchingEpoch(makeAddon({ versions: [] }))).toBeNull();
        });

        it('returns null when no project context', function () {
            var addon = makeAddon({ versions: [makeEpoch('14.0.0', '14.9.9')] });
            expect($scope.getMatchingEpoch(addon)).toBeNull();
        });

        it('returns the matching epoch when brxmVersion is in range', function () {
            $scope.projectContext.brxmVersion = '14.5.0';
            var epoch = makeEpoch('14.0.0', '14.9.9');
            var addon = makeAddon({ versions: [epoch] });
            expect($scope.getMatchingEpoch(addon)).toBe(epoch);
        });

        it('returns null when no epoch covers the project version', function () {
            $scope.projectContext.brxmVersion = '15.0.0';
            var addon = makeAddon({ versions: [makeEpoch('14.0.0', '14.9.9')] });
            expect($scope.getMatchingEpoch(addon)).toBeNull();
        });

        it('returns the first matching epoch when multiple epochs exist', function () {
            $scope.projectContext.brxmVersion = '13.5.0';
            var epoch14 = makeEpoch('14.0.0', '14.9.9', null, '2.0.0');
            var epoch13 = makeEpoch('13.0.0', '13.9.9', null, '1.5.0');
            var addon = makeAddon({ versions: [epoch14, epoch13] });
            expect($scope.getMatchingEpoch(addon)).toBe(epoch13);
        });
    });

    describe('getDisplayVersion()', function () {

        beforeEach(buildController);

        it('returns addon.version when no project context', function () {
            expect($scope.getDisplayVersion(makeAddon({ version: '2.0.0' }))).toBe('2.0.0');
        });

        it('returns empty string for null addon', function () {
            expect($scope.getDisplayVersion(null)).toBe('');
        });

        it('returns epoch version when compatible epoch has a different version', function () {
            $scope.projectContext.brxmVersion = '13.5.0';
            var epoch = makeEpoch('13.0.0', '13.9.9', null, '1.5.0');
            var addon = makeAddon({ version: '2.0.0', versions: [epoch] });
            expect($scope.getDisplayVersion(addon)).toBe('1.5.0');
        });

        it('returns addon.version when matching epoch version equals addon.version', function () {
            $scope.projectContext.brxmVersion = '14.5.0';
            var epoch = makeEpoch('14.0.0', '14.9.9', null, '2.0.0');
            var addon = makeAddon({ version: '2.0.0', versions: [epoch] });
            expect($scope.getDisplayVersion(addon)).toBe('2.0.0');
        });
    });

    describe('hasUpdate()', function () {

        beforeEach(buildController);

        it('returns false when not installed', function () {
            expect($scope.hasUpdate(makeAddon({ version: '2.0.0' }))).toBe(false);
        });

        it('returns true when display version is newer than installed version', function () {
            $scope.projectContext.installedAddons['test-addon'] = '1.0.0';
            expect($scope.hasUpdate(makeAddon({ version: '2.0.0' }))).toBe(true);
        });

        it('returns false when display version equals installed version', function () {
            $scope.projectContext.installedAddons['test-addon'] = '2.0.0';
            expect($scope.hasUpdate(makeAddon({ version: '2.0.0' }))).toBe(false);
        });

        it('returns false when installed version is newer (downgrade scenario)', function () {
            $scope.projectContext.installedAddons['test-addon'] = '3.0.0';
            expect($scope.hasUpdate(makeAddon({ version: '2.0.0' }))).toBe(false);
        });
    });

    describe('updateAddon()', function () {

        beforeEach(buildController);

        it('calls installAddon service with upgrade=true', function () {
            $scope.updateAddon(makeAddon());
            expect(svcMock.installAddon).toHaveBeenCalledWith('test-addon', true);
        });

        it('sets lastOperationName to Upgrade', function () {
            $scope.updateAddon(makeAddon());
            expect($scope.lastOperationName).toBe('Upgrade');
        });

        it('sets installing=false after completion', function () {
            $scope.updateAddon(makeAddon());
            $rootScope.$digest();
            expect($scope.installing).toBe(false);
        });
    });

    describe('fixAddon()', function () {

        beforeEach(buildController);

        it('calls fixAddon service method', function () {
            $scope.fixAddon(makeAddon());
            expect(svcMock.fixAddon).toHaveBeenCalledWith('test-addon');
        });

        it('sets lastOperationName to Fix', function () {
            $scope.fixAddon(makeAddon());
            expect($scope.lastOperationName).toBe('Fix');
        });

        it('sets installing=false after completion', function () {
            $scope.fixAddon(makeAddon());
            $rootScope.$digest();
            expect($scope.installing).toBe(false);
        });
    });

    describe('install error handling', function () {

        beforeEach(buildController);

        it('sets installError with friendly message for known error codes', function () {
            svcMock.installAddon = jest.fn(() => rejectedPromise({
                data: { errors: [{ code: 'ADDON_NOT_FOUND', message: 'raw message' }] }
            }));
            $scope.installAddon(makeAddon());
            $rootScope.$digest();
            expect($scope.installError.errors[0].message).toBe('Add-on not found in registry');
        });

        it('includes raw message as detail when friendly message differs from raw', function () {
            svcMock.installAddon = jest.fn(() => rejectedPromise({
                data: { errors: [{ code: 'ADDON_NOT_FOUND', message: 'raw message' }] }
            }));
            $scope.installAddon(makeAddon());
            $rootScope.$digest();
            expect($scope.installError.errors[0].detail).toBe('raw message');
        });

        it('sets detail to null when error code is unknown and message is used verbatim', function () {
            svcMock.installAddon = jest.fn(() => rejectedPromise({
                data: { errors: [{ code: 'UNKNOWN_CODE', message: 'custom message' }] }
            }));
            $scope.installAddon(makeAddon());
            $rootScope.$digest();
            expect($scope.installError.errors[0].message).toBe('custom message');
            expect($scope.installError.errors[0].detail).toBeNull();
        });

        it('falls back to generic message when error has no data.errors', function () {
            svcMock.installAddon = jest.fn(() => rejectedPromise({}));
            $scope.installAddon(makeAddon());
            $rootScope.$digest();
            expect($scope.installError.errors[0].message).toBe('Installation failed. Please check the server logs.');
        });

        it('sets installing=false after error', function () {
            svcMock.installAddon = jest.fn(() => rejectedPromise({}));
            $scope.installAddon(makeAddon());
            $rootScope.$digest();
            expect($scope.installing).toBe(false);
        });

        it('updates projectContext from server after successful install', function () {
            var newContext = {
                brxmVersion: '14.5.0', javaVersion: null,
                installedAddons: { 'test-addon': '2.0.0' }, misconfiguredAddons: {}
            };
            buildController();
            svcMock.getProjectContext = jest.fn(() => resolvedPromise(newContext));
            $scope.installAddon(makeAddon());
            $rootScope.$digest();
            expect($scope.projectContext.installedAddons['test-addon']).toBe('2.0.0');
        });
    });

    describe('refreshAddons()', function () {

        it('populates allAddons and availableCategories on success', function () {
            var addons = [
                makeAddon({ id: 'a1', category: 'security' }),
                makeAddon({ id: 'a2', category: 'seo' }),
                makeAddon({ id: 'a3', category: 'security' })
            ];
            svcMock.getAddons = jest.fn(() => resolvedPromise(addons));
            buildController(); // init calls refreshAddons(false)
            expect($scope.allAddons.length).toBe(3);
            expect($scope.availableCategories.length).toBe(2);
        });

        it('sets loading=true on start, false on finish', function () {
            var deferred = $q.defer();
            buildController();
            svcMock.getAddons = jest.fn(() => deferred.promise);

            $scope.refreshAddons(false);
            expect($scope.loading).toBe(true);

            deferred.resolve([]);
            $rootScope.$digest();
            expect($scope.loading).toBe(false);
        });

        it('sets error message when getAddons fails', function () {
            svcMock.getAddons = jest.fn(() => rejectedPromise(new Error('network')));
            buildController();
            $scope.refreshAddons(false);
            $rootScope.$digest();
            expect($scope.error).toBeTruthy();
        });

        it('calls refreshAddons service then reloads addons when forceRefresh=true', function () {
            buildController();
            svcMock.getAddons.mockClear();

            $scope.refreshAddons(true);
            $rootScope.$digest();
            expect(svcMock.refreshAddons).toHaveBeenCalled();
            expect(svcMock.getAddons).toHaveBeenCalled();
        });

        it('still loads addons when force-refresh HTTP call fails', function () {
            buildController();
            svcMock.refreshAddons = jest.fn(() => rejectedPromise(new Error('refresh failed')));
            svcMock.getAddons.mockClear();

            $scope.refreshAddons(true);
            $rootScope.$digest();
            expect(svcMock.getAddons).toHaveBeenCalled();
        });
    });
});

describe('marketplaceService', function () {

    var svc, $httpBackend;

    beforeEach(angular.mock.module('hippo.essentials'));

    beforeEach(inject(function (_marketplaceService_, _$httpBackend_) {
        svc = _marketplaceService_;
        $httpBackend = _$httpBackend_;
    }));

    afterEach(function () {
        $httpBackend.verifyNoOutstandingExpectation();
        $httpBackend.verifyNoOutstandingRequest();
    });

    describe('getProjectContext()', function () {

        it('maps missing fields to safe defaults', function () {
            var result;
            $httpBackend.expectGET('rest/dynamic/marketplace/project-context').respond(200, {});
            svc.getProjectContext().then(function (ctx) { result = ctx; });
            $httpBackend.flush();
            expect(result.brxmVersion).toBeNull();
            expect(result.javaVersion).toBeNull();
            expect(result.installedAddons).toEqual({});
            expect(result.misconfiguredAddons).toEqual({});
        });

        it('passes through all fields when present', function () {
            var result;
            var data = {
                brxmVersion: '14.5.0', javaVersion: '17',
                installedAddons: { 'addon-a': '1.0' },
                misconfiguredAddons: { 'addon-b': [{ type: 'WRONG_POM' }] }
            };
            $httpBackend.expectGET('rest/dynamic/marketplace/project-context').respond(200, data);
            svc.getProjectContext().then(function (ctx) { result = ctx; });
            $httpBackend.flush();
            expect(result.brxmVersion).toBe('14.5.0');
            expect(result.javaVersion).toBe('17');
            expect(result.installedAddons).toEqual({ 'addon-a': '1.0' });
            expect(result.misconfiguredAddons['addon-b'].length).toBe(1);
        });
    });

    describe('installAddon()', function () {

        it('POSTs to the install endpoint without query param when upgrade=false', function () {
            $httpBackend.expectPOST('rest/dynamic/marketplace/addons/my-addon/install').respond(200, {});
            svc.installAddon('my-addon', false);
            $httpBackend.flush();
        });

        it('appends ?upgrade=true when upgrade=true', function () {
            $httpBackend.expectPOST('rest/dynamic/marketplace/addons/my-addon/install?upgrade=true').respond(200, {});
            svc.installAddon('my-addon', true);
            $httpBackend.flush();
        });
    });

    describe('getAddons()', function () {

        it('returns empty array when response data is null', function () {
            var result;
            $httpBackend.expectGET('rest/dynamic/marketplace/addons').respond(200, null);
            svc.getAddons().then(function (addons) { result = addons; });
            $httpBackend.flush();
            expect(result).toEqual([]);
        });

        it('returns the addon array from the response', function () {
            var result;
            $httpBackend.expectGET('rest/dynamic/marketplace/addons').respond(200, [{ id: 'a' }]);
            svc.getAddons().then(function (addons) { result = addons; });
            $httpBackend.flush();
            expect(result.length).toBe(1);
        });
    });
});
