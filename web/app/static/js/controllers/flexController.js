'use strict';

var log = log4javascript.getLogger("flexController-logger");

angular.module('uluwatuControllers').controller('flexController', ['$scope', '$rootScope', '$filter', '$base64', 'AccountFlex', 'GlobalFlex', 'AccountDefaultFlex', 'AccountUsedForControllerFlex', 'SmartSense',
    function($scope, $rootScope, $filter, $base64, AccountFlex, GlobalFlex, AccountDefaultFlex, AccountUsedForControllerFlex, SmartSense) {

        $rootScope.smartSenseSubscription = SmartSense.get();
        $rootScope.flexs = AccountFlex.query();

        $scope.flex = {}

        $scope.checkReservedFlexIds = function () {
            var found = $rootScope.flexs.find(function(flex) {
                 return flex.subscriptionId === $scope.flex.subscriptionId;
            })
            $scope.flexCreationForm.flexsubscriptionid.$setValidity('used', found === undefined)
        }

        $scope.deleteFlex = function(flex) {
            GlobalFlex.delete({
                id: flex.id
            }, function(success) {
                $rootScope.flexs.splice($rootScope.flexs.indexOf(flex), 1);
                $scope.showSuccess($rootScope.msg.flex_delete_success1 + flex.id + $rootScope.msg.flex_delete_success2);
            }, function(error) {
                $scope.showError(error, $rootScope.msg.flex_delete_failed);
            });

        };

        $scope.createFlex = function() {
            var successHandler = function(result) {
                GlobalFlex.get({
                    id: result.id
                }, function(success) {
                    $rootScope.flexs.push(success);
                    $scope.flex = {}
                    $scope.showSuccess($rootScope.msg.flex_success1 + success.id + $rootScope.msg.flex_success2);
                    angular.element(document.querySelector('#panel-create-flexs-collapse-btn')).click();
                    if (success.default) {
                        $scope.changeDefaultFlex(success);
                    }
                    if (success.usedForController) {
                        $scope.changeUseForController(success);
                    }
                });
            };
            var errorHandler = function(error) {
                $scope.showError(error, $rootScope.msg.flex_failed);
            };

            $scope.flex.smartSenseSubscriptionId = getSmartSenseDbId();

            AccountFlex.save($scope.flex, successHandler, errorHandler);
            $scope.flexCreationForm.$setPristine();
        };


        $scope.changeDefaultFlex = function(flex) {
            AccountDefaultFlex.update({name: flex.name}, flex).$promise.then(function(success) {
                changeDefaultFlexWeb(flex)
            })
        }

        function changeDefaultFlexWeb(flex) {
            $rootScope.flexs.forEach(function (f) {
                f.default = f.subscriptionId === flex.subscriptionId;
            })
        }

        $scope.changeUseForController = function(flex) {
            AccountUsedForControllerFlex.update({name: flex.name}, flex).$promise.then(function(success) {
                changeUseForControllerWeb(flex)
            })
        }

        function changeUseForControllerWeb(flex) {
            $rootScope.flexs.forEach(function (f) {
                f.usedForController = f.subscriptionId === flex.subscriptionId;
            })
        }

        function getSmartSenseDbId() {
            if ($rootScope.smartSenseSubscription) {
                return $rootScope.smartSenseSubscription.id;
            } else {
                return null
            }
        }

    }
]);
