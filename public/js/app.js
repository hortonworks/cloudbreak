'use strict';
var $jq = jQuery.noConflict();

var regApp = angular.module('regApp', ['ngRoute']);

regApp.controller("regController", ['$scope', '$http',
    function ($scope, $http) {
        $scope.signUp = function() {
            $http({method: 'POST',dataType: 'json',url:  "/register",
                   data: {email: email.value, firstName: firstName.value, lastName: lastName.value, password: password.value,
                          company: company.value}
                  }).success(function(responseData){
                    if (responseData == 'SUCCESS'){
                        $jq('.carousel').carousel(1);
                    } else {
                        alert(responseData)
                    }
                  }).error(function (data, status, headers, config){
                     console.log("error :"+ status)
                  });
        }
    }
]);

regApp.controller("resetController", ['$scope', '$http',
    function ($scope, $http, $location) {
        $scope.resetPassword = function() {
                    var resetToken =  window.location.pathname.split('/')[2]
                    $http({method: 'POST',dataType: 'json', url:  "/reset/" + resetToken,
                           data: {password: resetPasswField.value}
                          }).success(function(responseData){
                            if (responseData == 'SUCCESS'){
                              console.log("password update succeed") // TODO
                              window.location = '/login'
                            } else {
                              console.log("password update failed") // TODO
                            }
                  }).error(function (data, status, headers, config){
                     console.log("error :"+ status)
                  });
        }
    }
]);

regApp.controller("loginController", ['$scope', '$http',
    function ($scope, $http, $location) {
        $scope.signIn = function() {
            $http({method: 'POST',dataType: 'json', url:  "/login",
                 data: {username: emailFieldLogin.value,  password: passwFieldLogin.value},
                 headers: {'Content-Type': 'application/json'}
            }).success(function(responseData){
                if (responseData == 'SUCCESS') {
                    window.location = '/confirm'
                } else {
                    alert(responseData)
                }
            }).error(function(data) {
                alert('Server error 500')
            });
        }
        $scope.forgetPassword = function() {
            $http({method: 'POST',dataType: 'json', url:  "/forget",
                 data: {email: emailFieldLogin.value},
                 headers: {'Content-Type': 'application/json'}
            }).success(function(responseData){
                if (responseData == 'SUCCESS') {
                    $jq("#login-forgot-passw").html("<i class='fa fa-question-circle fa-fw'></i> forgot my password")
                    $jq('#passwFieldLogin').prop("disabled", false);
                    $jq('#login-btn').removeClass('hidden');
                    $jq('#forgot-btn').addClass('hidden');
                    alert('forgot password email sent.')
                } else {
                    alert(responseData)
                }
            }).error(function(data) {
                alert('Server error 500')
            });
        }
    }
]);

regApp.controller("confirmController", ['$scope', '$http',
    function ($scope, $http, $location) {
        $scope.confirm = function(choose) {
            var target = '/login'
            $http({method: 'POST',dataType: 'json', url:  "/confirm",
                 data: {choose: choose},
                 headers: {'Content-Type': 'application/json'}
            }).success(function(responseData){
                if (responseData.statusCode == 302) {
                    window.location = responseData
                } else {
                  window.location = target
                }
            }).error(function(data) {
                 alert('Server error 500')
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