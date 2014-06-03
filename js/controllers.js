'use strict';

/* Controllers */

var cloudbreakControllers = angular.module('cloudbreakControllers', []);

cloudbreakControllers.controller('cloudbreakController', ['$scope', '$http', 'Templates', '$location', '$rootScope', '$q',
    function ($scope, $http, Templates, $location, $rootScope, $q) {
        $scope.form = undefined;
        $http.defaults.useXDomain = true;
        delete $http.defaults.headers.common['X-Requested-With'];
        $http.defaults.headers.common['Content-Type']= 'application/json';

        if($rootScope.apiUrl === null || $rootScope.apiUrl === undefined ) {
            $rootScope.apiUrl = "http://localhost:8080";
        }
        if($rootScope.basic_auth === null || $rootScope.basic_auth === undefined ) {
            $rootScope.basic_auth = "dXNlckBzZXEuY29tOnRlc3QxMjM=";
        }
        if($scope.credentials === null || $scope.credentials === undefined ) {
            $scope.credentials = [];
        }
        if($scope.templates === null || $scope.templates === undefined ) {
            $scope.templates = [];
        }
        if($scope.blueprints === null || $scope.blueprints === undefined ) {
            $scope.blueprints = [];
        }
        if($scope.stacks === null || $scope.stacks === undefined ) {
            $scope.stacks = [];
        }
        if($rootScope.activeCredential === null || $rootScope.activeCredential === undefined ) {
            $rootScope.activeCredential = {
                name: "a credential",
                id: -1
            };
        }
        if($rootScope.activeStack === null || $rootScope.activeStack === undefined ) {
            $rootScope.activeStack = {
                credential: null,
                data: null,
                template: null
            };
        }

        $scope.reloadCtrl = function(){
            console.log('reloading...');
            $route.reload();
        }

        $scope.signIn = function() {
            if(emailFieldLogin.value === "user@seq.com" && passwFieldLogin.value === "test123") {
                localStorage.signedIn = true;
                $rootScope.signedIn = true;
                $rootScope.activeUser = emailFieldLogin.value;
                $scope.doQuerys();
            }
        }

        $scope.signOut = function() {
            $rootScope.signedIn = false;
            localStorage.signedIn = false;
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
                $scope.credentials = data;
            }).error(function (data, status, headers, config) {
                console.log("unsuccess");
            });
        }

        $scope.deleteCredential = function(id) {
            $http({
                method: 'DELETE',
                dataType: 'json',
                withCredentials: true,
                url:  $rootScope.apiUrl + "/credential/" +id,
                headers: {
                    'Authorization': 'Basic ' + $rootScope.basic_auth,
                    'Content-Type': 'application/json'
                }
            }).success(function (data, status, headers, config) {
                $scope.credentials = data;
            }).error(function (data, status, headers, config) {
                console.log("unsuccess");
            });
        }

        $scope.getCredential = function(id) {
            var deferred = $q.defer();
            $http({
                method: 'GET',
                dataType: 'json',
                withCredentials: true,
                url:  $rootScope.apiUrl + "/credential/" + id,
                headers: {
                    'Authorization': 'Basic ' + $rootScope.basic_auth,
                    'Content-Type': 'application/json'
                }
            }).success(function (data, status, headers, config) {
                deferred.resolve(data);
            }).error(function (data, status, headers, config) {
                return deferred.reject();
            });
            return deferred.promise;
        }

        $scope.getStacks = function() {
            $http({
                method: 'GET',
                dataType: 'json',
                withCredentials: true,
                url:  $rootScope.apiUrl + "/stack",
                headers: {
                    'Authorization': 'Basic ' + $rootScope.basic_auth,
                    'Content-Type': 'application/json'
                }
            }).success(function (data, status, headers, config) {
                $scope.stacks = data;
            }).error(function (data, status, headers, config) {
                console.log("unsuccess");
            });
        }

        $scope.deleteStack = function(id) {
            $http({
                method: 'DELETE',
                dataType: 'json',
                withCredentials: true,
                url:  $rootScope.apiUrl + "/stack" +id,
                headers: {
                    'Authorization': 'Basic ' + $rootScope.basic_auth,
                    'Content-Type': 'application/json'
                }
            }).success(function (data, status, headers, config) {
                $scope.stacks = data;
            }).error(function (data, status, headers, config) {
                console.log("unsuccess");
            });
        }

        $scope.getTemplates = function() {
            $http({
                method: 'GET',
                dataType: 'json',
                withCredentials: true,
                url:  $rootScope.apiUrl + "/template",
                headers: {
                    'Authorization': 'Basic ' + $rootScope.basic_auth,
                    'Content-Type': 'application/json'
                }
            }).success(function (data, status, headers, config) {
                $scope.templates = data;
                console.log($scope.templates);
            }).error(function (data, status, headers, config) {
                console.log("unsuccess");
            });
        }

        $scope.getTemplate = function(id) {
            var deferred = $q.defer();
            $http({
                method: 'GET',
                dataType: 'json',
                withCredentials: true,
                url:  $rootScope.apiUrl + "/template/" + id,
                headers: {
                    'Authorization': 'Basic ' + $rootScope.basic_auth,
                    'Content-Type': 'application/json'
                }
            }).success(function (data, status, headers, config) {
                deferred.resolve(data);
            }).error(function (data, status, headers, config) {
                return deferred.reject();
            });
            return deferred.promise;
        }

        $scope.deleteTemplate = function(id) {
            var deferred = $q.defer();
            $http({
                method: 'DELETE',
                dataType: 'json',
                withCredentials: true,
                url:  $rootScope.apiUrl + "/template/" + id,
                headers: {
                    'Authorization': 'Basic ' + $rootScope.basic_auth,
                    'Content-Type': 'application/json'
                }
            }).success(function (data, status, headers, config) {
                deferred.resolve(data);
            }).error(function (data, status, headers, config) {
                return deferred.reject();
            });
            return deferred.promise;
        }

        $scope.getBluePrints = function() {
            $http({
                method: 'GET',
                dataType: 'json',
                withCredentials: true,
                url:  $rootScope.apiUrl + "/blueprint",
                headers: {
                    'Authorization': 'Basic ' + $rootScope.basic_auth,
                    'Content-Type': 'application/json'
                }
            }).success(function (data, status, headers, config) {
                $scope.blueprints = data;
            }).error(function (data, status, headers, config) {
                console.log("unsuccess");
            });
        }

        $scope.deleteBluePrint = function(id) {
            $http({
                method: 'DELETE',
                dataType: 'json',
                withCredentials: true,
                url:  $rootScope.apiUrl + "/blueprint/" +id,
                headers: {
                    'Authorization': 'Basic ' + $rootScope.basic_auth,
                    'Content-Type': 'application/json'
                }
            }).success(function (data, status, headers, config) {
                $scope.blueprints = data;
            }).error(function (data, status, headers, config) {
                console.log("unsuccess");
            });
        }


        $scope.createAwsTemplateRequest = function() {
            $scope.azureTemplate = false;
            $scope.awsTemplate = true;
        }

        $scope.createAzureTemplateRequest = function() {
            $scope.azureTemplate = true;
            $scope.awsTemplate = false;
        }

        $scope.createAwsCredentialRequest = function() {
            $scope.azureCredential = false;
            $scope.awsCredential = true;
        }

        $scope.createAzureCredentialRequest = function() {
            $scope.azureCredential = true;
            $scope.awsCredential = false;
        }


        $scope.changeActiveCredential = function(id) {
            for (var i = 0; i < $scope.credentials.length; i++) {
                if ($scope.credentials[i].id == id) {
                    $rootScope.activeCredential = $scope.credentials[i];
                    console.log($rootScope.activeCredential);
                    break;
                }
            }
        }

        $scope.changeActiveStack = function(id) {
            console.log("active stack change");
            for (var i = 0; i < $scope.stacks.length; i++) {
                if ($scope.stacks[i].id == id) {
                    $scope.getCredential($scope.stacks[i].credentialId).then(function(data){
                        $scope.activeStack.credential = data;
                    });
                    $scope.getTemplate($scope.stacks[i].templateId).then(function(data){
                        $scope.activeStack.template = data;
                    });
                    $scope.activeStack.data = $scope.stacks[i];
                    console.log($scope.activeStack);
                    break;
                }
            }
        }

        $scope.createFullStack = function() {
            var deferred = $q.defer();
            $http({
                method: 'POST',
                dataType: 'json',
                withCredentials: true,
                url:  $rootScope.apiUrl + "/stack",
                headers: {
                    'Authorization': 'Basic ' + $rootScope.basic_auth
                },
                data: {
                    nodeCount: cl_clusterSize.value,
                    name: cl_clusterName.value,
                    templateId: selectTemplate.value,
                    credentialId: $scope.activeCredential.id
                }
            }).success(function (data, status, headers, config) {
                console.log("ok");
                deferred.resolve(data.id);

            }).error(function (data, status, headers, config) {
                return deferred.reject();
            });
            return deferred.promise;
        }



        $scope.createStack = function() {
            console.log("stack creation started...");
            $scope.createFullStack().then(function(data) {
                $http({
                    method: 'POST',
                    dataType: 'json',
                    withCredentials: true,
                    url:  $rootScope.apiUrl + "/stack/" + data + "/cluster",
                    headers: {
                        'Authorization': 'Basic ' + $rootScope.basic_auth
                    },
                    data: {
                        clusterName: cl_clusterName.value,
                        blueprintId: selectBlueprint.value
                    }
                }).success(function (data, status, headers, config) {
                    console.log("ok");
                }).error(function (data, status, headers, config) {
                    console.log("unsuccess");
                });
            }, function(reason) {
                console.log("Failed");
            })
        }

        $scope.createBlueprint = function() {
            console.log("blueprint creation started...");
            $http({
                method: 'POST',
                dataType: 'json',
                withCredentials: true,
                url:  $rootScope.apiUrl + "/blueprint",
                headers: {
                    'Authorization': 'Basic ' + $rootScope.basic_auth
                },
                data: {
                    url: blueprintUrl.value,
                    ambariBlueprint: bluePrintText.value
                }
            }).success(function (data, status, headers, config) {
                $scope.getBluePrints()
            }).error(function (data, status, headers, config) {
                console.log("unsuccess");
                $scope.isFailedCreation = true;
            });
        }

        $scope.createAwsTemplate = function() {
            console.log("aws cluster creation started...");
            $http({
                method: 'POST',
                dataType: 'json',
                withCredentials: true,
                url:  $rootScope.apiUrl + "/template",
                headers: {
                    'Authorization': 'Basic ' + $rootScope.basic_auth
                },
                data: {
                    cloudPlatform: "AWS",
                    name: aws_tclusterName.value,
                    parameters: {
                        keyName: aws_tkeyName.value,
                        region: aws_tregion.value,
                        instanceType: aws_tinstanceType.value,
                        amiId: aws_tamiId.value
                    }
                }
            }).success(function (data, status, headers, config) {
                $scope.getTemplates();
            }).error(function (data, status, headers, config) {
                console.log("unsuccess");
                $scope.isFailedCreation = true;
            });
        }

        $scope.createAzureTemplate = function() {
            $scope.isSuccessCreation = true;
            $scope.isFailedCreation = false;
            $http({
                method: 'POST',
                dataType: 'json',
                withCredentials: true,
                url:  $rootScope.apiUrl + "/template",
                headers: {
                    'Authorization': 'Basic ' + $rootScope.basic_auth
                },
                data: {
                    cloudPlatform: "AZURE",
                    name: azure_tclusterName.value,
                    parameters: {
                        location: azure_tlocation.value,
                        description: azure_tdescription.value,
                        subnetAddressPrefix: azure_tsubnetAddressPrefix.value,
                        addressPrefix: azure_taddressPrefix.value,
                        deploymentSlot: azure_tdeploymentSlot.value,
                        vmType: azure_tvmType.value,
                        imageName: azure_timageName.value,
                        username: azure_tusername.value,
                        password: azure_tpassword.value,
                        sshString: azure_tsshString.value,
                        ports:[]
                    }
                }
            }).success(function (data, status, headers, config) {
                $scope.getTemplates();
            }).error(function (data, status, headers, config) {
                console.log("unsuccess");
                $scope.isFailedCreation = true;
            });
        }

        $scope.createAwsCredential = function() {
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
                    name: awscname.value,
                    parameters: {
                        amiId: tamiId.value,
                        roleArn: croleArn.value,
                        instanceProfileRoleArn: cinstanceProfileRoleArn.value
                    }
                }
            }).success(function (data, status, headers, config) {
                $scope.getCredentials();
            }).error(function (data, status, headers, config) {
                console.log("unsuccess");
                $scope.isFailedCreation = true;
            });
        }

        $scope.createAzureCredential = function() {
            $http({
                method: 'POST',
                dataType: 'json',
                withCredentials: true,
                url:  $rootScope.apiUrl + "/credential",
                headers: {
                    'Authorization': 'Basic ' + $rootScope.basic_auth
                },
                data: {
                    cloudPlatform: "AZURE",
                    name: cname.value,
                    parameters: {
                        subscriptionId: csubscriptionId.value,
                        jksPassword: cjksPassword.value
                    }
                }
            }).success(function (data, status, headers, config) {
                $scope.getCredentials();
            }).error(function (data, status, headers, config) {
                console.log("unsuccess");
                $scope.isFailedCreation = true;
            });
        }

        $scope.doQuerys = function() {
            $scope.getCredentials();
            $scope.getTemplates();
            $scope.getBluePrints();
            $scope.getStacks();
        }



        if (typeof (Storage) !== "undefined") {
            if (localStorage.signedIn === 'true') {
                $rootScope.signedIn = true;
                $rootScope.activeUser = "user@seq.com";
                $scope.doQuerys();
            }
        } else {
            console.log("No localstorage support!");
        }


    }
]);