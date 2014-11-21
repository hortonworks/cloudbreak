'use strict';

var log = log4javascript.getLogger("eventController-logger");

angular.module('uluwatuControllers').controller('eventController', ['$scope', '$rootScope', '$filter', 'UserEvents',
    function ($scope, $rootScope, $filter, UserEvents) {

        initFilter();
        $rootScope.events = {};

        $scope.loadEvents = function () {
            initFilter();
            $rootScope.events = UserEvents.query(function(success) {
                angular.forEach(success, function(item) {
                    item.eventTimestamp =  new Date(item.eventTimestamp).toISOString();
                    item.customTimeStamp = item.eventTimestamp.replace("T", " ").replace("Z", " ").substring(0, 19);
                });
            });
        }

        $scope.loadEvents();

        $scope.eventFilterFunction = function(element) {
            var isListedElement = true;
            if ($scope.localFilter.user !== "") {
                if (element.owner.toLowerCase().indexOf($scope.localFilter.user.toLowerCase()) == -1) {
                    isListedElement = false;
                }
            }
            if ($scope.localFilter.cloud !== "all") {
                if (element.cloud !== $scope.localFilter.cloud) {
                    isListedElement = false;
                }
            }
            if ($scope.localFilter.eventType !== "all") {
                if (element.eventType.toLowerCase() != ($scope.localFilter.eventType.toLowerCase())) {
                    isListedElement = false;
                }
            }
            return isListedElement;
        };

        $scope.clearFilter = function() {
            initFilter();
        }

        function initFilter() {
            $scope.localFilter = {
                user: "",
                cloud: "all",
                eventType: "all"
            };
        }

    }
]);
