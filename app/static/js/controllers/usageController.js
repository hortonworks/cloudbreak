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
                    item.monthString = new Date(item.day).getFullYear() + "-" +  new Date(item.day).getMonth();
                    item.monthDayString = new Date(item.day).toLocaleDateString();
                    if ($scope.gccFilterFunction(item)) {
                        var m = (parseFloat(item.instanceHours) * parseFloat($scope.gccPrice[item.machineType]));
                        $scope.gccSum.fullMoney += parseFloat(m);
                        $scope.gccSum.fullHours += parseFloat(item.instanceHours);
                        item.money = parseFloat(m).toFixed(2);
                        var result = $filter('filter')($scope.gccSum.items, {stackId: item.stackId}, true);
                        if (result.length > 0) {
                            var index = $scope.gccSum.items.indexOf(result[0]);
                            $scope.gccSum.items[index].money = (parseFloat(result[0].money) + parseFloat(m)).toFixed(2);
                        } else {
                            var newItem = item;
                            newItem.money = parseFloat(m).toFixed(2);
                            $scope.gccSum.items.push(newItem);
                        }
                    } else if($scope.azureFilterFunction(item)) {
                        var m = (parseFloat(item.instanceHours) * parseFloat($scope.azurePrice[item.machineType]));
                        $scope.azureSum.fullMoney += parseFloat(m);
                        $scope.azureSum.fullHours += parseFloat(item.instanceHours);
                        item.money = parseFloat(m).toFixed(2);
                        var result = $filter('filter')($scope.azureSum.items, {stackId: item.stackId}, true);
                        if (result.length > 0) {
                            var index = $scope.azureSum.items.indexOf(result[0]);
                            $scope.azureSum.items[index].money = (parseFloat(result[0].money) + parseFloat(m)).toFixed(2);
                        } else {
                            var newItem = item;
                            newItem.money = parseFloat(m).toFixed(2);
                            $scope.azureSum.items.push(newItem);
                        }
                    } else if($scope.awsFilterFunction(item)) {
                        var m = (parseFloat(item.instanceHours) * parseFloat($scope.awsPrice[item.machineType]));
                        $scope.awsSum.fullMoney += parseFloat(m);
                        $scope.awsSum.fullHours += parseFloat(item.instanceHours);
                        item.money = parseFloat(m).toFixed(2);
                        var result = $filter('filter')($scope.awsSum.items, {stackId: item.stackId}, true);
                        if (result.length > 0) {
                            var index = $scope.awsSum.items.indexOf(result[0]);
                            $scope.awsSum.items[index].money = (parseFloat(result[0].money) + parseFloat(m)).toFixed(2);
                        } else {
                            var newItem = item;
                            newItem.money = parseFloat(m).toFixed(2);
                            $scope.awsSum.items.push(newItem);
                        }
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

        $scope.cloudShowFunction = function(cloud) {
            try {
                return ($scope.localFilter.cloud == cloud || $scope.localFilter.cloud == 'all');
            } catch (err) {
                return false;
            }
        };

        $scope.selectedRegion =function() {
            if($filter('filter')($rootScope.config.AWS.awsRegions, { key: $scope.localFilter.zone}).length === 1) {
                $scope.localFilter.cloud = 'AWS';
            } else if ($filter('filter')($rootScope.config.AZURE.azureRegions, { key: $scope.localFilter.zone}).length === 1) {
                $scope.localFilter.cloud = 'AZURE';
            } else if($filter('filter')($rootScope.config.GCC.gccRegions, { key: $scope.localFilter.zone}).length === 1) {
                $scope.localFilter.cloud = 'GCC';
            }
        }

        $scope.selectedInstance =function() {
            if($filter('filter')($rootScope.config.AWS.instanceType, { key: $scope.localFilter.vmtype}).length === 1) {
                $scope.localFilter.cloud = 'AWS';
            } else if ($filter('filter')($rootScope.config.AZURE.azureVmTypes, { key: $scope.localFilter.vmtype}).length === 1) {
                $scope.localFilter.cloud = 'AZURE';
            } else if($filter('filter')($rootScope.config.GCC.gccInstanceTypes, { key: $scope.localFilter.vmtype}).length === 1) {
                $scope.localFilter.cloud = 'GCC';
            }
        }

        $scope.setDate = function(date) {
            $scope.localFilter.since = new Date(date);
        }

        function initSums() {
            $scope.awsSum={
                fullMoney: 0,
                fullHours: 0,
                items: []
            };
            $scope.gccSum={
                fullMoney: 0,
                fullHours: 0,
                items: []
            };
            $scope.azureSum={
                fullMoney: 0,
                fullHours: 0,
                items: []
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
