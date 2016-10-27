'use strict';

var $jq = jQuery.noConflict();

/* Controllers */
var log = log4javascript.getLogger("uluwatu-logger");
var popUpAppender = new log4javascript.PopUpAppender();
var layout = new log4javascript.PatternLayout("[%-5p] %m");
popUpAppender.setLayout(layout);

var uluwatuControllers = angular.module('uluwatuControllers', ['cgNotify']);

/*
 usage:
    - html: {{ "message : {0} {1}" | format:"0":"1"}}
    - controller: $filter("format")("message: {0} {1}", "0", "1")
 */
uluwatuControllers.filter("format", function() {
    return function(input) {
        var args = arguments;
        return input.replace(/\{(\d+)\}/g, function(match, capture) {
            return args[1 * capture + 1];
        });
    };
});

uluwatuControllers.controller('uluwatuController', ['$scope', '$http', 'User', '$rootScope', '$filter', 'UserPermission', 'ErrorHandler', 'notify',
    function($scope, $http, User, $rootScope, $filter, UserPermission, ErrorHandler, notify) {
        var orderBy = $filter('orderBy');
        $scope.user = User.get();

        $scope.errormessage = "";
        $scope.statusclass = "";
        $http.defaults.headers.common['Content-Type'] = 'application/json';

        $scope.lastOrderPredicate = 'name';

        $scope.statusMessage = "";
        $scope.shortStatusMessage = "";
        $scope.statusclass = "";
        getUserPermission();

        $scope.modifyStatusMessage = function(message, name) {
            var now = new Date();
            var date = now.toTimeString().split(" ")[0];
            if (name) {
                $scope.statusMessage = date + " " + name + ": " + message;
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
            if (prefix) {
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

        $scope.popup = function(message, modifyClass) {
            notify({
                message: message,
                position: 'right',
                classes: modifyClass,
                duration: 4000,
                onClose: function() {
                    var el = angular.element(document.querySelector('[data-closing="true"]'));
                    if (el != undefined) {
                        el.remove();
                    };
                }
            });
        }

        $scope.popupInfo = function(message) {
            $scope.popup(message.toString(), 'alert-info');
        }

        $scope.popupWarning = function(message) {
            $scope.popup(message.toString(), 'alert-warning');
        }

        $scope.popupError = function(message) {
            $scope.popup(message.toString(), 'alert-danger');
        }

        $scope.popupSuccess = function(message) {
            $scope.popup(message.toString(), 'alert-success');
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
            if ($scope.lastOrderPredicate == 'uptime') {
                $scope.orderByUptime();
            } else {
                $scope.order($scope.lastOrderPredicate, false);
            }
        };

        $scope.eventTimestampAsFloat = function(element) {
            return parseFloat(element.eventTimestamp);
        }

        $scope.isWriteScope = function(name, groups) {
            if (searchScopeElement(name, groups) != null) {
                return true;
            } else {
                return false;
            }
        }

        var searchScopeElement = function(element, groups) {
            var result = null;
            if (groups != undefined && groups != null) {
                angular.forEach(groups, function(item) {
                    if (endsWith(item.display, element)) {
                        result = item;
                        return result;
                    }
                });
            }
            return result;
        }

        var endsWith = function(str, suffix) {
            if (str != undefined && str.indexOf(suffix, str.length - suffix.length) !== -1) {
                return true;
            } else {
                return false;
            }
        }

        $scope.firstVisible = function(platforms) {
            for (var i = 0; i < platforms.length; i++) {
                if ($scope.isVisible(platforms[i])) {
                    return i;
                }
            }
            return -1;
        }

        $scope.isVisible = function(platform) {
            return $rootScope.params.platforms.indexOf(platform) > -1
        }

        $scope.countList = function(list) {
            var resList = $filter('filter')(list, $scope.filterByVisiblePlatform);
            return resList.length;
        }

        $scope.filterByVisiblePlatform = function(resource) {
            if (resource.cloudPlatform) {
                return $scope.isVisible(resource.cloudPlatform)
            } else {
                return $scope.isVisible("BYOS")
            }
        }

        $scope.filterByImportedStack = function(resource) {
            if (resource.orchestrator) {
                if (resource.orchestrator.type === "MARATHON") {
                    return $scope.isVisible("BYOS")
                }
            }
        }

        $rootScope.deselectActiveCluster = function() {
            $rootScope.activeCluster = {};
        };

        function getUserPermission() {
            UserPermission.get(function(success) {
                $scope.user.admin = success.admin;
            });
        }
    }
]);