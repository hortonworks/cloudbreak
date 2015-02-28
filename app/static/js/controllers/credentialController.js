'use strict';

var log = log4javascript.getLogger("credentialController-logger");

angular.module('uluwatuControllers').controller('credentialController', ['$scope', '$rootScope', '$base64', 'UserCredential', 'AccountCredential', 'GlobalCredential', 'GlobalCredentialCertificate',
    function ($scope, $rootScope, $base64, UserCredential, AccountCredential, GlobalCredential, GlobalCredentialCertificate) {
        $rootScope.credentials = AccountCredential.query();
        $scope.credentialInCreation = false;
        $scope.credentialAws = {};
        $scope.credentialAzure = {};
        $scope.credentialGcc = {};
        $scope.credentialOpenstack = {};
        $scope.azureCredential = false;
        $scope.awsCredential = true;
        $scope.gccCredential = false;
        $scope.oepnstackCredential = false;
        $scope.awsCredentialForm = {};
        $scope.gccCredentialForm = {};
        $scope.azureCredentialForm = {};
        $scope.openstackCredentialForm = {};
        $scope.gcc = {};
        $scope.gcc.p12 = "";

        $scope.createAwsCredentialRequest = function() {
            $scope.azureCredential = false;
            $scope.awsCredential = true;
            $scope.gccCredential = false;
            $scope.openstackCredential = false;
        }

        $scope.createAzureCredentialRequest = function() {
            $scope.azureCredential = true;
            $scope.awsCredential = false;
            $scope.gccCredential = false;
            $scope.openstackCredential = false;
        }

        $scope.createGccCredentialRequest = function() {
            $scope.azureCredential = false;
            $scope.awsCredential = false;
            $scope.gccCredential = true;
            $scope.openstackCredential = false;
        }

        $scope.createOpenstackCredentialRequest = function() {
          $scope.azureCredential = false;
          $scope.awsCredential = false;
          $scope.gccCredential = false;
          $scope.openstackCredential = true;
        }

        $scope.createAwsCredential = function() {
            $scope.credentialAws.cloudPlatform = "AWS";
            $scope.credentialInCreation = true;

            if ($scope.credentialAws.public){
                AccountCredential.save($scope.credentialAws, function(result) {
                    handleAwsCredentialSuccess(result);
                }, function (error) {
                    $scope.showError(error, $rootScope.error_msg.aws_credential_failed);
                    $scope.credentialInCreation = false;
                });
            } else {
                UserCredential.save($scope.credentialAws, function(result) {
                    handleAwsCredentialSuccess(result);
                }, function (error) {
                    $scope.showError(error, $rootScope.error_msg.aws_credential_failed);
                    $scope.credentialInCreation = false;
                });
            }

            function handleAwsCredentialSuccess(result) {
                $scope.credentialAws.id = result.id;
                $rootScope.credentials.push($scope.credentialAws);
                $scope.credentialAws = {};
                $scope.showSuccess($rootScope.error_msg.aws_credential_success1 + result.id + $rootScope.error_msg.aws_credential_success2);
                $scope.awsCredentialForm.$setPristine();
                collapseCreateCredentialFormPanel();
                $scope.credentialInCreation = false;
            }
        }

        $scope.createOpenstackCredential = function() {
          $scope.credentialOpenstack.cloudPlatform = "OPENSTACK";
          $scope.credentialInCreation = true;

          if ($scope.credentialOpenstack.public){
            AccountCredential.save($scope.credentialOpenstack, function(result) {
              handleOpenstackCredentialSuccess(result)
            }, function (error) {
              $scope.showError(error, $rootScope.error_msg.openstack_credential_failed);
              $scope.credentialInCreation = false;
            });
          } else {
            UserCredential.save($scope.credentialOpenstack, function(result) {
              handleOpenstackCredentialSuccess(result)
            }, function (error) {
              $scope.showError(error, $rootScope.error_msg.openstack_credential_failed);
              $scope.credentialInCreation = false;
            });
          }

          function handleOpenstackCredentialSuccess(result) {
            $scope.credentialOpenstack.id = result.id;
            $rootScope.credentials.push($scope.credentialOpenstack);
            $scope.credentialOpenstack = {};
            $scope.showSuccess($rootScope.error_msg.openstack_credential_success1 + result.id + $rootScope.error_msg.openstack_credential_success2);
            $scope.credentialOpenstackForm.$setPristine();
            collapseCreateCredentialFormPanel();
            $scope.credentialInCreation = false;
          }
        }

        $scope.createAzureCredential = function() {
            $scope.credentialAzure.cloudPlatform = "AZURE";
            $scope.credentialAzure.publicKey = $base64.encode($scope.credentialAzure.publicKey)

            if ($scope.credentialAzure.public){
                AccountCredential.save($scope.credentialAzure, function(result){
                    handleAzureCredentialSuccess(result)
                }, function (error) {
                    $scope.showError(error, $rootScope.error_msg.azure_credential_failed);
                    $scope.credentialInCreation = false;
                    $scope.credentialAzure.publicKey = $base64.decode($scope.credentialAzure.publicKey)
                });
            } else {
                UserCredential.save($scope.credentialAzure, function(result){
                    handleAzureCredentialSuccess(result)
                }, function (error) {
                    $scope.showError(error, $rootScope.error_msg.azure_credential_failed);
                    $scope.credentialInCreation = false;
                    $scope.credentialAzure.publicKey = $base64.decode($scope.credentialAzure.publicKey)
                });
            }

            function handleAzureCredentialSuccess(result) {
                $scope.credentialAzure.id = result.id;
                $rootScope.credentials.push($scope.credentialAzure);
                $scope.credentialAzure = {};
                $scope.showSuccess($rootScope.error_msg.azure_credential_success1 + result.id + $rootScope.error_msg.azure_credential_success2);
                window.location.href = "credentials/certificate/" + result.id
                $scope.azureCredentialForm.$setPristine();
                collapseCreateCredentialFormPanel();
            }

        }

        $scope.createGccCredential = function() {
            $scope.credentialGcc.cloudPlatform = "GCC";
            $scope.credentialInCreation = true

            var p12File = $scope.gcc.p12
            var reader = new FileReader();

            reader.onloadend = function(evt) {
                if (evt.target.readyState == FileReader.DONE) {
                  $scope.credentialGcc.parameters.serviceAccountPrivateKey = $base64.encode(evt.target.result);

                  if ($scope.credentialGcc.public) {
                    AccountCredential.save($scope.credentialGcc, function(result){
                      handleGccCredentialSuccess(result)
                    }, function (error) {
                      $scope.showError(error, $rootScope.error_msg.gcc_credential_failed);
                      $scope.credentialInCreation = false;
                    });
                  } else {
                    UserCredential.save($scope.credentialGcc, function(result){
                      handleGccCredentialSuccess(result)
                    }, function (error) {
                      $scope.showError(error, $rootScope.error_msg.gcc_credential_failed);
                      $scope.credentialInCreation = false;
                    });
                  }
                }
            };

            function handleGccCredentialSuccess(result) {
                 $scope.credentialGcc.id = result.id;
                 $rootScope.credentials.push($scope.credentialGcc);
                 $scope.credentialGcc = {};
                 $scope.showSuccess($rootScope.error_msg.gcc_credential_success1 + result.id + $rootScope.error_msg.gcc_credential_success2);
                 $scope.credentialInCreation = false;
                 $scope.gccCredentialForm.$setPristine();
                 collapseCreateCredentialFormPanel();
            }

            var blob = p12File.slice(0, p12File.size);
            reader.readAsBinaryString(blob);

        }

        $scope.deleteCredential = function(credential) {
            GlobalCredential.delete({ id: credential.id }, function(success){
                $rootScope.credentials.splice($rootScope.credentials.indexOf(credential), 1 );
                $scope.showSuccess($rootScope.error_msg.credential_delete_success1 + credential.id + $rootScope.error_msg.credential_delete_success2);
            }, function(error){
                $scope.showError(error);
            });

        }

        function collapseCreateCredentialFormPanel() {
          angular.element(document.querySelector('#panel-create-credentials-collapse-btn')).click();
        }
    }]
);
