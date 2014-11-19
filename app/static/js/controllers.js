'use strict';

var $jq = jQuery.noConflict();

/* Controllers */
var log = log4javascript.getLogger("uluwatu-logger");
var popUpAppender = new log4javascript.PopUpAppender();
var layout = new log4javascript.PatternLayout("[%-5p] %m");
popUpAppender.setLayout(layout);

var uluwatuControllers = angular.module('uluwatuControllers', []);

uluwatuControllers.controller('uluwatuController', ['$scope', '$http', 'User', '$rootScope', '$filter',
    function ($scope, $http, User, $rootScope, $filter) {
        var orderBy = $filter('orderBy');
        $scope.user = User.get();

        var socket = io();
        socket.on('notification', handleNotification);

        $scope.errormessage = "";
        $scope.statusclass = "";
        $http.defaults.headers.common['Content-Type'] = 'application/json';

        $scope.azureTemplate = false;
        $scope.awsTemplate = true;
        $scope.azureCredential = false;
        $scope.awsCredential = true;
        $scope.lastOrderPredicate = 'name';

        $scope.statusMessage = "";
        $scope.statusclass = "";

        $scope.modifyStatusMessage = function(message, name) {
            var now = new Date();
            var date = now.toTimeString().split(" ")[0];
            if (name) {
                $scope.statusMessage = date + " " + name +  " " + message;
            } else {
                $scope.statusMessage = date + " " + message;
            }
        }

        $scope.modifyStatusClass = function(status) {
            $scope.statusclass = status;
        }

        $scope.addPanelJQueryEventListeners = function(panel) {
          addPanelJQueryEventListeners(panel);
        }

        $scope.addClusterFormJQEventListeners = function() {
          addClusterFormJQEventListeners();
        }

        $scope.addActiveClusterJQEventListeners = function() {
          addActiveClusterJQEventListeners();
        }

        $scope.addClusterListPanelJQEventListeners = function() {
           addClusterListPanelJQEventListeners();
        }

        $scope.addDatePickerPanelJQueryEventListeners = function() {
            addDatePickerPanelJQueryEventListeners();
        }

        $scope.addCrudControls = function() {
          addCrudControls();
        }

        $scope.order = function(predicate, reverse) {
          $scope.lastOrderPredicate = predicate;
          $rootScope.clusters = orderBy($rootScope.clusters, predicate, reverse);
        }

        $scope.orderByUptime = function() {
          $scope.lastOrderPredicate = 'uptime';
          $rootScope.clusters = orderBy($rootScope.clusters,
            function(element) {
                return parseInt(element.hoursUp * 60 + element.minutesUp);
            },
            false);
        }

        $scope.orderClusters = function() {
            if($scope.lastOrderPredicate == 'uptime') {
                $scope.orderByUptime();
            } else {
                $scope.order($scope.lastOrderPredicate, false);
            }
        };

        function handleNotification(notification) {
          console.log(notification)
          var eventType = notification.eventType;
          switch(eventType) {
            case "CLUSTER_CREATION_FAILED":
              handleStatusChange(notification, $rootScope.error_msg.cluster_create_failed, "has-error");
              break;
            case "REQUESTED":
              handleStatusChange(notification, $rootScope.error_msg.stack_create_requested, "has-success");
              break;
            case "CREATE_IN_PROGRESS":
              handleStatusChange(notification, $rootScope.error_msg.stack_create_in_progress, "has-success");
              break;
            case "UPDATE_IN_PROGRESS":
              handleStatusChange(notification, $rootScope.error_msg.cluster_update_inprogress, "has-success");
              break;
            case "CREATE_FAILED":
              handleStatusChange(notification, $rootScope.error_msg.stack_create_failed, "has-error");
              break;
            case "START_REQUESTED":
              handleStatusChange(notification, $rootScope.error_msg.cluster_start_requested, "has-success");
              break;
            case "START_IN_PROGRESS":
              handleStatusChange(notification, $rootScope.error_msg.cluster_start_in_progress, "has-success");
              break;
            case "START_FAILED":
              handleStatusChange(notification, $rootScope.error_msg.cluster_start_failed, "has-error");
              break;
            case "STOPPED":
              handleStatusChange(notification, $rootScope.error_msg.cluster_stopped, "has-success");
              break;
            case "STOP_REQUESTED":
              handleStatusChange(notification, $rootScope.error_msg.cluster_stop_requested, "has-success");
              break;
            case "STOP_IN_PROGRESS":
              handleStatusChange(notification, $rootScope.error_msg.cluster_stop_in_progress, "has-success");
              break;
            case "STOP_FAILED":
              handleStatusChange(notification, $rootScope.error_msg.cluster_stop_failed, "has-error");
              break;
            case "DELETE_FAILED":
              handleStatusChange(notification, $rootScope.error_msg.stack_delete_failed, "has-success");
              break;
            case "DELETE_IN_PROGRESS":
              handleStatusChange(notification, $rootScope.error_msg.stack_delete_in_progress, "has-success");
              break;
            case "DELETE_COMPLETED":
              handleStatusChange(notification, $rootScope.error_msg.stack_delete_completed, "has-success");
              $rootScope.clusters = $filter('filter')($rootScope.clusters, function(value, index) { return value.id != notification.stackId;});
              break;
            case "AVAILABLE":
              handleAvailableNotification(notification);
              break;
            case "UPTIME_NOTIFICATION":
              handleUptimeNotification(notification);
              break;
          }
          $scope.$apply();

          function handleStatusChange(notification, message, statusClass){
            var actCluster = $filter('filter')($rootScope.clusters, { id: notification.stackId })[0];
            actCluster.status = notification.eventType;
            $scope.modifyStatusMessage(message, actCluster.name);
            $scope.modifyStatusClass(statusClass);
          }

          function handleAvailableNotification(notification) {
            var actCluster = $filter('filter')($rootScope.clusters, { id: notification.stackId })[0];
            var msg = notification.eventMessage;
            if (msg != null && msg != undefined && msg.indexOf("AMBARI_IP:") > -1) {
              actCluster.ambariServerIp = msg.split(':')[1];
            }
            var nodeCount = notification.nodeCount;
            if (nodeCount != null && nodeCount != undefined) {
              actCluster.nodeCount = nodeCount;
            }
            actCluster.status = notification.eventType;
            $scope.modifyStatusMessage($rootScope.error_msg.cluster_create_completed, actCluster.name);
            $scope.modifyStatusClass("has-success");
          }

          function handleUptimeNotification(notification) {
            var SECONDS_PER_MINUTE = 60;
            var MILLIS_PER_SECOND = 1000;
            var runningInMs = parseInt(notification.eventMessage);
            var minutes = ((runningInMs/ (MILLIS_PER_SECOND * SECONDS_PER_MINUTE)) % SECONDS_PER_MINUTE);
            var hours = (runningInMs / (MILLIS_PER_SECOND * SECONDS_PER_MINUTE * SECONDS_PER_MINUTE));
            var actCluster = $filter('filter')($rootScope.clusters, { id: notification.stackId })[0];
            actCluster.minutesUp = parseInt(minutes);
            actCluster.hoursUp = parseInt(hours);
          }
        }
    }
]);
