'use strict';

var log = log4javascript.getLogger("sssdConfigController-logger");

angular.module('uluwatuControllers').controller('sssdConfigController', ['$scope', '$rootScope', '$filter', '$base64', 'UserSssdConfig', 'AccountSssdConfig', 'GlobalSssdConfig', 'File', 'ErrorHandler',
    function($scope, $rootScope, $filter, $base64, UserSssdConfig, AccountSssdConfig, GlobalSssdConfig, File, ErrorHandler) {

        $rootScope.sssdConfigs = AccountSssdConfig.query();
        initalizeSssdConfig();

        $scope.createSssdConfig = function() {
            $scope.sssdConfigCreationForm.$setPristine();
            var successHandler = function(result) {
                GlobalSssdConfig.get({
                    id: result.id
                }, function(success) {
                    $rootScope.sssdConfigs.push(success);
                    initalizeSssdConfig();
                    $scope.showSuccess($rootScope.msg.sssdconfig_success1 + success.id + $rootScope.msg.sssdconfig_success2);
                    angular.element(document.querySelector('#panel-create-sssdconfigs-collapse-btn')).click();
                });
            };
            var errorHandler = function(error) {
                $scope.showError(error, $rootScope.msg.sssdconfig_failed);
            };

            if ($scope.sssdConfigPublicInAccount) {
                AccountSssdConfig.save($scope.sssdConfig, successHandler, errorHandler);
            } else {
                UserSssdConfig.save($scope.sssdConfig, successHandler, errorHandler);
            }
        };

        $scope.generateSssdConfigFromFile = function() {
            File.getBase64ContentById("sssdconfigfile", function(content) {
                if (content) {
                    $scope.sssdConfig.configuration = $base64.decode(content);
                }
                $scope.$apply();
                if ($scope.sssdConfigCreationForm.$error.sssdconfig) {
                    $scope.sssdConfigCreationForm.$error.sssdconfig[0].$setDirty();
                    $scope.$apply();
                }
            });
        };

        $scope.deleteSssdConfig = function(config) {
            GlobalSssdConfig.delete({
                id: config.id
            }, function(success) {
                $rootScope.sssdConfigs.splice($rootScope.sssdConfigs.indexOf(config), 1);
                $scope.showSuccess($rootScope.msg.sssdconfig_delete_success1 + config.id + $rootScope.msg.sssdconfig_delete_success2);
            }, function(error) {
                $scope.showError(error, $rootScope.msg.sssdconfig_delete_failed);
            });
        };

        $scope.changeContentType = function() {
            var sssdConfig = getEmptySssdConfig();
            sssdConfig.name = $scope.sssdConfig.name;
            sssdConfig.description = $scope.sssdConfig.description;
            $scope.sssdConfig = angular.copy(sssdConfig);
            $scope.sssdConfigCreationForm.$setPristine();
            var inputs = ['sssdconfigname', 'sssdconfigdescription'];
            for (var i = 0; i < inputs.length; i++) {
                var input = $scope.sssdConfigCreationForm[inputs[i]];
                if (input.$viewValue || input.$modelValue) {
                    input.$setDirty();
                }
            }
        };

        function getEmptySssdConfig() {
            return {
                name: "",
                description: null,
                configuration: null,
                providerType: null,
                url: null,
                schema: null,
                baseSearch: null,
                tlsReqcert: null,
                adServer: null,
                kerberosServer: null,
                kerberosRealm: null
            }
        }

        function initalizeSssdConfig() {
            $scope.sssdConfigType = 'PARAMS';
            $scope.sssdConfigPublicInAccount = false;
            $scope.sssdConfig = getEmptySssdConfig();
        }
    }
]);