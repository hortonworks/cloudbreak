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
                    if (responseData.message == 'SUCCESS'){
                        $scope.email = email.value;
                        $jq('.carousel').carousel(1);
                    } else {
                        $scope.message = responseData.message
                        $jq("#msgDialog").modal('show');
                    }
                  }).error(function (data, status, headers, config){
                        $scope.message = data.message
                        $jq("#msgDialog").modal('show');
                  });
        }
    }
]);

regApp.controller("resetController", ['$scope', '$http',
    function ($scope, $http, $location) {
        $scope.resetPassword = function() {
                    var email = null;
                    var url = window.location.href.split('?')
                    if (url.length == 2 && url[1].split('=').length == 2 && url[1].split('=')[0] == 'email') {
                        email = url[1].split('=')[1];
                        var resetToken =  window.location.pathname.split('/')[2]
                          $http({method: 'POST',dataType: 'json', url:  "/reset/" + resetToken,
                             data: {password: resetPasswField.value, email: email}
                        }).success(function(responseData){
                           if (responseData.message == 'SUCCESS'){
                              $scope.message = "password update succeed"
                              $jq("#errorDialog").modal('show');
                              window.location = '/'
                              } else {
                                $scope.message = 'password update failed'
                                $jq("#errorDialog").modal('show');
                              }
                        }).error(function (data, status, headers, config){
                           $scope.message = data.message
                           $jq("#msgDialog").modal('show');
                        });
                    } else {
                        $scope.message = 'Email query parameter is missing (or there are more query parameters)'
                        $jq("#errorDialog").modal('show');
                    }
        }
    }
]);

regApp.controller("loginController", ['$scope', '$http', '$rootScope',
    function ($scope, $http, $rootScope) {
        $scope.$watch($scope.message, function(){
          if ($scope.message.length > 0){
            $jq("#msgDialog").modal('show');
          }
        });
        $scope.forgetPassword = function() {
            $http({method: 'POST',dataType: 'json', url:  "/forget",
                 data: {email: email.value},
                 headers: {'Content-Type': 'application/json'}
            }).success(function(responseData){
                if (responseData.message == 'SUCCESS') {
                    $jq("#login-forgot-passw").html("<i class='fa fa-question-circle fa-fw'></i> reset my password")
                    $jq('#password').prop("disabled", false);
                    $jq('#login-btn').removeClass('hidden');
                    $jq('#forgot-btn').addClass('hidden');
                    $scope.message = 'reset password email sent to ' + email.value;
                    $jq(".modal-header h4").text($scope.message)
                    $jq("#msgDialog").modal('show');
                } else {
                    $scope.message = responseData.message;
                    $jq(".modal-header h4").text($scope.message)
                    $jq("#msgDialog").modal('show');
                }
            }).error(function(data) {
                    $scope.message = data.message
                    $jq(".modal-header h4").text($scope.message)
                    $jq("#msgDialog").modal('show');
            });
        }
    }
]);

regApp.controller("regForAccController", ['$scope', '$http',
    function ($scope, $http) {
            $scope.signUpByInvite = function() {
                $http({method: 'POST',dataType: 'json',url:  "/registerForAccount",
                       data: {email: email.value, firstName: firstName.value, lastName: lastName.value, password: password.value,
                              company: company.value}
                      }).success(function(responseData){
                        if (responseData.message == 'SUCCESS'){
                            $scope.email = email.value;
                            $jq('.carousel').carousel(1);
                        } else {
                            $scope.message = responseData.message
                            $jq("#msgDialog").modal('show');
                        }
                      }).error(function (data, status, headers, config){
                            $scope.message = data.message
                            $jq("#msgDialog").modal('show');
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
