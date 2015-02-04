'use strict';

var log = log4javascript.getLogger("usageController-logger");

angular.module('uluwatuControllers').controller('usageController', ['$scope', '$rootScope', '$filter', 'UserUsages', 'AccountUsages',
    function ($scope, $rootScope, $filter, UserUsages, AccountUsages) {
        $scope.regions = [];

        $scope.clearFilter = function() {
            initFilter();
        }

        $scope.loadUsages = function () {
            initSums();
            var filterStartDate = $scope.usageFilter.startDate;
            var filterEndDate = new Date($scope.usageFilter.endDate.getTime());
            var param = createRequestParams(filterStartDate);
            var chartsData = { gcp: [], azure: [], aws: [], cloud: $scope.usageFilter.provider};

            populateChartsDataBySelectedPeriod(chartsData, filterStartDate, filterEndDate);

            if ($scope.$parent.user.admin != undefined && $scope.$parent.user.admin) {
              $scope.usages = AccountUsages.query({ param: param }, function(success) {
                processUsages(success, chartsData);
              });
            } else {
              $scope.usages = UserUsages.query({ param: param }, function(success) {
                processUsages(success, chartsData);
              });
            }
        }

        function createRequestParams(filterStartDate) {
          var param = "";
          var filterEndDate = new Date($scope.usageFilter.endDate.getTime());
          filterEndDate.setDate(filterEndDate.getDate() + 1);
          param =  param.concat(filterStartDate !== null ? "since=".concat(filterStartDate.getTime().toString().concat("&")) : "");
          param =  param.concat(filterEndDate !== null ? "filterenddate=".concat(filterEndDate.getTime().toString().concat("&")) : "");
          param =  param.concat($scope.usageFilter.user !== "all" ? "user=".concat($scope.usageFilter.user.concat("&")) : "");
          param = param.concat($scope.usageFilter.provider !== "all" ? "cloud=".concat($scope.usageFilter.provider.concat("&")) : "");
          param = param.concat($scope.usageFilter.region !== "all" ? "zone=".concat($scope.usageFilter.region.concat("&")) : "");
          if( param.substr(-1) === "&" ) {
            param = param.substring(0, param.length - 1);
          }
          return param;
        }

        function populateChartsDataBySelectedPeriod(chartsData, filterStartDate, filterEndDate){
          var usagesPerMonth;
          var startDay = new Date(filterStartDate.getFullYear(), filterStartDate.getMonth(), filterStartDate.getDate(), 0, 0, 0, 0);
          var endDay = new Date(filterEndDate.getFullYear(), filterEndDate.getMonth(), filterEndDate.getDate(), 0, 0, 0, 0);
          while(startDay <= endDay) {
            var shortDate = startDay.getFullYear()+'-'+(startDay.getMonth()+1)+'-'+startDay.getDate();
            chartsData.gcp.push({ 'date': startDay.getTime(), 'hours': parseInt(0)});
            chartsData.azure.push({ 'date': startDay.getTime(), 'hours': parseInt(0)});
            chartsData.aws.push({ 'date': startDay.getTime(), 'hours': parseInt(0)});
            startDay.setDate(startDay.getDate()+1);
          }
        }

        function processUsages(usages, chartsData) {
          angular.forEach(usages, function(item) {
            item.monthString = new Date(item.day).getFullYear() + "-" +  new Date(item.day).getMonth();
            item.monthDayString = new Date(item.day).toLocaleDateString();

            var calculatedCost;
            var usageByProvider;
            var chartsDataByProvider;
            if ($scope.gccFilterFunction(item)) {
              usageByProvider = $scope.gccSum;
              chartsDataByProvider = chartsData.gcp;
            } else if($scope.azureFilterFunction(item)) {
              usageByProvider = $scope.azureSum;
              chartsDataByProvider = chartsData.azure;
            } else if($scope.awsFilterFunction(item)) {
              usageByProvider = $scope.awsSum;
              chartsDataByProvider = chartsData.aws;
            }

            addUsageToChartsData(item, chartsDataByProvider);
            addUsageToSum(item, calculatedCost, usageByProvider)

          });
          $scope.orderUsagesBy('stackName', false);
          $scope.dataOfCharts = chartsData;
        }

        function addUsageToChartsData(item, chartsDataByProvider) {
          var actDay = new Date(item.day);
          var shortDate = new Date(actDay.getFullYear(), actDay.getMonth(), actDay.getDate(), 0, 0, 0, 0);
          var chartData = $filter('filter')(chartsDataByProvider, { 'date': shortDate.getTime()}, true);
          if (chartData.length > 0) {
            chartData[0].hours += item.instanceHours;
          }
        }

        function addUsageToSum(item, calculatedCost, usageByProvider) {
          usageByProvider.fullHours += parseFloat(item.instanceHours);
          var result = $filter('filter')(usageByProvider.items, {stackId: item.stackId}, true);
          if (result.length > 0) {
            result[0].instanceHours = result[0].instanceHours + item.instanceHours;
            result[0].instanceGroups.push({instanceType: item.instanceType, hours: item.instanceHours, name: item.hostGroup});
          } else {
            item.instanceGroups = [];
            item.instanceGroups.push({instanceType: item.instanceType, hours:  item.instanceHours, name: item.hostGroup});
            usageByProvider.items.push(item);
          }
        }

        $scope.gccFilterFunction = function(element) {
            try {
                return element.provider.match('GCC') ? true : false;
            } catch (err) {
                return false;
            }
        };

        $scope.awsFilterFunction = function(element) {
            try {
                return element.provider.match('AWS') ? true : false;
            }  catch (err) {
                return false;
            }
        };

        $scope.azureFilterFunction = function(element) {
            try {
                return element.provider.match('AZURE') ? true : false;
            } catch (err) {
                return false;
            }
        };

        $scope.selectRegionsByProvider = function() {
          $scope.regions = [];
          $scope.usageFilter.region = 'all';

          if ($scope.usageFilter.provider == 'AWS' || $scope.usageFilter.provider == 'all') {
            $rootScope.config.AWS.awsRegions.forEach(function(item) {$scope.regions.push(item);});
          }

          if ($scope.usageFilter.provider == 'AZURE' || $scope.usageFilter.provider == 'all') {
            $rootScope.config.AZURE.azureRegions.forEach(function(item) {$scope.regions.push(item);});
          }

          if ($scope.usageFilter.provider == 'GCC' || $scope.usageFilter.provider == 'all') {
            $rootScope.config.GCC.gccRegions.forEach(function(item) {$scope.regions.push(item);});
          }
        };

        $scope.selectProviderByRegion = function() {
            if($filter('filter')($rootScope.config.AWS.awsRegions, { key: $scope.usageFilter.region}).length === 1) {
                $scope.usageFilter.provider = 'AWS';
            } else if ($filter('filter')($rootScope.config.AZURE.azureRegions, { key: $scope.usageFilter.region}).length === 1) {
                $scope.usageFilter.provider = 'AZURE';
            } else if($filter('filter')($rootScope.config.GCC.gccRegions, { key: $scope.usageFilter.region}).length === 1) {
                $scope.usageFilter.provider = 'GCC';
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
          $scope.gccSum.items = $filter('orderBy')($scope.gccSum.items, predicate, reverse);
          $scope.azureSum.items = $filter('orderBy')($scope.azureSum.items, predicate, reverse);
          $scope.awsSum.items = $filter('orderBy')($scope.awsSum.items, predicate, reverse);
        }

        function initSums() {
            $scope.awsSum = {
                fullHours: 0,
                items: []
            };
            $scope.gccSum = {
                fullHours: 0,
                items: []
            };
            $scope.azureSum = {
                fullHours: 0,
                items: []
            };
        }

        function initFilter() {
          var endDate = new Date();
          var startDate = new Date();
          startDate.setMonth(startDate.getMonth()-1);
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
