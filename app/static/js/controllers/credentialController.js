'use strict';

var log = log4javascript.getLogger("credentialController-logger");

angular.module('uluwatuControllers').controller('credentialController', ['$scope', '$rootScope', '$base64', 'UserCredential', 'GlobalCredential', 'GlobalCredentialCertificate',
    function ($scope, $rootScope, $base64, UserCredential, GlobalCredential, GlobalCredentialCertificate) {
        $rootScope.credentials = UserCredential.query();
        $rootScope.credentialInCreation = false;
        $scope.credentialAws = {};
        $scope.credentialAzure = {};
        $scope.credentialGcc = {};
        $scope.azureCredential = false;
        $scope.awsCredential = true;
        $scope.gccCredential = false;
        $scope.awsCredentialForm = {};
        $scope.gccCredentialForm = {};
        $scope.azureCredentialForm = {};
        $scope.gcc = {};
        $scope.gcc.p12 = "";

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
            $rootScope.credentialInCreation = true;
            UserCredential.save($scope.credentialAws, function(result) {
                $scope.credentialAws.id = result.id;
                $rootScope.credentials.push($scope.credentialAws);
                $scope.credentialAws = {};
                $scope.modifyStatusMessage($rootScope.error_msg.aws_credential_success1 + result.id + $rootScope.error_msg.aws_credential_success2);
                $scope.modifyStatusClass("has-success");
                $scope.awsCredentialForm.$setPristine();
                collapseCreateCredentialFormPanel();
                $rootScope.credentialInCreation = false;
            }, function (error) {
                $scope.modifyStatusMessage($rootScope.error_msg.aws_credential_failed + error.data.message);
                $scope.modifyStatusClass("has-error");
                $rootScope.credentialInCreation = false;
                // $scope.isFailedCreation = true;
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
                collapseCreateCredentialFormPanel();
            }, function (error) {
                $scope.modifyStatusMessage($rootScope.error_msg.azure_credential_failed + error.data.message);
                $scope.modifyStatusClass("has-error");
            });
        }

        $scope.createGccCredential = function() {
            $scope.credentialGcc.cloudPlatform = "GCC";
            $rootScope.credentialInCreation = true

            var p12File = $scope.gcc.p12
            var reader = new FileReader();

            reader.onloadend = function(evt) {
                if (evt.target.readyState == FileReader.DONE) {
                  $scope.credentialGcc.parameters.serviceAccountPrivateKey = $base64.encode(evt.target.result);
                  UserCredential.save($scope.credentialGcc, function(result){
                      $scope.credentialGcc.id = result.id;
                      $rootScope.credentials.push($scope.credentialGcc);
                      $scope.credentialGcc = {};
                      $scope.modifyStatusMessage($rootScope.error_msg.gcc_credential_success1 + result.id + $rootScope.error_msg.gcc_credential_success2);
                      $scope.modifyStatusClass("has-success");
                      $rootScope.credentialInCreation = false;
                      $scope.gccCredentialForm.$setPristine();
                      collapseCreateCredentialFormPanel();
                  }, function (error) {
                      $scope.modifyStatusMessage($rootScope.error_msg.gcc_credential_failed + error.data.message);
                      $scope.modifyStatusClass("has-error");
                      $rootScope.credentialInCreation = false;
                  });
                }
            };

            var blob = p12File.slice(0, p12File.size);
            reader.readAsBinaryString(blob);

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

        function collapseCreateCredentialFormPanel() {
          angular.element(document.querySelector('#panel-create-credentials-collapse-btn')).click();
        }
    }]
);
