'use strict';

var log = log4javascript.getLogger("notificationController-logger");

angular.module('uluwatuControllers').controller('notificationController', ['$scope', '$rootScope', '$filter', 'Cluster', 'GlobalStack',
function ($scope, $rootScope, $filter, Cluster, GlobalStack) {
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

      if (successEvents.indexOf(eventType) > -1) {
        $scope.showSuccess(notification.eventMessage, notification.stackName);
        handleStatusChange(notification);
      } else if (errorEvents.indexOf(eventType) > -1) {
        $scope.showError(notification.eventMessage, notification.stackName);
        handleStatusChange(notification);
      } else {
        switch(eventType) {
          case "DELETE_COMPLETED":
            $scope.showSuccess(notification.eventMessage, notification.stackName);
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

    function handleStatusChange(notification){
      var actCluster = $filter('filter')($rootScope.clusters, { id: notification.stackId })[0];
      if (actCluster != undefined) {
        actCluster.status = notification.eventType;
        addNotificationToGlobalEvents(notification);
      }
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
      refreshMetadata(notification)
      actCluster.status = notification.eventType;
      $scope.showSuccess(msg, actCluster.name);
      addNotificationToGlobalEvents(notification);
      $rootScope.$broadcast('START_PERISCOPE_CLUSTER', actCluster, msg);
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
      item.customTimeStamp =  new Date(item.eventTimestamp).toLocaleDateString() + " " + new Date(item.eventTimestamp).toLocaleTimeString();
      $rootScope.events.push(item);
    }

    function refreshMetadata(notification) {
       GlobalStack.get({ id: notification.stackId }, function(success) {
          var metadata = []
          angular.forEach(success.instanceGroups, function(item) {
             angular.forEach(item.metadata, function(item1) {
               metadata.push(item1)
             });
          });
          $rootScope.activeCluster.metadata = metadata // trigger activeCluster.metadata
       });
    }
  }
]);
