'use strict';

var log = log4javascript.getLogger("credentialController-logger");

angular.module('uluwatuControllers').controller('credentialController', ['$scope', '$rootScope', '$base64', 'UserCredential', 'AccountCredential', 'GlobalCredential', 'GlobalCredentialCertificate',
    function ($scope, $rootScope, $base64, UserCredential, AccountCredential, GlobalCredential, GlobalCredentialCertificate) {
        $rootScope.credentials = AccountCredential.query();
        $rootScope.credentialInCreation = false;
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
            $scope.oepnstackCredential = false;
        }

        $scope.createAzureCredentialRequest = function() {
            $scope.azureCredential = true;
            $scope.awsCredential = false;
            $scope.gccCredential = false;
            $scope.oepnstackCredential = false;
        }

        $scope.createGccCredentialRequest = function() {
            $scope.azureCredential = false;
            $scope.awsCredential = false;
            $scope.gccCredential = true;
            $scope.oepnstackCredential = false;
        }

        $scope.createOpenstackCredentialRequest = function() {
          $scope.azureCredential = false;
          $scope.awsCredential = false;
          $scope.gccCredential = false;
          $scope.openstackCredential = true;
        }

        $scope.createAwsCredential = function() {
            $scope.credentialAws.cloudPlatform = "AWS";
            $rootScope.credentialInCreation = true;

            if ($scope.credentialAws.public){
                AccountCredential.save($scope.credentialAws, function(result) {
                    handleAwsCredentialSuccess(result)
                }, function (error) {
                    handleAwsCredentialError(error)
                });
            } else {
                UserCredential.save($scope.credentialAws, function(result) {
                    handleAwsCredentialSuccess(result)
                }, function (error) {
                    handleAwsCredentialError(error)
                });
            }

            function handleAwsCredentialSuccess(result) {
                $scope.credentialAws.id = result.id;
                $rootScope.credentials.push($scope.credentialAws);
                $scope.credentialAws = {};
                $scope.modifyStatusMessage($rootScope.error_msg.aws_credential_success1 + result.id + $rootScope.error_msg.aws_credential_success2);
                $scope.modifyStatusClass("has-success");
                $scope.awsCredentialForm.$setPristine();
                collapseCreateCredentialFormPanel();
                $rootScope.credentialInCreation = false;
            }

            function handleAwsCredentialError(error) {
                $scope.modifyStatusMessage($rootScope.error_msg.aws_credential_failed + error.data.message);
                $scope.modifyStatusClass("has-error");
                $rootScope.credentialInCreation = false;
            }
        }

        $scope.createOpenstackCredential = function() {
          $scope.credentialOpenstack.cloudPlatform = "OPENSTACK";
          $rootScope.credentialInCreation = true;

          if ($scope.credentialOpenstack.public){
            AccountCredential.save($scope.credentialOpenstack, function(result) {
              handleOpenstackCredentialSuccess(result)
            }, function (error) {
              handleOpenstackCredentialError(error)
            });
          } else {
            UserCredential.save($scope.credentialOpenstack, function(result) {
              handleOpenstackCredentialSuccess(result)
            }, function (error) {
              handleOpenstackCredentialError(error)
            });
          }

          function handleOpenstackCredentialSuccess(result) {
            $scope.credentialOpenstack.id = result.id;
            $rootScope.credentials.push($scope.credentialOpenstack);
            $scope.credentialOpenstack = {};
            $scope.modifyStatusMessage($rootScope.error_msg.openstack_credential_success1 + result.id + $rootScope.error_msg.openstack_credential_success2);
            $scope.modifyStatusClass("has-success");
            $scope.credentialOpenstackForm.$setPristine();
            collapseCreateCredentialFormPanel();
            $rootScope.credentialInCreation = false;
          }

          function handleOpenstackCredentialError(error) {
            $scope.modifyStatusMessage($rootScope.error_msg.openstack_credential_failed + error.data.message);
            $scope.modifyStatusClass("has-error");
            $rootScope.credentialInCreation = false;
          }
        }

        $scope.createAzureCredential = function() {
            $scope.credentialAzure.cloudPlatform = "AZURE";
            $scope.credentialAzure.publicKey = $base64.encode($scope.credentialAzure.publicKey)

            if ($scope.credentialAzure.public){
                AccountCredential.save($scope.credentialAzure, function(result){
                    handleAzureCredentialSuccess(result)
                }, function (error) {
                    handleAzureCredentialError(error)
                });
            } else {
                UserCredential.save($scope.credentialAzure, function(result){
                    handleAzureCredentialSuccess(result)
                }, function (error) {
                    handleAzureCredentialError(error)
                });
            }

            function handleAzureCredentialSuccess(result) {
                $scope.credentialAzure.id = result.id;
                $rootScope.credentials.push($scope.credentialAzure);
                $scope.credentialAzure = {};
                $scope.modifyStatusMessage($rootScope.error_msg.azure_credential_success1 + result.id + $rootScope.error_msg.azure_credential_success2);
                $scope.modifyStatusClass("has-success");
                window.location.href = "credentials/certificate/" + result.id
                $scope.azureCredentialForm.$setPristine();
                collapseCreateCredentialFormPanel();
            }

            function handleAzureCredentialError(error) {
                $scope.credentialAzure.publicKey = $base64.decode($scope.credentialAzure.publicKey)
                $scope.modifyStatusMessage($rootScope.error_msg.azure_credential_failed + error.data.message);
                $scope.modifyStatusClass("has-error");
            }
        }

        $scope.createGccCredential = function() {
            $scope.credentialGcc.cloudPlatform = "GCC";
            $rootScope.credentialInCreation = true

            var p12File = $scope.gcc.p12
            var reader = new FileReader();

            reader.onloadend = function(evt) {
                if (evt.target.readyState == FileReader.DONE) {
                  $scope.credentialGcc.parameters.serviceAccountPrivateKey = $base64.encode(evt.target.result);

                  if ($scope.credentialGcc.public) {
                    AccountCredential.save($scope.credentialGcc, function(result){
                      handleGccCredentialSuccess(result)
                    }, function (error) {
                      handleGccCredentialError(error)
                    });
                  } else {
                    UserCredential.save($scope.credentialGcc, function(result){
                      handleGccCredentialSuccess(result)
                    }, function (error) {
                      handleGccCredentialError(error)
                    });
                  }
                }
            };

            function handleGccCredentialSuccess(result) {
                 $scope.credentialGcc.id = result.id;
                 $rootScope.credentials.push($scope.credentialGcc);
                 $scope.credentialGcc = {};
                 $scope.modifyStatusMessage($rootScope.error_msg.gcc_credential_success1 + result.id + $rootScope.error_msg.gcc_credential_success2);
                 $scope.modifyStatusClass("has-success");
                 $rootScope.credentialInCreation = false;
                 $scope.gccCredentialForm.$setPristine();
                 collapseCreateCredentialFormPanel();
            }

            function handleGccCredentialError(error) {
                 $scope.modifyStatusMessage($rootScope.error_msg.gcc_credential_failed + error.data.message);
                 $scope.modifyStatusClass("has-error");
                 $rootScope.credentialInCreation = false;
            }

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
