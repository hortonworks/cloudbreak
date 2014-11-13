'use strict';

var log = log4javascript.getLogger("mainController-logger");
var $jq = jQuery.noConflict();

angular.module('uluwatuControllers').controller('mainController', ['$scope', '$rootScope', '$filter', '$interval',
    function ($scope, $rootScope, $filter, $interval) {

        $scope.managementShow = true;
        $scope.eventShow = false;

        $scope.showManagement = function () {
            $scope.managementShow = true;
            $scope.eventShow = false;
        }

        $scope.showEvents = function () {
            $scope.managementShow = false;
            $scope.eventShow = true;
        }

    }]);
