'use strict';

var log = log4javascript.getLogger("templateController-logger");

angular.module('uluwatuControllers').controller('templateController', [
    '$scope', '$rootScope', '$filter', 'UserTemplate', 'AccountTemplate', 'GlobalTemplate', 'UserConstraint', 'AccountConstraint', 'GlobalConstraint',
    function($scope, $rootScope, $filter, UserTemplate, AccountTemplate, GlobalTemplate, UserConstraint, AccountConstraint, GlobalConstraint) {

        $rootScope.templates = AccountTemplate.query();
        $rootScope.constraints = AccountConstraint.query();
        $scope.awsTemplateForm = {};
        $scope.gcpTemplateForm = {};
        $scope.openstackTemplateForm = {};
        $scope.mesosTemplateForm = {};
        $scope.awsInstanceType = {};
        initializeAzureTemp();
        initializeAwsTemp();
        initializeGcpTemp();
        initializeOpenstackTemp();
        initializeMesosTemp();
        $scope.showAlert = false;
        $scope.alertMessage = "";
        var firstVisiblePlatform = $scope.firstVisible(["AWS", "AZURE_RM", "BYOS", "GCP", "OPENSTACK"]);
        if (firstVisiblePlatform != -1) {
            $scope[["awsTemplate", "azureTemplate", "mesosTemplate", "gcpTemplate", "openstackTemplate"][firstVisiblePlatform]] = true;
        }

        $scope.createAwsTemplateRequest = function() {
            $scope.azureTemplate = false;
            $scope.awsTemplate = true;
            $scope.gcpTemplate = false;
            $scope.openstackTemplate = false;
            $scope.mesosTemplate = false;
        }

        $scope.createAzureTemplateRequest = function() {
            $scope.azureTemplate = true;
            $scope.awsTemplate = false;
            $scope.gcpTemplate = false;
            $scope.openstackTemplate = false;
            $scope.mesosTemplate = false;
        }

        $scope.createGcpTemplateRequest = function() {
            $scope.azureTemplate = false;
            $scope.awsTemplate = false;
            $scope.gcpTemplate = true;
            $scope.openstackTemplate = false;
            $scope.mesosTemplate = false;
        }

        $scope.createOpenstackTemplateRequest = function() {
            $scope.azureTemplate = false;
            $scope.awsTemplate = false;
            $scope.gcpTemplate = false;
            $scope.openstackTemplate = true;
            $scope.mesosTemplate = false;
        }

        $scope.createMesosTemplateRequest = function() {
            $scope.azureTemplate = false;
            $scope.awsTemplate = false;
            $scope.gcpTemplate = false;
            $scope.openstackTemplate = false;
            $scope.mesosTemplate = true;
        }


        $scope.createAwsTemplate = function() {
            $scope.awsTemp.cloudPlatform = 'AWS';
            $scope.awsTemp.parameters.sshLocation = '0.0.0.0/0';
            if ($scope.awsTemp.public) {
                AccountTemplate.save($scope.awsTemp, function(result) {
                    handleAwsTemplateSuccess(result)
                }, function(error) {
                    $scope.showError(error, $rootScope.msg.aws_template_failed);
                    $scope.showErrorMessageAlert();
                });
            } else {
                UserTemplate.save($scope.awsTemp, function(result) {
                    handleAwsTemplateSuccess(result)
                }, function(error) {
                    $scope.showError(error, $rootScope.msg.aws_template_failed);
                    $scope.showErrorMessageAlert();
                });
            }

            function handleAwsTemplateSuccess(result) {
                $scope.awsTemp.id = result.id;
                $rootScope.templates.push($scope.awsTemp);
                initializeAwsTemp();
                $scope.showSuccess($filter("format")($rootScope.msg.aws_template_success, String(result.id)));
                $scope.awsTemplateForm.$setPristine();
                collapseCreateTemplateFormPanel();
                $scope.unShowErrorMessageAlert();
            }
        }

        $scope.createOpenstackTemplate = function() {
            $scope.openstackTemp.cloudPlatform = 'OPENSTACK';
            if ($scope.openstackTemp.public) {
                AccountTemplate.save($scope.openstackTemp, function(result) {
                    handleOpenstackTemplateSuccess(result)
                }, function(error) {
                    $scope.showError(error, $rootScope.msg.openstack_template_failed);
                    $scope.showErrorMessageAlert();
                });
            } else {
                UserTemplate.save($scope.openstackTemp, function(result) {
                    handleOpenstackTemplateSuccess(result)
                }, function(error) {
                    $scope.showError(error, $rootScope.msg.openstack_template_failed);
                    $scope.showErrorMessageAlert();
                });
            }

            function handleOpenstackTemplateSuccess(result) {
                $scope.openstackTemp.id = result.id;
                $rootScope.templates.push($scope.openstackTemp);
                initializeOpenstackTemp();
                $scope.showSuccess($filter("format")($rootScope.msg.openstack_template_success, String(result.id)));
                $scope.openstackTemplateForm.$setPristine();
                collapseCreateTemplateFormPanel();
                $scope.unShowErrorMessageAlert()
            }

        }

        $scope.createGcpTemplate = function() {
            $scope.gcpTemp.cloudPlatform = 'GCP';
            if ($scope.gcpTemp.public) {
                AccountTemplate.save($scope.gcpTemp, function(result) {
                    handleGcpTemplateSuccess(result)
                }, function(error) {
                    $scope.showError(error, $rootScope.msg.gcp_template_failed);
                    $scope.showErrorMessageAlert();
                });
            } else {
                UserTemplate.save($scope.gcpTemp, function(result) {
                    handleGcpTemplateSuccess(result)
                }, function(error) {
                    $scope.showError(error, $rootScope.msg.gcp_template_failed);
                    $scope.showErrorMessageAlert();
                });
            }

            function handleGcpTemplateSuccess(result) {
                $scope.gcpTemp.id = result.id;
                $rootScope.templates.push($scope.gcpTemp);
                initializeGcpTemp();
                $scope.showSuccess($filter("format")($rootScope.msg.gcp_template_success, String(result.id)));
                $scope.gcpTemplateForm.$setPristine();
                collapseCreateTemplateFormPanel();
                $scope.unShowErrorMessageAlert();
            }
        }

        $scope.createAzureTemplate = function() {
            $scope.azureTemp.cloudPlatform = "AZURE_RM";
            if ($scope.azureTemp.public) {
                AccountTemplate.save($scope.azureTemp, function(result) {
                    handleAzureTemplateSuccess(result)
                }, function(error) {
                    $scope.showError(error, $rootScope.msg.azure_template_failed);
                    $scope.showErrorMessageAlert();
                });
            } else {
                UserTemplate.save($scope.azureTemp, function(result) {
                    handleAzureTemplateSuccess(result)
                }, function(error) {
                    $scope.showError(error, $rootScope.msg.azure_template_failed);
                    $scope.showErrorMessageAlert();
                });
            }

            function handleAzureTemplateSuccess(result) {
                $scope.azureTemp.id = result.id;
                $rootScope.templates.push($scope.azureTemp);
                initializeAzureTemp();
                $scope.showSuccess($filter("format")($rootScope.msg.azure_template_success, String(result.id)));
                $scope.azureTemplateForm.$setPristine();
                collapseCreateTemplateFormPanel();
                $scope.unShowErrorMessageAlert();
            }
        }

        $scope.createMesosTemplate = function() {
            if ($scope.mesosTemp.public) {
                AccountConstraint.save($scope.mesosTemp, function(result) {
                    handleMesosTemplateSuccess(result)
                }, function(error) {
                    $scope.showError(error, $rootScope.msg.mesos_template_failed);
                    $scope.showErrorMessageAlert();
                });
            } else {
                UserConstraint.save($scope.mesosTemp, function(result) {
                    handleMesosTemplateSuccess(result)
                }, function(error) {
                    $scope.showError(error, $rootScope.msg.mesos_template_failed);
                    $scope.showErrorMessageAlert();
                });
            }

            function handleMesosTemplateSuccess(result) {
                $scope.mesosTemp.id = result.id;
                $rootScope.constraints.push($scope.mesosTemp);
                initializeMesosTemp();
                $scope.showSuccess($filter("format")($rootScope.msg.mesos_template_success, String(result.id)));
                $scope.mesosTemplateForm.$setPristine();
                collapseCreateTemplateFormPanel();
                $scope.unShowErrorMessageAlert();
            }
        }

        $scope.deleteTemplate = function(template) {
            GlobalTemplate.delete({
                id: template.id
            }, function(success) {
                $rootScope.templates.splice($rootScope.templates.indexOf(template), 1);
                $scope.showSuccess($filter("format")($rootScope.msg.template_delete_success, String(template.id)));
            }, function(error) {
                $scope.showError(error, $rootScope.msg.template_delete_failed)
            });
        }

        $scope.deleteConstraint = function(constraint) {
            GlobalConstraint.delete({
                id: constraint.id
            }, function(success) {
                $rootScope.constraints.splice($rootScope.constraints.indexOf(constraint), 1);
                $scope.showSuccess($filter("format")($rootScope.msg.constraint_delete_success, String(constraint.id)));
            }, function(error) {
                $scope.showError(error, $rootScope.msg.constraint_delete_failed)
            });
        }


        $scope.filterByVolumetype = function(volume) {
            try {
                if ((isAwsVolumeEncryptable(volume) && isAwsEncryptionSet()) || !isAwsEncryptionSet()) {
                    return true;
                }
                return false;
            } catch (err) {
                return true;
            }
        }

        function isAwsEncryptionSet() {
            return $scope.awsTemp.parameters.encrypted;
        }

        function isAwsVolumeEncryptable(volume) {
            return volume !== 'ephemeral';
        }

        $scope.changeAwsInstanceType = function() {
            var instanceType = $scope.awsTemp.parameters.instanceType;
            $scope.awsInstanceType = $filter('filter')($rootScope.params.vmTypes.AWS, {
                value: instanceType
            }, true)[0];
            if ($scope.awsTemp.volumeType == 'ephemeral') {
                $scope.awsTemp.parameters.encrypted = false;
            }
        }

        function collapseCreateTemplateFormPanel() {
            angular.element(document.querySelector('#panel-create-templates-collapse-btn')).click();
        }

        function initializeAwsTemp() {
            $scope.awsTemp = {
                volumeCount: 1,
                volumeSize: 100,
                volumeType: $rootScope.params.defaultDisks.AWS,
                instanceType: $rootScope.params.defaultVmTypes.AWS,
                parameters: {
                    sshLocation: "0.0.0.0/0",
                    encrypted: false
                }
            }
        }

        function initializeAzureTemp() {
            $scope.azureTemp = {
                volumeCount: 1,
                volumeSize: 100,
                volumeType: "HDD",
                instanceType: $rootScope.params.defaultVmTypes.AZURE_RM,
                parameters: {}
            }
        }

        function initializeMesosTemp() {
            $scope.mesosTemp = {
                cpu: 2,
                memory: 4096
            }
        }


        function initializeOpenstackTemp() {
            $scope.openstackTemp = {
                volumeCount: 1,
                volumeSize: 100,
                volumeType: "HDD",
                parameters: {}
            }
        }

        function initializeGcpTemp() {
            $scope.gcpTemp = {
                volumeCount: 1,
                volumeSize: 100,
                instanceType: $rootScope.params.defaultVmTypes.GCP,
                volumeType: $rootScope.params.defaultDisks.GCP,
                parameters: {}
            }
        }

        $scope.filterByCloudPlatform = function(topology) {
            return (topology.cloudPlatform === 'AWS' && $scope.awsTemplate) ||
                (topology.cloudPlatform === 'GCP' && $scope.gcpTemplate) ||
                (topology.cloudPlatform === 'AZURE_RM' && $scope.azureTemplate) ||
                (topology.cloudPlatform === 'OPENSTACK' && $scope.openstackTemplate)
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
    }
]);