'use strict';

/* App Module */

var cloudbreakApp = angular.module('cloudbreakApp', ['ngRoute', 'uluwatuControllers', 'uluwatuServices']);


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

    $httpProvider.responseInterceptors.push(authInterceptor);

} ]);
