'use strict';

var log = log4javascript.getLogger("credentialController-logger");

angular.module('uluwatuControllers').controller('credentialController', ['$scope', '$rootScope', 'UserCredential', 'GlobalCredential', 'GlobalCredentialCertificate',
    function ($scope, $rootScope, UserCredential, GlobalCredential, GlobalCredentialCertificate) {
        $rootScope.credentials = UserCredential.query();
        $scope.credentialAws = {};
        $scope.credentialAzure = {};
        $scope.credentialGcc = {};
        $scope.awsCredentialInCreate = false;
        $scope.gccCredentialInCreate = false;
        $scope.azureCredential = false;
        $scope.awsCredential = true;
        $scope.gccCredential = false;

        $scope.createAwsCredentialRequest = function() {
            $scope.azureCredential = false;
            $scope.awsCredential = true;
            $scope.gccCredential = false;
        }

        $scope.createAzureCredentialRequest = function() {
            $scope.azureCredential = true;
            $scope.awsCredential = false;
            $scope.gccCredential = false;
        }

        $scope.createGccCredentialRequest = function() {
            $scope.azureCredential = false;
            $scope.awsCredential = false;
            $scope.gccCredential = true;
        }

        $scope.createAwsCredential = function() {
            $scope.credentialAws.cloudPlatform = "AWS";
            $rootScope.awsCredentialInCreate = true;
            // $scope.awsCredentialInCreate = true;
            UserCredential.save($scope.credentialAws, function(result) {
                $scope.credentialAws.id = result.id;
                $rootScope.credentials.push($scope.credentialAws);
                $scope.credentialAws = {};
                $scope.modifyStatusMessage($rootScope.error_msg.aws_credential_success1 + result.id + $rootScope.error_msg.aws_credential_success2);
                $scope.modifyStatusClass("has-success");
                $scope.awsCredentialInCreate = false;
                $scope.awsCredentialForm.$setPristine();
                // $scope.awsCredentialInCreate = false;
            }, function (error) {
                $scope.modifyStatusMessage($rootScope.error_msg.aws_credential_failed + error.data.message);
                $scope.modifyStatusClass("has-error");
                $scope.awsCredentialInCreate = false;
                // $scope.isFailedCreation = true;
                // $scope.awsCredentialInCreate = false;
            });
        }

        $scope.createAzureCredential = function() {
            $scope.credentialAzure.cloudPlatform = "AZURE";

            UserCredential.save($scope.credentialAzure, function(result){
                $scope.credentialAzure.id = result.id;
                $rootScope.credentials.push($scope.credentialAzure);
                $scope.credentialAzure = {};
                $scope.modifyStatusMessage($rootScope.error_msg.azure_credential_success1 + result.id + $rootScope.error_msg.azure_credential_success2);
                $scope.modifyStatusClass("has-success");
                $scope.getAzureCertification(result.id);
                $scope.azureCredentialForm.$setPristine();
            }, function (error) {
                $scope.modifyStatusMessage($rootScope.error_msg.azure_credential_failed + error.data.message);
                $scope.modifyStatusClass("has-error");
            });
        }

        $scope.createGccCredential = function() {
            $scope.credentialGcc.cloudPlatform = "GCC";
            $rootScope.gccCredentialInCreate = true;

            UserCredential.save($scope.credentialGcc, function(result){
                $scope.credentialGcc.id = result.id;
                $rootScope.credentials.push($scope.credentialGcc);
                $scope.credentialGcc = {};
                $scope.modifyStatusMessage($rootScope.error_msg.azure_credential_success1 + result.id + $rootScope.error_msg.azure_credential_success2);
                $scope.modifyStatusClass("has-success");
                $scope.gccCredentialInCreate = false;
                $scope.gccCredentialForm.$setPristine();
            }, function (error) {
                $scope.modifyStatusMessage($rootScope.error_msg.azure_credential_failed + error.data.message);
                $scope.modifyStatusClass("has-error");
                $scope.gccCredentialInCreate = false;
            });
        }

        $scope.getAzureCertification = function(credentialId) {
            GlobalCredentialCertificate.get({id:credentialId}, function(result) {
                var blob = new Blob([result.cert], { type: 'text/plain' });
                saveAs(blob, 'azure.cer');
            }, function(error){
                $scope.modifyStatusMessage("Azure certificate request failed: " + error.data.message);
                $scope.modifyStatusClass("has-error");
            });
        }

        $scope.deleteCredential = function(credential) {
            GlobalCredential.delete({ id: credential.id }, function(success){
                $rootScope.credentials.splice($rootScope.credentials.indexOf(credential), 1 );
                $scope.modifyStatusMessage($rootScope.error_msg.credential_delete_success1 + credential.id + $rootScope.error_msg.credential_delete_success2);
                $scope.modifyStatusClass("has-success");
            }, function(error){
                $scope.modifyStatusMessage(error.data.message);
                $scope.modifyStatusClass("has-error");
            });

        }
    }]
);
