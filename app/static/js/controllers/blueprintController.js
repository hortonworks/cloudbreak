'use strict';

var log = log4javascript.getLogger("blueprintController-logger");

angular.module('uluwatuControllers').controller('blueprintController', ['$scope', '$rootScope', 'UserBlueprint', 'AccountBlueprint', 'GlobalBlueprint',
    function ($scope, $rootScope, UserBlueprint, AccountBlueprint, GlobalBlueprint) {
        $rootScope.blueprints = AccountBlueprint.query();
        initializeBlueprint();

        $scope.createBlueprint = function () {
            if ($scope.blueprint.public) {
                AccountBlueprint.save($scope.blueprint, function (result) {
                    GlobalBlueprint.get({ id: result.id}, function(success) {
                        handleBlueprintSuccess(success)
                    });
                }, function (error) {
                    handleBlueprintError(error)
                });           
            } else {
                UserBlueprint.save($scope.blueprint, function (result) {
                    GlobalBlueprint.get({ id: result.id}, function(success) {
                        handleBlueprintSuccess(success)
                    });
                }, function (error) {
                    handleBlueprintError(error)
                });
            }
            
            function handleBlueprintSuccess(success) {
                $rootScope.blueprints.push(success);
                initializeBlueprint();
                $scope.modifyStatusMessage($rootScope.error_msg.blueprint_success1 + success.id + $rootScope.error_msg.blueprint_success2);
                $scope.modifyStatusClass("has-success");
                $scope.blueprintForm.$setPristine();
                angular.element(document.querySelector('#panel-create-blueprints-collapse-btn')).click();
            }
            
            function handleBlueprintError(error) {
                $scope.modifyStatusMessage($rootScope.error_msg.blueprint_template_failed + ": " + error.data.message);
                $scope.modifyStatusClass("has-error");            
            }
        }
        
        $scope.copyAndEditBlueprint = function(blueprint) {
             var newBlueprint = angular.copy(blueprint)
             angular.element(document.querySelector('#panel-create-blueprints-collapse')).collapse('show');
             angular.element(document.querySelector('#panel-blueprint-collapse' + blueprint.id)).collapse('hide')
             $scope.blueprint = newBlueprint;
             $scope.blueprint.name = ""
             $scope.blueprint.ambariBlueprint.toString = function(){ // formatting textarea
                     return JSON.stringify(this, null, "    ");
             }
        }

        $scope.deleteBlueprint = function (blueprint) {
            GlobalBlueprint.delete({ id: blueprint.id }, function (success) {
                $rootScope.blueprints.splice($rootScope.blueprints.indexOf(blueprint), 1);
                $scope.modifyStatusMessage($rootScope.error_msg.blueprint_delete_success1 + blueprint.id + $rootScope.error_msg.blueprint_delete_success2);
                $scope.modifyStatusClass("has-success");
            }, function (error) {
                $scope.modifyStatusMessage($rootScope.error_msg.blueprint_delete_failed  + ": " + error.data.message);
                $scope.modifyStatusClass("has-error");
            });
        }

        function initializeBlueprint() {
            $scope.blueprint = {}
        }
    }
]);
