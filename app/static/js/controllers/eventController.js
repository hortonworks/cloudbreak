'use strict';

var log = log4javascript.getLogger("eventController-logger");

angular.module('uluwatuControllers').controller('eventController', ['$scope', '$rootScope', '$filter', 'UserEvents',
    function ($scope, $rootScope, $filter, UserEvents) {

        initFilter();

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
