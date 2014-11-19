'use strict';

var log = log4javascript.getLogger("mainController-logger");
var $jq = jQuery.noConflict();

angular.module('uluwatuControllers').controller('mainController', ['$scope', '$rootScope', '$filter', '$interval',
    function ($scope, $rootScope, $filter, $interval) {

        $scope.managementShow = true;
        $scope.eventShow = false;

        $scope.showManagement = function () {
            $scope.managementShow = true;
            $scope.eventShow = false;
        }

        $scope.showEvents = function () {
            $scope.managementShow = false;
            $scope.eventShow = true;
        }


        $rootScope.config = {
            'GCC' : {
                gccRegions: [
                    {key: 'US_CENTRAL1_A', value: "us-central1-a"},
                    {key: 'US_CENTRAL1_B', value: "us-central1-b"},
                    {key: 'US_CENTRAL1_F', value: "us-central1-f"},
                    {key: 'EUROPE_WEST1_B', value: "europe-west1-b"},
                    {key: 'ASIA_EAST1_A', value: "asia-east1-a"},
                    {key: 'ASIA_EAST1_B', value: "asia-east1-b"}
                ],
                gccDiskTypes: [
                    {key: 'HDD', value: 'Magnetic'},
                    {key: 'SSD', value: 'SSD'}
                ],
                gccInstanceTypes: [
                    {key: 'N1_STANDARD_1', value: 'n1-standard-1'},
                    {key: 'N1_STANDARD_2', value:'n1-standard-2'},
                    {key: 'N1_STANDARD_4', value:'n1-standard-4'},
                    {key: 'N1_STANDARD_8', value:'n1-standard-8'},
                    {key: 'N1_STANDARD_16', value:'n1-standard-16'},
                    {key: 'N1_HIGHMEM_2', value:'n1-highmem-2'},
                    {key: 'N1_HIGHMEM_4', value:'n1-highmem-4'},
                    {key: 'N1_HIGHMEM_8', value:'n1-highmem-8'},
                    {key: 'N1_HIGHMEM_16', value:'n1-highmem-16'},
                    {key: 'N1_HIGHCPU_2', value:'n1-highcpu-2'},
                    {key: 'N1_HIGHCPU_4', value:'n1-highcpu-4'},
                    {key: 'N1_HIGHCPU_8', value:'n1-highcpu-8'},
                    {key: 'N1_HIGHCPU_16', value:'n1-highcpu-16'}
                ]
            },
            'AZURE': {
                azureRegions: [
                    {key: 'BRAZIL_SOUTH', value: 'Brazil South'},
                    {key: 'EAST_ASIA', value: 'East Asia'},
                    {key: 'EAST_US', value: 'East US'},
                    {key: 'NORTH_EUROPE', value: 'North Europe'},
                    {key: 'WEST_US', value: 'West US'}
                ],
                azureVmTypes: [
                    {key: 'SMALL', value: 'Small'},
                    {key: 'MEDIUM', value: 'Medium'},
                    {key: 'LARGE', value: 'Large'},
                    {key: 'EXTRA_LARGE', value: 'Extra Large'}
                ]
            },
            'AWS': {
                volumeTypes: [
                    {key: 'Gp2', value: 'SSD'},
                    {key: 'Standard', value: 'Magnetic'}
                ],
                awsRegions : [
                    {key: 'US_EAST_1', value: 'US East(N. Virginia)'},
                    {key: 'US_WEST_1', value: 'US West (N. California)'},
                    {key: 'US_WEST_2', value: 'US West (Oregon)'},
                    {key: 'EU_WEST_1', value: 'EU (Ireland)'},
                    {key: 'AP_SOUTHEAST_1', value: 'Asia Pacific (Singapore)'},
                    {key: 'AP_SOUTHEAST_2', value: 'Asia Pacific (Sydney)'},
                    {key: 'AP_NORTHEAST_1', value: 'Asia Pacific (Tokyo)'},
                    {key: 'SA_EAST_1', value: 'South America (São Paulo)'}
                ],
                amis: [
                    {key: 'US_EAST_1', value: 'ami-5edf6136'},
                    {key: 'US_WEST_1', value: 'ami-4fc1cb0a'},
                    {key: 'US_WEST_2', value: 'ami-29367a19'},
                    {key: 'EU_WEST_1', value: 'ami-e66cc191'},
                    {key: 'AP_SOUTHEAST_1', value: 'ami-7cedcb2e'},
                    {key: 'AP_SOUTHEAST_2', value: 'ami-2f385515'},
                    {key: 'AP_NORTHEAST_1', value: 'ami-757e4974'},
                    {key: 'SA_EAST_1', value: 'ami-91f0448c'}
                ],
                instanceType: [
                    {key: 'T2Micro', value: 'T2Micro'},
                    {key: 'T2Small', value: 'T2Small'},
                    {key: 'T2Medium', value: 'T2Medium'},
                    {key: 'M3Medium', value: 'M3Medium'},
                    {key: 'M3Large', value: 'M3Large'},
                    {key: 'M3Xlarge', value: 'M3Xlarge'},
                    {key: 'M32xlarge', value: 'M32xlarge'},
                    {key: 'C3large', value: 'C3large'},
                    {key: 'C3xlarge', value: 'C3xlarge'},
                    {key: 'C32xlarge', value: 'C32xlarge'},
                    {key: 'C34xlarge', value: 'C34xlarge'},
                    {key: 'C38xlarge', value: 'C38xlarge'}
                ]

            },
            'EVENT_TYPE': {
                "REQUESTED": "requested",
                "CREATE_IN_PROGRESS": "create in progress",
                "AVAILABLE": "available",
                "UPDATE_IN_PROGRESS": "update in progress",
                "CREATE_FAILED": "create failed",
                "DELETE_IN_PROGRESS": "delete in progress",
                "DELETE_FAILED": "delete failed",
                "DELETE_COMPLETED": "delete completed",
                "STOPPED": "stopped",
                "STOP_REQUESTED": "stop requested",
                "START_REQUESTED": "start requested",
                "STOP_IN_PROGRESS": "stop in progress",
                "START_IN_PROGRESS": "start in progress",
                "START_FAILED": "start failed",
                "STOP_FAILED": "stop failed",
                "BILLING_STARTED": "billing started",
                "BILLING_STOPPED": "billing stopped"
            }
        }


    }]);
