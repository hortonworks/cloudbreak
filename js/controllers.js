'use strict';

/* Controllers */

var provisioningControllers = angular.module('provisioningControllers', []);



provisioningControllers.controller('ProvisioningController', ['$scope', '$http', 'Templates', '$location', '$rootScope',
    function ($scope, $http, Templates, $location, $rootScope) {

        $scope.form = undefined;
        $scope.apiUrl = "http://localhost:8080";
        $http.defaults.headers.common['x-auth-token']= $scope.token;
        $http.defaults.headers.common['Content-Type']= 'application/json';
        $rootScope.signedIn = false;
        $rootScope.providers = [];

        $scope.reloadCtrl = function(){
            console.log('reloading...');
            $route.reload();
        }

        $scope.signIn = function() {
            if(username.value === "user" && password.value === "pass") {
                localStorage.signedIn = true;
                $rootScope.signedIn = true;
            }
        }

        $scope.signOut = function() {
            $rootScope.signedIn = false;
            $scope.signUpOk = localStorage.signUpOk;
            $scope.token = "";
            localStorage.signedIn = false;
            localStorage.removeItem('token');

        }

        if (typeof (Storage) !== "undefined") {
            if (localStorage.signedIn === 'true') {
                $rootScope.signedIn = true;
            }
        } else {
            console.log("No localstorage support!");
        }

    }
]);


provisioningControllers.controller('AwsController', ['$scope', '$http', 'Templates', '$location', '$rootScope',
    function ($scope, $http, Templates, $location, $rootScope) {
        $scope.form = undefined;
        $scope.apiUrl = "http://localhost:8080";
        $http.defaults.headers.common['x-auth-token']= $scope.token;
        $http.defaults.headers.common['Content-Type']= 'application/json';

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

        $scope.reloadCtrl = function(){
            console.log('reloading...');
            $route.reload();
        }
    }
]);

provisioningControllers.controller('CloudProviderController', ['$scope', '$http', 'Templates', '$location', '$rootScope',
    function ($scope, $http, Templates, $location, $rootScope) {
        $scope.form = undefined;
        $scope.apiUrl = "http://localhost:8080";
        $http.defaults.headers.common['x-auth-token']= $scope.token;
        $http.defaults.headers.common['Content-Type']= 'application/json';
        $scope.supportedProviders = ["Azure", "AWS"];
        $scope.awsProvider = false;
        $scope.azureProvider = false;


        $scope.reloadCtrl = function(){
            console.log('reloading...');
            $route.reload();
        }

        $scope.createProvider = function(providerName) {
            if(providerName === "Azure") {
                $scope.azureProvider = true;
                $scope.awsProvider = false;
            } else if(providerName === "AWS") {
                $scope.azureProvider = false;
                $scope.awsProvider = true;
            } else {
                $scope.awsProvider = false;
                $scope.azureProvider = false;
            }
        }

        $scope.createAwsProvider = function($location) {
            console.log("create aws");
            var awsObject = {type:"aws", secretKey: secretKey.value, accessKey: accessKey.value};
            secretKey.value = "";
            accessKey.value = "";
            $rootScope.providers.push(awsObject);
        }

        $scope.createAzureProvider = function($location) {
            console.log("create azure");
            var azureObject = {type:"azure", subscriptionId: subscriptionId.value, keystorePath: keystorePath.value, keystorePassword: keystorePassword.value};
            subscriptionId.value = "";
            keystorePath.value = "";
            keystorePassword.value = "";
            $rootScope.providers.push(azureObject);
        }

        $scope.createProvider();
    }
]);
