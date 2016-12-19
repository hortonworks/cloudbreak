'use strict';

var log = log4javascript.getLogger("periscopeController-logger");

angular.module('uluwatuControllers').controller('periscopeController', ['$scope', '$rootScope', '$filter', 'PeriscopeCluster', 'MetricAlert',
    'TimeAlert', 'ScalingPolicy', 'Cluster', 'PeriscopeClusterState', 'PeriscopeClusterScalingConfiguration', 'MetricDefinitions', 'PeriscopeClusterScalingHistory', 'ErrorHandler',
    function($scope, $rootScope, $filter, PeriscopeCluster, MetricAlert, TimeAlert, ScalingPolicy, Cluster, PeriscopeClusterState,
        PeriscopeClusterScalingConfiguration, MetricDefinitions, PeriscopeClusterScalingHistory, ErrorHandler) {
        $rootScope.periscopeClusters = PeriscopeCluster.query();
        $scope.alerts = [];
        $scope.policies = {};
        $scope.scalingConfiguration = {};
        $scope.scalingAction = {};
        $scope.alert = {};
        $rootScope.scalingHistory = {};
        $scope.metricBasedAlarm = true;
        $scope.timeBasedAlarm = false;
        $scope.actPeriscopeCluster = undefined;
        $scope.alertDefinitions = [];
        resetAlarmForms();
        resetScalingActionForm();

        $rootScope.$watch("activeCluster || activeCluster.cluster.status == 'AVAILABLE' && activeCluster.status == 'AVAILABLE'", function(uluCluster, oldUluCluster) {
            if (uluCluster.cluster != undefined && uluCluster.cluster.ambariServerIp != undefined) {
                var periCluster = selectPeriscopeClusterByAmbariIp(uluCluster.cluster.ambariServerIp);
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
            return periCluster != undefined && $rootScope.activeCluster.cluster.ambariServerIp != undefined && $rootScope.activeCluster.cluster.ambariServerIp == periCluster.host && periCluster.state == 'RUNNING';
        }

        function setActivePeriClusterWithResources(periscopeCluster) {
            $scope.actPeriscopeCluster = periscopeCluster;
            $scope.autoScalingSLAPoliciesEnabled = true;
            if ($rootScope.activeCluster.cluster.status == 'AVAILABLE' && $rootScope.activeCluster.status == 'AVAILABLE') {
                getAlertDefinitions(periscopeCluster.id);
                getAlarms(periscopeCluster.id);
                getScalingConfigurations(periscopeCluster.id);
                getScalingHistory(periscopeCluster.id);
                getScalingPolicies(periscopeCluster.id);
            }
        }

        function disableAutoScalingPolicies() {
            $scope.actPeriscopeCluster = undefined;
            $scope.autoScalingSLAPoliciesEnabled = false;
        }

        function getAlertDefinitions(id) {
            $scope.alertDefinitions = [{label:"HDFS Capacity Utilization", name: "hdfs_usage"}];
//            MetricDefinitions.query({
//                id: id
//            }, function(success) {
//                success.forEach(function(el) {
//                    $scope.alertDefinitions.push(el);
//                });
//            });
        }

        function getAlarms(id) {
            $scope.alerts = [];
            MetricAlert.query({
                id: id
            }, function(success) {
                success.forEach(function(el) {
                    $scope.alerts.push(el);
                });
            });
            TimeAlert.query({
                id: id
            }, function(success) {
                success.forEach(function(el) {
                    $scope.alerts.push(el);
                });
            });
        }

        function getScalingConfigurations(id) {
            PeriscopeClusterScalingConfiguration.get({
                id: id
            }, function(success) {
                $scope.scalingConfiguration = success;
            });
        }

        function getScalingHistory(id) {
            PeriscopeClusterScalingHistory.query({
                id: id
            }, function(success) {
                angular.forEach(success, function(item) {
                    item.eventTimestampAsFloat = item.timestamp;
                    item.timestamp = new Date(item.timestamp).toLocaleString();
                });
                $rootScope.scalingHistory = success;
                $scope.pagination = {
                    currentPage: 1,
                    itemsPerPage: 10,
                    totalItems: $rootScope.scalingHistory.length
                }
            }, function(error) {
                console.log(error);
            });
        }

        $scope.$watchCollection(function() {
            return $rootScope.scalingHistory;
        }, function() {
            if ($rootScope.scalingHistory != null) {
                paginateScalingHistory();
            }
        });

        $scope.$watch('pagination.currentPage + pagination.itemsPerPage', function() {
            if ($rootScope.scalingHistory != null) {
                paginateScalingHistory();
            }
        });

        function paginateScalingHistory() {
            if ($scope.pagination != null && $rootScope.scalingHistory.length > 0) {
                $scope.pagination.totalItems = $rootScope.scalingHistory.length;
                var begin = (($scope.pagination.currentPage - 1) * $scope.pagination.itemsPerPage),
                    end = begin + $scope.pagination.itemsPerPage;
                $scope.filteredScalingHistory = $rootScope.scalingHistory.slice(begin, end);
            } else {
                $scope.filteredScalingHistory = [];
            }
        }

        function getScalingPolicies(id) {
            ScalingPolicy.query({
                id: id
            }, function(policies) {
                $scope.policies = policies;
                resetScalingActionForm();
            });
        }

        $scope.enableAutoScaling = function() {
            var uluCluster = $rootScope.activeCluster;
            var periCluster = $filter('filter')($rootScope.periscopeClusters, function(value, index) {
                return value.stackId == uluCluster.id;
            }, true)[0];
            if (periCluster == undefined) {
                createPeriscopeCluster(uluCluster);
            } else if (periCluster.state == 'SUSPENDED') {
                startPeriscopeCluster(uluCluster);
            }
        }

        $scope.disableAutoScaling = function() {
            var uluCluster = $rootScope.activeCluster;
            var periCluster = $filter('filter')($rootScope.periscopeClusters, function(value, index) {
                return value.stackId == uluCluster.id;
            }, true)[0];
            var periscopeClusterStatus = {
                'state': 'SUSPENDED'
            };
            PeriscopeClusterState.save({
                id: periCluster.id
            }, periscopeClusterStatus, function(success) {
                periCluster.state = 'SUSPENDED';
                disableAutoScalingPolicies();
            }, function(error) {
                $scope.showError($rootScope.msg.peri_cluster_update_failed + ": " + ErrorHandler.handleError(error));
            });
        }

        $scope.activateMetricAlertCreationForm = function(isMetricAlert) {
            $scope.metricBasedAlarm = isMetricAlert;
            $scope.timeBasedAlarm = !isMetricAlert;
            resetAlarmForms();
        }

        $scope.createAlert = function() {
            if ($scope.actPeriscopeCluster != undefined) {
                var periClusterId = $scope.actPeriscopeCluster.id;
                if ($scope.metricBasedAlarm) {
                    MetricAlert.save({
                        id: periClusterId
                    }, $scope.alert, createAlarmSuccessHandler, createAlarmErrorHandler);
                } else {
                    TimeAlert.save({
                        id: periClusterId
                    }, $scope.alert, createAlarmSuccessHandler, createAlarmErrorHandler);
                }
            }
        }

        function createAlarmSuccessHandler(success) {
            $scope.alerts.push(success);
            $scope.metricBasedAlertForm.$setPristine();
            $scope.timeBasedAlertForm.$setPristine();
            resetAlarmForms();
            angular.element(document.querySelector('#panel-create-periscope-alert-btn')).click();
        }

        function createAlarmErrorHandler(error) {
            $scope.showErrorMessage($rootScope.msg.alarm_creation_failed + ": " + ErrorHandler.handleError(error));
        }

        function resetAlarmForms() {
            $scope.alert = {}
            if ($scope.timeBasedAlarm) {
                $scope.alert.timeZone = 'Etc/GMT';
            }
        }

        $scope.deleteAlarm = function(alert) {
            if ($scope.actPeriscopeCluster != undefined) {
                if (alert.alertDefinition != undefined) {
                    MetricAlert.delete({
                        id: $scope.actPeriscopeCluster.id,
                        alertId: alert.id
                    }, function(success) {
                        deleteAlarmSuccessHandler(success, alert, $scope.actPeriscopeCluster.id)
                    }, deleteAlarmErrorHandler);
                } else {
                    TimeAlert.delete({
                        id: $scope.actPeriscopeCluster.id,
                        alertId: alert.id
                    }, function(success) {
                        deleteAlarmSuccessHandler(success, alert, $scope.actPeriscopeCluster.id)
                    }, deleteAlarmErrorHandler);
                }
            }
        }

        function deleteAlarmSuccessHandler(success, alarm, periClusterId) {
            $scope.alerts = $filter('filter')($scope.alerts, function(value, index) {
                return value.id != alarm.id;
            }, true);
            getScalingPolicies(periClusterId);
        }

        function deleteAlarmErrorHandler(error) {
            $scope.showErrorMessage($rootScope.msg.alarm_delete_failed + ": " + ErrorHandler.handleError(error));
        }

        function selectPeriscopeClusterByAmbariIp(ambariServerIp) {
            var periCluster = undefined;
            var periClusters = $filter('filter')($rootScope.periscopeClusters, function(value, index) {
                return value.host == ambariServerIp;
            }, true);
            if (periClusters != undefined && periClusters.length > 0) {
                periCluster = periClusters[0];
            }
            return periCluster;
        }

        $scope.updateScalingConfiguration = function() {
            if ($scope.actPeriscopeCluster != undefined) {
                PeriscopeClusterScalingConfiguration.save({
                    id: $scope.actPeriscopeCluster.id
                }, $scope.scalingConfiguration, function(success) {
                    $scope.scalingConfiguration = success;
                }, function(error) {
                    $scope.showErrorMessage($rootScope.msg.peri_cluster_update_failed + ": " + ErrorHandler.handleError(error));
                });
            }
        }

        $scope.createPolicy = function() {
            if ($scope.actPeriscopeCluster != undefined) {
                ScalingPolicy.save({
                    id: $scope.actPeriscopeCluster.id
                }, $scope.scalingAction.policy, function(success) {
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
                ScalingPolicy.delete({
                    id: $scope.actPeriscopeCluster.id,
                    policyId: policy.id
                }, function(success) {
                    $scope.policies = $filter('filter')($scope.policies, function(value, index) {
                        return value.id != policy.id;
                    }, true);
                }, function(error) {
                    $scope.showErrorMessage($rootScope.msg.scaling_policy_delete_failed + ": " + ErrorHandler.handleError(error));
                });
            }
        }

        $scope.$on('DELETE_PERISCOPE_CLUSTER', function(event, stackId) {
            var periCluster = $filter('filter')($rootScope.periscopeClusters, function(value, index) {
                return value.stackId == stackId;
            }, true)[0];
            if (periCluster != undefined) {
                PeriscopeCluster.delete({
                    id: periCluster.id
                }, function(success) {
                    $rootScope.periscopeClusters = $filter('filter')($rootScope.periscopeClusters, function(value, index) {
                        return value.id != periCluster.id;
                    });
                }, function(error) {
                    $scope.showErrorMessage($rootScope.msg.peri_cluster_delete_failed + ": " + ErrorHandler.handleError(error));
                });
            }
        });

        $scope.$on('START_PERISCOPE_CLUSTER', function(event, uluwatuCluster, message) {
            if (message.indexOf("Cluster started successfully") > -1) {
                startPeriscopeCluster(uluwatuCluster);
            }
        });

        function startPeriscopeCluster(uluCluster) {
            Cluster.get({
                id: uluCluster.id
            }, function(cluster) {
                if (cluster.status == 'AVAILABLE') {
                    var periCluster = $filter('filter')($rootScope.periscopeClusters, function(value, index) {
                        return value.stackId == uluCluster.id;
                    }, true)[0];
                    if (periCluster != undefined) {
                        //update ambari ip after start
                        var ambariJson = createAmbariJsonFromUluwatuCluster(uluCluster);
                        PeriscopeCluster.update({
                            id: periCluster.id
                        }, ambariJson, function(success) {
                            periCluster.host = uluCluster.cluster.ambariServerIp;
                            //set state to RUNNING
                            var periscopeClusterStatus = {
                                'state': 'RUNNING'
                            };
                            PeriscopeClusterState.save({
                                id: periCluster.id
                            }, periscopeClusterStatus, function(success) {
                                periCluster.state = 'RUNNING';
                                if (isSelectedUluClusterEqualsPeriClusterAndRunning(periCluster)) {
                                    setActivePeriClusterWithResources(periCluster);
                                }
                            }, function(error) {
                                $scope.showErrorMessage($rootScope.msg.peri_cluster_start_failed + ": " + ErrorHandler.handleError(error));
                            });

                        }, function(error) {
                            $scope.showErrorMessage($rootScope.msg.peri_cluster_start_failed + ": " + ErrorHandler.handleError(error));
                        });
                    }
                }
            });
        }

        function createPeriscopeCluster(uluCluster) {
            Cluster.get({
                id: uluCluster.id
            }, function(cluster) {
                if (cluster.status == 'AVAILABLE') {
                    var ambariJson = createAmbariJsonFromUluwatuCluster(uluCluster);
                    PeriscopeCluster.save(ambariJson, function(periCluster) {
                        $rootScope.periscopeClusters.push(periCluster);
                        var periCluster = selectPeriscopeClusterByAmbariIp(uluCluster.cluster.ambariServerIp);
                        if (isSelectedUluClusterEqualsPeriClusterAndRunning(periCluster)) {
                            setActivePeriClusterWithResources(periCluster);
                        }
                    }, function(error) {
                        $scope.showErrorMessage($rootScope.msg.peri_cluster_creation_failed + ": " + ErrorHandler.handleError(error));
                    });
                } else {
                    $scope.showWarningMessage($rootScope.msg.cluster_not_available_yet);
                }
            }, function(error) {
                $scope.showWarningMessage($rootScope.msg.cluster_not_available_yet);
            });
        }

        $scope.timeZoneOrderGetter = function(mapEntry) {
            var result = 0;
            if (mapEntry.key != undefined && mapEntry.key != 'Etc/GMT') {
                result = parseInt(mapEntry.key.replace("Etc/GMT", ""));
            }
            return result;
        }

        function createAmbariJsonFromUluwatuCluster(uluCluster) {
            return {
                'host': uluCluster.cluster.ambariServerIp,
                'port': uluCluster.gatewayPort,
                'user': uluCluster.cluster.userName,
                'pass': uluCluster.cluster.password
            };
        }
    }
]);