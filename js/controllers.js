'use strict';

var $jq = jQuery.noConflict();

/* Controllers */
var Base64={_keyStr:"ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/=",encode:function(e){var t="";var n,r,i,s,o,u,a;var f=0;e=Base64._utf8_encode(e);while(f<e.length){n=e.charCodeAt(f++);r=e.charCodeAt(f++);i=e.charCodeAt(f++);s=n>>2;o=(n&3)<<4|r>>4;u=(r&15)<<2|i>>6;a=i&63;if(isNaN(r)){u=a=64}else if(isNaN(i)){a=64}t=t+this._keyStr.charAt(s)+this._keyStr.charAt(o)+this._keyStr.charAt(u)+this._keyStr.charAt(a)}return t},decode:function(e){var t="";var n,r,i;var s,o,u,a;var f=0;e=e.replace(/[^A-Za-z0-9\+\/\=]/g,"");while(f<e.length){s=this._keyStr.indexOf(e.charAt(f++));o=this._keyStr.indexOf(e.charAt(f++));u=this._keyStr.indexOf(e.charAt(f++));a=this._keyStr.indexOf(e.charAt(f++));n=s<<2|o>>4;r=(o&15)<<4|u>>2;i=(u&3)<<6|a;t=t+String.fromCharCode(n);if(u!=64){t=t+String.fromCharCode(r)}if(a!=64){t=t+String.fromCharCode(i)}}t=Base64._utf8_decode(t);return t},_utf8_encode:function(e){e=e.replace(/\r\n/g,"\n");var t="";for(var n=0;n<e.length;n++){var r=e.charCodeAt(n);if(r<128){t+=String.fromCharCode(r)}else if(r>127&&r<2048){t+=String.fromCharCode(r>>6|192);t+=String.fromCharCode(r&63|128)}else{t+=String.fromCharCode(r>>12|224);t+=String.fromCharCode(r>>6&63|128);t+=String.fromCharCode(r&63|128)}}return t},_utf8_decode:function(e){var t="";var n=0;var r=c1=c2=0;while(n<e.length){r=e.charCodeAt(n);if(r<128){t+=String.fromCharCode(r);n++}else if(r>191&&r<224){c2=e.charCodeAt(n+1);t+=String.fromCharCode((r&31)<<6|c2&63);n+=2}else{c2=e.charCodeAt(n+1);c3=e.charCodeAt(n+2);t+=String.fromCharCode((r&15)<<12|(c2&63)<<6|c3&63);n+=3}}return t}}
var log = log4javascript.getLogger("cloudbreak-logger");
var popUpAppender = new log4javascript.PopUpAppender();
var layout = new log4javascript.PatternLayout("[%-5p] %m");
popUpAppender.setLayout(layout);
var cloudbreakControllers = angular.module('cloudbreakControllers', []);

