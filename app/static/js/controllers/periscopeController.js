'use strict';

var log = log4javascript.getLogger("periscopeController-logger");

angular.module('uluwatuControllers').controller('periscopeController', ['$scope', '$rootScope', '$filter', 'Alarm',
    function ($scope, $rootScope, $filter, Alarm) {

        $scope.alarms = [];
        $scope.scalingAction = {}
        $scope.metricBasedAlarm = true;
        $scope.timeBasedAlarm = false;
        $scope.alarm = {}
        setMetricAlarmDefaultValues();

        $rootScope.$watch('activeCluster', function(newVal, oldVal){
          var cluster = newVal.cluster;
          if (cluster != undefined && cluster.id != undefined) {
            console.log(cluster.id);
            getAlarms(cluster.id);
          }
        }, false);

        $scope.activateMetricAlarmCreationForm = function(isMetricAlarm) {
          $scope.metricBasedAlarm = isMetricAlarm;
          $scope.timeBasedAlarm = !isMetricAlarm;
          $scope.alarm = {}
          if (isMetricAlarm) {
            setMetricAlarmDefaultValues();
          }
        }

        function setMetricAlarmDefaultValues() {
          $scope.alarm.metric = "PENDING_CONTAINERS";
          $scope.alarm.comparisonOperator = "EQUALS";
        }

        function getAlarms(id){
          console.log($rootScope.activeCluster);
          Alarm.query({id: id}, function (alarms) {
            $scope.alarms = alarms;
            console.log('alarms: ' + alarms);
          });
        }
    }
]);
