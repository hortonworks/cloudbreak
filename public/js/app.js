'use strict';
var $jq = jQuery.noConflict();

var regApp = angular.module('regApp', ['ngRoute']);

regApp.controller("regController", ['$scope', '$http', '$location',
    function ($scope, $http) {
        $scope.signUp = function() {
            $http({method: 'POST',dataType: 'json',url:  "/registration",
                   data: {email: email.value, firstName: firstName.value, lastName: lastName.value, password: password.value,
                          company: company.value}
                  }).success(function(responseData){
                    $jq('.carousel').carousel(1);
                  }).error(function (data, status, headers, config){
                     console.log("error :"+ status)
                  });
        }
    }
]);

regApp.directive('match', function($parse) {
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