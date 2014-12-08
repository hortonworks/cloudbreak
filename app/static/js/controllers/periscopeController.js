'use strict';

var log = log4javascript.getLogger("periscopeController-logger");

angular.module('uluwatuControllers').controller('periscopeController', ['$scope', '$rootScope', '$filter', 'PeriscopeCluster', 'MetricAlarm',
  'TimeAlarm', 'ScalingPolicy', 'Cluster', 'PeriscopeClusterState',
  function ($scope, $rootScope, $filter, PeriscopeCluster, MetricAlarm, TimeAlarm, ScalingPolicy, Cluster, PeriscopeClusterState) {
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
            setActivePeriClusterWithResources(uluCluster.ambariServerIp);
          }
        }, false);

        function setActivePeriClusterWithResources(ambariServerIp) {
          var periCluster = selectActivePeriClusterByAmbariIp(ambariServerIp);
          if (isActiveUluClusterEqualsPeriCluster(periCluster)) {
            console.log(periCluster)
            $scope.actPeriscopeCluster = periCluster;
            getAlarms(periCluster.id);
            getScalingPolicies(periCluster.id);
          } else {
            //cluster is not ready yet disable autscaling functions
            $scope.actPeriscopeCluster = undefined;
          }
        }

        function isActiveUluClusterEqualsPeriCluster(periCluster) {
          return periCluster != undefined && $rootScope.activeCluster.ambariServerIp != undefined
            && $rootScope.activeCluster.ambariServerIp == periCluster.ambariServerIp;
        }

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
              MetricAlarm.delete({id: $scope.actPeriscopeCluster.id, alarmId: alarm.id}, function(success) { deleteSuccessHandler(success, alarm, $scope.actPeriscopeCluster.id) });
            } else {
              TimeAlarm.delete({id: $scope.actPeriscopeCluster.id, alarmId: alarm.id}, function(success) { deleteSuccessHandler(success, alarm, $scope.actPeriscopeCluster.id) });
            }
          }
        }

        function deleteSuccessHandler(success, alarm, periClusterId) {
          $scope.alarms = $filter('filter')($scope.alarms, function(value, index) { return value.id != alarm.id; }, true);
          getScalingPolicies(periClusterId);
        }

        function getAlarms(id){
          $scope.alarms=[];
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
          });
        }

        function selectActivePeriClusterByAmbariIp(ambariServerIp) {
          var periCluster = undefined;
          var periClusters = $filter('filter')($rootScope.periscopeClusters, function(value, index) { return value.host == ambariServerIp; }, true);
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
            angular.element(document.querySelector('#create-policy-collapse-btn')).click();
            ScalingPolicy.save({id: $scope.actPeriscopeCluster.id}, $scope.scalingAction, function(success) {
              $scope.policies = success;
              $scope.scalingActionBaseForm.$setPristine();
              $scope.policyForm.$setPristine();
              resetScalingActionForm();
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

        $scope.$on('DELETE_PERISCOPE_CLUSTER', function(event, ambariIp) {
            var periCluster = selectActivePeriClusterByAmbariIp(ambariIp);
            if (periCluster != undefined) {
              console.log('Delete periscope cluster with host: ' + periCluster.host);
              console.log(periCluster);
              PeriscopeCluster.delete({id: periCluster.id}, function(success){
                $rootScope.periscopeClusters = $filter('filter')($rootScope.periscopeClusters, function(value, index) { return value.id != periCluster.id;});
              });
            }
        });

        $scope.$on('CREATE_OR_START_PERISCOPE_CLUSTER', function(event, uluwatuCluster, message) {
          console.log(uluwatuCluster)
          if (message.indexOf("Cluster installation successfully") > -1) {
            createPeriscopeCluster(uluwatuCluster);
          } else if(message.indexOf("Cluster started successfully") > -1) {
            startPeriscopeCluster(uluwatuCluster);
          }
        });

        function createPeriscopeCluster(uluCluster) {
          console.log('Get cluster with id for create: ' + uluCluster.id)
          Cluster.get({id: uluCluster.id}, function(cluster){
            if (cluster.status == 'AVAILABLE') {
              var ambariJson = {
                'host': uluCluster.ambariServerIp,
                'port': '8080',
                'user': uluCluster.userName,
                'pass': uluCluster.password
              };
              PeriscopeCluster.save(ambariJson, function(periCluster){
                console.log(periCluster);
                $rootScope.periscopeClusters.push(periCluster);
                setActivePeriClusterWithResources(uluCluster.ambariServerIp);
              }, function(error) {

              });
            }
          });
        }

        function startPeriscopeCluster(uluCluster) {
          console.log('Get cluster with id for start: ' + uluCluster.id)
          Cluster.get({id: uluCluster.id}, function(cluster){
            if (cluster.status == 'AVAILABLE') {
              var periCluster = selectActivePeriClusterByAmbariIp(uluCluster.ambariServerIp);
              console.log(periCluster)
              if (periCluster != undefined) {
                var periscopeClusterStatus = { 'state': 'RUNNING'};
                PeriscopeClusterState.save({id: periCluster.id}, periscopeClusterStatus, function(success) {
                  console.log('start of periscope cluster was successfully....');
                  console.log(success)
                  setActivePeriClusterWithResources(uluCluster.ambariServerIp);
                }, function(error){
                });
              }
            }
          });
        }
    }
]);
