'use strict';

var log = log4javascript.getLogger("usageController-logger");

angular.module('uluwatuControllers').controller('usageController', ['$scope', '$rootScope', '$filter', 'UserUsages', 'AccountUsages',
    function ($scope, $rootScope, $filter, UserUsages, AccountUsages) {
        $scope.regions = [];

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


        //requested interval grouped by months
        // var month = new Array();
        // month[0] = "January";
        // month[1] = "February";
        // month[2] = "March";
        // month[3] = "April";
        // month[4] = "May";
        // month[5] = "June";
        // month[6] = "July";
        // month[7] = "August";
        // month[8] = "September";
        // month[9] = "October";
        // month[10] = "November";
        // month[11] = "December";
        //requested interval grouped by months

        $scope.clearFilter = function() {
            initFilter();
        }

        $scope.loadUsages = function () {
            var usagesSince = $scope.usageFilter.startDate;
            var param = createRequestParams(usagesSince);

            //requested interval grouped by months
            // var usagesPerMonth;
            // var date = new Date();
            // var targetMonth = new Date(usagesSince.getFullYear(), usagesSince.getMonth(), 1, 0, 0, 0, 0);
            // var actMonth = new Date(date.getFullYear(), date.getMonth(), 1, 0, 0, 0, 0);
            // console.log('target date(since): '+usagesSince)
            // while(targetMonth <= actMonth) {
            //   console.log('year:' + actMonth.getFullYear() + 'month:' + month[actMonth.getMonth()]);
            //   actMonth.setMonth(actMonth.getMonth()-1);
            // }
            //requested interval grouped by months

            $scope.usages = AccountUsages.query({ param: param }, function(success) {
                initSums();
                console.log(success);
                var usagesByMonth = new Array();
                angular.forEach(success, function(item) {
                    item.monthString = new Date(item.day).getFullYear() + "-" +  new Date(item.day).getMonth();
                    item.monthDayString = new Date(item.day).toLocaleDateString();

                    var calculatedCost;
                    var usageByProvider;
                    if ($scope.gccFilterFunction(item)) {
                        calculatedCost = parseFloat(item.instanceHours) * parseFloat($scope.gccPrice[item.machineType]);
                        usageByProvider = $scope.gccSum;
                    } else if($scope.azureFilterFunction(item)) {
                        calculatedCost = parseFloat(item.instanceHours) * parseFloat($scope.azurePrice[item.machineType]);
                        usageByProvider = $scope.azureSum;
                    } else if($scope.awsFilterFunction(item)) {
                        calculatedCost = parseFloat(item.instanceHours) * parseFloat($scope.awsPrice[item.machineType]);
                        usageByProvider = $scope.awsSum;
                    }

                    usageByProvider.fullHours += parseFloat(item.instanceHours);
                    var newFullMoney = parseFloat(usageByProvider.fullMoney) + parseFloat(calculatedCost);
                    usageByProvider.fullMoney = parseFloat(newFullMoney).toFixed(2);
                    item.money = parseFloat(calculatedCost).toFixed(2);
                    var result = $filter('filter')(usageByProvider.items, {stackId: item.stackId}, true);
                    if (result.length > 0) {
                      result[0].money = (parseFloat(result[0].money) + parseFloat(calculatedCost)).toFixed(2);
                      result[0].instanceHours = result[0].instanceHours + item.instanceHours;
                    } else {
                      usageByProvider.items.push(item);
                    }

                });
            });
        }

        function createRequestParams(usagesSince) {
          var param = "";
          var filterEndDate = new Date($scope.usageFilter.endDate.getTime());
          filterEndDate.setDate(filterEndDate.getDate() + 1);
          param =  param.concat(usagesSince !== null ? "since=".concat(usagesSince.getTime().toString().concat("&")) : "");
          param =  param.concat(filterEndDate !== null ? "filterenddate=".concat(filterEndDate.getTime().toString().concat("&")) : "");
          param =  param.concat($scope.usageFilter.user !== "all" ? "user=".concat($scope.usageFilter.user.concat("&")) : "");
          param = param.concat($scope.usageFilter.cloud !== "all" ? "cloud=".concat($scope.usageFilter.cloud.concat("&")) : "");
          param = param.concat($scope.usageFilter.zone !== "all" ? "zone=".concat($scope.usageFilter.zone.concat("&")) : "");
          if( param.substr(-1) === "&" ) {
            param = param.substring(0, param.length - 1);
          }
          return param;
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

        $scope.selectRegionsByProvider = function() {
          $scope.regions = [];
          $scope.usageFilter.zone = 'all';

          if ($scope.usageFilter.cloud == 'AWS' || $scope.usageFilter.cloud == 'all') {
            $rootScope.config.AWS.awsRegions.forEach(function(item) {$scope.regions.push(item);});
          }

          if ($scope.usageFilter.cloud == 'AZURE' || $scope.usageFilter.cloud == 'all') {
            $rootScope.config.AZURE.azureRegions.forEach(function(item) {$scope.regions.push(item);});
          }

          if ($scope.usageFilter.cloud == 'GCC' || $scope.usageFilter.cloud == 'all') {
            $rootScope.config.GCC.gccRegions.forEach(function(item) {$scope.regions.push(item);});
          }
        };

        $scope.selectProviderByRegion = function() {
            if($filter('filter')($rootScope.config.AWS.awsRegions, { key: $scope.usageFilter.zone}).length === 1) {
                $scope.usageFilter.cloud = 'AWS';
            } else if ($filter('filter')($rootScope.config.AZURE.azureRegions, { key: $scope.usageFilter.zone}).length === 1) {
                $scope.usageFilter.cloud = 'AZURE';
            } else if($filter('filter')($rootScope.config.GCC.gccRegions, { key: $scope.usageFilter.zone}).length === 1) {
                $scope.usageFilter.cloud = 'GCC';
            }
        }

        $scope.setStartDate = function(dateString) {
            // console.log('startdate'+date)
            $scope.usageFilter.startDate = floorToDay(dateString);
            $scope.$apply();
        }

        $scope.setEndDate = function(dateString) {
          // console.log('enddate'+date)
          $scope.usageFilter.endDate = floorToDay(dateString);
          $scope.$apply();
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
          var endDate = new Date();
          var startDate = new Date();
          startDate.setMonth(startDate.getMonth()-1);
          $scope.usageFilter = {
              startDate: floorToDay(startDate),
              endDate: floorToDay(endDate),
              user: "all",
              cloud: "all",
              zone: "all"
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
