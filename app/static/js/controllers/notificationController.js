'use strict';

var log = log4javascript.getLogger("notificationController-logger");

angular.module('uluwatuControllers').controller('notificationController', ['$scope', '$rootScope', '$filter',
function ($scope, $rootScope, $filter) {
    var successEvents = [ "REQUESTED",
                          "CREATE_IN_PROGRESS",
                          "UPDATE_IN_PROGRESS",
                          "START_REQUESTED",
                          "START_IN_PROGRESS",
                          "STOPPED",
                          "STOP_REQUESTED",
                          "STOP_IN_PROGRESS",
                          "DELETE_IN_PROGRESS" ];

    var errorEvents = [ "CLUSTER_CREATION_FAILED",
                        "CREATE_FAILED",
                        "START_FAILED",
                        "DELETE_FAILED",
                        "UPDATE_FAILED",
                        "STOP_FAILED" ];

    var socket = io();
    socket.on('notification', handleNotification);

    function handleNotification(notification) {
      var eventType = notification.eventType;

      if (eventType!="UPTIME_NOTIFICATION") {
        console.log(notification)
      }

      if (successEvents.indexOf(eventType) > -1) {
        handleStatusChange(notification, "has-success");
      } else if (errorEvents.indexOf(eventType) > -1) {
        handleStatusChange(notification, "has-error");
      } else {
        switch(eventType) {
          case "DELETE_COMPLETED":
            handleStatusChange(notification, "has-success");
            $rootScope.clusters = $filter('filter')($rootScope.clusters, function(value, index) { return value.id != notification.stackId;});
            break;
          case "AVAILABLE":
            handleAvailableNotification(notification);
            break;
          case "UPTIME_NOTIFICATION":
            handleUptimeNotification(notification);
            break;
        }
      }

      $scope.$apply();
    }

    function handleStatusChange(notification, statusClass){
      var actCluster = $filter('filter')($rootScope.clusters, { id: notification.stackId })[0];
      actCluster.status = notification.eventType;
      $scope.modifyStatusMessage(notification.eventMessage, actCluster.name);
      $scope.modifyStatusClass(statusClass);
      addNotificationToGlobalEvents(notification);
    }

    function handleAvailableNotification(notification) {
      var actCluster = $filter('filter')($rootScope.clusters, { id: notification.stackId })[0];
      var msg = notification.eventMessage;
      var indexOfAmbariIp = msg.indexOf("AMBARI_IP:");
      if (msg != null && msg != undefined && indexOfAmbariIp > -1) {
        actCluster.ambariServerIp = msg.split(':')[1];
        msg = msg.substr(0, indexOfAmbariIp);
      }
      var nodeCount = notification.nodeCount;
      if (nodeCount != null && nodeCount != undefined) {
        actCluster.nodeCount = nodeCount;
      }
      actCluster.status = notification.eventType;
      $scope.modifyStatusMessage(msg, actCluster.name);
      $scope.modifyStatusClass("has-success");
      addNotificationToGlobalEvents(notification);
    }

    function handleUptimeNotification(notification) {
      var actCluster = $filter('filter')($rootScope.clusters, { id: notification.stackId })[0];
      if (actCluster != undefined) {
        var SECONDS_PER_MINUTE = 60;
        var MILLIS_PER_SECOND = 1000;
        var runningInMs = parseInt(notification.eventMessage);
        var minutes = ((runningInMs/ (MILLIS_PER_SECOND * SECONDS_PER_MINUTE)) % SECONDS_PER_MINUTE);
        var hours = (runningInMs / (MILLIS_PER_SECOND * SECONDS_PER_MINUTE * SECONDS_PER_MINUTE));
        actCluster.minutesUp = parseInt(minutes);
        actCluster.hoursUp = parseInt(hours);
      }
    }

    function addNotificationToGlobalEvents(item) {
      item.eventTimestamp =  new Date(item.eventTimestamp).toISOString();
      item.customTimeStamp =  new Date(item.eventTimestamp).toLocaleDateString() + " " + new Date(item.eventTimestamp).toLocaleTimeString();
      $rootScope.events.push(item);
    }
  }
]);