cloudbreakControllers.controller('cloudbreakController', ['$scope', '$http', 'Templates', '$location', '$rootScope', '$q', '$window',
    function ($scope, $http, Templates, $location, $rootScope, $q, $window) {
        $scope.form = undefined;
        $http.defaults.useXDomain = true;
        delete $http.defaults.headers.common['X-Requested-With'];
        $http.defaults.headers.common['Content-Type']= 'application/json';

        var confirmSignUpToken = ($location.search()['confirmSignUpToken']);
        if (confirmSignUpToken != null) {
            $http.get('connection.properties').then(function (response) {
                            $rootScope.apiUrl = response.data.backend_url;
                            $http({
                              url: $rootScope.apiUrl + '/users/confirm/' + confirmSignUpToken,
                              method: "GET",
                              headers: {'Content-Type': 'application/x-www-form-urlencoded'}
                              }).success(function(responseData){
                                 if (responseData && responseData.length != 0) {
                                    $jq('.carousel').carousel(1);
                                 } else {
                                    alert("Sign up confirmation is failed.")
                                 }
                              }).error(function (data, status, headers, config) {
                                  alert("Sign up confirmation is failed.")
                              });
                        });
        }

        var resetToken = ($location.search()['resetToken']);
        if (resetToken != null) {
           $rootScope.resetToken = resetToken;
           $jq('.carousel').carousel(5);
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
        if($scope.statusMessage === null || $scope.statusMessage === undefined) {
            $scope.statusMessage = "";
        }

        if($rootScope.activeCredential === null || $rootScope.activeCredential === undefined ) {
            $rootScope.activeCredential = {
                name: "select a credential",
                id: -1
            };
        }
        if($rootScope.activeStack === null || $rootScope.activeStack === undefined ) {
            $rootScope.activeStack = {
                credential: null,
                data: null,
                template: null,
                blueprint: null
            };
        }

        $scope.azureTemplate = false;
        $scope.awsTemplate = true;
        $scope.azureCredential = false;
        $scope.awsCredential = true;

        $scope.reloadCtrl = function(){
            log.info('reloading...');
            $route.reload();
        }

        $scope.signUp = function() {
            $http({
                method: 'POST',
                dataType: 'json',
                url:  $rootScope.apiUrl + "/users",
                data: {
                    email: signUpEmailField.value,
                    firstName: signUpFirstNameField.value,
                    lastName: signUpLastNameField.value,
                    password: signUpPasswField.value,
                    company: signUpCompanyField.value
                }
            }).success(function(responseData){
                $jq('.outerCarousel').carousel(3);
            })
            .error(function (data, status, headers, config){
                if (status == 400) {
                    alert("User already exists.")
                } else {
                    alert("Internal server error.")
                }
            });
        }

        $scope.signIn = function() {
                $http({
                    method: 'GET',
                    dataType: 'json',
                    url:  $rootScope.apiUrl + "/login",
                    headers: {
                        'Authorization': 'Basic ' + Base64.encode(emailFieldLogin.value + ":" + passwFieldLogin.value),
                        'Content-Type': 'application/json'
                    }
                }).success(function (data, status, headers, config) {
                    localStorage.signedIn = true;
                    $rootScope.signedIn = true;
                    localStorage.activeUser = emailFieldLogin.value;
                    $rootScope.activeUser = localStorage.activeUser;
                    localStorage.password64 = Base64.encode(emailFieldLogin.value + ":" + passwFieldLogin.value);
                    $rootScope.password64 = localStorage.password64;
                    $scope.doQuerys();
                }).error(function (data, status, headers, config) {
                    if (status == 401) {
                        alert("Invalid username/password combination")
                    } else {
                        alert("Internal server error");
                    }
                });
        }

        $scope.signOut = function() {
            $rootScope.signedIn = false;
            localStorage.signedIn = false;
            localStorage.removeItem('password64');
            localStorage.removeItem('activeUser');
        }

        $scope.forgotPassword = function() {
            $http.get('connection.properties').then(function (response) {
                $rootScope.apiUrl = response.data.backend_url;
                    $http({
                      method: 'POST',
                      url: $rootScope.apiUrl + '/password/reset',
                      data: { email: emailFieldLogin.value } ,
                      headers: {'Content-Type': 'application/json'}
                     }).success(function (responseData) {
                        if (responseData && responseData.length != 0) {
                            $jq('.outerCarousel').carousel(4);
                        } else {
                            alert("Internal server error");
                        }
                     }).error(function (data, status, headers, config){
                        if (status == 404){
                            alert("There is not susch username");
                        } else {
                            alert("Internal server error");
                        }
                     });
                });
        }

        $scope.resetPassword = function() {
            $http.get('connection.properties').then(function (response) {
                 $rootScope.apiUrl = response.data.backend_url;
                 $http({
                      method: 'POST',
                      url: $rootScope.apiUrl + '/password/reset/'+ $rootScope.resetToken,
                      data: { password: resetPasswField.value },
                      headers: {'Content-Type': 'application/json'}
                 }).success(function(responseData){
                    if (responseData && responseData.length != 0 && responseData == $rootScope.resetToken){
                        $jq('.outerCarousel').carousel(1);
                        $rootScope.resetToken = null;
                    } else {
                        alert("Password change failed.")
                    }
                 }).error(function (data, status, headers, config){
                    if (status == 404){
                        alert("There's no user for token" + $rootScope.resetToken)
                    } else {
                        alert("Internal server error.");
                    }
                 });
            });
        }

        $scope.getLedStyle = function(status) {
            switch(status) {
                case "REQUESTED":
                    return "state2-run-blink";
                case "CREATE_IN_PROGRESS":
                    return "state2-run-blink";
                case "CREATE_COMPLETED":
                    return "state5-run";
                case "CREATE_FAILED":
                    return "state3-stop";
                case "DELETE_IN_PROGRESS":
                    return "state0-stop-blink";
                case "DELETE_COMPLETED":
                    return "state3-stop";
                default: return "state3-stop";
            }
        }

        $scope.getTitleStatus = function(status) {
            switch(status) {
                case "REQUESTED":
                    return "requested";
                case "CREATE_IN_PROGRESS":
                    return "creation in progress";
                case "CREATE_COMPLETED":
                    return "creation completed";
                case "CREATE_FAILED":
                    return "creation failed";
                case "DELETE_IN_PROGRESS":
                    return "deletion in progress";
                case "DELETE_COMPLETED":
                    return "deletion completed";
                default: return "stopped";
            }
        }

        $scope.getButtonStyle = function(status) {
            switch(status) {
                case "REQUESTED":
                    return "fa-pause";
                case "CREATE_IN_PROGRESS":
                    return "fa-pause";
                case "CREATE_COMPLETED":
                    return "fa-stop";
                case "CREATE_FAILED":
                    return "fa-play";
                case "DELETE_IN_PROGRESS":
                    return "fa-pause";
                case "DELETE_COMPLETED":
                    return "fa-stop";
                default: return "fa-stop";
            }
        }

        $scope.getCredentials = function() {
            $http({
                method: 'GET',
                dataType: 'json',
                withCredentials: true,
                url:  $rootScope.apiUrl + "/credentials",
                headers: {
                    'Authorization': 'Basic ' + $rootScope.basic_auth,
                    'Content-Type': 'application/json'
                }
            }).success(function (data, status, headers, config) {
                $scope.credentials = data;
            }).error(function (data, status, headers, config) {
                $scope.statusMessage = data;
            });
        }

        $scope.deleteCredential = function(id) {
            $http({
                method: 'DELETE',
                dataType: 'json',
                withCredentials: true,
                url:  $rootScope.apiUrl + "/credentials/" +id,
                headers: {
                    'Authorization': 'Basic ' + $rootScope.basic_auth,
                    'Content-Type': 'application/json'
                }
            }).success(function (data, status, headers, config) {
                $scope.getCredentials();
                $scope.statusMessage = "Credential '" + id + "' deleted successfully";
            }).error(function (data, status, headers, config) {
                $scope.statusMessage = data;
                $scope.getCredentials();
            });
        }

        $scope.getCredential = function(id) {
            var deferred = $q.defer();
            $http({
                method: 'GET',
                dataType: 'json',
                withCredentials: true,
                url:  $rootScope.apiUrl + "/credentials/" + id,
                headers: {
                    'Authorization': 'Basic ' + $rootScope.basic_auth,
                    'Content-Type': 'application/json'
                }
            }).success(function (data, status, headers, config) {
                deferred.resolve(data);
            }).error(function (data, status, headers, config) {
                $scope.statusMessage = data;
                return deferred.reject();
            });
            return deferred.promise;
        }

        $scope.getStacks = function() {
            $http({
                method: 'GET',
                dataType: 'json',
                withCredentials: true,
                url:  $rootScope.apiUrl + "/stacks",
                headers: {
                    'Authorization': 'Basic ' + $rootScope.basic_auth,
                    'Content-Type': 'application/json'
                }
            }).success(function (data, status, headers, config) {
                $scope.stacks = data;
            }).error(function (data, status, headers, config) {
                log.info("getStack was unsucces: " + data.message);
            });
        }

        $scope.deleteStack = function(id) {
            $http({
                method: 'DELETE',
                dataType: 'json',
                withCredentials: true,
                url:  $rootScope.apiUrl + "/stacks/" +id,
                headers: {
                    'Authorization': 'Basic ' + $rootScope.basic_auth,
                    'Content-Type': 'application/json'
                }
            }).success(function (data, status, headers, config) {
                $scope.getStacks();
                $scope.statusMessage = "Cluster " + id + " terminated successfully";
            }).error(function (data, status, headers, config) {
                $scope.getStacks();
            });
        }

        $scope.getTemplates = function() {
            $http({
                method: 'GET',
                dataType: 'json',
                withCredentials: true,
                url:  $rootScope.apiUrl + "/templates",
                headers: {
                    'Authorization': 'Basic ' + $rootScope.basic_auth,
                    'Content-Type': 'application/json'
                }
            }).success(function (data, status, headers, config) {
                $scope.templates = data;
            }).error(function (data, status, headers, config) {
                log.info("getTemplates was unsuccess: " + data.message);
            });
        }

        $scope.getTemplate = function(id) {
            var deferred = $q.defer();
            $http({
                method: 'GET',
                dataType: 'json',
                withCredentials: true,
                url:  $rootScope.apiUrl + "/templates/" + id,
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
            $http({
                method: 'DELETE',
                dataType: 'json',
                withCredentials: true,
                url:  $rootScope.apiUrl + "/templates/" + id,
                headers: {
                    'Authorization': 'Basic ' + $rootScope.basic_auth,
                    'Content-Type': 'application/json'
                }
            }).success(function (data, status, headers, config) {
                $scope.getTemplates();
                $scope.statusMessage = "Template '" + id + "' deleted successfully";
            }).error(function (data, status, headers, config) {
                $scope.getTemplates();
            });
        }

        $scope.getBluePrints = function() {
            $http({
                method: 'GET',
                dataType: 'json',
                withCredentials: true,
                url:  $rootScope.apiUrl + "/blueprints",
                headers: {
                    'Authorization': 'Basic ' + $rootScope.basic_auth,
                    'Content-Type': 'application/json'
                }
            }).success(function (data, status, headers, config) {
                $scope.blueprints = data;
            }).error(function (data, status, headers, config) {
                log.info("getBluePrints was unsucces: " + data.message);
            });
        }

        $scope.getBluePrint = function(id) {
            var deferred = $q.defer();
            $http({
                method: 'GET',
                dataType: 'json',
                withCredentials: true,
                url:  $rootScope.apiUrl + "/blueprints/" +id,
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

        $scope.deleteBluePrint = function(id) {
            $http({
                method: 'DELETE',
                dataType: 'json',
                withCredentials: true,
                url:  $rootScope.apiUrl + "/blueprints/" +id,
                headers: {
                    'Authorization': 'Basic ' + $rootScope.basic_auth,
                    'Content-Type': 'application/json'
                }
            }).success(function (data, status, headers, config) {
                $scope.getBluePrints();
                $scope.statusMessage = "Blueprint '" + id + "' deleted successfully";
            }).error(function (data, status, headers, config) {
                $scope.getBluePrints();
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
                    break;
                }
            }
        }

        $scope.changeActiveStack = function(id) {
            log.info("active stack change");
            for (var i = 0; i < $scope.stacks.length; i++) {
                if ($scope.stacks[i].id == id) {
                    $scope.getCredential($scope.stacks[i].credentialId).then(function(data){
                        $scope.activeStack.credential = data;
                    });
                    $scope.getTemplate($scope.stacks[i].templateId).then(function(data){
                        $scope.activeStack.template = data;
                    });
                    $scope.getBluePrint($scope.stacks[i].cluster.blueprintId).then(function(data){
                        $scope.activeStack.blueprint = data;
                    });
                    $scope.activeStack.data = $scope.stacks[i];
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
                url:  $rootScope.apiUrl + "/stacks",
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
                $scope.getStacks()
                deferred.resolve(data.id);
                cl_clusterSize.value = "";
            }).error(function (data, status, headers, config) {
                $scope.statusMessage = "The creation of stack failed";
                log.info("Creation of full stack was unsucces: " + data.message);
                return deferred.reject();
            });
            return deferred.promise;
        }



        $scope.createStack = function() {
            $scope.statusMessage = "";
            for(var i = 0; i < $scope.blueprints.length; i++) {
              var blueprintId = selectBlueprint.value
              if($scope.blueprints[i].id === blueprintId) {
                if ($scope.blueprints[i].hostGroupCount > cl_clusterSize.value){
                  $scope.statusMessage = "Number of host groups cannot be larger than the node count.";
                  return;
                }
                if ($scope.blueprints[i].hostGroupCount === 1 && cl_clusterSize.value != 1){
                  $scope.statusMessage = "Single host group blueprints can only be used with single node clusters.";
                  return;
                }
              }
            }

            $scope.createFullStack().then(function(data) {
                $http({
                    method: 'POST',
                    dataType: 'json',
                    withCredentials: true,
                    url:  $rootScope.apiUrl + "/stacks/" + data + "/cluster",
                    headers: {
                        'Authorization': 'Basic ' + $rootScope.basic_auth
                    },
                    data: {
                        clusterName: cl_clusterName.value,
                        blueprintId: selectBlueprint.value
                    }
                }).success(function (data, status, headers, config) {
                    $scope.statusMessage = "Cluster '" + cl_clusterName.value + "' was created succesfully.";
                    log.info("Stack creation was succes: " + data.message);
                    $scope.statusMessage = "Cluster created succesfully.";
                    cl_clusterName.value = "";

                    // don't ask just read on
                    $jq('.carousel').carousel(0);
                    // enable toolbar buttons
                    $jq('#toggle-cluster-block-btn').removeClass('disabled');
                    $jq('#sort-clusters-btn').removeClass('disabled');
                    $jq('#create-cluster-btn').removeClass('disabled');
                    $jq("#notification-n-filtering").prop("disabled", false);
                }).error(function (data, status, headers, config) {
                    $scope.statusMessage = "The creation of cluster failed";
                    log.info("Stack creation failed: " + data.message);
                });
            }, function(reason) {
                log.info("Full Stack creation failed...");
            })
        }

        $scope.createBlueprint = function() {
            log.info("blueprint creation started...");
            try {
              JSON.parse(bluePrintText.value)
              $http({
                  method: 'POST',
                  dataType: 'json',
                  withCredentials: true,
                  url:  $rootScope.apiUrl + "/blueprints",
                  headers: {
                      'Authorization': 'Basic ' + $rootScope.basic_auth
                  },
                  data: {
                      url: blueprintUrl.value,
                      ambariBlueprint: JSON.parse(bluePrintText.value),
                      name: bluePrintName.value,
                      description: bluePrintDescription.value
                  }
              }).success(function (data, status, headers, config) {
                  $scope.statusMessage = "Blueprint '"+ data.id + "' was created succesfully.";
                  blueprintUrl.value = "";
                  bluePrintText.value = "";
                  bluePrintName.value = "";
                  bluePrintDescription.value = "";
                  $scope.getBluePrints()
              }).error(function (data, status, headers, config) {
                  $scope.statusMessage = "The creation of blueprint failed: " + data.message;
                  $scope.isFailedCreation = true;
              });
            } catch(err){
              $scope.statusMessage = "Blueprint is not a JSON";
            }
        }

        $scope.createAwsTemplate = function() {
            log.info("aws cluster creation started...");
            $http({
                method: 'POST',
                dataType: 'json',
                withCredentials: true,
                url:  $rootScope.apiUrl + "/templates",
                headers: {
                    'Authorization': 'Basic ' + $rootScope.basic_auth
                },
                data: {
                    cloudPlatform: "AWS",
                    name: aws_tclusterName.value,
                    description: aws_tdescription.value,
                    parameters: {
                        sshLocation: aws_tsshLocation.value,
                        region: aws_tregion.value,
                        instanceType: aws_tinstanceType.value,
                        amiId: getAmi(aws_tregion.value)
                    }
                }
            }).success(function (data, status, headers, config) {
                $scope.statusMessage = "AWS template '" + data.id + "' was created successfully";
                $scope.getTemplates();
                aws_tclusterName.value = "";
                aws_tdescription.value = "";
                aws_tsshLocation.value = "";
            }).error(function (data, status, headers, config) {
                $scope.statusMessage = "Creation of AWS template failed: " + data.message;
            });
        }

        $scope.createAzureTemplate = function() {
            $scope.isSuccessCreation = true;
            $scope.isFailedCreation = false;
            $http({
                method: 'POST',
                dataType: 'json',
                withCredentials: true,
                url:  $rootScope.apiUrl + "/templates",
                headers: {
                    'Authorization': 'Basic ' + $rootScope.basic_auth
                },
                data: {
                    cloudPlatform: "AZURE",
                    name: azure_tclusterName.value,
                    description: azure_tdescription.value,
                    parameters: {
                        location: azure_tlocation.value,
                        vmType: azure_tvmType.value,
                        imageName: "ambari-docker-v1",
                        ports:[]
                    }
                }
            }).success(function (data, status, headers, config) {
                $scope.statusMessage = "Azure template '" + data.id + "' was created successfully";
                $scope.getTemplates();
                azure_tclusterName.value = "";
                azure_tdescription.value = "";
                azure_tclusterName.value = "";
            }).error(function (data, status, headers, config) {
                $scope.statusMessage = "Azure template creation failed: " + data.message;
            });
        }

        $scope.createAwsCredential = function() {
            log.info("create aws credential");
            $http({
                method: 'POST',
                dataType: 'json',
                withCredentials: true,
                url:  $rootScope.apiUrl + "/credentials",
                headers: {
                    'Authorization': 'Basic ' + $rootScope.basic_auth
                },
                data: {
                    cloudPlatform: "AWS",
                    name: awscname.value,
                    description: awscdescription.value,
                    publicKey: aws_sshPublicKey.value,
                    parameters: {
                        roleArn: croleArn.value
                    }
                }
            }).success(function (data, status, headers, config) {
                $scope.statusMessage = "AWS credential '" + data.id + "' was created successfully";
                $scope.getCredentials();
                awscname.value = "";
                croleArn.value = "";
                awscdescription.value = "";
                aws_sshPublicKey.value = "";
            }).error(function (data, status, headers, config) {
                $scope.statusMessage = "AWS template creation failed: " + data.message;
                $scope.isFailedCreation = true;
            });
        }

        $scope.createAzureCredential = function() {
            $http({
                method: 'POST',
                dataType: 'json',
                withCredentials: true,
                url:  $rootScope.apiUrl + "/credentials",
                headers: {
                    'Authorization': 'Basic ' + $rootScope.basic_auth
                },
                data: {
                    cloudPlatform: "AZURE",
                    name: cname.value,
                    description: cdescription.value,
                    publicKey: azure_sshPublicKey.value,
                    parameters: {
                        subscriptionId: csubscriptionId.value,
                        jksPassword: cjksPassword.value
                    }
                }
            }).success(function (data, status, headers, config) {
                $scope.getCredentials();
                $scope.statusMessage = "Azure credential '" + data.id + "' was created successfully";
                cname.value = "";
                cdescription.value = "";
                csubscriptionId.value = "";
                cjksPassword.value = "";
                azure_sshPublicKey.value = "";
            }).error(function (data, status, headers, config) {
                $scope.statusMessage = "Azure credential creation failed: " + data.message;
            });
        }

        $scope.doQuerys = function() {
            $http.get('connection.properties').then(function (response) {
                $rootScope.apiUrl = response.data.backend_url;
                $rootScope.basic_auth = localStorage.password64;
                $scope.getCredentials();
                $scope.getTemplates();
                $scope.getBluePrints();
                $scope.getStacks();
                connect($rootScope.apiUrl);
            });
        }

        $scope.getAzureCertification = function(credentialId){
            $http({
                method: 'GET',
                dataType: 'json',
                withCredentials: true,
                url:  $rootScope.apiUrl + "/credentials/certificate/" + credentialId,
                headers: {
                    'Authorization': 'Basic ' + $rootScope.basic_auth,
                    'Content-Type': 'application/json'
                }
            }).success(function (data, status, headers, config) {
                console.log("Azure certificate request was succes: " + data);
                var blob = new Blob([data], { type: 'text/plain' });
                saveAs(blob, 'azure.cer');
            }).error(function (data, status, headers, config) {
                log.info("Azure certificate request was unsucces: " + data.message);
            });
        }

        var stompClient = null;
        function connect(url) {
            var socket = new SockJS(url + '/notification');
            stompClient = Stomp.over(socket);
            stompClient.connect({}, function(frame) {
                self.username = localStorage.activeUser;
                stompClient.subscribe('/user/topic/stack', function(stackInfo){
                    $scope.getStacks();
                    logStackInfo(JSON.parse(stackInfo.body));
                });
                stompClient.subscribe('/user/topic/cluster',  function(clusterInfo){
                    $scope.getStacks();
                    logClusterInfo(JSON.parse(clusterInfo.body));
                });
                stompClient.subscribe('/user/topic/uptime', function(timeInfo){
                    $scope.updateTimeInfo(timeInfo);
                });
                stompClient.subscribe('/user/topic/blueprint', function(timeInfo){
                    $scope.getBluePrints();
                });
            });
        }

        $scope.updateTimeInfo = function(timeInfo) {
            for(var i = 0; i < $scope.stacks.length; i++) {
                for(var j = 0; j < JSON.parse(timeInfo.body).length; j++) {
                    if($scope.stacks[i].id === JSON.parse(timeInfo.body)[j].stackId) {
                        var calc = JSON.parse(timeInfo.body)[j].uptime;
                        $scope.stacks[i].cluster.hoursUp = parseInt((calc / (1000 * 60 * 60)));
                        $scope.stacks[i].cluster.minutesUp =  parseInt((calc / (1000 * 60)) % 60);
                    }
                }
            }
            $scope.$apply();
        }

        function disconnect() {
            stompClient.disconnect();
            setConnected(false);
            log.info("Disconnected");
        }

        function logStackInfo(body) {
            if(body.status === 'CREATE_COMPLETED') {
                $scope.statusMessage = body.name + ": Nodes started, Ambari server is available. Starting cluster installation...";
            } else if(body.status === 'CREATE_IN_PROGRESS')  {
                $scope.statusMessage = body.name + ": Creating VPC and nodes...";
            }  else if(body.status === 'CREATE_FAILED')  {
                $scope.statusMessage = body.name + ": Failed to create nodes.";
            }  else if(body.status === 'DELETE_IN_PROGRESS')  {
                $scope.statusMessage = body.name + ": Terminating nodes...";
            }  else if(body.status === 'DELETE_COMPLETED')  {
                $scope.statusMessage = body.name + ": Nodes terminated successfully.";
            } else {
                $scope.statusMessage = body.name + ": Something went wrong.";
            }
        }

        function getAmi(regionValue) {
            if(regionValue === 'US_EAST_1') {
                return "ami-d89955b0";
            } else if (regionValue === 'US_WEST_1') {
                return "ami-314a4974";
            } else if (regionValue === 'US_WEST_2') {
                return "ami-17b0c927";
            } else if (regionValue === 'EU_WEST_1') {
                return "ami-7778af00";
            } else if (regionValue === 'AP_SOUTHEAST_1') {
                return "ami-468bd214";
            } else if (regionValue === 'AP_SOUTHEAST_2') {
                return "ami-678bec5d";
            } else if (regionValue === 'AP_NORTHEAST_1') {
                return "ami-0bcd9d0a";
            } else {
                return "ami-1b0ca206";
            }
        }

        function logClusterInfo(body) {
            if(body.status === 'CREATE_COMPLETED') {
                $scope.statusMessage = body.name + ": Hadoop cluster was created successfully.";
            } else if(body.status === 'CREATE_IN_PROGRESS')  {
                $scope.statusMessage = body.name + "Creating Hadoop cluster with Ambari...";
            }  else if(body.status === 'CREATE_FAILED') {
                $scope.statusMessage = body.name + ": Failed to create Hadoop cluster.";
            } else {
                $scope.statusMessage = body.name + ": Something went wrong.";
            }
        }

        if (typeof (Storage) !== "undefined") {
            $http.get('connection.properties').then(function (response) {
                $rootScope.apiUrl = response.data.backend_url;
            });
            if (localStorage.signedIn === 'true' && localStorage.activeUser && localStorage.password64) {
                $rootScope.signedIn = true;
                $rootScope.activeUser = localStorage.activeUser;
                $rootScope.basic_auth = localStorage.password64;
                $scope.doQuerys();
            }
        } else {
            log.info("No localstorage support!");
        }

        $http.get('messages.properties').then(function (messages) {
            $rootScope.error_msg = messages.data
        })
    }
]);
