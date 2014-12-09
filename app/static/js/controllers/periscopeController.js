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
            // console.log($rootScope.periscopeClusters)
            var periCluster = selectPeriscopeClusterByAmbariIp(uluCluster.ambariServerIp);
            console.log($rootScope.activeCluster.ambariServerIp)
            console.log(periCluster)
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

        $scope.enableAutoScaling = function() {
          var uluCluster = $rootScope.activeCluster;
          var periCluster = $filter('filter')($rootScope.periscopeClusters, function(value, index) { return value.stackId == uluCluster.id; }, true)[0];
          if (periCluster == undefined) {
            console.log('create new periscope cluster....')
            createPeriscopeCluster(uluCluster);
          } else if (periCluster.state == 'SUSPENDED') {
            console.log('start periscope cluster....')
            startPeriscopeCluster(uluCluster);
          }
        }

        $scope.disableAutoScaling = function() {
          console.log('disable autoscaling....')
          var uluCluster = $rootScope.activeCluster;
          var periCluster = $filter('filter')($rootScope.periscopeClusters, function(value, index) { return value.stackId == uluCluster.id; }, true)[0];
          var periscopeClusterStatus = { 'state': 'SUSPENDED'};
          PeriscopeClusterState.save({id: periCluster.id}, periscopeClusterStatus, function(success) {
            console.log('periscope cluster was SUSPENDED successfully....');
            periCluster.state = 'SUSPENDED';
            disableAutoScalingPolicies();
          }, function(error){
            console.log(error)
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

        $scope.$on('DELETE_PERISCOPE_CLUSTER', function(event, stackId) {
            var periCluster = $filter('filter')($rootScope.periscopeClusters, function(value, index) { return value.stackId == stackId; }, true)[0];
            if (periCluster != undefined) {
              console.log('Delete periscope cluster with host: ' + periCluster.host);
              console.log(periCluster);
              PeriscopeCluster.delete({id: periCluster.id}, function(success){
                $rootScope.periscopeClusters = $filter('filter')($rootScope.periscopeClusters, function(value, index) { return value.id != periCluster.id;});
              });
            }
        });

        $scope.$on('START_PERISCOPE_CLUSTER', function(event, uluwatuCluster, message) {
          console.log(uluwatuCluster)
          if(message.indexOf("Cluster started successfully") > -1) {
            startPeriscopeCluster(uluwatuCluster);
          }
        });

        function startPeriscopeCluster(uluCluster) {
          console.log('Get cluster with id for start: ' + uluCluster.id)
          Cluster.get({id: uluCluster.id}, function(cluster){
            if (cluster.status == 'AVAILABLE') {
              var periCluster = $filter('filter')($rootScope.periscopeClusters, function(value, index) { return value.stackId == uluCluster.id; }, true)[0];
              console.log(periCluster)
              if (periCluster != undefined) {
                //update ambari ip after start
                var ambariJson = {
                  'host': uluCluster.ambariServerIp,
                  'port': '8080',
                  'user': uluCluster.userName,
                  'pass': uluCluster.password
                };
                PeriscopeCluster.update({id: periCluster.id}, ambariJson, function(success) {
                  periCluster.host = uluCluster.ambariServerIp;
                  //set state to RUNNING
                  var periscopeClusterStatus = { 'state': 'RUNNING'};
                  PeriscopeClusterState.save({id: periCluster.id}, periscopeClusterStatus, function(success) {
                    console.log('start of periscope cluster was successfully....');
                    console.log(success)
                    periCluster.state = 'RUNNING';
                    if (isSelectedUluClusterEqualsPeriClusterAndRunning(periCluster)) {
                      setActivePeriClusterWithResources(periCluster);
                    }
                  }, function(error){
                    console.log(error)
                  });

                }, function(error) {
                  console.log(error);
                });
              }
            }
          });
        }

        function createPeriscopeCluster(uluCluster) {
          console.log('Get cluster with id for create: ' + uluCluster.id)
          Cluster.get({id: uluCluster.id}, function(cluster) {
            if (cluster.status == 'AVAILABLE') {
              console.log(cluster)
              var ambariJson = {
                'host': uluCluster.ambariServerIp,
                'port': '8080',
                'user': uluCluster.userName,
                'pass': uluCluster.password
              };
              PeriscopeCluster.save(ambariJson, function(periCluster){
                console.log(periCluster);
                $rootScope.periscopeClusters.push(periCluster);

                var periCluster = selectPeriscopeClusterByAmbariIp(uluCluster.ambariServerIp);
                if (isSelectedUluClusterEqualsPeriClusterAndRunning(periCluster)) {
                  setActivePeriClusterWithResources(periCluster);
                }
              }, function(error) {
                console.log(error)
              });
            } else {
              console.log('not available....')
            }
          }, function(error) {
            console.log(error)
          });
        }
    }
]);
