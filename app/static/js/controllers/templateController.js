'use strict';

var log = log4javascript.getLogger("templateController-logger");

angular.module('uluwatuControllers').controller('templateController', ['$scope', '$rootScope', 'UserTemplate', 'GlobalTemplate',
    function ($scope, $rootScope, UserTemplate, GlobalTemplate) {

        $scope.volumeTypes = {
            'Gp2': 'SSD',
            'Standard': 'Magnetic'
        }

        $scope.awsRegions = {
            'US_EAST_1': 'US East(N. Virginia)',
            'US_WEST_1': 'US West (N. California)',
            'US_WEST_2': 'US West (Oregon)',
            'EU_WEST_1': 'EU (Ireland)',
            'AP_SOUTHEAST_1': 'Asia Pacific (Singapore)',
            'AP_SOUTHEAST_2': 'Asia Pacific (Sydney)',
            'AP_NORTHEAST_1': 'Asia Pacific (Tokyo)',
            'SA_EAST_1': 'South America (São Paulo)'
        }

        $scope.amis = {
            'US_EAST_1': 'ami-0609d86e',
            'US_WEST_1': 'ami-73000d36',
            'US_WEST_2': 'ami-07bdc737',
            'EU_WEST_1': 'ami-e09e4397',
            'AP_SOUTHEAST_1': 'ami-363f6464',
            'AP_SOUTHEAST_2': 'ami-3d9ef807',
            'AP_NORTHEAST_1': 'ami-8dbde48c',
            'SA_EAST_1': 'ami-17f45c0a'
        }

        $scope.azureRegions = {
            'NORTH_EUROPE': 'North Europe',
            'EAST_ASIA': 'East Asia',
            'EAST_US': 'East US',
            'WEST_US': 'West US',
            'BRAZIL_SOUTH': 'Brazil South'
        }

        $scope.azureVmTypes = {
            'SMALL': 'Small',
            'MEDIUM': 'Medium',
            'LARGE': 'Large',
            'EXTRA_LARGE': 'Extra Large'
        }

        $rootScope.templates = UserTemplate.query();
        $scope.azureTemp = {};
        $scope.gccTemp = {};
        $scope.awsTemplateForm = {};
        $scope.gccTemplateForm = {};
        $scope.azureTemplateForm = {};
        initializeAwsTemp();

        $scope.createAwsTemplateRequest = function() {
            $scope.azureTemplate = false;
            $scope.awsTemplate = true;
            $scope.gccTemplate = false;
        }

        $scope.createAzureTemplateRequest = function() {
            $scope.azureTemplate = true;
            $scope.awsTemplate = false;
            $scope.gccTemplate = false;
        }

        $scope.createGccTemplateRequest = function() {
            $scope.azureTemplate = false;
            $scope.awsTemplate = false;
            $scope.gccTemplate = true;
        }

        $scope.createAwsTemplate = function () {
            $scope.awsTemp.cloudPlatform = 'AWS';
            $scope.awsTemp.parameters.amiId = $scope.amis[$scope.awsTemp.parameters.region];

            UserTemplate.save($scope.awsTemp, function (result) {
                $scope.awsTemp.id = result.id;
                $rootScope.templates.push($scope.awsTemp);
                initializeAwsTemp();
                $scope.modifyStatusMessage($rootScope.error_msg.aws_template_success1 + result.id + $rootScope.error_msg.aws_template_success2);
                $scope.modifyStatusClass("has-success");
                $scope.awsTemplateForm.$setPristine();
            }, function (error) {
                $scope.modifyStatusMessage($rootScope.error_msg.aws_template_failed + ": " + error.data.message);
                $scope.modifyStatusClass("has-error");
            });
        }

        $scope.createGccTemplate = function () {
            $scope.gccTemp.cloudPlatform = 'GCC';

            UserTemplate.save($scope.gccTemp, function (result) {
                $scope.gccTemp.id = result.id;
                $rootScope.templates.push($scope.gccTemp);
                $scope.gccTemp = {};
                $scope.modifyStatusMessage($rootScope.error_msg.aws_template_success1 + result.id + $rootScope.error_msg.aws_template_success2);
                $scope.modifyStatusClass("has-success");
                $scope.gccTemplateForm.$setPristine();
            }, function (error) {
                $scope.modifyStatusMessage($rootScope.error_msg.aws_template_failed + ": " + error.data.message);
                $scope.modifyStatusClass("has-error");
            });
        }

        $scope.createAzureTemplate = function () {
            $scope.azureTemp.cloudPlatform = "AZURE";
            $scope.azureTemp.parameters.imageName = "ambari-docker-v1";

            UserTemplate.save($scope.azureTemp, function (result) {
                $scope.azureTemp.id = result.id;
                $rootScope.templates.push($scope.azureTemp);
                $scope.azureTemp = {};
                $scope.modifyStatusMessage($rootScope.error_msg.azure_template_success1 + result.id + $rootScope.error_msg.azure_template_success2);
                $scope.modifyStatusClass("has-success");
                $scope.azureTemplateForm.$setPristine();
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

        function initializeAwsTemp() {
            $scope.awsTemp = {
                parameters: {
                    sshLocation: "0.0.0.0/0"
                }
            }
        }
    }
]);
