'use strict';

/* App Module */

var cloudbreakApp = angular.module('cloudbreakApp', ['ngRoute', 'base64', 'uluwatuControllers', 'uluwatuServices']);


cloudbreakApp.directive('match', function($parse) {
  return {
    require: 'ngModel',
    link: function(scope, elem, attrs, ctrl) {
      scope.$watch(function() {
        return $parse(attrs.match)(scope) === ctrl.$modelValue;
      }, function(currentValue) {
        ctrl.$setValidity('mismatch', currentValue);
      });
    }
  };
});

cloudbreakApp.directive('validjson', function($parse) {
    return {
        require: 'ngModel',
        link: function(scope, elem, attrs, ctrl) {
            ctrl.$parsers.unshift(function(viewValue) {
                var valid = false;
                var json = {};
                try {
                    json = JSON.parse(viewValue);
                    valid = true;
                } catch (err){}
                ctrl.$setValidity('validjson', valid);
                return valid ? json : undefined;
            });
        }
    };
});

cloudbreakApp.directive('file', function(){
    return {
        scope: {
            file: '='
        },
        link: function(scope, el, attrs){
             el.bind('change', function(event){
                scope.file = event.target.files[0];
                scope.$apply();
            });
        }
    };
});


cloudbreakApp.config([ '$routeProvider', '$locationProvider', '$httpProvider', function($routeProvider, $locationProvider, $httpProvider) {
    $routeProvider.when('/', {
        templateUrl: 'partials/dashboard.html',
        controller: 'uluwatuController'
    }) .otherwise({
        redirectTo : '/'
    });

    var authInterceptor = ['$rootScope', '$q', '$window', function (scope, $q, $window) {
        function success(response) {
            return response;
        }
        function error(response) {
            var status = response.status;
            if (response.status === 401){
              $window.location.href = scope.authorizeUrl;
            }
            return $q.reject(response);
        }
        return function (promise) {
            return promise.then(success, error);
        }
    }];

    $httpProvider.interceptors.push(authInterceptor);

} ]);

cloudbreakApp.run(function ($rootScope, $http) {
    $http.get('messages.properties').then(function (messages) {
        $rootScope.error_msg = messages.data
        $rootScope.titleStatus = {
            "REQUESTED": $rootScope.error_msg.title_requested,
            "CREATE_IN_PROGRESS": $rootScope.error_msg.title_create_in_progress,
            "UPDATE_IN_PROGRESS": $rootScope.error_msg.title_update_in_progress,
            "AVAILABLE": $rootScope.error_msg.title_create_completed,
            "CREATE_FAILED": $rootScope.error_msg.title_create_failed,
            "DELETE_IN_PROGRESS": $rootScope.error_msg.title_delete_in_progress,
            "DELETE_COMPLETED": $rootScope.error_msg.title_delete_completed
        }
    });
});
