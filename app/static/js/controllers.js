'use strict';

var $jq = jQuery.noConflict();

/* Controllers */
var log = log4javascript.getLogger("uluwatu-logger");
var popUpAppender = new log4javascript.PopUpAppender();
var layout = new log4javascript.PatternLayout("[%-5p] %m");
popUpAppender.setLayout(layout);

var uluwatuControllers = angular.module('uluwatuControllers', []);

uluwatuControllers.controller('uluwatuController', ['$scope', '$http', 'User', '$rootScope',
    function ($scope, $http, User, $rootScope) {
        $scope.user = User.get();

        $scope.errormessage = "";
        $scope.statusclass = "";
        $scope.awsCredentialInCreate = false;
        $http.defaults.headers.common['Content-Type'] = 'application/json';

        $scope.azureTemplate = false;
        $scope.awsTemplate = true;
        $scope.azureCredential = false;
        $scope.awsCredential = true;

        $scope.statusMessage = "";
        $scope.statusclass = "";

        $http.get('messages.properties').then(function (messages) {
            $rootScope.error_msg = messages.data
        });

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

        $scope.signOut = function() {

        }

        function logStackInfo(body) {
            if(body.status === 'AVAILABLE') {
                $scope.modifyStatusMessage($rootScope.error_msg.stack_create_completed, body.name);
                $scope.modifyStatusClass("has-success");
            } else if(body.status === 'CREATE_IN_PROGRESS')  {
                $scope.modifyStatusMessage($rootScope.error_msg.stack_create_in_progress, body.name);
                $scope.modifyStatusClass("has-success");
            } else if(body.status === 'UPDATE_IN_PROGRESS')  {
                $scope.modifyStatusMessage($rootScope.error_msg.stack_update_in_progress, body.name);
                $scope.modifyStatusClass("has-success");
            }  else if(body.status === 'CREATE_FAILED')  {
                $scope.modifyStatusMessage($rootScope.error_msg.stack_create_failed, body.name);
                $scope.modifyStatusClass("has-error");
            }  else if(body.status === 'DELETE_IN_PROGRESS')  {
                $scope.modifyStatusMessage($rootScope.error_msg.stack_delete_in_progress, body.name);
                $scope.modifyStatusClass("has-success");
            }  else if(body.status === 'DELETE_COMPLETED')  {
                $scope.modifyStatusMessage($rootScope.error_msg.stack_delete_completed, body.name);
                $scope.modifyStatusClass("has-success");
            } else {
                $scope.modifyStatusMessage($rootScope.error_msg.stack_else, body.name);
                $scope.modifyStatusClass("has-error");
            }
        }

        function logClusterInfo(body) {
            if(body.status === 'AVAILABLE') {
                $scope.modifyStatusMessage($rootScope.error_msg.cluster_create_completed, body.name);
                $scope.modifyStatusClass("has-success");
            } else if(body.status === 'CREATE_IN_PROGRESS')  {
                $scope.modifyStatusMessage($rootScope.error_msg.cluster_create_inprogress, body.name);
                $scope.modifyStatusClass("has-success");
            } else if(body.status === 'UPDATE_IN_PROGRESS')  {
                $scope.modifyStatusMessage($rootScope.error_msg.cluster_update_inprogress, body.name);
                $scope.modifyStatusClass("has-success");
            }  else if(body.status === 'CREATE_FAILED') {
                $scope.modifyStatusMessage($rootScope.error_msg.cluster_create_failed, body.name);
                $scope.modifyStatusClass("has-error");
            } else {
                $scope.modifyStatusMessage($rootScope.error_msg.cluster_else, body.name);
                $scope.modifyStatusClass("has-error");
            }
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

        $scope.addCrudControls = function() {
          addCrudControls();
        }
    }
]);
