'use strict';

var $jq = jQuery.noConflict();

/* Controllers */
var log = log4javascript.getLogger("uluwatu-logger");
var popUpAppender = new log4javascript.PopUpAppender();
var layout = new log4javascript.PatternLayout("[%-5p] %m");
popUpAppender.setLayout(layout);

var uluwatuControllers = angular.module('uluwatuControllers', ['cgNotify']);

uluwatuControllers.controller('uluwatuController', ['$scope', '$http', 'User', '$rootScope', '$filter', 'UserPermission', 'ErrorHandler', 'notify',
    function ($scope, $http, User, $rootScope, $filter, UserPermission, ErrorHandler, notify) {
        var orderBy = $filter('orderBy');
        $scope.user = User.get();

        $scope.errormessage = "";
        $scope.statusclass = "";
        $http.defaults.headers.common['Content-Type'] = 'application/json';

        $scope.azureTemplate = false;
        $scope.awsTemplate = true;
        $scope.azureCredential = false;
        $scope.awsCredential = true;
        $scope.lastOrderPredicate = 'name';

        $scope.statusMessage = "";
        $scope.shortStatusMessage = "";
        $scope.statusclass = "";
        getUserPermission();

        $scope.modifyStatusMessage = function(message, name) {
            var now = new Date();
            var date = now.toTimeString().split(" ")[0];
            if (name) {
                $scope.statusMessage = date + " " + name +  ": " + message;
            } else {
                $scope.statusMessage = date + " " + message;
            }
            $scope.shortStatusMessage = $scope.statusMessage.substring(0, 100) + '...';
        }

        $scope.modifyStatusClass = function(status) {
            $scope.statusclass = status;
        }

        $scope.showError = function(error, prefix) {
            var errorMsg = ErrorHandler.handleError(error);
            if (prefix){
                $scope.showErrorMessage(errorMsg, prefix)
            } else {
                $scope.showErrorMessage(errorMsg);
            }
        }

        $scope.showErrorMessage = function(message, prefix) {
            $scope.modifyStatusMessage(message, prefix);
            $scope.modifyStatusClass("has-error");
            $scope.popupError($scope.statusMessage);
        }

        $scope.showWarningMessage = function(message) {
          $scope.modifyStatusMessage(message);
          $scope.modifyStatusClass("has-warning");
          $scope.popupWarning($scope.statusMessage);
        }

        $scope.showSuccess = function(message, prefix) {
          $scope.modifyStatusMessage(message);
          $scope.modifyStatusClass("has-success");
          $scope.popupSuccess($scope.statusMessage);
        }

        $scope.popupInfo = function(message) {
            notify({
                message: message.toString(),
                position: 'right',
                classes: 'alert-info',
                duration: 4000
            });
        }

        $scope.popupWarning = function(message) {
            notify({
                message: message.toString(),
                position: 'right',
                classes: 'alert-warning',
                duration: 4000
            });
        }

        $scope.popupError = function(message) {
            notify({
                message: message.toString(),
                position: 'right',
                classes: 'alert-danger',
                duration: 4000
            });
        }

        $scope.popupSuccess = function(message) {
            notify({
                message: message.toString(),
                position: 'right',
                classes: 'alert-success',
                duration: 4000
            });
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

        $scope.eventTimestampAsFloat = function(element) {
          return parseFloat(element.eventTimestamp);
        }

        function getUserPermission(){
          UserPermission.get(function(success){
            $scope.user.admin = success.admin;
          });
        }
    }
]);
