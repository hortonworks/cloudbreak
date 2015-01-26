'use strict';

var log = log4javascript.getLogger("clusterController-logger");
var $jq = jQuery.noConflict();

angular.module('uluwatuControllers').controller('clusterController', ['$scope', '$rootScope', '$filter', 'UluwatuCluster', 'GlobalStack', 'Cluster', '$interval', 'UserEvents',
    function ($scope, $rootScope, $filter, UluwatuCluster, GlobalStack, Cluster, $interval, UserEvents) {

        $rootScope.ledStyles = {
            "REQUESTED": "state2-run-blink",
            "CREATE_IN_PROGRESS": "state2-run-blink",
            "UPDATE_IN_PROGRESS": "state2-run-blink",
            "AVAILABLE": "state5-run",
            "DELETE_IN_PROGRESS": "state0-stop-blink",
            "DELETE_COMPLETED": "state3-stop",
            "STOPPED": "state4-ready",
            "START_REQUESTED": "state1-ready-blink",
            "START_IN_PROGRESS": "state1-ready-blink",
            "STOP_REQUESTED": "state1-ready-blink",
            "STOP_IN_PROGRESS": "state1-ready-blink",
            "CREATE_FAILED": "state3-stop",
            "START_FAILED": "state3-stop",
            "DELETE_FAILED": "state3-stop",
            "UPDATE_FAILED": "state3-stop",
            "STOP_FAILED": "state3-stop"
        }

        $rootScope.buttonStyles = {
            "REQUESTED": "fa-pause",
            "CREATE_IN_PROGRESS": "fa-pause",
            "UPDATE_IN_PROGRESS": "fa-pause",
            "AVAILABLE": "fa-stop",
            "DELETE_IN_PROGRESS": "fa-pause",
            "DELETE_COMPLETED": "fa-stop",
            "STOPPED": "fa-play",
            "START_REQUESTED": "fa-refresh",
            "START_IN_PROGRESS": "fa-refresh",
            "STOP_REQUESTED": "fa-refresh",
            "STOP_IN_PROGRESS": "fa-refresh",
            "CREATE_FAILED": "fa-play",
            "START_FAILED": "fa-play",
            "DELETE_FAILED": "fa-play",
            "UPDATE_FAILED": "fa-play",
            "STOP_FAILED": "fa-play"
        }

        $rootScope.activeCluster = {};

        $scope.detailsShow = true;
        $scope.periscopeShow = false;
        $scope.metricsShow = false;
        getUluwatuClusters();
        initCluster();

        $scope.selectedBlueprintChange = function () {
            var actualBp = $filter('filter')($rootScope.blueprints, { id: $scope.cluster.blueprintId});
            var hostgroups = [];
            actualBp[0].ambariBlueprint.host_groups.forEach(function(k){
                hostgroups.push({templateId: null, group: k.name, nodeCount: 0});
            });
            $scope.cluster.instanceGroups = hostgroups;
        }

        $scope.createCluster = function () {
            var blueprint = $filter('filter')($rootScope.blueprints, {id: $scope.cluster.blueprintId}, true)[0];

            if (blueprint.hostGroupCount > $scope.cluster.nodeCount) {
                $scope.modifyStatusMessage($rootScope.error_msg.hostgroup_invalid_node_count);
                $scope.modifyStatusClass("has-error");
                return;
            }
            if (blueprint.hostGroupCount === 1 && $scope.cluster.nodeCount != 1) {
                $scope.modifyStatusMessage($rootScope.error_msg.hostgroup_single_invalid);
                $scope.modifyStatusClass("has-error");
                return;
            }

            $scope.cluster.credentialId = $rootScope.activeCredential.id;
            UluwatuCluster.save($scope.cluster, function (result) {
                var nodeCount = 0;
                angular.forEach(result.instanceGroups, function(group) {
                  nodeCount += group.nodeCount;
                });
                result.nodeCount = nodeCount;
                result.cloudPlatform = $filter('filter')($rootScope.credentials, {id: $rootScope.activeCredential.id}, true)[0].cloudPlatform;
                result.public = $scope.cluster.public;
                angular.forEach(result.instanceGroups, function(item) {
                  item.templateId = parseFloat(item.templateId);
                });
                result.blueprintId = parseFloat(result.blueprintId);
                $rootScope.clusters.push(result);
                initCluster();
                $jq('.carousel').carousel(0);
                // enable toolbar buttons
                $jq('#toggle-cluster-block-btn').removeClass('disabled');
                $jq('#sort-clusters-btn').removeClass('disabled');
                $jq('#create-cluster-btn').removeClass('disabled');
                $jq("#notification-n-filtering").prop("disabled", false);
                $scope.clusterCreationForm.$setPristine();
            }, function(failure) {
                $scope.modifyStatusMessage($rootScope.error_msg.cluster_failed + ": " + failure.data.message);
                $scope.modifyStatusClass("has-error");
            });
        }

        $scope.deleteCluster = function (cluster) {
            UluwatuCluster.delete(cluster, function (result) {
                var actCluster = $filter('filter')($rootScope.clusters, { id: cluster.id }, true)[0];
                actCluster.status = "DELETE_IN_PROGRESS";
                $scope.modifyStatusMessage($rootScope.error_msg.cluster_delete_success1 + cluster.id + $rootScope.error_msg.cluster_delete_success2);
                $scope.modifyStatusClass("has-success");
                $scope.$broadcast('DELETE_PERISCOPE_CLUSTER', cluster.id);
            }, function (failure){
                $scope.modifyStatusMessage($rootScope.error_msg.cluster_delete_failed + ": " + failure.data.message);
                $scope.modifyStatusClass("has-error");
            });
        }
        $scope.changeActiveCluster = function (clusterId) {
            $rootScope.activeCluster = $filter('filter')($rootScope.clusters, { id: clusterId })[0];
            $rootScope.activeClusterBlueprint = $filter('filter')($rootScope.blueprints, { id: $rootScope.activeCluster.blueprintId})[0];
            $rootScope.activeClusterCredential = $filter('filter')($rootScope.credentials, {id: $rootScope.activeCluster.credentialId}, true)[0];
            $rootScope.activeCluster.cloudPlatform =  $rootScope.activeClusterCredential.cloudPlatform;
            GlobalStack.get({ id: clusterId }, function(success) {
                    $rootScope.activeCluster.description = success.description;
                    $rootScope.activeCluster.metadata = [];
                    angular.forEach($rootScope.activeCluster.instanceGroups, function(item) {
                      angular.forEach(item.metadata, function(item1) {
                        $rootScope.activeCluster.metadata.push(item1)
                      });
                    });
                    $scope.pagination = {
                                currentPage: 1,
                                itemsPerPage: 10,
                                totalItems: $rootScope.activeCluster.metadata.length
                    }
                }
            );
        }
         $scope.$watch('pagination.currentPage + pagination.itemsPerPage', function(){
            if ($rootScope.activeCluster.metadata != null) {
                paginateMetadata();
            }
         });

        function paginateMetadata() {
            var begin = (($scope.pagination.currentPage - 1) * $scope.pagination.itemsPerPage),
            end = begin + $scope.pagination.itemsPerPage;
            $scope.filteredActiveClusterData = $rootScope.activeCluster.metadata.slice(begin, end);
        }

        $scope.getSelectedTemplate = function (templateId) {
            return $filter('filter')($rootScope.templates, { id: templateId}, true)[0];
        }

        $rootScope.events = [];

        $scope.loadEvents = function () {
            $rootScope.events = UserEvents.query(function(success) {
                angular.forEach(success, function(item) {
                    item.customTimeStamp =  new Date(item.eventTimestamp).toLocaleDateString() + " " + new Date(item.eventTimestamp).toLocaleTimeString();
                });
            });
        }

        $scope.loadEvents();

        $scope.stopCluster = function (activeCluster) {
            var newStatus = {"status":"STOPPED"};
            Cluster.update({id: activeCluster.id}, newStatus, function(success){

                GlobalStack.update({id: activeCluster.id}, newStatus, function(result){
                  activeCluster.status = "STOP_REQUESTED";
                }, function(error) {
                  $scope.modifyStatusMessage($rootScope.error_msg.cluster_stop_failed + ": " + error.data.message);
                  $scope.modifyStatusClass("has-error");
                });

            }, function(error) {
              $scope.modifyStatusMessage($rootScope.error_msg.cluster_stop_failed + ": " + error.data.message);
              $scope.modifyStatusClass("has-error");
            });
        }

        $scope.startCluster = function (activeCluster) {
            var newStatus = {"status":"STARTED"};
            GlobalStack.update({id: activeCluster.id}, newStatus, function(result){

                Cluster.update({id: activeCluster.id}, newStatus, function(success){
                    activeCluster.status = "START_REQUESTED";
                }, function(error) {
                  $scope.modifyStatusMessage($rootScope.error_msg.cluster_start_failed + ": " + error.data.message);
                  $scope.modifyStatusClass("has-error");
                });

            }, function(error) {
              $scope.modifyStatusMessage($rootScope.error_msg.cluster_start_failed + ": " + error.data.message);
              $scope.modifyStatusClass("has-error");
            });
        }

        $scope.requestStatusChange = function(cluster) {
            if(cluster.status == "STOPPED") {
                $scope.startCluster(cluster);
            } else if(cluster.status == "AVAILABLE") {
                $scope.stopCluster(cluster);
            }
        }

        function getUluwatuClusters(){
          UluwatuCluster.query(function (clusters) {
              $rootScope.clusters = clusters;
              angular.forEach($rootScope.clusters, function(item) {
                   var nodeCount = 0;
                   angular.forEach(item.instanceGroups, function(group) {
                       nodeCount += group.nodeCount;
                   });
                   item.nodeCount = nodeCount;
              });
              $scope.$parent.orderClusters();
          });
        }

        function initCluster(){
            $scope.cluster = {
                password: "admin",
                userName: "admin"
            };
        }

        $scope.showDetails = function () {
            $scope.detailsShow = true;
            $scope.periscopeShow = false;
            $scope.metricsShow = false;
        }

        $scope.showPeriscope = function () {
            $scope.detailsShow = false;
            $scope.periscopeShow = true;
            $scope.metricsShow = false;
        }

        $scope.showMetrics = function () {
            $scope.detailsShow = false;
            $scope.periscopeShow = false;
            $scope.metricsShow = true;
        }


        $scope.logFilterFunction = function(element) {
            try {
                if (element.stackId === $rootScope.activeCluster.id) {
                    return (!element.eventType.match('BILLING_STARTED') && !element.eventType.match('BILLING_STOPPED') && !element.eventType.match('BILLING_CHANGED')) ? true : false;
                } else {
                    return false;
                }
            } catch (err) {
                return false;
            }
        }

        $scope.selectCluster = function(cluster) {
            $scope.selectedCluster = cluster
        }

    }]);
