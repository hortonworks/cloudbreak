'use strict';

var log = log4javascript.getLogger("usageController-logger");

angular.module('uluwatuControllers').controller('usageController', ['$scope', '$rootScope', '$filter', 'UserUsages',
    function ($scope, $rootScope, $filter, UserUsages) {

        initFilter();
        initSums();

        $scope.gccPrice = {
            'n1-standard-1': 0.045,
            'n1-standard-2': 0.089,
            'n1-standard-4': 0.177,
            'n1-standard-8': 0.353,
            'n1-standard-16': 0.706,
            'n1-highmem-2': 0.104,
            'n1-highmem-4': 0.208,
            'n1-highmem-8': 0.415,
            'n1-highmem-16': 0.829,
            'n1-highcpu-2': 0.056,
            'n1-highcpu-4': 0.112,
            'n1-highcpu-8': 0.224,
            'n1-highcpu-16': 0.448
        }

        $scope.azurePrice = {
            'SMALL': 0.018,
            'MEDIUM': 0.094,
            'LARGE': 0.188,
            'EXTRA_LARGE': 0.376
        }

        $scope.awsPrice = {
            'T2Micro': 0.013,
            'T2Small': 0.026,
            'T2Medium': 0.052,
            'M3Medium': 0.070,
            'M3Large': 0.140,
            'M3Xlarge': 0.280,
            'M32xlarge': 0.560
        }


        $scope.clearFilter = function() {
            initFilter();
        }

        $scope.loadUsages = function () {

            var param = "";
            param =  param.concat($scope.localFilter.since !== null ? "since=".concat(new Date($scope.localFilter.since).getTime().toString().concat("&")) : "");
            param =  param.concat($scope.localFilter.user !== "" ? "user=".concat($scope.localFilter.user.concat("&")) : "");
            param = param.concat($scope.localFilter.account !== "" ? "account=".concat($scope.localFilter.account.concat("&")) : "");
            param = param.concat($scope.localFilter.cloud !== "all" ? "cloud=".concat($scope.localFilter.cloud.concat("&")) : "");
            param = param.concat($scope.localFilter.zone !== "all" ? "zone=".concat($scope.localFilter.zone.concat("&")) : "");
            param = param.concat($scope.localFilter.vmtype !== "any" ? "vmtype=".concat($scope.localFilter.vmtype.concat("&")) : "");
            param = param.concat($scope.localFilter.hours !== "" ? "hours=".concat($scope.localFilter.hours.concat("&")) : "");
            if( param.substr(-1) === "&" ) {
                param = param.substring(0, param.length - 1);
            }

            $scope.usages = UserUsages.query({ param: param }, function(success) {
                initSums();
                angular.forEach(success, function(item) {
                    if ($scope.gccFilterFunction(item)) {
                        $scope.gccSum.fullMoney += parseFloat(item.instanceHours) * parseFloat($scope.gccPrice[item.machineType]);
                        $scope.gccSum.fullHours += parseFloat(item.instanceHours);
                        item.money = (parseFloat(item.instanceHours) * parseFloat($scope.gccPrice[item.machineType])).toFixed(2);
                    } else if($scope.azureFilterFunction(item)) {
                        $scope.azureSum.fullMoney += parseFloat(item.instanceHours) * parseFloat($scope.azurePrice[item.machineType]);
                        $scope.azureSum.fullHours += parseFloat(item.instanceHours);
                        item.money = (parseFloat(item.instanceHours) * parseFloat($scope.azurePrice[item.machineType])).toFixed(2);
                    } else if($scope.awsFilterFunction(item)) {
                        $scope.awsSum.fullMoney += parseFloat(item.instanceHours) * parseFloat($scope.awsPrice[item.machineType]);
                        $scope.awsSum.fullHours += parseFloat(item.instanceHours);
                        item.money = (parseFloat(item.instanceHours) * parseFloat($scope.awsPrice[item.machineType])).toFixed(2);
                    }
                });
                $scope.gccSum.fullMoney = $scope.gccSum.fullMoney.toFixed(2);
                $scope.azureSum.fullMoney =  $scope.azureSum.fullMoney.toFixed(2);
                $scope.awsSum.fullMoney =  $scope.awsSum.fullMoney.toFixed(2);
            });
        }

        $scope.gccFilterFunction = function(element) {
            try {
                return element.cloud.match('GCC') ? true : false;
            } catch (err) {
                return false;
            }
        };

        $scope.awsFilterFunction = function(element) {
            try {
                return element.cloud.match('AWS') ? true : false;
            }  catch (err) {
                return false;
            }
        };

        $scope.azureFilterFunction = function(element) {
            try {
                return element.cloud.match('AZURE') ? true : false;
            } catch (err) {
                return false;
            }
        };

        $scope.setDate = function(date) {
            $scope.localFilter.since = new Date(date);
        }

        function initSums() {
            $scope.awsSum={
                fullMoney: 0,
                fullHours: 0
            };
            $scope.gccSum={
                fullMoney: 0,
                fullHours: 0
            };
            $scope.azureSum={
                fullMoney: 0,
                fullHours: 0
            };
        }

        function initFilter() {
            $scope.localFilter = {
                since: null,
                user: "",
                account: "",
                cloud: "all",
                zone: "all",
                vmtype: "any",
                hours: ""
            };
        }

    }
]);
