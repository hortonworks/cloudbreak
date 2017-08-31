'use strict';

var log = log4javascript.getLogger("credentialController-logger");

angular.module('uluwatuControllers').controller('credentialController', [
    '$scope', '$rootScope', '$filter', '$base64', '$interpolate', 'UserCredential', 'AccountCredential', 'GlobalCredential', 'InteractiveLogin', 'GlobalCredentialCertificate', 'AccountStack', 'UserStack', 'GlobalStack', 'DefaultSsh',
    function($scope, $rootScope, $filter, $base64, $interpolate, UserCredential, AccountCredential, GlobalCredential, InteractiveLogin ,GlobalCredentialCertificate, AccountStack, UserStack, GlobalStack, DefaultSsh) {
        $rootScope.credentials = AccountCredential.query();
        $rootScope.importedStacks = AccountStack.query();

        $scope.credentialAws = {
            parameters: {
                selector: 'role-based'
            },
            publicKey: $scope.defaultSshKey
        };

        $scope.credentialYarn = {};
        $scope.credentialInCreation = false;
        $scope.mesosStack = {};
        $scope.mesosStac = false;
        $scope.awsCredentialForm = {};
        $scope.gcpCredentialForm = {};
        $scope.openstackCredentialForm = {};
        $scope.gcp = {};
        $scope.gcp.p12 = "";
        $scope.showAlert = false;
        $scope.alertMessage = "";
        var firstVisiblePlatform = $scope.firstVisible(["AWS", "AZURE", "BYOS", "GCP", "OPENSTACK"]);
        if (firstVisiblePlatform != -1) {
            $scope[["awsCredential", "azureCredential", "mesosCredential", "gcpCredential", "openstackCredential", "yarnCredential"][firstVisiblePlatform]] = true;
        }

        $scope.createAzureCredentialRequest = function() {
            $scope.awsCredential = false;
            $scope.gcpCredential = false;
            $scope.openstackCredential = false;
            $scope.azureCredential = true;
            $scope.mesosCredential = false;
            $scope.yarnCredential = false;
        }

        $scope.createAwsCredentialRequest = function() {
            $scope.awsCredential = true;
            $scope.gcpCredential = false;
            $scope.openstackCredential = false;
            $scope.azureCredential = false;
            $scope.mesosCredential = false;
            $scope.yarnCredential = false;
        }

        $scope.createGcpCredentialRequest = function() {
            $scope.awsCredential = false;
            $scope.gcpCredential = true;
            $scope.openstackCredential = false;
            $scope.azureCredential = false;
            $scope.mesosCredential = false;
            $scope.yarnCredential = false;
        }

        $scope.createOpenstackCredentialRequest = function() {
            $scope.awsCredential = false;
            $scope.gcpCredential = false;
            $scope.openstackCredential = true;
            $scope.azureCredential = false;
            $scope.mesosCredential = false;
            $scope.yarnCredential = false;
        }

        $scope.importMesosStackRequest = function() {
            $scope.awsCredential = false;
            $scope.gcpCredential = false;
            $scope.openstackCredential = false;
            $scope.azureCredential = false;
            $scope.mesosCredential = true;
            $scope.yarnCredential = false;
        }

        $scope.importYarnCredentialRequest = function() {
            $scope.awsCredential = false;
            $scope.gcpCredential = false;
            $scope.openstackCredential = false;
            $scope.azureCredential = false;
            $scope.mesosCredential = false;
            $scope.yarnCredential = true;
        }

        $scope.refreshCertificateFile = function(credentialId) {
            GlobalCredentialCertificate.update({
                id: credentialId
            }, {}, function(result) {
                window.location.href = "credentials/certificate/" + credentialId;
            });
        }

        $scope.filterByCloudPlatform = function(topology) {
            return (topology.cloudPlatform === 'AWS' && $scope.awsCredential) ||
                (topology.cloudPlatform === 'GCP' && $scope.gcpCredential) ||
                (topology.cloudPlatform === 'AZURE' && $scope.azureCredential) ||
                (topology.cloudPlatform === 'OPENSTACK' && $scope.openstackCredential)
        }

        $scope.createAwsCredential = function() {
            $scope.credentialAws.cloudPlatform = "AWS";
            $scope.credentialInCreation = true;

            if ($scope.credentialAws.public) {
                AccountCredential.save($scope.credentialAws, function(result) {
                    handleAwsCredentialSuccess(result);
                }, function(error) {
                    $scope.showError(error, $rootScope.msg.aws_credential_failed);
                    $scope.credentialInCreation = false;
                    $scope.showErrorMessageAlert();
                });
            } else {
                UserCredential.save($scope.credentialAws, function(result) {
                    handleAwsCredentialSuccess(result);
                }, function(error) {
                    $scope.showError(error, $rootScope.msg.aws_credential_failed);
                    $scope.credentialInCreation = false;
                    $scope.showErrorMessageAlert();
                });
            }

            function handleAwsCredentialSuccess(result) {
                $scope.credentialAws.id = result.id;
                $rootScope.credentials.push($scope.credentialAws);
                $scope.showSuccess($filter("format")($rootScope.msg.aws_credential_success, String(result.name)));
                $scope.awsCredentialForm.$setPristine();
                collapseCreateCredentialFormPanel();
                $scope.credentialInCreation = false;
                $scope.unShowErrorMessageAlert();
                $scope.credentialAws = {
                    parameters: {
                        selector: 'role-based',
                        publicKey: $scope.defaultSshKey
                    }
                };
            }
        }

        $scope.createOpenstackCredential = function() {
            $scope.credentialOpenstack.cloudPlatform = "OPENSTACK";
            $scope.credentialInCreation = true;
            if ($scope.credentialOpenstack.parameters.keystoneVersion === "cb-keystone-v2") {
                $scope.credentialOpenstack.parameters.selector = $scope.credentialOpenstack.parameters.keystoneVersion;
            } else {
                $scope.credentialOpenstack.parameters.selector = $scope.credentialOpenstack.parameters.keystoneAuthScope;
            }

            if ($scope.credentialOpenstack.public) {
                AccountCredential.save($scope.credentialOpenstack, handleOpenstackCredentialSuccess, handleOpenstackCredentailCreationError);
            } else {
                UserCredential.save($scope.credentialOpenstack, handleOpenstackCredentialSuccess, handleOpenstackCredentailCreationError);
            }

            function handleOpenstackCredentialSuccess(result) {
                $scope.credentialOpenstack.id = result.id;
                $rootScope.credentials.push($scope.credentialOpenstack);
                $scope.credentialOpenstack = {
                    parameters: {
                        keystoneVersion: "cb-keystone-v2",
                        facing: "public",
                        publicKey: $scope.defaultSshKey
                    }
                };
                $scope.showSuccess($filter("format")($rootScope.msg.openstack_credential_success, String(result.name)));
                $scope.openstackCredentialForm.$setPristine();
                collapseCreateCredentialFormPanel();
                $scope.credentialInCreation = false;
                $scope.unShowErrorMessageAlert();
            }

            function handleOpenstackCredentailCreationError(error) {
                $scope.showError(error, $rootScope.msg.openstack_credential_failed);
                $scope.credentialInCreation = false;
                $scope.showErrorMessageAlert();
            }
        }

        $scope.createGcpCredential = function() {
            $scope.credentialGcp.cloudPlatform = "GCP";
            $scope.credentialInCreation = true;

            var p12File = $scope.gcp.p12
            var reader = new FileReader();

            reader.onloadend = function(evt) {
                if (evt.target.readyState == FileReader.DONE) {
                    $scope.credentialGcp.parameters.serviceAccountPrivateKey = $base64.encode(evt.target.result);

                    if ($scope.credentialGcp.public) {
                        AccountCredential.save($scope.credentialGcp, function(result) {
                            handleGcpCredentialSuccess(result)
                        }, function(error) {
                            $scope.showError(error, $rootScope.msg.gcp_credential_failed);
                            $scope.credentialInCreation = false;
                            $scope.showErrorMessageAlert();
                        });
                    } else {
                        UserCredential.save($scope.credentialGcp, function(result) {
                            handleGcpCredentialSuccess(result)
                        }, function(error) {
                            $scope.showError(error, $rootScope.msg.gcp_credential_failed);
                            $scope.credentialInCreation = false;
                            $scope.showErrorMessageAlert();
                        });
                    }
                }
            };

            function handleGcpCredentialSuccess(result) {
                $scope.credentialGcp.id = result.id;
                $rootScope.credentials.push($scope.credentialGcp);
                $scope.credentialGcp = {
                    publicKey: $scope.defaultSshKey
                };
                $scope.showSuccess($filter("format")($rootScope.msg.gcp_credential_success, result.name));
                $scope.credentialInCreation = false;
                $scope.gcpCredentialForm.$setPristine();
                collapseCreateCredentialFormPanel();
                $scope.unShowErrorMessageAlert();
            }

            var blob = p12File.slice(0, p12File.size);
            reader.readAsBinaryString(blob);

        }

        $scope.importMesosStack = function() {
            $scope.credentialInCreation = true;
            $scope.mesosStack.orchestrator.type = "MARATHON";
            if ($scope.mesosStack.public) {
                AccountStack.save($scope.mesosStack, function(result) {
                    stackSuccessHandler(result)
                }, function(error) {
                    $scope.showError(error, $rootScope.msg.mesos_credential_failed);
                    $scope.credentialInCreation = false;
                    $scope.showErrorMessageAlert();
                });
            } else {
                UserStack.save($scope.mesosStack, function(result) {
                    stackSuccessHandler(result)
                }, function(error) {
                    $scope.showError(error, $rootScope.msg.mesos_credential_failed);
                    $scope.credentialInCreation = false;
                    $scope.showErrorMessageAlert();
                });
            }

            function stackSuccessHandler(result) {
                $scope.mesosStack.id = result.id;
                $rootScope.importedStacks.push($scope.mesosStack);
                $scope.mesosStack = {};
                $scope.showSuccess($filter("format")($rootScope.msg.mesos_credential_success, String(result.name)));
                $scope.mesosImportStackForm.$setPristine();
                collapseCreateCredentialFormPanel();
                $scope.credentialInCreation = false;
                $scope.unShowErrorMessageAlert();
            }
        }

        $scope.createYarnCredential = function() {
            $scope.credentialInCreation = true;
            $scope.credentialYarn.cloudPlatform = "BYOS";
            $scope.credentialYarn.parameters.type = "YARN";

            if ($scope.credentialYarn.public) {
                AccountCredential.save($scope.credentialYarn, function(result) {
                    yarnCredentialSuccessHandler(result)
                }, function(error) {
                    $scope.showError(error, $rootScope.msg.yarn_credential_failed);
                    $scope.credentialInCreation = false;
                    $scope.showErrorMessageAlert();
                });
            } else {
                UserCredential.save($scope.credentialYarn, function(result) {
                    yarnCredentialSuccessHandler(result)
                }, function(error) {
                    $scope.showError(error, $rootScope.msg.yarn_credential_failed);
                    $scope.credentialInCreation = false;
                    $scope.showErrorMessageAlert();
                });
            }

            function yarnCredentialSuccessHandler(result) {
                $scope.credentialYarn.id = result.id;
                $rootScope.credentials.push($scope.credentialYarn);
                $scope.credentialYarn = {};
                $scope.showSuccess($filter("format")($rootScope.msg.yarn_credential_success, String(result.name)));
                $scope.yarnImportStackForm.$setPristine();
                collapseCreateCredentialFormPanel();
                $scope.credentialInCreation = false;
                $scope.unShowErrorMessageAlert();
            }
        }

        $scope.deleteCredential = function(credential) {
            GlobalCredential.delete({
                id: credential.id
            }, function(success) {
                $rootScope.credentials.splice($rootScope.credentials.indexOf(credential), 1);
                $scope.showSuccess($filter("format")($rootScope.msg.credential_delete_success, credential.id));
            }, function(error) {
                $scope.showError(error);
            });

        }

        $scope.deleteImportedStack = function(importedStack) {
            GlobalStack.delete({
                id: importedStack.id
            }, function(success) {
                $rootScope.importedStacks.splice($rootScope.importedStacks.indexOf(importedStack), 1);
                $scope.showSuccess($filter("format")($rootScope.msg.credential_delete_success, importedStack.id));
            }, function(error) {
                if (error.status === 400) {
                    $scope.showErrorMessage($rootScope.msg.credential_delete_failed);
                } else {
                    $scope.showError(error);
                }
            });

        }

        $scope.getTopologyNameById = function(topologyId) {
            var result;
            angular.forEach($rootScope.topologies, function(topology) {
                if (topology.id === topologyId) {
                    result = topology.name;
                }
            });
            return result;
        }

        $scope.unShowErrorMessageAlert = function() {
            $scope.showAlert = false;
            $scope.alertMessage = "";
        }

        $scope.showErrorMessageAlert = function() {
            $scope.showAlert = true;
            $scope.alertMessage = $scope.statusMessage;
        }

        $scope.getCredentialParameters = function(credential) {
            if (credential) {
                var props = angular.copy(credential.parameters)
                delete props.password
                delete props.selector
                return props
            }
        }

        $scope.getParameterLabel = function(key) {
            return $rootScope.msg[$interpolate("credential_openstack_form_{{key}}_label")({
                key: key
            })]
        }

        function collapseCreateCredentialFormPanel() {
            angular.element(document.querySelector('#panel-create-credentials-collapse-btn')).click();
        }
    }
]);