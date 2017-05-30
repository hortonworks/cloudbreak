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
        $scope.yarnTemplateForm = {};
        $scope.awsInstanceType = {};
        $scope.showAlert = false;
        $scope.alertMessage = "";
        var firstVisiblePlatform = $scope.firstVisible(["AWS", "AZURE", "BYOS", "GCP", "OPENSTACK"]);
        if (firstVisiblePlatform != -1) {
            $scope[["awsTemplate", "azureTemplate", "mesosTemplate", "gcpTemplate", "openstackTemplate", "yarntemplate"][firstVisiblePlatform]] = true;
        }

        $scope.createAwsTemplateRequest = function() {
            $scope.azureTemplate = false;
            $scope.awsTemplate = true;
            $scope.gcpTemplate = false;
            $scope.openstackTemplate = false;
            $scope.mesosTemplate = false;
            $scope.yarnTemplate = false;
        }

        $scope.createAzureTemplateRequest = function() {
            $scope.azureTemplate = true;
            $scope.awsTemplate = false;
            $scope.gcpTemplate = false;
            $scope.openstackTemplate = false;
            $scope.mesosTemplate = false;
            $scope.yarnTemplate = false;
        }

        $scope.createGcpTemplateRequest = function() {
            $scope.azureTemplate = false;
            $scope.awsTemplate = false;
            $scope.gcpTemplate = true;
            $scope.openstackTemplate = false;
            $scope.mesosTemplate = false;
            $scope.yarnTemplate = false;
        }

        $scope.createOpenstackTemplateRequest = function() {
            $scope.azureTemplate = false;
            $scope.awsTemplate = false;
            $scope.gcpTemplate = false;
            $scope.openstackTemplate = true;
            $scope.mesosTemplate = false;
            $scope.yarnTemplate = false;
        }

        $scope.createMesosTemplateRequest = function() {
            $scope.azureTemplate = false;
            $scope.awsTemplate = false;
            $scope.gcpTemplate = false;
            $scope.openstackTemplate = false;
            $scope.mesosTemplate = true;
            $scope.yarnTemplate = false;
        }

        $scope.createYarnTemplateRequest = function() {
            $scope.azureTemplate = false;
            $scope.awsTemplate = false;
            $scope.gcpTemplate = false;
            $scope.openstackTemplate = false;
            $scope.mesosTemplate = false;
            $scope.yarnTemplate = true;
        }

        $scope.createAwsTemplate = function() {
            $scope.awsTemp.cloudPlatform = 'AWS';
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
                GlobalTemplate.get({
                    id: result.id
                }, function(templ) {
                    $rootScope.templates.push(templ);
                    initializeAwsTemp();
                    $scope.showSuccess($filter("format")($rootScope.msg.aws_template_success, String(result.id)));
                    $scope.awsTemplateForm.$setPristine();
                    collapseCreateTemplateFormPanel();
                    $scope.unShowErrorMessageAlert();
                });
            }
        }

        $scope.createOpenstackTemplate = function() {
            $scope.openstackTemp.cloudPlatform = 'OPENSTACK';
            if ($scope.openstackTemp.volumeCount === 0) {
                $scope.openstackTemp.volumeSize = null
            }

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
            $scope.azureTemp.cloudPlatform = "AZURE";
            if ($scope.azureTemp.parameters.managedDisk) {
                $scope.azureTemp.volumeType = $rootScope.params.defaultDisks.AZURE;
            }
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
            $scope.mesosTemp.orchestratorType = "MARATHON";
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

        $scope.createYarnTemplate = function() {
            $scope.yarnTemp.orchestratorType = "YARN";
            if ($scope.yarnTemp.public) {
                AccountConstraint.save($scope.yarnTemp, function(result) {
                    handleYarnTemplateSuccess(result)
                }, function(error) {
                    $scope.showError(error, $rootScope.msg.yarn_template_failed);
                    $scope.showErrorMessageAlert();
                });
            } else {
                UserConstraint.save($scope.yarnTemp, function(result) {
                    handleYarnTemplateSuccess(result)
                }, function(error) {
                    $scope.showError(error, $rootScope.msg.yarn_template_failed);
                    $scope.showErrorMessageAlert();
                });
            }

            function handleYarnTemplateSuccess(result) {
                $scope.yarnTemp.id = result.id;
                $rootScope.constraints.push($scope.yarnTemp);
                initializeYarnTemp();
                $scope.showSuccess($filter("format")($rootScope.msg.yarn_template_success, String(result.id)));
                $scope.yarnTemplateForm.$setPristine();
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
                    var instanceTypeParams = $filter('filter')($rootScope.params.vmTypes.AWS, {
                        value: $scope.awsTemp.instanceType
                    }, true)[0];
                    var ephemeralVolumeConfig = $filter('filter')(instanceTypeParams.vmTypeMetaJson.configs, {
                        volumeParameterType: 'EPHEMERAL'
                    }, true)[0];
                    if (volume !== 'ephemeral' || (ephemeralVolumeConfig !== null && ephemeralVolumeConfig.maximumNumber !== 0)) {
                        return true;
                    }
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

        $scope.changeInstanceType = function(instanceType, volumeType, platform, templateTemp, instanceTypeChanged) {
            instanceTypeChanged = instanceTypeChanged || false;
            var actualInstanceType = $filter('filter')($rootScope.params.vmTypes[platform], {
                value: instanceType
            }, true)[0];
            var diskMapping = $rootScope.params.diskMappings[platform][volumeType];
            if (diskMapping !== null && diskMapping !== undefined && actualInstanceType !== null && actualInstanceType !== undefined) {
                var diskConfig = $filter('filter')(actualInstanceType.vmTypeMetaJson.configs, {
                    volumeParameterType: diskMapping
                }, true)[0];
                if (instanceTypeChanged && (diskConfig == null || diskConfig === undefined || diskConfig.maximumNumber == 0)) {
                    templateTemp.volumeType = volumeType = $rootScope.params.defaultDisks[platform];
                    diskConfig = $filter('filter')(actualInstanceType.vmTypeMetaJson.configs, {
                        volumeParameterType: $rootScope.params.diskMappings[platform][volumeType]
                    }, true)[0];
                }
                templateTemp.maxDiskNumber = diskConfig.maximumNumber;
                templateTemp.minDiskNumber = diskConfig.minimumNumber;
                templateTemp.minDiskSize = diskConfig.minimumSize;
                templateTemp.maxDiskSize = diskConfig.maximumSize;
                templateTemp.CPUs = actualInstanceType.vmTypeMetaJson.properties.Cpu;
                templateTemp.RAMs = actualInstanceType.vmTypeMetaJson.properties.Memory;
                if (templateTemp.maxDiskNumber === templateTemp.minDiskNumber) {
                    templateTemp.volumeCount = templateTemp.maxDiskNumber;
                } else if (templateTemp.volumeCount < templateTemp.minDiskNumber) {
                    templateTemp.volumeCount = defaultAwsVolumeCount;
                } else if (templateTemp.volumeCount > templateTemp.maxDiskNumber) {
                    templateTemp.volumeCount = templateTemp.maxDiskNumber;
                } else {
                    templateTemp.volumeCount = defaultAwsVolumeCount;
                }
                if (templateTemp.minDiskSize === templateTemp.maxDiskSize) {
                    templateTemp.volumeSize = templateTemp.maxDiskSize;
                } else if (templateTemp.volumeSize < templateTemp.minDiskSize) {
                    templateTemp.volumeSize = defaultAwsVolumeSize;
                } else if (templateTemp.volumeSize > templateTemp.maxDiskSize) {
                    templateTemp.volumeSize = templateTemp.maxDiskSize;
                } else {
                    templateTemp.volumeSize = defaultAwsVolumeSize;
                }
                if (volumeType == 'ephemeral') {
                    templateTemp.volumeCount = null;
                    templateTemp.volumeSize = null;
                }
            } else {
                templateTemp.maxDiskNumber = 24;
                templateTemp.minDiskNumber = 0;
                templateTemp.minDiskSize = 1;
                templateTemp.maxDiskSize = 10000;
                templateTemp.CPUs = null;
                templateTemp.RAMs = null;
            }
        }

        function collapseCreateTemplateFormPanel() {
            angular.element(document.querySelector('#panel-create-templates-collapse-btn')).click();
        }

        var defaultAwsVolumeCount = 1;
        var defaultAwsVolumeSize = 100;

        function initializeAwsTemp() {
            $scope.awsTemp = {
                volumeCount: defaultAwsVolumeCount,
                volumeSize: defaultAwsVolumeSize,
                volumeType: $rootScope.params.defaultDisks.AWS,
                instanceType: $rootScope.params.defaultVmTypes.AWS,
                parameters: {
                    encrypted: false
                }
            };
            $scope.changeInstanceType($scope.awsTemp.instanceType, $scope.awsTemp.volumeType, "AWS", $scope.awsTemp);
        }

        function initializeAzureTemp() {
            $scope.azureTemp = {
                volumeCount: 1,
                volumeSize: 100,
                volumeType: $rootScope.params.defaultDisks.AZURE,
                instanceType: $rootScope.params.defaultVmTypes.AZURE,
                parameters: {
                    managedDisk: true
                }
            };
            $scope.changeInstanceType($scope.azureTemp.instanceType, $scope.azureTemp.volumeType, "AZURE", $scope.azureTemp);
        }

        function initializeMesosTemp() {
            $scope.mesosTemp = {
                cpu: 2,
                memory: 4096
            }
        }

        function initializeYarnTemp() {
            $scope.yarnTemp = {
                cpu: 2,
                memory: 4096,
                disk: 500
            }
        }

        function initializeOpenstackTemp() {
            $scope.openstackTemp = {
                volumeCount: 1,
                volumeSize: 10,
                volumeType: "HDD",
                parameters: {}
            }
            $scope.changeInstanceType($scope.openstackTemp.instanceType, $scope.openstackTemp.volumeType, "OPENSTACK", $scope.openstackTemp);
        }

        function initializeGcpTemp() {
            $scope.gcpTemp = {
                volumeCount: 1,
                volumeSize: 100,
                instanceType: $rootScope.params.defaultVmTypes.GCP,
                volumeType: $rootScope.params.defaultDisks.GCP,
                parameters: {}
            }
            $scope.changeInstanceType($scope.gcpTemp.instanceType, $scope.gcpTemp.volumeType, "GCP", $scope.gcpTemp);
        }

        $scope.filterByCloudPlatform = function(topology) {
            return (topology.cloudPlatform === 'AWS' && $scope.awsTemplate) ||
                (topology.cloudPlatform === 'GCP' && $scope.gcpTemplate) ||
                (topology.cloudPlatform === 'AZURE' && $scope.azureTemplate) ||
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

        initializeAzureTemp();
        initializeAwsTemp();
        initializeGcpTemp();
        initializeOpenstackTemp();
        initializeMesosTemp();
        initializeYarnTemp();
    }
]);