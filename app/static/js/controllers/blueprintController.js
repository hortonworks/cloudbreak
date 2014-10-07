'use strict';

var log = log4javascript.getLogger("blueprintController-logger");

angular.module('uluwatuControllers').controller('blueprintController', ['$scope', '$rootScope', 'UserBlueprint', 'GlobalBlueprint',
    function ($scope, $rootScope, UserBlueprint, GlobalBlueprint) {
        $rootScope.blueprints = UserBlueprint.query();
        initializeBlueprint();

        $scope.createBlueprint = function () {
            if ($scope.blueprint.ambariBlueprint){
                var bpJson = JSON.parse($scope.blueprint.ambariBlueprint);
                $scope.blueprint.ambariBlueprint = bpJson;
            }
            UserBlueprint.save($scope.blueprint, function (result) {
                $scope.blueprint.id = result.id;
                $rootScope.blueprints.push($scope.blueprint);
                initializeBlueprint();
                $scope.modifyStatusMessage($rootScope.error_msg.blueprint_success1 + result.id + $rootScope.error_msg.blueprint_success2);
                $scope.modifyStatusClass("has-success");
                $scope.blueprintForm.$setPristine();
                angular.element(document.querySelector('#panel-create-blueprints-collapse-btn')).click();
            }, function (error) {
                $scope.modifyStatusMessage($rootScope.error_msg.blueprint_template_failed + ": " + error.data.message);
                $scope.modifyStatusClass("has-error");
            });
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
