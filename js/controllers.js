'use strict';

/* Controllers */

var provisioningControllers = angular.module('provisioningControllers', []);

var wait = function() {
    var millisecondsToWait = 2000;
    setTimeout(function() {
        console.log("timeout...")
    }, millisecondsToWait);
}

provisioningControllers.controller('ProvisioningController', ['$scope', '$http', 'Templates', '$location', '$rootScope',
    function ($scope, $http, Templates, $location, $rootScope) {
        $scope.form = undefined;
        $http.defaults.useXDomain = true;
        delete $http.defaults.headers.common['X-Requested-With'];
        $http.defaults.headers.common['Content-Type']= 'application/json';

        $rootScope.signedIn = false;
        if($rootScope.providers === null || $rootScope.providers === undefined ) {
            $rootScope.providers = [];
        }
        if($rootScope.infras === null || $rootScope.infras === undefined ) {
            $rootScope.infras = [];
        }
        if($rootScope.cloudinstances === null || $rootScope.cloudinstances === undefined ) {
            $rootScope.cloudinstances = [];
        }
        if($rootScope.apiUrl === null || $rootScope.apiUrl === undefined ) {
            $rootScope.apiUrl = "http://localhost:8080";
        }
        if($rootScope.basic_auth === null || $rootScope.basic_auth === undefined ) {
            $rootScope.basic_auth = "dXNlckBzZXEuY29tOnRlc3QxMjM=";
        }
        $rootScope.blueprints = ["hadoop", "phoenix", "custom"];

        $scope.reloadCtrl = function(){
            console.log('reloading...');
            $route.reload();
        }

        $scope.signIn = function() {
            if(username.value === "user@seq.com" && password.value === "test123") {
                localStorage.signedIn = true;
                $rootScope.signedIn = true;
                $scope.doQuerys();
            }
        }

        $scope.signOut = function() {
            $rootScope.signedIn = false;
            $scope.signUpOk = localStorage.signUpOk;
            $scope.token = "";
            localStorage.signedIn = false;
            localStorage.removeItem('token');

        }

        $scope.deleteCloudInstance = function(id) {
            $rootScope.cloudinstances.splice( id, 1 );
        }

        $scope.deleteInfra = function(id) {
            $rootScope.infras.splice( id, 1 );
        }

        $scope.deleteCloudService = function(id) {
            $rootScope.providers.splice( id, 1 );
        }

        $scope.getCredentials = function() {
            $http({
                method: 'GET',
                dataType: 'json',
                withCredentials: true,
                url:  $rootScope.apiUrl + "/credential",
                headers: {
                    'Authorization': 'Basic ' + $rootScope.basic_auth,
                    'Content-Type': 'application/json'
                }
            }).success(function (data, status, headers, config) {
                console.log(data);
                $rootScope.providers = data;
            }).error(function (data, status, headers, config) {
                console.log("unsuccess");
            });
        }

        $scope.getInfras = function() {
            $http({
                method: 'GET',
                dataType: 'json',
                withCredentials: true,
                url:  $rootScope.apiUrl + "/infra",
                headers: {
                    'Authorization': 'Basic ' + $rootScope.basic_auth,
                    'Content-Type': 'application/json'
                }
            }).success(function (data, status, headers, config) {
                $rootScope.infras = data;
            }).error(function (data, status, headers, config) {
                console.log("unsuccess");
            });
        }

        $scope.getCloudInstances = function() {
            $http({
                method: 'GET',
                dataType: 'json',
                withCredentials: true,
                url:  $rootScope.apiUrl + "/cloud",
                headers: {
                    'Authorization': 'Basic ' + $rootScope.basic_auth,
                    'Content-Type': 'application/json'
                }
            }).success(function (data, status, headers, config) {
                $rootScope.cloudinstances = data;
            }).error(function (data, status, headers, config) {
                console.log("unsuccess");
            });
        }



        $scope.doQuerys = function() {
            $scope.getCredentials();
            $scope.getInfras();
            $scope.getCloudInstances();
        }

        if (typeof (Storage) !== "undefined") {
            if (localStorage.signedIn === 'true') {
                $rootScope.signedIn = true;
                $scope.doQuerys();
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

        $scope.createAwsCluster = function() {
            console.log("aws cluster creation started...");
            $scope.isSuccessCreation = true;
            $scope.isFailedCreation = false;
            if ($scope.isSuccessCreation === true ) {
                wait();
                var infraObject = {
                    location: cllocation.value,
                    name:  clname.value,
                    keyname:  keyname.value,
                    cloudPlatform: 'aws'
                };
                $rootScope.infras.push(infraObject);
                $location.path("/");
            }
        }

    }
]);

provisioningControllers.controller('CloudInstanceController', ['$scope', '$http', 'Templates', '$location', '$rootScope',
    function ($scope, $http, Templates, $location, $rootScope) {
        $scope.form = undefined;
        $scope.apiUrl = "http://localhost:8080";
        $http.defaults.headers.common['x-auth-token']= $scope.token;
        $http.defaults.headers.common['Content-Type']= 'application/json';

        $scope.reloadCtrl = function(){
            console.log('reloading...');
            $route.reload();
        }

        $scope.createCloudInstance = function() {
            var cloudObject = {
                clusterSize: clusterSize.value,
                infraId: "1"
            };
            $rootScope.cloudinstances.push(cloudObject);

            $location.path("/");
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

        $scope.createAzureCluster = function() {
            console.log("azure cluster creation started...");
            $scope.isSuccessCreation = true;
            $scope.isFailedCreation = false;
            if ($scope.isSuccessCreation === true ) {
                wait();
                var infraObject = {
                    location: cllocation.value,
                    name: clname.value,
                    description: description.value,
                    subnetAddressPrefix: subnetAddressPrefix.value,
                    deploymentSlot: deploymentSlot.value,
                    disableSshPasswordAuthentication: disableSshPasswordAuthentication.value,
                    vmType: vmType.value,
                    imageName: imageName.value,
                    userName: userName.value,
                    password: password.value,
                    sshString: sshString.value,
                    cloudPlatform: 'azure'
                };
                $rootScope.infras.push(infraObject);

                $location.path("/");
            }
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

        $scope.createAwsProvider = function() {
            console.log("create aws");
            $http({
                method: 'POST',
                dataType: 'json',
                withCredentials: true,
                url:  $rootScope.apiUrl + "/credential",
                headers: {
                    'Authorization': 'Basic ' + $rootScope.basic_auth
                },
                data: {
                    cloudPlatform: "AWS",
                    parameters: {
                        roleArn: roleArn.value
                    }
                }
            }).success(function (data, status, headers, config) {
                console.log("success");
                $scope.isSuccessCreation = true;
                $scope.isFailedCreation = false;
            }).error(function (data, status, headers, config) {
                console.log("unsuccess");
                $scope.isSuccessCreation = false;
                $scope.isFailedCreation = true;
            });
            if ($scope.isSuccessCreation === true ) {
                wait();
                $location.path("/");
            }
        }

        $scope.createAzureProvider = function() {
            $http({
                method: 'POST',
                dataType: 'json',
                withCredentials: true,
                url:  $rootScope.apiUrl + "/credential",
                headers: {
                    'Authorization': 'Basic ' + $rootScope.basic_auth,
                },
                data: {
                    cloudPlatform: "AZURE",
                    parameters: {
                        subscriptionId: subscriptionId.value,
                        jksPassword: jksPassword.value
                    }
                }
            }).success(function (data, status, headers, config) {
                console.log("success");
                $scope.isSuccessCreation = true;
                $scope.isFailedCreation = false;
            }).error(function (data, status, headers, config) {
                console.log("unsuccess");
                $scope.isSuccessCreation = false;
                $scope.isFailedCreation = true;
            });
            if ($scope.isSuccessCreation === true ) {
                wait();
                $location.path("/");
            }
        }
        $scope.createProvider();
    }
]);
