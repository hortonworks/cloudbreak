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
                    {key: 'US_CENTRAL1_A', value: "us-central1-a", cloud: 'GCC'},
                    {key: 'US_CENTRAL1_B', value: "us-central1-b", cloud: 'GCC'},
                    {key: 'US_CENTRAL1_F', value: "us-central1-f", cloud: 'GCC'},
                    {key: 'EUROPE_WEST1_B', value: "europe-west1-b", cloud: 'GCC'},
                    {key: 'ASIA_EAST1_A', value: "asia-east1-a", cloud: 'GCC'},
                    {key: 'ASIA_EAST1_B', value: "asia-east1-b", cloud: 'GCC'}
                ],
                gccDiskTypes: [
                    {key: 'HDD', value: 'Magnetic'},
                    {key: 'SSD', value: 'SSD'}
                ],
                gccInstanceTypes: [
                    {key: 'N1_STANDARD_1', value: 'n1-standard-1', cloud: 'GCC'},
                    {key: 'N1_STANDARD_2', value:'n1-standard-2', cloud: 'GCC'},
                    {key: 'N1_STANDARD_4', value:'n1-standard-4', cloud: 'GCC'},
                    {key: 'N1_STANDARD_8', value:'n1-standard-8', cloud: 'GCC'},
                    {key: 'N1_STANDARD_16', value:'n1-standard-16', cloud: 'GCC'},
                    {key: 'N1_HIGHMEM_2', value:'n1-highmem-2', cloud: 'GCC'},
                    {key: 'N1_HIGHMEM_4', value:'n1-highmem-4', cloud: 'GCC'},
                    {key: 'N1_HIGHMEM_8', value:'n1-highmem-8', cloud: 'GCC'},
                    {key: 'N1_HIGHMEM_16', value:'n1-highmem-16', cloud: 'GCC'},
                    {key: 'N1_HIGHCPU_2', value:'n1-highcpu-2', cloud: 'GCC'},
                    {key: 'N1_HIGHCPU_4', value:'n1-highcpu-4', cloud: 'GCC'},
                    {key: 'N1_HIGHCPU_8', value:'n1-highcpu-8', cloud: 'GCC'},
                    {key: 'N1_HIGHCPU_16', value:'n1-highcpu-16', cloud: 'GCC'}
                ]
            },
            'AZURE': {
                azureRegions: [
                    {key: 'BRAZIL_SOUTH', value: 'Brazil South', cloud: 'AZURE'},
                    {key: 'EAST_ASIA', value: 'East Asia', cloud: 'AZURE'},
                    {key: 'EAST_US', value: 'East US', cloud: 'AZURE'},
                    {key: 'NORTH_EUROPE', value: 'North Europe', cloud: 'AZURE'},
                    {key: 'WEST_US', value: 'West US', cloud: 'AZURE'},
                    {key: 'WEST_EUROPE', value: 'West EU', cloud: 'AZURE'}
                ],
                azureVmTypes: [
                    {key: 'SMALL', value: 'Small', cloud: 'AZURE'},
                    {key: 'MEDIUM', value: 'Medium', cloud: 'AZURE'},
                    {key: 'LARGE', value: 'Large', cloud: 'AZURE'},
                    {key: 'EXTRA_LARGE', value: 'Extra Large', cloud: 'AZURE'}
                ]
            },
            'AWS': {
                volumeTypes: [
                    {key: 'Gp2', value: 'SSD'},
                    {key: 'Standard', value: 'Magnetic'}
                ],
                awsRegions : [
                    {key: 'US_EAST_1', value: 'US East(N. Virginia)', cloud: 'AWS'},
                    {key: 'US_WEST_1', value: 'US West (N. California)', cloud: 'AWS'},
                    {key: 'US_WEST_2', value: 'US West (Oregon)', cloud: 'AWS'},
                    {key: 'EU_WEST_1', value: 'EU (Ireland)', cloud: 'AWS'},
                    {key: 'AP_SOUTHEAST_1', value: 'Asia Pacific (Singapore)', cloud: 'AWS'},
                    {key: 'AP_SOUTHEAST_2', value: 'Asia Pacific (Sydney)', cloud: 'AWS'},
                    {key: 'AP_NORTHEAST_1', value: 'Asia Pacific (Tokyo)', cloud: 'AWS'},
                    {key: 'SA_EAST_1', value: 'South America (São Paulo)', cloud: 'AWS'}
                ],
                amis: [
                    {key: 'US_EAST_1', value: 'ami-d06e0eb8'},
                    {key: 'US_WEST_1', value: 'ami-8dccdec8'},
                    {key: 'US_WEST_2', value: 'ami-f9a6f7c9'},
                    {key: 'EU_WEST_1', value: 'ami-9856e8ef'},
                    {key: 'AP_SOUTHEAST_1', value: 'ami-cb496699'},
                    {key: 'AP_SOUTHEAST_2', value: 'ami-b392fa89'},
                    {key: 'AP_NORTHEAST_1', value: 'ami-1c17181d'},
                    {key: 'SA_EAST_1', value: 'ami-b101b1ac'}
                ],
                instanceType: [
                    {key: 'T2Micro', value: 'T2Micro', cloud: 'AWS'},
                    {key: 'T2Small', value: 'T2Small', cloud: 'AWS'},
                    {key: 'T2Medium', value: 'T2Medium', cloud: 'AWS'},
                    {key: 'M3Medium', value: 'M3Medium', cloud: 'AWS'},
                    {key: 'M3Large', value: 'M3Large', cloud: 'AWS'},
                    {key: 'M3Xlarge', value: 'M3Xlarge', cloud: 'AWS'},
                    {key: 'M32xlarge', value: 'M32xlarge', cloud: 'AWS'},
                    {key: 'C3large', value: 'C3large', cloud: 'AWS'},
                    {key: 'C3xlarge', value: 'C3xlarge', cloud: 'AWS'},
                    {key: 'C32xlarge', value: 'C32xlarge', cloud: 'AWS'},
                    {key: 'C34xlarge', value: 'C34xlarge', cloud: 'AWS'},
                    {key: 'C38xlarge', value: 'C38xlarge', cloud: 'AWS'}
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
            },
            'EVENT_CLASS': {
                "REQUESTED": "has-warning",
                "CREATE_IN_PROGRESS": "has-warning",
                "AVAILABLE": "has-success",
                "UPDATE_IN_PROGRESS": "has-warning",
                "CREATE_FAILED": "has-error",
                "DELETE_IN_PROGRESS": "has-warning",
                "DELETE_FAILED": "has-error",
                "DELETE_COMPLETED": "has-success",
                "STOPPED": "has-success",
                "STOP_REQUESTED": "has-warning",
                "START_REQUESTED": "has-warning",
                "STOP_IN_PROGRESS": "has-warning",
                "START_IN_PROGRESS": "has-warning",
                "START_FAILED": "has-error",
                "STOP_FAILED": "has-error",
                "BILLING_STARTED": "has-success",
                "BILLING_STOPPED": "has-success"
            },
            'TIME_ZONES': [
              {key: 'Etc/GMT+1', value: 'GMT-1'},
              {key: 'Etc/GMT+2', value: 'GMT-2'},
              {key: 'Etc/GMT+3', value: 'GMT-3'},
              {key: 'Etc/GMT+4', value: 'GMT-4'},
              {key: 'Etc/GMT+5', value: 'GMT-5'},
              {key: 'Etc/GMT+6', value: 'GMT-6'},
              {key: 'Etc/GMT+7', value: 'GMT-7'},
              {key: 'Etc/GMT+8', value: 'GMT-8'},
              {key: 'Etc/GMT+9', value: 'GMT-9'},
              {key: 'Etc/GMT+10', value: 'GMT-10'},
              {key: 'Etc/GMT+11', value: 'GMT-11'},
              {key: 'Etc/GMT+12', value: 'GMT-12'},
              {key: 'Etc/GMT', value: 'GMT/UTC'},
              {key: 'Etc/GMT-1', value: 'GMT+1'},
              {key: 'Etc/GMT-2', value: 'GMT+2'},
              {key: 'Etc/GMT-3', value: 'GMT+3'},
              {key: 'Etc/GMT-4', value: 'GMT+4'},
              {key: 'Etc/GMT-5', value: 'GMT+5'},
              {key: 'Etc/GMT-6', value: 'GMT+6'},
              {key: 'Etc/GMT-7', value: 'GMT+7'},
              {key: 'Etc/GMT-8', value: 'GMT+8'},
              {key: 'Etc/GMT-9', value: 'GMT+9'},
              {key: 'Etc/GMT-10', value: 'GMT+10'},
              {key: 'Etc/GMT-11', value: 'GMT+11'},
              {key: 'Etc/GMT-12', value: 'GMT+12'}
            ]
        }


    }]);
