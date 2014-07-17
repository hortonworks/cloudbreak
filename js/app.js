'use strict';

/* App Module */

var cloudbreakApp = angular.module('cloudbreakApp', ['ngRoute', 'cloudbreakControllers', 'cloudbreakServices']);

cloudbreakApp.directive('validjson', function($parse) {
  return {
    require: 'ngModel',
    link: function(scope, elem, attrs, ctrl) {
      ctrl.$parsers.unshift(function(viewValue) {
        try {
          JSON.parse(viewValue)
          ctrl.$setValidity('validjson', true);
        } catch (err){
          ctrl.$setValidity('validjson', false);
        }
      });
    }
  };
});

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

cloudbreakApp.config([ '$routeProvider', function($routeProvider) {
    $routeProvider.when('/', {
        controller: 'cloudbreakController'
    }) .otherwise({
        redirectTo : '/'
    });
 /*       .when('/console', {
        templateUrl : 'partials/console.html',
        controller: 'consoleController'
    })*/

} ]);
