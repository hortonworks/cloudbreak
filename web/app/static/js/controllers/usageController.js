'use strict';

var log = log4javascript.getLogger("usageController-logger");

angular.module('uluwatuControllers').controller('usageController', ['$scope', '$rootScope', '$filter', 'UserUsages', 'AccountUsages',
    function($scope, $rootScope, $filter, UserUsages, AccountUsages) {
        $scope.regions = [];

        $scope.clearFilter = function() {
            initFilter();
        }

        $scope.loadUsages = function() {
            initSums();
            var filterStartDate = $scope.usageFilter.startDate;
            var filterEndDate = new Date($scope.usageFilter.endDate.getTime());
            var param = createRequestParamObj(filterStartDate);
            var chartsData = {
                gcp: [],
                azure: [],
                aws: [],
                openstack: [],
                cloud: $scope.usageFilter.provider
            };

            populateChartsDataBySelectedPeriod(chartsData, filterStartDate, filterEndDate);

            if ($scope.$parent.user.admin != undefined && $scope.$parent.user.admin) {
                $scope.usages = AccountUsages.query(param, function(success) {
                    processUsages(success, chartsData);
                });
            } else {
                $scope.usages = UserUsages.query(param, function(success) {
                    processUsages(success, chartsData);
                });
            }
        }

        function createRequestParamObj(filterStartDate) {
            var param = {};
            var filterEndDate = new Date($scope.usageFilter.endDate.getTime());
            filterEndDate.setDate(filterEndDate.getDate() + 1);

            if (filterStartDate !== null) {
                param.since = filterStartDate.getTime().toString();
            }
            if (filterEndDate !== null) {
                param.filterenddate = filterEndDate.getTime().toString();
            }
            if ($scope.usageFilter.user !== "all") {
                param.user = $scope.usageFilter.user;
            }
            if ($scope.usageFilter.provider !== "all") {
                param.cloud = $scope.usageFilter.provider;
            }
            if ($scope.usageFilter.region !== "all") {
                param.zone = $scope.usageFilter.region;
            }
            return param;
        }

        function populateChartsDataBySelectedPeriod(chartsData, filterStartDate, filterEndDate) {
            var usagesPerMonth;
            var startDay = new Date(filterStartDate.getFullYear(), filterStartDate.getMonth(), filterStartDate.getDate(), 0, 0, 0, 0);
            var endDay = new Date(filterEndDate.getFullYear(), filterEndDate.getMonth(), filterEndDate.getDate(), 0, 0, 0, 0);
            while (startDay <= endDay) {
                var shortDate = startDay.getFullYear() + '-' + (startDay.getMonth() + 1) + '-' + startDay.getDate();
                chartsData.gcp.push({
                    'date': startDay.getTime(),
                    'hours': parseInt(0)
                });
                chartsData.azure.push({
                    'date': startDay.getTime(),
                    'hours': parseInt(0)
                });
                chartsData.aws.push({
                    'date': startDay.getTime(),
                    'hours': parseInt(0)
                });
                chartsData.openstack.push({
                    'date': startDay.getTime(),
                    'hours': parseInt(0)
                });
                startDay.setDate(startDay.getDate() + 1);
            }
        }

        function processUsages(usages, chartsData) {
            angular.forEach(usages, function(item) {
                item.monthString = new Date(item.day).getFullYear() + "-" + new Date(item.day).getMonth();
                item.monthDayString = new Date(item.day).toLocaleDateString();

                var calculatedCost;
                var usageByProvider;
                var chartsDataByProvider;
                if ($scope.elementProviderEquals(item, 'GCP')) {
                    usageByProvider = $scope.gcpSum;
                    chartsDataByProvider = chartsData.gcp;
                } else if ($scope.elementProviderEquals(item, 'AZURE')) {
                    usageByProvider = $scope.azureSum;
                    chartsDataByProvider = chartsData.azure;
                } else if ($scope.elementProviderEquals(item, 'AWS')) {
                    usageByProvider = $scope.awsSum;
                    chartsDataByProvider = chartsData.aws;
                } else if ($scope.elementProviderEquals(item, 'OPENSTACK')) {
                    usageByProvider = $scope.openstackSum;
                    chartsDataByProvider = chartsData.openstack;
                }

                if (chartsDataByProvider != undefined && usageByProvider != undefined) {
                    addUsageToChartsData(item, chartsDataByProvider);
                    addUsageToSum(item, calculatedCost, usageByProvider)
                }

            });
            $scope.orderUsagesBy('stackName', false);
            $scope.dataOfCharts = chartsData;
        }

        function addUsageToChartsData(item, chartsDataByProvider) {
            var actDay = new Date(item.day);
            var shortDate = new Date(actDay.getFullYear(), actDay.getMonth(), actDay.getDate(), 0, 0, 0, 0);
            var chartData = $filter('filter')(chartsDataByProvider, {
                'date': shortDate.getTime()
            }, true);
            if (chartData != undefined && chartData.length > 0) {
                chartData[0].hours += item.instanceHours;
            }
        }

        function addUsageToSum(item, calculatedCost, usageByProvider) {
            usageByProvider.fullHours += parseFloat(item.instanceHours);
            var result = $filter('filter')(usageByProvider.items, {
                stackId: item.stackId
            }, true);
            if (result != undefined && result.length > 0) {
                result[0].instanceHours = result[0].instanceHours + item.instanceHours;
                var summedGroups = $filter('filter')(result[0].instanceGroups, {
                    name: item.instanceGroup
                }, true);
                if (summedGroups.length > 0) {
                    summedGroups[0].hours = summedGroups[0].hours + item.instanceHours;
                } else {
                    result[0].instanceGroups.push({
                        instanceType: item.instanceType,
                        hours: item.instanceHours,
                        name: item.instanceGroup
                    });
                }
            } else {
                item.instanceGroups = [];
                item.instanceGroups.push({
                    instanceType: item.instanceType,
                    hours: item.instanceHours,
                    name: item.instanceGroup
                });
                usageByProvider.items.push(item);
            }
        }

        $scope.elementProviderEquals = function(element, provider) {
            try {
                return element.provider.match(provider) ? true : false;
            } catch (err) {
                return false;
            }
        };

        $scope.selectRegionsByProvider = function() {
            $scope.regions = [];
            $scope.usageFilter.region = 'all';

            if ($scope.usageFilter.provider == 'AWS' || $scope.usageFilter.provider == 'all') {
                $rootScope.params.regions.AWS.forEach(function(item) {
                    $scope.regions.push(item);
                });
            }

            if ($scope.usageFilter.provider == 'AZURE' || $scope.usageFilter.provider == 'all') {
                $rootScope.params.regions.AZURE.forEach(function(item) {
                    $scope.regions.push(item);
                });
            }

            if ($scope.usageFilter.provider == 'GCP' || $scope.usageFilter.provider == 'all') {
                $rootScope.params.regions.GCP.forEach(function(item) {
                    $scope.regions.push(item);
                });
            }
        };

        $scope.selectProviderByRegion = function() {
            if ($filter('filter')($rootScope.params.regions.AWS, $scope.usageFilter.region).length === 1) {
                $scope.usageFilter.provider = 'AWS';
            } else if ($filter('filter')($rootScope.params.regions.AZURE, $scope.usageFilter.region).length === 1) {
                $scope.usageFilter.provider = 'AZURE';
            } else if ($filter('filter')($rootScope.params.regions.GCP, $scope.usageFilter.region).length === 1) {
                $scope.usageFilter.provider = 'GCP';
            }
        }

        $scope.setStartDate = function(dateString) {
            $scope.usageFilter.startDate = floorToDay(dateString);
            $scope.$digest();
        }

        $scope.setEndDate = function(dateString) {
            $scope.usageFilter.endDate = floorToDay(dateString);
            $scope.$digest();
        }

        $scope.orderUsagesBy = function(predicate, reverse) {
            $scope.gcpSum.items = $filter('orderBy')($scope.gcpSum.items, predicate, reverse);
            $scope.azureSum.items = $filter('orderBy')($scope.azureSum.items, predicate, reverse);
            $scope.awsSum.items = $filter('orderBy')($scope.awsSum.items, predicate, reverse);
        }

        function initSums() {
            $scope.awsSum = {
                fullHours: 0,
                items: []
            };
            $scope.gcpSum = {
                fullHours: 0,
                items: []
            };
            $scope.azureSum = {
                fullHours: 0,
                items: []
            };
            $scope.openstackSum = {
                fullHours: 0,
                items: []
            };
        }

        function initFilter() {
            var endDate = new Date();
            var startDate = new Date();
            startDate.setMonth(startDate.getMonth() - 1);
            $scope.usageFilter = {
                startDate: floorToDay(startDate),
                endDate: floorToDay(endDate),
                user: "all",
                provider: "all",
                region: "all"
            };
            $scope.selectRegionsByProvider();
        }

        function floorToDay(dateString) {
            var date = new Date(dateString);
            return new Date(date.getFullYear(), date.getMonth(), date.getDate(), 0, 0, 0, 0);
        }

        initFilter();
        initSums();
    }
]);