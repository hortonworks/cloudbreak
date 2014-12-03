'use strict';

var log = log4javascript.getLogger("periscopeController-logger");

angular.module('uluwatuControllers').controller('periscopeController', ['$scope', '$rootScope', '$filter', 'PeriscopeCluster', 'MetricAlarm', 'TimeAlarm', 'AlarmPolicy',
    function ($scope, $rootScope, $filter, PeriscopeCluster, MetricAlarm, TimeAlarm, AlarmPolicy) {
        $rootScope.periscopeClusters = PeriscopeCluster.query();
        $scope.alarms = [];
        $scope.policies = {};
        $scope.scalingAction = {}
        $scope.alarm = {}
        $scope.metricBasedAlarm = true;
        $scope.timeBasedAlarm = false;
        $scope.actPeriscopeCluster = undefined;
        resetAlarmForms();

        $rootScope.$watch('activeCluster', function(uluCluster, oldUluCluster){
          if (uluCluster.ambariServerIp != undefined) {
            console.log($rootScope.periscopeClusters)
            var periCluster = selectActivePeriClusterByAmbariIp(uluCluster);
            if(periCluster != undefined) {
              console.log(periCluster)
              $scope.actPeriscopeCluster = periCluster;
              getAlarms(periCluster.id);
              getAlarmPolicies(periCluster.id)
            } else {
              //cluster is not ready yet disable autscaling functions
              $scope.actPeriscopeCluster = undefined;
            }
          }
        }, false);

        $scope.activateMetricAlarmCreationForm = function(isMetricAlarm) {
          $scope.metricBasedAlarm = isMetricAlarm;
          $scope.timeBasedAlarm = !isMetricAlarm;
          resetAlarmForms();
        }

        $scope.createAlarm = function() {
          if ($scope.alarm.email != undefined) {
            var notifications = [];
            notifications.push({
              "target": [$scope.alarm.email],
              "notificationType": "EMAIL"
            });
            $scope.alarm.notifications = notifications;
            delete $scope.alarm.email;
          }

          if ($scope.actPeriscopeCluster != undefined) {
            var periClusterId = $scope.actPeriscopeCluster.id;
            console.log($scope.alarm)
            if ($scope.metricBasedAlarm) {
              MetricAlarm.save({id: periClusterId}, $scope.alarm, createAlarmSuccessHandler);
            } else {
              TimeAlarm.save({id: periClusterId}, $scope.alarm, createAlarmSuccessHandler);
            }
          }
        }

        function createAlarmSuccessHandler(success) {
          console.log(success);
          success.alarms.forEach(function(el) {
            var exist=false;
            $scope.alarms.forEach(function(val) { if (el.id == val.id) {exist=true;} })
            if (!exist) {
              $scope.alarms.push(el);
            }
          });
          resetAlarmForms();
          angular.element(document.querySelector('#panel-create-periscope-alarm-btn')).click();
        }

        function resetAlarmForms() {
          $scope.alarm = {}
          if ($scope.metricBasedAlarm) {
            $scope.alarm.metric = "PENDING_CONTAINERS";
            $scope.alarm.comparisonOperator = "EQUALS";
          }
        }

        $scope.deleteAlarm = function(alarm) {
          if ($scope.actPeriscopeCluster != undefined) {
            console.log('Deleting alarm.....')
            console.log(alarm);
            if (alarm.metric != undefined) {
              MetricAlarm.delete({id: $scope.actPeriscopeCluster.id, alarmId: alarm.id}, function(success) { deleteSuccessHandler(success, alarm) });
            } else {
              TimeAlarm.delete({id: $scope.actPeriscopeCluster.id, alarmId: alarm.id}, function(success) { deleteSuccessHandler(success, alarm) });
            }
          }
        }

        function deleteSuccessHandler(success, alarm) {
          $scope.alarms = $filter('filter')($scope.alarms, function(value, index) { return value.id != alarm.id; }, true);
        }

        function getAlarms(id){
          $scope.alarms=[];
          console.log($rootScope.activeCluster);
          MetricAlarm.query({id: id}, function (success) {
            success.alarms.forEach(function(el){ $scope.alarms.push(el); });
          });
          TimeAlarm.query({id: id}, function (success) {
            success.alarms.forEach(function(el){ $scope.alarms.push(el); });
          });
        }

        function getAlarmPolicies(id){
          AlarmPolicy.query({id: id}, function (policies) {
            $scope.policies = policies;
            console.log(policies);
          });
        }

        function selectActivePeriClusterByAmbariIp(uluCluster) {
          var periCluster = undefined;
          var periClusters = $filter('filter')($rootScope.periscopeClusters, function(value, index) { return value.host == uluCluster.ambariServerIp; }, true);
          if (periClusters != undefined && periClusters.length > 0) {
            periCluster = periClusters[0];
          }
          return periCluster;
        }

    }
]);
