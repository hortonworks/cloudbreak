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
            var periCluster = selectPeriscopeClusterByAmbariIp(uluCluster.ambariServerIp);
            if (isSelectedUluClusterEqualsPeriClusterAndRunning(periCluster)) {
              setActivePeriClusterWithResources(periCluster);
            } else {
              disableAutoScalingPolicies();
            }
          } else {
            disableAutoScalingPolicies();
          }
        }, false);

        function isSelectedUluClusterEqualsPeriClusterAndRunning(periCluster) {
          return periCluster != undefined && $rootScope.activeCluster.ambariServerIp != undefined
            && $rootScope.activeCluster.ambariServerIp == periCluster.host && periCluster.state == 'RUNNING';
        }

        function setActivePeriClusterWithResources(periscopeCluster) {
          $scope.actPeriscopeCluster = periscopeCluster;
          $scope.autoScalingSLAPoliciesEnabled = true;
          getAlarms(periscopeCluster.id);
          getScalingConfigurations(periscopeCluster.id);
          getScalingPolicies(periscopeCluster.id);
        }

        function disableAutoScalingPolicies() {
          $scope.actPeriscopeCluster = undefined;
          $scope.autoScalingSLAPoliciesEnabled = false;
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
          });
        }

        function getScalingPolicies(id){
          ScalingPolicy.query({id: id}, function (policies) {
            $scope.policies = policies;
            resetScalingActionForm();
          });
        }

        $scope.enableAutoScaling = function() {
          var uluCluster = $rootScope.activeCluster;
          var periCluster = $filter('filter')($rootScope.periscopeClusters, function(value, index) { return value.stackId == uluCluster.id; }, true)[0];
          if (periCluster == undefined) {
            createPeriscopeCluster(uluCluster);
          } else if (periCluster.state == 'SUSPENDED') {
            startPeriscopeCluster(uluCluster);
          }
        }

        $scope.disableAutoScaling = function() {
          var uluCluster = $rootScope.activeCluster;
          var periCluster = $filter('filter')($rootScope.periscopeClusters, function(value, index) { return value.stackId == uluCluster.id; }, true)[0];
          var periscopeClusterStatus = { 'state': 'SUSPENDED'};
          PeriscopeClusterState.save({id: periCluster.id}, periscopeClusterStatus, function(success) {
            periCluster.state = 'SUSPENDED';
            disableAutoScalingPolicies();
          }, function(error){
            $scope.showError($rootScope.error_msg.peri_cluster_update_failed + ": " + error.data.message);
          });
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
              MetricAlarm.save({id: periClusterId}, $scope.alarm, createAlarmSuccessHandler, createAlarmErrorHandler);
            } else {
              TimeAlarm.save({id: periClusterId}, $scope.alarm, createAlarmSuccessHandler, createAlarmErrorHandler);
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

        function createAlarmErrorHandler(error) {
          $scope.showErrorMessage($rootScope.error_msg.alarm_creation_failed + ": " + error.data.message);
        }

        function resetAlarmForms() {
          $scope.alarm = {}
          if ($scope.metricBasedAlarm) {
            $scope.alarm.metric = "PENDING_CONTAINERS";
            $scope.alarm.comparisonOperator = "EQUALS";
          } else {
            $scope.alarm.timeZone = 'Etc/GMT';
          }
        }

        $scope.deleteAlarm = function(alarm) {
          if ($scope.actPeriscopeCluster != undefined) {
            if (alarm.metric != undefined) {
              MetricAlarm.delete({id: $scope.actPeriscopeCluster.id, alarmId: alarm.id}, function(success) { deleteAlarmSuccessHandler(success, alarm, $scope.actPeriscopeCluster.id) }, deleteAlarmErrorHandler);
            } else {
              TimeAlarm.delete({id: $scope.actPeriscopeCluster.id, alarmId: alarm.id}, function(success) { deleteAlarmSuccessHandler(success, alarm, $scope.actPeriscopeCluster.id) }, deleteAlarmErrorHandler);
            }
          }
        }

        function deleteAlarmSuccessHandler(success, alarm, periClusterId) {
          $scope.alarms = $filter('filter')($scope.alarms, function(value, index) { return value.id != alarm.id; }, true);
          getScalingPolicies(periClusterId);
        }

        function deleteAlarmErrorHandler(error) {
          $scope.showErrorMessage($rootScope.error_msg.alarm_delete_failed + ": " + error.data.message);
        }

        function selectPeriscopeClusterByAmbariIp(ambariServerIp) {
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
              $scope.scalingConfiguration = success;
            }, function(error) {
              $scope.showErrorMessage($rootScope.error_msg.peri_cluster_update_failed + ": " + error.data.message);
            });
          }
        }

        $scope.createPolicy = function() {
          if ($scope.actPeriscopeCluster != undefined) {
            ScalingPolicy.save({id: $scope.actPeriscopeCluster.id}, $scope.scalingAction.policy, function(success) {
              angular.element(document.querySelector('#create-policy-collapse-btn')).click();
              getScalingPolicies($scope.actPeriscopeCluster.id);
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
            }, function(error) {
              $scope.showErrorMessage($rootScope.error_msg.scaling_policy_delete_failed + ": " + error.data.message);
            });
          }
        }

        $scope.$on('DELETE_PERISCOPE_CLUSTER', function(event, stackId) {
          var periCluster = $filter('filter')($rootScope.periscopeClusters, function(value, index) { return value.stackId == stackId; }, true)[0];
          if (periCluster != undefined) {
            PeriscopeCluster.delete({id: periCluster.id}, function(success){
              $rootScope.periscopeClusters = $filter('filter')($rootScope.periscopeClusters, function(value, index) { return value.id != periCluster.id;});
            }, function(error) {
              $scope.showErrorMessage($rootScope.error_msg.peri_cluster_delete_failed + ": " + error.data.message);
            });
          }
        });

        $scope.$on('START_PERISCOPE_CLUSTER', function(event, uluwatuCluster, message) {
          if(message.indexOf("Cluster started successfully") > -1) {
            startPeriscopeCluster(uluwatuCluster);
          }
        });

        function startPeriscopeCluster(uluCluster) {
          Cluster.get({id: uluCluster.id}, function(cluster){
            if (cluster.status == 'AVAILABLE') {
              var periCluster = $filter('filter')($rootScope.periscopeClusters, function(value, index) { return value.stackId == uluCluster.id; }, true)[0];
              if (periCluster != undefined) {
                //update ambari ip after start
                var ambariJson = createAmbariJsonFromUluwatuCluster(uluCluster);
                PeriscopeCluster.update({id: periCluster.id}, ambariJson, function(success) {
                  periCluster.host = uluCluster.ambariServerIp;
                  //set state to RUNNING
                  var periscopeClusterStatus = { 'state': 'RUNNING'};
                  PeriscopeClusterState.save({id: periCluster.id}, periscopeClusterStatus, function(success) {
                    periCluster.state = 'RUNNING';
                    if (isSelectedUluClusterEqualsPeriClusterAndRunning(periCluster)) {
                      setActivePeriClusterWithResources(periCluster);
                    }
                  }, function(error){
                    $scope.showErrorMessage($rootScope.error_msg.peri_cluster_start_failed + ": " + error.data.message);
                  });

                }, function(error) {
                  $scope.showErrorMessage($rootScope.error_msg.peri_cluster_start_failed + ": " + error.data.message);
                });
              }
            }
          });
        }

        function createPeriscopeCluster(uluCluster) {
          Cluster.get({id: uluCluster.id}, function(cluster) {
            if (cluster.status == 'AVAILABLE') {
              var ambariJson = createAmbariJsonFromUluwatuCluster(uluCluster);
              PeriscopeCluster.save(ambariJson, function(periCluster){
                $rootScope.periscopeClusters.push(periCluster);
                var periCluster = selectPeriscopeClusterByAmbariIp(uluCluster.ambariServerIp);
                if (isSelectedUluClusterEqualsPeriClusterAndRunning(periCluster)) {
                  setActivePeriClusterWithResources(periCluster);
                }
              }, function(error) {
                $scope.showErrorMessage($rootScope.error_msg.peri_cluster_creation_failed + ": " + error.data.message);
              });
            } else {
              $scope.showWarningMessage($rootScope.error_msg.cluster_not_available_yet);
            }
          }, function(error) {
            $scope.showWarningMessage($rootScope.error_msg.cluster_not_available_yet);
          });
        }

        $scope.timeZoneOrderGetter = function(mapEntry) {
          var result = 0;
          if(mapEntry.key != undefined && mapEntry.key != 'Etc/GMT') {
            result = parseInt(mapEntry.key.replace("Etc/GMT",""));
          }
          return result;
        }

        function createAmbariJsonFromUluwatuCluster(uluCluster) {
          return {
            'host': uluCluster.ambariServerIp,
            'port': '8080',
            'user': uluCluster.userName,
            'pass': uluCluster.password
          };
        }
    }
]);
