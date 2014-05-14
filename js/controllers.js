'use strict';

/* Controllers */

var provisioningControllers = angular.module('provisioningControllers', []);

provisioningControllers.controller('ProvisioningController', ['$scope', '$http', 'Templates', '$location', '$rootScope',
    function ($scope, $http, Templates, $location, $rootScope) {
        $scope.form = undefined;
        $scope.apiUrl = "http://localhost:8080";
        $http.defaults.headers.common['x-auth-token']= $scope.token;
        $http.defaults.headers.common['Content-Type']= 'application/json';
        $scope.signedIn = false;

        $scope.reloadCtrl = function(){
            console.log('reloading...');
            $route.reload();
        }

    }
]);

provisioningControllers.controller('AwsController', ['$scope', '$http', 'Templates', '$location', '$rootScope',
    function ($scope, $http, Templates, $location, $rootScope) {
        $scope.form = undefined;
        $scope.apiUrl = "http://localhost:8080";
        $http.defaults.headers.common['x-auth-token']= $scope.token;
        $http.defaults.headers.common['Content-Type']= 'application/json';
        $scope.signedIn = false;

        $scope.reloadCtrl = function(){
            console.log('reloading...');
            $route.reload();
        }

    }
]);

provisioningControllers.controller('AzureController', ['$scope', '$http', 'Templates', '$location', '$rootScope',
    function ($scope, $http, Templates, $location, $rootScope) {
        $scope.form = undefined;
        $scope.apiUrl = "http://localhost:8080";
        $http.defaults.headers.common['x-auth-token']= $scope.token;
        $http.defaults.headers.common['Content-Type']= 'application/json';
        $scope.signedIn = false;

        $scope.reloadCtrl = function(){
            console.log('reloading...');
            $route.reload();
        }

    }
]);