/*
 * Copyright 2025 Bloomreach
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

(function () {
    'use strict';

    var REST_BASE = 'rest/dynamic/marketplace';

    var LIFECYCLE_CLASSES = {
        active: 'lifecycle-active',
        deprecated: 'lifecycle-deprecated',
        eol: 'lifecycle-eol',
        beta: 'lifecycle-beta'
    };

    var CATEGORY_CLASSES = [
        'integration', 'security', 'seo', 'developer-tools',
        'content-management', 'analytics', 'utility'
    ];

    // =========================================================================
    // Version Utilities
    // =========================================================================

    function parseVersion(v) {
        if (!v) return [];
        return v.replace(/[^\d.]/g, '').split('.').map(Number);
    }

    function compareVersions(v1, v2) {
        if (!v1 || !v2) return 0;
        var parts1 = parseVersion(v1);
        var parts2 = parseVersion(v2);
        var len = Math.max(parts1.length, parts2.length);
        for (var i = 0; i < len; i++) {
            var p1 = parts1[i] || 0;
            var p2 = parts2[i] || 0;
            if (p1 < p2) return -1;
            if (p1 > p2) return 1;
        }
        return 0;
    }

    // =========================================================================
    // Marketplace API Service
    // =========================================================================

    angular.module('hippo.essentials')
        .factory('marketplaceService', ['$http', '$q', '$log', function ($http, $q, $log) {

            function mapProjectContext(data) {
                return {
                    brxmVersion: data.brxmVersion || null,
                    javaVersion: data.javaVersion || null,
                    installedAddons: data.installedAddons || {}
                };
            }

            return {
                getAddons: function () {
                    return $http.get(REST_BASE + '/addons')
                        .then(function (response) {
                            return response.data || [];
                        });
                },

                refreshAddons: function () {
                    return $http.post(REST_BASE + '/refresh');
                },

                getProjectContext: function () {
                    return $http.get(REST_BASE + '/project-context')
                        .then(function (response) {
                            return mapProjectContext(response.data || {});
                        });
                },

                refreshProjectContext: function () {
                    return $http.post(REST_BASE + '/project-context/refresh');
                },

                installAddon: function (addonId, upgrade) {
                    var url = REST_BASE + '/addons/' + addonId + '/install';
                    if (upgrade) {
                        url += '?upgrade=true';
                    }
                    return $http.post(url).then(function (response) {
                        return response.data;
                    });
                },

                uninstallAddon: function (addonId) {
                    return $http.post(REST_BASE + '/addons/' + addonId + '/uninstall')
                        .then(function (response) {
                            return response.data;
                        });
                }
            };
        }]);

    // =========================================================================
    // Marketplace Controller
    // =========================================================================

    angular.module('hippo.essentials')
        .controller('forgeMarketplaceCtrl', [
            '$scope', '$log', 'marketplaceService',
            function ($scope, $log, marketplaceService) {

                // -----------------------------------------------------------------
                // State
                // -----------------------------------------------------------------

                $scope.allAddons = [];
                $scope.addons = [];
                $scope.selectedAddon = null;
                $scope.availableCategories = [];

                $scope.filter = { query: '', category: '' };
                $scope.quickFilter = 'all';
                $scope.sortBy = 'recommended';
                $scope.resultCounts = { total: 0, filtered: 0 };

                $scope.loading = false;
                $scope.error = null;
                $scope.projectContextLoading = false;

                $scope.installing = false;
                $scope.installResult = null;
                $scope.installError = null;

                $scope.showUninstallConfirm = false;
                $scope.addonToUninstall = null;

                $scope.projectContext = {
                    brxmVersion: null,
                    javaVersion: null,
                    installedAddons: {}
                };

                // -----------------------------------------------------------------
                // Project Context Helpers
                // -----------------------------------------------------------------

                $scope.hasProjectContext = function () {
                    return $scope.projectContext.brxmVersion !== null;
                };

                $scope.getInstalledCount = function () {
                    return Object.keys($scope.projectContext.installedAddons || {}).length;
                };

                $scope.getUpdatableCount = function () {
                    return $scope.allAddons.filter($scope.hasUpdate).length;
                };

                // -----------------------------------------------------------------
                // Installation Status
                // -----------------------------------------------------------------

                $scope.isInstalled = function (addon) {
                    return addon && addon.id &&
                        $scope.projectContext.installedAddons.hasOwnProperty(addon.id);
                };

                $scope.getInstalledVersion = function (addon) {
                    if (!addon || !addon.id) return null;
                    return $scope.projectContext.installedAddons[addon.id] || null;
                };

                $scope.hasUpdate = function (addon) {
                    if (!$scope.isInstalled(addon)) return false;
                    var installed = $scope.getInstalledVersion(addon);
                    return installed && addon.version &&
                        compareVersions(addon.version, installed) > 0;
                };

                $scope.getInstallStatus = function (addon) {
                    if ($scope.hasUpdate(addon)) {
                        return { status: 'update', label: 'Update Available', class: 'badge-update' };
                    }
                    if ($scope.isInstalled(addon)) {
                        return { status: 'installed', label: 'Installed', class: 'badge-installed' };
                    }
                    return { status: 'available', label: '', class: '' };
                };

                // -----------------------------------------------------------------
                // Compatibility
                // -----------------------------------------------------------------

                $scope.getCompatibilityStatus = function (addon) {
                    if (!addon || !addon.compatibility || !addon.compatibility.brxm) {
                        return { status: 'unknown', label: '', class: '' };
                    }
                    var compat = addon.compatibility.brxm;
                    var projectVersion = $scope.projectContext.brxmVersion;
                    var minOk = !compat.min || compareVersions(projectVersion, compat.min) >= 0;
                    var maxOk = !compat.max || compareVersions(projectVersion, compat.max) <= 0;

                    if (minOk && maxOk) {
                        return { status: 'compatible', label: 'Compatible', class: 'badge-compatible' };
                    }
                    return { status: 'incompatible', label: 'Incompatible', class: 'badge-incompatible' };
                };

                // -----------------------------------------------------------------
                // Display Helpers
                // -----------------------------------------------------------------

                $scope.getLifecycleClass = function (status) {
                    if (!status) return 'lifecycle-unknown';
                    return LIFECYCLE_CLASSES[status.toLowerCase()] || 'lifecycle-unknown';
                };

                $scope.formatCategory = function (category) {
                    if (!category) return '';
                    return category.replace(/_/g, ' ').toLowerCase()
                        .replace(/\b\w/g, function (c) { return c.toUpperCase(); });
                };

                $scope.getCategoryClass = function (category) {
                    if (!category) return 'default';
                    var normalized = category.toLowerCase().replace(/_/g, '-');
                    return CATEGORY_CLASSES.indexOf(normalized) >= 0 ? normalized : 'default';
                };

                $scope.getArtifactsByTarget = function (addon) {
                    var groups = { cms: [], site: [], other: [] };
                    if (!addon || !addon.artifacts) return groups;

                    addon.artifacts.forEach(function (artifact) {
                        if (!artifact.maven) return;
                        var aid = (artifact.maven.artifactId || '').toLowerCase();
                        if (aid.indexOf('cms') >= 0 || aid.indexOf('-repository') >= 0) {
                            groups.cms.push(artifact);
                        } else if (aid.indexOf('site') >= 0 || aid.indexOf('hst') >= 0) {
                            groups.site.push(artifact);
                        } else {
                            groups.other.push(artifact);
                        }
                    });
                    return groups;
                };

                $scope.getGitHubPagesUrl = function (addon) {
                    if (!addon || !addon.repository || !addon.repository.url) return null;
                    var match = addon.repository.url.match(/github\.com\/([^\/]+)\/([^\/]+)/);
                    return match ? 'https://' + match[1] + '.github.io/' + match[2] + '/' : null;
                };

                $scope.getIssuesUrl = function (addon) {
                    if (!addon || !addon.repository || !addon.repository.url) return null;
                    return addon.repository.url.replace(/\/$/, '') + '/issues';
                };

                $scope.getReleasesUrl = function (addon) {
                    if (!addon || !addon.repository || !addon.repository.url) return null;
                    return addon.repository.url.replace(/\/$/, '') + '/releases';
                };

                // -----------------------------------------------------------------
                // Filtering and Sorting
                // -----------------------------------------------------------------

                function extractCategories(addons) {
                    var categorySet = {};
                    addons.forEach(function (addon) {
                        if (addon.category) categorySet[addon.category] = true;
                    });
                    return Object.keys(categorySet).sort().map(function (cat) {
                        return { value: cat, label: $scope.formatCategory(cat) };
                    });
                }

                function getSortComparator() {
                    switch ($scope.sortBy) {
                        case 'name-asc':
                            return function (a, b) { return (a.name || '').localeCompare(b.name || ''); };
                        case 'name-desc':
                            return function (a, b) { return (b.name || '').localeCompare(a.name || ''); };
                        default:
                            return function (a, b) { return (a.name || '').localeCompare(b.name || ''); };
                    }
                }

                $scope.applyFilters = function () {
                    var results = $scope.allAddons.slice();
                    $scope.resultCounts.total = results.length;

                    // Text search
                    var query = ($scope.filter.query || '').toLowerCase().trim();
                    if (query) {
                        results = results.filter(function (addon) {
                            var nameMatch = addon.name && addon.name.toLowerCase().indexOf(query) >= 0;
                            var descMatch = addon.description && addon.description.toLowerCase().indexOf(query) >= 0;
                            return nameMatch || descMatch;
                        });
                    }

                    // Category filter
                    var category = ($scope.filter.category || '').toLowerCase();
                    if (category) {
                        results = results.filter(function (addon) {
                            return addon.category && addon.category.toLowerCase() === category;
                        });
                    }

                    // Quick filter
                    if ($scope.quickFilter === 'installed') {
                        results = results.filter($scope.isInstalled);
                    } else if ($scope.quickFilter === 'updatable') {
                        results = results.filter($scope.hasUpdate);
                    } else if ($scope.quickFilter === 'compatible') {
                        results = results.filter(function (addon) {
                            return $scope.getCompatibilityStatus(addon).status === 'compatible';
                        });
                    }

                    results.sort(getSortComparator());
                    $scope.resultCounts.filtered = results.length;
                    $scope.addons = results;
                };

                $scope.setQuickFilter = function (filter) {
                    $scope.quickFilter = filter;
                    $scope.applyFilters();
                };

                $scope.setSortBy = function (sort) {
                    $scope.sortBy = sort;
                    $scope.applyFilters();
                };

                $scope.search = function () {
                    $scope.applyFilters();
                };

                $scope.clearSearch = function () {
                    $scope.filter.query = '';
                    $scope.applyFilters();
                };

                $scope.filterAddons = function () {
                    $scope.applyFilters();
                };

                // -----------------------------------------------------------------
                // Selection
                // -----------------------------------------------------------------

                $scope.selectAddon = function (addon) {
                    $scope.selectedAddon = addon;
                    $scope.clearInstallFeedback();
                };

                $scope.clearSelection = function () {
                    $scope.selectedAddon = null;
                };

                // -----------------------------------------------------------------
                // Keyboard Navigation
                // -----------------------------------------------------------------

                $scope.handleKeyDown = function ($event, addon, index) {
                    var key = $event.key;
                    if (key === 'Enter' || key === ' ') {
                        $event.preventDefault();
                        $scope.selectAddon(addon);
                    } else if (key === 'ArrowDown' || key === 'ArrowUp') {
                        $event.preventDefault();
                        var nextIndex = key === 'ArrowDown' ? index + 1 : index - 1;
                        if (nextIndex >= 0 && nextIndex < $scope.addons.length) {
                            var items = document.querySelectorAll('.addon-item');
                            if (items[nextIndex]) items[nextIndex].focus();
                        }
                    }
                };

                // -----------------------------------------------------------------
                // Clipboard
                // -----------------------------------------------------------------

                $scope.copyToClipboard = function (artifact, $event) {
                    var version = artifact.maven.version || $scope.selectedAddon.version;
                    var text = '<dependency>\n' +
                        '  <groupId>' + artifact.maven.groupId + '</groupId>\n' +
                        '  <artifactId>' + artifact.maven.artifactId + '</artifactId>\n' +
                        '  <version>' + version + '</version>\n' +
                        '</dependency>';

                    var button = $event.target;
                    navigator.clipboard.writeText(text).then(function () {
                        button.textContent = 'Copied!';
                        button.classList.add('copied');
                        setTimeout(function () {
                            button.textContent = 'Copy';
                            button.classList.remove('copied');
                        }, 2000);
                    }).catch(function (err) {
                        $log.error('Failed to copy to clipboard', err);
                    });
                };

                // -----------------------------------------------------------------
                // Data Loading
                // -----------------------------------------------------------------

                $scope.refreshAddons = function (forceRefresh) {
                    $scope.loading = true;
                    $scope.error = null;

                    var loadAddons = function () {
                        marketplaceService.getAddons()
                            .then(function (addons) {
                                $scope.allAddons = addons;
                                $scope.availableCategories = extractCategories(addons);
                                $scope.applyFilters();
                                $log.info('Loaded ' + addons.length + ' addons');
                            })
                            .catch(function (error) {
                                $log.error('Failed to load addons', error);
                                $scope.error = 'Failed to load add-ons. Please try again.';
                            })
                            .finally(function () {
                                $scope.loading = false;
                            });
                    };

                    if (forceRefresh) {
                        marketplaceService.refreshAddons()
                            .then(function () {
                                loadAddons();
                                $scope.refreshProjectContext();
                            })
                            .catch(function (error) {
                                $log.error('Failed to refresh', error);
                                loadAddons();
                            });
                    } else {
                        loadAddons();
                    }
                };

                $scope.loadProjectContext = function () {
                    $scope.projectContextLoading = true;
                    marketplaceService.getProjectContext()
                        .then(function (context) {
                            $scope.projectContext = context;
                            $log.info('Loaded project context: brxm=' + context.brxmVersion +
                                ', installed=' + Object.keys(context.installedAddons).length);
                            $scope.applyFilters();
                        })
                        .catch(function (error) {
                            $log.warn('Failed to load project context', error);
                        })
                        .finally(function () {
                            $scope.projectContextLoading = false;
                        });
                };

                $scope.refreshProjectContext = function () {
                    marketplaceService.refreshProjectContext()
                        .then(function () {
                            $scope.loadProjectContext();
                        })
                        .catch(function (error) {
                            $log.error('Failed to refresh project context', error);
                        });
                };

                // -----------------------------------------------------------------
                // Installation Operations
                // -----------------------------------------------------------------

                var ERROR_MESSAGES = {
                    'ADDON_NOT_FOUND': 'Add-on not found in registry',
                    'PROJECT_BASEDIR_NOT_SET': 'Project directory not configured',
                    'MISSING_TARGET': 'Add-on has no installable artifacts',
                    'TARGET_POM_NOT_FOUND': 'Required POM file not found',
                    'NO_DEPENDENCIES_SECTION': 'POM file missing dependencies section',
                    'ALREADY_INSTALLED': 'Add-on is already installed',
                    'NOT_INSTALLED': 'Add-on is not currently installed',
                    'PROPERTY_CONFLICT': 'Version property conflict detected',
                    'IO_ERROR': 'Failed to write changes to disk'
                };

                function formatInstallError(error, operationName) {
                    if (error.data && error.data.errors && error.data.errors.length > 0) {
                        var formattedErrors = error.data.errors.map(function (err) {
                            var friendlyMessage = ERROR_MESSAGES[err.code] || err.message;
                            return {
                                code: err.code,
                                message: friendlyMessage,
                                detail: err.message !== friendlyMessage ? err.message : null
                            };
                        });
                        return { errors: formattedErrors };
                    }
                    return {
                        errors: [{ message: operationName + ' failed. Please check the server logs.' }]
                    };
                }

                function hasInstallableArtifacts(addon) {
                    return addon.artifacts && addon.artifacts.some(function (a) {
                        return a.type === 'maven-lib' && a.maven && a.target;
                    });
                }

                function performInstallOperation(addon, operation, operationName) {
                    $scope.installing = true;
                    $scope.installResult = null;
                    $scope.installError = null;

                    operation()
                        .then(function (result) {
                            $scope.installResult = result;
                            return marketplaceService.getProjectContext();
                        })
                        .then(function (context) {
                            $scope.projectContext = context;
                            $scope.applyFilters();
                        })
                        .catch(function (error) {
                            $scope.installError = formatInstallError(error, operationName);
                        })
                        .finally(function () {
                            $scope.installing = false;
                        });
                }

                $scope.canInstall = function (addon) {
                    return !$scope.isInstalled(addon) && !$scope.installing && hasInstallableArtifacts(addon);
                };

                $scope.canUpdate = function (addon) {
                    return $scope.hasUpdate(addon) && !$scope.installing && hasInstallableArtifacts(addon);
                };

                $scope.canUninstall = function (addon) {
                    return $scope.isInstalled(addon) && !$scope.installing && hasInstallableArtifacts(addon);
                };

                $scope.installAddon = function (addon) {
                    performInstallOperation(addon, function () {
                        return marketplaceService.installAddon(addon.id, false);
                    }, 'Installation');
                };

                $scope.updateAddon = function (addon) {
                    performInstallOperation(addon, function () {
                        return marketplaceService.installAddon(addon.id, true);
                    }, 'Upgrade');
                };

                $scope.uninstallAddon = function (addon) {
                    performInstallOperation(addon, function () {
                        return marketplaceService.uninstallAddon(addon.id);
                    }, 'Uninstall');
                };

                $scope.clearInstallFeedback = function () {
                    $scope.installResult = null;
                    $scope.installError = null;
                };

                // -----------------------------------------------------------------
                // Uninstall Confirmation
                // -----------------------------------------------------------------

                $scope.confirmUninstall = function (addon) {
                    $scope.showUninstallConfirm = true;
                    $scope.addonToUninstall = addon;
                };

                $scope.cancelUninstall = function () {
                    $scope.showUninstallConfirm = false;
                    $scope.addonToUninstall = null;
                };

                $scope.proceedUninstall = function () {
                    $scope.showUninstallConfirm = false;
                    $scope.uninstallAddon($scope.addonToUninstall);
                    $scope.addonToUninstall = null;
                };

                // -----------------------------------------------------------------
                // Initialize
                // -----------------------------------------------------------------

                $scope.refreshAddons(false);
                $scope.loadProjectContext();
            }
        ]);
})();
