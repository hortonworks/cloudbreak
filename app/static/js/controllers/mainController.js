'use strict';

var log = log4javascript.getLogger("mainController-logger");
var $jq = jQuery.noConflict();

angular.module('uluwatuControllers')
    .run(['$rootScope', 'PlatformParameters',
        function($rootScope, PlatformParameters) {
            PlatformParameters.get().$promise.then(function(success) {
                $rootScope.params = {};
                $rootScope.params.regions = success.regions.regions;
                $rootScope.params.defaultRegions = success.regions.defaultRegions;
                $rootScope.params.zones = success.regions.availabilityZones;
                $rootScope.params.diskTypes = success.disks.diskTypes;
                $rootScope.params.defaultDisks = success.disks.defaultDisks;
                $rootScope.params.vmTypes = success.virtualMachines.virtualMachines;
                $rootScope.params.defaultVmTypes = success.virtualMachines.defaultVirtualMachines;
            }, function(error) {
                $rootScope.params.regions = {};
                $rootScope.params.defaultRegions = {};
                $rootScope.params.zones = {};
                $rootScope.params.diskTypes = {};
                $rootScope.params.defaultDisks = {};
                $rootScope.params.vmTypes = {};
                $rootScope.params.defaultVmTypes = {};
            });
        }
    ])
    .controller('mainController', ['$scope', '$rootScope', '$filter', '$interval', 'PlatformParameters',
        function($scope, $rootScope, $filter, $interval, PlatformParameters) {

            $rootScope.fileReadAvailable = window.File && window.FileReader && window.FileList && window.Blob ? true : false;

            $scope.showManagement = true;
            $scope.showAccountPanel = false;

            $scope.activateManagement = function() {
                $scope.showManagement = true;
                $scope.showAccountPanel = false;
            }

            $scope.activateAccountPanel = function() {
                $scope.showManagement = false;
                $scope.showAccountPanel = true;
            }

            $rootScope.config = {
                regionDisplayNames: {
                    get: function(provider, nameId) {
                        if (provider !== undefined && nameId !== undefined && this[provider] !== undefined && this[provider][nameId] !== undefined) {
                            return this[provider][nameId].value;
                        }
                        return nameId;
                    },
                    getById: function(nameId) {
                        var result = nameId,
                            that = this;
                        angular.forEach(that, function(value, key) {
                            if (typeof value !== "function" && that.get(key, nameId) !== nameId) {
                                result = that.get(key, nameId);
                            }
                        });
                        return result;
                    },
                    'AWS': {
                        'us-east-1': {
                            value: 'US East(N. Virginia)'
                        },
                        'us-west-1': {
                            value: 'US West (N. California)'
                        },
                        'us-west-2': {
                            value: 'US West (Oregon)'
                        },
                        'eu-west-1': {
                            value: 'EU (Ireland)'
                        },
                        'eu-central-1': {
                            value: 'EU (Frankfurt)'
                        },
                        'ap-southeast-1': {
                            value: 'Asia Pacific (Singapore)'
                        },
                        'ap-southeast-2': {
                            value: 'Asia Pacific (Sydney)'
                        },
                        'ap-northeast-1': {
                            value: 'Asia Pacific (Tokyo)'
                        },
                        'sa-east-1': {
                            value: 'South America (São Paulo)'
                        },
                    },
                    'GCP': {
                        'us-central1': {
                            value: "Central US"
                        },
                        'europe-west1': {
                            value: "Western Europe"
                        },
                        'asia-east1': {
                            value: "East Asia"
                        },
                        'us-east1': {
                            value: "Eastern US"
                        }
                    }
                },
                diskDisplayNames: {
                    get: function(provider, nameId) {
                        if (provider !== undefined && nameId !== undefined && this[provider] !== undefined && this[provider][nameId] !== undefined) {
                            return this[provider][nameId];
                        }
                        return nameId;
                    },
                    'GCP': {
                        'pd-standard': 'Standard persistent disks (HDD)',
                        'pd-ssd': 'Solid-state persistent disks (SSD)'
                    },
                    'AWS': {
                        'ephemeral': 'Ephemeral',
                        'standard': 'Magnetic',
                        'gp2': 'General Purpose (SSD)'
                    }
                },
                'RECIPE_TYPE': {
                    content_types: [{
                        key: 'SCRIPT',
                        value: 'SCRIPT'
                    }, {
                        key: 'FILE',
                        value: 'FILE'
                    }, {
                        key: 'URL',
                        value: 'URL'
                    }],
                    execution_types: [{
                        key: 'ALL_NODES',
                        value: 'ALL_NODES'
                    }, {
                        key: 'ONE_NODE',
                        value: 'ONE_NODE'
                    }]
                },
                'BLUEPRINT_TYPE': [{
                    key: 'TEXT',
                    value: 'TEXT'
                }, {
                    key: 'FILE',
                    value: 'FILE'
                }, {
                    key: 'URL',
                    value: 'URL'
                }],
                'EVENT_TYPE': {
                    "REQUESTED": "requested",
                    "CREATE_IN_PROGRESS": "create in progress",
                    "AVAILABLE": "available",
                    "UPDATE_IN_PROGRESS": "update in progress",
                    "UPDATE_FAILED": "update failed",
                    "CREATE_FAILED": "create failed",
                    "DELETE_IN_PROGRESS": "delete in progress",
                    "DELETE_FAILED": "delete failed",
                    "DELETE_COMPLETED": "delete completed",
                    "STOPPED": "stopped",
                    "STOP_REQUESTED": "stop requested",
                    "START_REQUESTED": "start requested",
                    "STOP_IN_PROGRESS": "stop in progress",
                    "START_IN_PROGRESS": "start in progress",
                    "START_FAILED": "start failed",
                    "STOP_FAILED": "stop failed",
                    "BILLING_STARTED": "billing started",
                    "BILLING_STOPPED": "billing stopped",
                    "WAIT_FOR_SYNC": "unknown",
                },
                'EVENT_CLASS': {
                    "REQUESTED": "has-warning",
                    "CREATE_IN_PROGRESS": "has-warning",
                    "AVAILABLE": "has-success",
                    "UPDATE_IN_PROGRESS": "has-warning",
                    "UPDATE_FAILED": "has-error",
                    "CREATE_FAILED": "has-error",
                    "DELETE_IN_PROGRESS": "has-warning",
                    "DELETE_FAILED": "has-error",
                    "DELETE_COMPLETED": "has-success",
                    "STOPPED": "has-success",
                    "STOP_REQUESTED": "has-warning",
                    "START_REQUESTED": "has-warning",
                    "STOP_IN_PROGRESS": "has-warning",
                    "START_IN_PROGRESS": "has-warning",
                    "START_FAILED": "has-error",
                    "STOP_FAILED": "has-error",
                    "BILLING_STARTED": "has-success",
                    "BILLING_STOPPED": "has-success",
                    "WAIT_FOR_SYNC": "has-error"
                },
                'TIME_ZONES': [{
                    key: 'Etc/GMT+1',
                    value: 'GMT-1'
                }, {
                    key: 'Etc/GMT+2',
                    value: 'GMT-2'
                }, {
                    key: 'Etc/GMT+3',
                    value: 'GMT-3'
                }, {
                    key: 'Etc/GMT+4',
                    value: 'GMT-4'
                }, {
                    key: 'Etc/GMT+5',
                    value: 'GMT-5'
                }, {
                    key: 'Etc/GMT+6',
                    value: 'GMT-6'
                }, {
                    key: 'Etc/GMT+7',
                    value: 'GMT-7'
                }, {
                    key: 'Etc/GMT+8',
                    value: 'GMT-8'
                }, {
                    key: 'Etc/GMT+9',
                    value: 'GMT-9'
                }, {
                    key: 'Etc/GMT+10',
                    value: 'GMT-10'
                }, {
                    key: 'Etc/GMT+11',
                    value: 'GMT-11'
                }, {
                    key: 'Etc/GMT+12',
                    value: 'GMT-12'
                }, {
                    key: 'Etc/GMT',
                    value: 'GMT/UTC'
                }, {
                    key: 'Etc/GMT-1',
                    value: 'GMT+1'
                }, {
                    key: 'Etc/GMT-2',
                    value: 'GMT+2'
                }, {
                    key: 'Etc/GMT-3',
                    value: 'GMT+3'
                }, {
                    key: 'Etc/GMT-4',
                    value: 'GMT+4'
                }, {
                    key: 'Etc/GMT-5',
                    value: 'GMT+5'
                }, {
                    key: 'Etc/GMT-6',
                    value: 'GMT+6'
                }, {
                    key: 'Etc/GMT-7',
                    value: 'GMT+7'
                }, {
                    key: 'Etc/GMT-8',
                    value: 'GMT+8'
                }, {
                    key: 'Etc/GMT-9',
                    value: 'GMT+9'
                }, {
                    key: 'Etc/GMT-10',
                    value: 'GMT+10'
                }, {
                    key: 'Etc/GMT-11',
                    value: 'GMT+11'
                }, {
                    key: 'Etc/GMT-12',
                    value: 'GMT+12'
                }]
            }


        }
    ]);