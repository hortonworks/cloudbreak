'use strict';

/* App Module */

var cloudbreakApp = angular.module('cloudbreakApp', ['ngRoute', 'base64', 'blockUI', 'ui.bootstrap', 'uluwatuControllers', 'uluwatuServices']);

(function() {
    fetchAccountPreferences().then(fetchOptions).then(fetchConfigData).then(fetchLocalConfigData).then(bootstrapApplication);

    function fetchAccountPreferences() {
        var initInjector = angular.injector(["ng"]);
        var $http = initInjector.get("$http");

        return $http.get("/accountpreferences").then(function(response) {
            cloudbreakApp.constant("accountpreferences", response.data);
        }, function(errorResponse) {
            cloudbreakApp.constant("accountpreferences", null);
            console.log(errorResponse);
        });
    }

    function fetchOptions() {
        var initInjector = angular.injector(["ng"]);
        var $http = initInjector.get("$http");
        return $http.get("/settings/all").then(function(response) {
            cloudbreakApp.constant("settings", response.data);
        }, function(errorResponse) {
            cloudbreakApp.constant("settings", null);
            console.log(errorResponse);
        });
    }

    function fetchConfigData() {
        var initInjector = angular.injector(["ng"]);
        var $http = initInjector.get("$http");
        return $http.get("/connectors").then(function(response) {
            cloudbreakApp.constant("initconf", response.data);
        }, function(errorResponse) {
            cloudbreakApp.constant("initconf", null);
            console.log(errorResponse);
        });
    }

    function fetchLocalConfigData() {
        var initInjector = angular.injector(["ng"]);
        var $http = initInjector.get("$http");
        return $http.get("config/displaynames.json").then(function(response) {
            function Config(data) {
                angular.extend(this, data);
            }
            Config.prototype.getValue = function(names, provider, nameId) {
                if (provider !== undefined && nameId !== undefined && names[provider] !== undefined && names[provider][nameId] !== undefined) {
                    return names[provider][nameId];
                }
                return nameId;
            };

            Config.prototype.getRegion = function(provider, nameId) {
                return this.getValue(this.region, provider, nameId);
            };
            Config.prototype.getRegionById = function(nameId) {
                var self = this,
                    result = nameId;
                angular.forEach(this.region, function(val, key) {
                    if (typeof val !== "function" && self.getRegion(key, nameId) !== nameId) {
                        result = self.getRegion(key, nameId);
                    }
                });
                return result;
            };
            Config.prototype.getDisk = function(provider, nameId) {
                return this.getValue(this.disk, provider, nameId);
            };
            Config.prototype.getPropertyName = function(category, nameId) {
                var names = this[category]
                if (nameId !== undefined && names[nameId] !== undefined) {
                    return names[nameId];
                }
                return nameId;
            };
            cloudbreakApp.constant("displayNames", new Config(response.data));
        }, function(errorResponse) {
            cloudbreakApp.constant("displayNames", null);
            console.log(errorResponse);
        });
    }

    function bootstrapApplication() {
        angular.element(document).ready(function() {
            angular.bootstrap(document, ['cloudbreakApp', 'ngRoute', 'base64', 'blockUI', 'ui.bootstrap', 'uluwatuControllers', 'uluwatuServices']);
        });
    }
}());

cloudbreakApp.directive('match', function($parse) {
    return {
        require: 'ngModel',
        link: function(scope, elem, attrs, ctrl) {
            scope.$watch(function() {
                return $parse(attrs.match)(scope) === ctrl.$modelValue;
            }, function(currentValue) {
                ctrl.$setValidity('mismatch', currentValue);
            });
        }
    };
});

cloudbreakApp.directive('validjson', function($parse) {
    return {
        require: 'ngModel',
        link: function(scope, elem, attrs, ctrl) {
            ctrl.$parsers.unshift(function(viewValue) {
                var valid = false;
                var json = {};
                try {
                    json = JSON.parse(viewValue);
                    valid = true;
                } catch (err) {}
                ctrl.$setValidity('validjson', valid);
                return valid ? json : undefined;
            });
        }
    };
});

cloudbreakApp.directive('file', function() {
    return {
        scope: {
            file: '='
        },
        link: function(scope, el, attrs) {
            el.bind('change', function(event) {
                scope.file = event.target.files[0];
                scope.$apply();
            });
        }
    };
});

cloudbreakApp.directive('sssdconfig', function() {
    return {
        require: 'ngModel',
        link: function(scope, elem, attrs, ngModel) {
            var testSssdConfig = function(config) {
                if (!config) {
                    return true;
                }
                var propertyIsValid = function(section, key) {
                    var section = ini.get(section);
                    return section && section.get(key) && typeof(section.get(key).value) == "string" && section.get(key).value.length
                }
                try {
                    var ini = Ini.parse(config);
                    if (!propertyIsValid("sssd", "services") || !propertyIsValid("sssd", "domains")) {
                        return false;
                    }
                    var domains = ini.get("sssd").get("domains").value.split(",");
                    for (var i = 0; i < domains.length; i++) {
                        var domain = "domain/" + domains[i].trim();
                        if (!propertyIsValid(domain, "id_provider") || !propertyIsValid(domain, "auth_provider")) {
                            return false;
                        }
                    }
                    return true;
                } catch (e) {}
                return false
            }

            ngModel.$parsers.unshift(function(value) {
                ngModel.$setValidity('sssdconfig', testSssdConfig(value));
                return value;
            });

            ngModel.$formatters.unshift(function(value) {
                ngModel.$setValidity('sssdconfig', testSssdConfig(value));
                return value;
            });
        }
    };
});

