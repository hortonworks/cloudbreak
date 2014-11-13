'use strict';

var log = log4javascript.getLogger("eventController-logger");

angular.module('uluwatuControllers').controller('eventController', ['$scope', '$rootScope', '$filter', 'UserEvents',
    function ($scope, $rootScope, $filter, UserEvents) {

        $scope.loadEvents = function () {
            $scope.events = UserEvents.query();
        }

    }
]);
