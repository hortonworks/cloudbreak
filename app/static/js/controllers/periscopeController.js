'use strict';

var log = log4javascript.getLogger("periscopeController-logger");

angular.module('uluwatuControllers').controller('periscopeController', ['$scope', '$rootScope', '$filter', 'PeriscopeCluster', 'MetricAlarm', 'TimeAlarm', 'ScalingPolicy',
    function ($scope, $rootScope, $filter, PeriscopeCluster, MetricAlarm, TimeAlarm, ScalingPolicy) {
        $rootScope.periscopeClusters = PeriscopeCluster.query();
        $scope.alarms = [];
        $scope.policies = {};
        $scope.scalingAction = {}
        $scope.alarm = {}
        $scope.metricBasedAlarm = true;
        $scope.timeBasedAlarm = false;
        $scope.actPeriscopeCluster = undefined;
        resetAlarmForms();
        resetScalingActionForm();

        $rootScope.$watch('activeCluster', function(uluCluster, oldUluCluster){
          if (uluCluster.ambariServerIp != undefined) {
            var periCluster = selectActivePeriClusterByAmbariIp(uluCluster);
            if(periCluster != undefined) {
              console.log(periCluster)
              $scope.actPeriscopeCluster = periCluster;
              getAlarms(periCluster.id);
              getScalingPolicies(periCluster.id)
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
            if ($scope.metricBasedAlarm) {
              MetricAlarm.save({id: periClusterId}, $scope.alarm, createAlarmSuccessHandler);
            } else {
              TimeAlarm.save({id: periClusterId}, $scope.alarm, createAlarmSuccessHandler);
            }
          }
        }

        function createAlarmSuccessHandler(success) {
          success.alarms.forEach(function(el) {
            var exist=false;
            $scope.alarms.forEach(function(val) { if (el.id == val.id) {exist=true;} })
            if (!exist) {
              $scope.alarms.push(el);
            }
          });
          $scope.metricBasedAlarmForm.$setPristine();
          $scope.timeBasedAlarmForm.$setPristine();
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

        function getScalingPolicies(id){
          ScalingPolicy.query({id: id}, function (policies) {
            $scope.policies = policies;
            resetScalingActionForm();
            console.log('policies: ')
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

        $scope.createPolicy = function() {
          if ($scope.actPeriscopeCluster != undefined) {
            var newPolicy = $scope.scalingAction.policy;
            var newPolicies = [];
            if ($scope.policies.scalingPolicies != undefined) {
              $scope.policies.scalingPolicies.forEach(function(el) { newPolicies.push(el); });
            }
            newPolicies.push(newPolicy);
            $scope.scalingAction.scalingPolicies = newPolicies;
            delete $scope.scalingAction.policy;
            ScalingPolicy.save({id: $scope.actPeriscopeCluster.id}, $scope.scalingAction, function(success) {
              $scope.policies = success;
              $scope.scalingActionBaseForm.$setPristine();
              resetScalingActionForm();
              angular.element(document.querySelector('#create-policy-collapse-btn')).click();
            });
          }
        }

        function resetScalingActionForm() {
          $scope.scalingAction = {};
          $scope.scalingAction.policy = {};
          $scope.scalingAction.policy.adjustmentType = "NODE_COUNT";
          var p = $scope.policies;
          if (p.cooldown != -1 && p.minSize != -1 && p.maxSize != -1) {
            $scope.scalingAction.cooldown = p.cooldown;
            $scope.scalingAction.minSize = p.minSize;
            $scope.scalingAction.maxSize = p.maxSize;
          } else {
            $scope.scalingAction.cooldown = 10;
            $scope.scalingAction.minSize = 3;
            $scope.scalingAction.maxSize = 100;
          }
        }

        $scope.deletePolicy = function(policy) {
          if ($scope.actPeriscopeCluster != undefined) {
            ScalingPolicy.delete({id: $scope.actPeriscopeCluster.id, policyId: policy.id}, function(success) {
              $scope.policies = success;
            });
          }
        }
    }
]);