cloudbreakApp.config(['$routeProvider', '$locationProvider', function($routeProvider, $locationProvider) {
    $routeProvider.when('/', {
        templateUrl: 'partials/dashboard.html',
        controller: 'uluwatuController'
    }).otherwise({
        redirectTo: '/'
    });
}]).factory('authHttpResponseInterceptor', ['$q', '$window', function($q, $window) {
    return {
        response: function(response) {
            if (response.status === 401) {
                $window.location.href = '/logout';
            }
            return response || $q.when(response);
        },
        responseError: function(response) {
            if (response.status === 401) {
                $window.location.href = '/logout';
            }
            return $q.reject(response);
        }
    }
}]).config(['$httpProvider', function($httpProvider) {
    $httpProvider.interceptors.push('authHttpResponseInterceptor');
}]).config(function(blockUIConfig) {
    blockUIConfig.autoInjectBodyBlock = false
    blockUIConfig.requestFilter = function(config) {
        var block = false
        if (config.url.match(/^(.*)templates($|\/).*/) || config.url.match(/^(.*)blueprints($|\/).*/) || config.url.match(/^(.*)credentials($|\/).*/) || config.url.match(/^(.*)users($|\?).*/) || config.url.match(/^(.*)permission($|\?).*/) || config.url.match(/^(.*)stacks($|\/).*/) || config.url.match(/^periscope\/clusters($|\/).*/) || config.url.match(/^(.*)usages($|\?).*/)) {
            block = true
        }
        if (!block) {
            return block
        }
    };
});

cloudbreakApp.run(function($rootScope, $http) {
        $http.get('messages.properties').then(function(messages) {
            $rootScope.msg = messages.data
            $rootScope.titleStatus = {
                "REQUESTED": $rootScope.msg.title_requested,
                "CREATE_IN_PROGRESS": $rootScope.msg.title_create_in_progress,
                "UPDATE_IN_PROGRESS": $rootScope.msg.title_update_in_progress,
                "AVAILABLE": $rootScope.msg.title_create_completed,
                "CREATE_FAILED": $rootScope.msg.title_create_failed,
                "DELETE_IN_PROGRESS": $rootScope.msg.title_delete_in_progress,
                "DELETE_COMPLETED": $rootScope.msg.title_delete_completed
            }
        });
    })
    .run(['$rootScope', 'settings',
        function($rootScope, settings) {
            if (settings !== null) {
                $rootScope.settings = settings;
            } else {
                $rootScope.settings = {};
            }
        }
    ])
    .run(['$rootScope', 'initconf',
        function($rootScope, initconf) {
            if (initconf !== null) {
                $rootScope.params = {};
                $rootScope.params.regions = initconf.regions.regions;
                $rootScope.params.defaultRegions = initconf.regions.defaultRegions;
                $rootScope.params.zones = initconf.regions.availabilityZones;
                $rootScope.params.diskTypes = initconf.disks.diskTypes;
                $rootScope.params.defaultDisks = initconf.disks.defaultDisks;
                $rootScope.params.vmTypes = initconf.virtualMachines.virtualMachines;
                $rootScope.params.defaultVmTypes = initconf.virtualMachines.defaultVirtualMachines;
                $rootScope.params.platformVariants = initconf.variants.platformToVariants;
                $rootScope.params.defaultVariants = initconf.variants.defaultVariants;
            } else {
                $rootScope.params = {};
                $rootScope.params.regions = {};
                $rootScope.params.defaultRegions = {};
                $rootScope.params.zones = {};
                $rootScope.params.diskTypes = {};
                $rootScope.params.defaultDisks = {};
                $rootScope.params.vmTypes = {};
                $rootScope.params.defaultVmTypes = {};
                $rootScope.params.platformVariants = {};
                $rootScope.params.defaultVariants = {};
            }
        }
    ])
    .run(['$rootScope', 'displayNames',
        function($rootScope, displayNames) {
            if (displayNames !== null) {
                $rootScope.displayNames = displayNames;
            } else {
                $rootScope.displayNames = {};
            }
        }
    ])
    .run(['$rootScope', 'accountpreferences',
        function($rootScope, accountpreferences) {
            if (accountpreferences !== null && accountpreferences.platforms) {
                $rootScope.params.platforms = accountpreferences.platforms.split(',')
            } else {
                var platforms = [];
                angular.forEach($rootScope.params.platformVariants, function(value, key) {
                    platforms.push(key)
                })
                $rootScope.params.platforms = platforms
            }
        }
    ]);

cloudbreakApp.directive('startdatevalidation', function($parse) {
    return {
        require: 'ngModel',
        link: function(scope, elem, attrs, ctrl) {
            scope.$watch(function() {
                return $parse(attrs.startdatevalidation)(scope.usageFilter) >= ctrl.$modelValue;
            }, function(currentValue) {
                ctrl.$setValidity('startDateInvalid', currentValue);
            });
        }
    };
});

cloudbreakApp.directive('enddatevalidation', function($parse) {
    return {
        require: 'ngModel',
        link: function(scope, elem, attrs, ctrl) {
            scope.$watch(function() {
                return (new Date) > ctrl.$modelValue;
            }, function(currentValue) {
                ctrl.$setValidity('endDateInvalid', currentValue);
            });
        }
    };
});

cloudbreakApp.directive('match', function($parse) {
    return {
        require: 'ngModel',
        link: function(scope, elem, attrs, ctrl) {
            scope.$watch(function() {
                return $parse(attrs.match)(scope) === ctrl.$modelValue;
            }, function(currentValue) {
                ctrl.$setValidity('mismatch', currentValue);
            });
        }
    };
});