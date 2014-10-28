'use strict';

var log = log4javascript.getLogger("templateController-logger");

angular.module('uluwatuControllers').controller('templateController', ['$scope', '$rootScope', 'UserTemplate', 'GlobalTemplate',
    function ($scope, $rootScope, UserTemplate, GlobalTemplate) {

        $rootScope.volumeTypes = {
            'Gp2': 'SSD',
            'Standard': 'Magnetic'
        }

        $rootScope.awsRegions = {
            'US_EAST_1': 'US East(N. Virginia)',
            'US_WEST_1': 'US West (N. California)',
            'US_WEST_2': 'US West (Oregon)',
            'EU_WEST_1': 'EU (Ireland)',
            'AP_SOUTHEAST_1': 'Asia Pacific (Singapore)',
            'AP_SOUTHEAST_2': 'Asia Pacific (Sydney)',
            'AP_NORTHEAST_1': 'Asia Pacific (Tokyo)',
            'SA_EAST_1': 'South America (São Paulo)'
        }

        $rootScope.amis = {
            'US_EAST_1': 'ami-5edf6136',
            'US_WEST_1': 'ami-4fc1cb0a',
            'US_WEST_2': 'ami-29367a19',
            'EU_WEST_1': 'ami-e66cc191',
            'AP_SOUTHEAST_1': 'ami-7cedcb2e',
            'AP_SOUTHEAST_2': 'ami-2f385515',
            'AP_NORTHEAST_1': 'ami-757e4974',
            'SA_EAST_1': 'ami-91f0448c'
        }

        $rootScope.azureRegions = {
            'BRAZIL_SOUTH': 'Brazil South',
            'EAST_ASIA': 'East Asia',
            'EAST_US': 'East US',
            'NORTH_EUROPE': 'North Europe',
            'WEST_US': 'West US'
        }

        $rootScope.azureVmTypes = {
            'SMALL': 'Small',
            'MEDIUM': 'Medium',
            'LARGE': 'Large',
            'EXTRA_LARGE': 'Extra Large'
        }

        $rootScope.gccRegions = {
            'US_CENTRAL1_A': 'us-central1-a',
            'US_CENTRAL1_B': 'us-central1-b',
            'US_CENTRAL1_F': 'us-central1-f',
            'EUROPE_WEST1_A': 'europe-west1-a',
            'EUROPE_WEST1_B': 'europe-west1-b',
            'ASIA_EAST1_A': 'asia-east1-a',
            'ASIA_EAST1_B': 'asia-east1-b'
        }

        $rootScope.gccDiskTypes = {
            'HDD': 'HDD',
            'SSD': 'SSD'
        }

        $rootScope.gccInstanceTypes = {
            'N1_STANDARD_1': 'n1-standard-1',
            'N1_STANDARD_2': 'n1-standard-2',
            'N1_STANDARD_4': 'n1-standard-4',
            'N1_STANDARD_8': 'n1-standard-8',
            'N1_STANDARD_16': 'n1-standard-16',
            'N1_HIGHMEM_2': 'n1-highmem-2',
            'N1_HIGHMEM_4': 'n1-highmem-4',
            'N1_HIGHMEM_8': 'n1-highmem-8',
            'N1_HIGHMEM_16': 'n1-highmem-16',
            'N1_HIGHCPU_2': 'n1-highcpu-2',
            'N1_HIGHCPU_4': 'n1-highcpu-4',
            'N1_HIGHCPU_8': 'n1-highcpu-8',
            'N1_HIGHCPU_16': 'n1-highcpu-16'
        }

        $rootScope.templates = UserTemplate.query();
        $scope.azureTemp = {};
        $scope.gccTemp = {};
        $scope.azureTemp = {};
        $scope.awsTemplateForm = {};
        $scope.gccTemplateForm = {};
        initializeAzureTemp();
        initializeAwsTemp();
        initializeGccTemp();

        $scope.createAwsTemplateRequest = function () {
            $scope.azureTemplate = false;
            $scope.awsTemplate = true;
            $scope.gccTemplate = false;
        }

        $scope.createAzureTemplateRequest = function () {
            $scope.azureTemplate = true;
            $scope.awsTemplate = false;
            $scope.gccTemplate = false;
        }

        $scope.createGccTemplateRequest = function () {
            $scope.azureTemplate = false;
            $scope.awsTemplate = false;
            $scope.gccTemplate = true;
        }

        $scope.createAwsTemplate = function () {
            $scope.awsTemp.cloudPlatform = 'AWS';
            $scope.awsTemp.parameters.amiId = $rootScope.amis[$scope.awsTemp.parameters.region];

            UserTemplate.save($scope.awsTemp, function (result) {
                $scope.awsTemp.id = result.id;
                $rootScope.templates.push($scope.awsTemp);
                initializeAwsTemp();
                $scope.modifyStatusMessage($rootScope.error_msg.aws_template_success1 + result.id + $rootScope.error_msg.aws_template_success2);
                $scope.modifyStatusClass("has-success");
                $scope.awsTemplateForm.$setPristine();
                collapseCreateTemplateFormPanel();
            }, function (error) {
                $scope.modifyStatusMessage($rootScope.error_msg.aws_template_failed + ": " + error.data.message);
                $scope.modifyStatusClass("has-error");
            });
        }

        $scope.createGccTemplate = function () {
            $scope.gccTemp.cloudPlatform = 'GCC';
            $scope.gccTemp.parameters.gccImageType = "DEBIAN_HACK";
            $scope.gccTemp.parameters.volumeType = $rootScope.gccDiskTypes[$scope.gccTemp.parameters.volumeType];

            UserTemplate.save($scope.gccTemp, function (result) {
                $scope.gccTemp.id = result.id;
                $rootScope.templates.push($scope.gccTemp);
                initializeGccTemp();
                $scope.modifyStatusMessage($rootScope.error_msg.gcc_template_success1 + result.id + $rootScope.error_msg.gcc_template_success2);
                $scope.modifyStatusClass("has-success");
                $scope.gccTemplateForm.$setPristine();
                collapseCreateTemplateFormPanel();
            }, function (error) {
                $scope.modifyStatusMessage($rootScope.error_msg.gcc_template_failed + ": " + error.data.message);
                $scope.modifyStatusClass("has-error");
            });
        }

        $scope.createAzureTemplate = function () {
            $scope.azureTemp.cloudPlatform = "AZURE";
            $scope.azureTemp.parameters.imageName = "ambari-docker-v1";

            UserTemplate.save($scope.azureTemp, function (result) {
                $scope.azureTemp.id = result.id;
                $rootScope.templates.push($scope.azureTemp);
                initializeAzureTemp();
                $scope.modifyStatusMessage($rootScope.error_msg.azure_template_success1 + result.id + $rootScope.error_msg.azure_template_success2);
                $scope.modifyStatusClass("has-success");
                $scope.azureTemplateForm.$setPristine();
                collapseCreateTemplateFormPanel();
            }, function (error) {
                $scope.modifyStatusMessage($rootScope.error_msg.azure_template_failed + ": " + error.data.message);
                $scope.modifyStatusClass("has-error");
            });
        }

        $scope.deleteTemplate = function (template) {
            GlobalTemplate.delete({ id: template.id }, function (success) {
                $rootScope.templates.splice($rootScope.templates.indexOf(template), 1);
                $scope.modifyStatusMessage($rootScope.error_msg.template_delete_success1 + template.id + $rootScope.error_msg.template_delete_success2);
                $scope.modifyStatusClass("has-success");
            }, function (error) {
                $scope.modifyStatusMessage($rootScope.error_msg.template_delete_failed + ": " + error.data.message);
                $scope.modifyStatusClass("has-error");
            });
        }

        function collapseCreateTemplateFormPanel() {
          angular.element(document.querySelector('#panel-create-templates-collapse-btn')).click();
        }

        function initializeAwsTemp() {
            $scope.awsTemp = {
                parameters: {
                    sshLocation: "0.0.0.0/0",
                    region: "EU_WEST_1",
                    instanceType: "T2Medium",
                    volumeType: "Standard"
                }
            }
        }

        function initializeAzureTemp() {
            $scope.azureTemp = {
                parameters: {
                    location: "NORTH_EUROPE",
                    vmType: "MEDIUM"
                }
            }
        }

        function initializeGccTemp() {
            $scope.gccTemp = {
                parameters: {
                    gccInstanceType: "N1_STANDARD_2",
                    gccZone: "EUROPE_WEST1_A",
                    volumeType: "HDD"
                }
            }
        }
    }
]);
