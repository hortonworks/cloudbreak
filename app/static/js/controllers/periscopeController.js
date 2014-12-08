'use strict';

var log = log4javascript.getLogger("periscopeController-logger");

angular.module('uluwatuControllers').controller('periscopeController', ['$scope', '$rootScope', '$filter', 'PeriscopeCluster', 'MetricAlarm',
  'TimeAlarm', 'ScalingPolicy', 'Cluster', 'PeriscopeClusterState', 'PeriscopeClusterScalingConfiguration',
  function ($scope, $rootScope, $filter, PeriscopeCluster, MetricAlarm, TimeAlarm, ScalingPolicy, Cluster, PeriscopeClusterState, PeriscopeClusterScalingConfiguration) {
        $rootScope.periscopeClusters = PeriscopeCluster.query();
        $scope.alarms = [];
        $scope.policies = {};
        $scope.scalingConfiguration = {};
        $scope.scalingAction = {};
        $scope.alarm = {};
        $scope.metricBasedAlarm = true;
        $scope.timeBasedAlarm = false;
        $scope.actPeriscopeCluster = undefined;
        resetAlarmForms();
        resetScalingActionForm();

        $rootScope.$watch('activeCluster', function(uluCluster, oldUluCluster){
          if (uluCluster.ambariServerIp != undefined) {
            console.log($rootScope.periscopeClusters)
            setActivePeriClusterWithResources(uluCluster.ambariServerIp);
          }
        }, false);

        function setActivePeriClusterWithResources(ambariServerIp) {
          var periCluster = selectActivePeriClusterByAmbariIp(ambariServerIp);
          console.log($rootScope.activeCluster.ambariServerIp)
          if (isActiveUluClusterEqualsPeriCluster(periCluster)) {
            console.log(periCluster)
            $scope.actPeriscopeCluster = periCluster;
            getAlarms(periCluster.id);
            getScalingConfigurations(periCluster.id);
            getScalingPolicies(periCluster.id);
          } else {
            //cluster is not ready yet disable autscaling functions
            $scope.actPeriscopeCluster = undefined;
          }
        }

        function isActiveUluClusterEqualsPeriCluster(periCluster) {
          return periCluster != undefined && $rootScope.activeCluster.ambariServerIp != undefined
            && $rootScope.activeCluster.ambariServerIp == periCluster.host;
        }

        $scope.activateMetricAlarmCreationForm = function(isMetricAlarm) {
          $scope.metricBasedAlarm = isMetricAlarm;
          $scope.timeBasedAlarm = !isMetricAlarm;
          resetAlarmForms();
        }

        $scope.createAlarm = function() {
          if ($scope.actPeriscopeCluster != undefined) {
            if ($scope.alarm.email != undefined) {
              var notifications = [];
              notifications.push({
                "target": [$scope.alarm.email],
                "notificationType": "EMAIL"
              });
              $scope.alarm.notifications = notifications;
              delete $scope.alarm.email;
            }

            var periClusterId = $scope.actPeriscopeCluster.id;
            if ($scope.metricBasedAlarm) {
              MetricAlarm.save({id: periClusterId}, $scope.alarm, createAlarmSuccessHandler);
            } else {
              TimeAlarm.save({id: periClusterId}, $scope.alarm, createAlarmSuccessHandler);
            }
          }
        }

        function createAlarmSuccessHandler(success) {
          console.log(success)
          $scope.alarms.push(success);
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
            success.forEach(function(el) { $scope.alarms.push(el); });
          });
          TimeAlarm.query({id: id}, function (success) {
            success.forEach(function(el) { $scope.alarms.push(el); });
          });
        }

        function getScalingConfigurations(id) {
          PeriscopeClusterScalingConfiguration.get({id: id}, function(success) {
            $scope.scalingConfiguration = success;
          }, function(error) {
            console.log(error);
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

        $scope.updateScalingConfiguration = function() {
          if ($scope.actPeriscopeCluster != undefined) {
            PeriscopeClusterScalingConfiguration.save({id: $scope.actPeriscopeCluster.id}, $scope.scalingConfiguration, function(success) {
              console.log($scope.scalingConfiguration)
              $scope.scalingConfiguration = success;
            }, function(error) {
              console.log(error);
            });
          }
        }

        $scope.createPolicy = function() {
          if ($scope.actPeriscopeCluster != undefined) {
            ScalingPolicy.save({id: $scope.actPeriscopeCluster.id}, $scope.scalingAction.policy, function(success) {
              console.log(success)
              angular.element(document.querySelector('#create-policy-collapse-btn')).click();
              $scope.policies.push(success);
              $scope.policyForm.$setPristine();
              resetScalingActionForm();
            });
          }
        }

        function resetScalingActionForm() {
          $scope.scalingAction = {};
          $scope.scalingAction.policy = {};
          $scope.scalingAction.policy.adjustmentType = "NODE_COUNT";
        }

        $scope.deletePolicy = function(policy) {
          if ($scope.actPeriscopeCluster != undefined) {
            ScalingPolicy.delete({id: $scope.actPeriscopeCluster.id, policyId: policy.id}, function(success) {
              $scope.policies = $filter('filter')($scope.policies, function(value, index) { return value.id != policy.id; }, true);
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
