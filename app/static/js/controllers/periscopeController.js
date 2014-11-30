'use strict';

var log = log4javascript.getLogger("periscopeController-logger");

angular.module('uluwatuControllers').controller('periscopeController', ['$scope', '$rootScope', '$filter',
    function ($scope, $rootScope, $filter) {
        console.log("periscopeController is initialized....")

        $scope.alarm = {}
        $scope.scalingAction = {}
        $scope.metricBasedAlarm = true;
        $scope.timeBasedAlarm = false;

        $scope.activateMetricAlarmCreationForm = function(isMetricAlarm) {
          $scope.metricBasedAlarm = isMetricAlarm;
          $scope.timeBasedAlarm = !isMetricAlarm;
        }

    }
]);
