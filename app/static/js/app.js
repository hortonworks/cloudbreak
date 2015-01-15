'use strict';

/* App Module */

var cloudbreakApp = angular.module('cloudbreakApp', ['ngRoute', 'base64', 'blockUI', 'ui.bootstrap', 'uluwatuControllers', 'uluwatuServices']);


cloudbreakApp.directive('match', function($parse) {
  return {
    require: 'ngModel',
    link: function(scope, elem, attrs, ctrl) {
      scope.$watch(function() {
        return $parse(attrs.match)(scope) === ctrl.$modelValue;
      }, function(currentValue) {
        ctrl.$setValidity('mismatch', currentValue);
      });
    }
  };
});

cloudbreakApp.directive('validjson', function($parse) {
    return {
        require: 'ngModel',
        link: function(scope, elem, attrs, ctrl) {
            ctrl.$parsers.unshift(function(viewValue) {
                var valid = false;
                var json = {};
                try {
                    json = JSON.parse(viewValue);
                    valid = true;
                } catch (err){}
                ctrl.$setValidity('validjson', valid);
                return valid ? json : undefined;
            });
        }
    };
});

cloudbreakApp.directive('file', function(){
    return {
        scope: {
            file: '='
        },
        link: function(scope, el, attrs){
             el.bind('change', function(event){
                scope.file = event.target.files[0];
                scope.$apply();
            });
        }
    };
});


cloudbreakApp.config([ '$routeProvider', '$locationProvider', function($routeProvider, $locationProvider) {
    $routeProvider.when('/', {
        templateUrl: 'partials/dashboard.html',
        controller: 'uluwatuController'
    }) .otherwise({
        redirectTo : '/'
    });
    }]).factory('authHttpResponseInterceptor',['$q', '$window', function ($q, $window) {
         return {
             response: function(response){
                 if (response.status === 401){
                    $window.location.href = '/logout';
                 }
                 return response || $q.when(response);
             },
             responseError: function(response) {
                 if (response.status === 401){
                     $window.location.href = '/logout';
                 }
                 return $q.reject(response);
             }
         }
     }]).config(['$httpProvider',function($httpProvider) {
         $httpProvider.interceptors.push('authHttpResponseInterceptor');
     }]).config(function(blockUIConfig) {
          blockUIConfig.autoInjectBodyBlock = false
          blockUIConfig.requestFilter = function(config) {
            var block = false
            if (config.url.match(/^(.*)templates($|\/).*/) || config.url.match(/^(.*)blueprints($|\/).*/)
            || config.url.match(/^(.*)credentials($|\/).*/) || config.url.match(/^periscope\/clusters($|\/).*/)
            || (config.url.match(/^stacks\/(\d+)\/cluster($|\/).*/) && config.method == 'POST')){
                block = true
            } else if (config.url.match(/^(.*)usages($|\?).*/) && config.method == 'GET') {
                block = true;
            } else if (config.url.match(/^(.*)users($|\?).*/) || config.url.match(/^(.*)permission($|\?).*/)){
                block = true;
            }
            if (!block) {
                return block
            }
          };
     });

cloudbreakApp.run(function ($rootScope, $http) {
    $http.get('messages.properties').then(function (messages) {
        $rootScope.error_msg = messages.data
        $rootScope.titleStatus = {
            "REQUESTED": $rootScope.error_msg.title_requested,
            "CREATE_IN_PROGRESS": $rootScope.error_msg.title_create_in_progress,
            "UPDATE_IN_PROGRESS": $rootScope.error_msg.title_update_in_progress,
            "AVAILABLE": $rootScope.error_msg.title_create_completed,
            "CREATE_FAILED": $rootScope.error_msg.title_create_failed,
            "DELETE_IN_PROGRESS": $rootScope.error_msg.title_delete_in_progress,
            "DELETE_COMPLETED": $rootScope.error_msg.title_delete_completed
        }
    });
});

cloudbreakApp.directive('startdatevalidation', function($parse) {
  return {
    require: 'ngModel',
    link: function(scope, elem, attrs, ctrl) {
      scope.$watch(function() {
        return $parse(attrs.startdatevalidation)(scope.usageFilter) >= ctrl.$modelValue;
      }, function(currentValue) {
        ctrl.$setValidity('startDateInvalid', currentValue);
      });
    }
  };
});

cloudbreakApp.directive('enddatevalidation', function($parse) {
  return {
    require: 'ngModel',
    link: function(scope, elem, attrs, ctrl) {
      scope.$watch(function() {
        return (new Date) > ctrl.$modelValue;
      }, function(currentValue) {
        ctrl.$setValidity('endDateInvalid', currentValue);
      });
    }
  };
});

cloudbreakApp.directive('usagecharts', function() {
  return {
    restrict: 'A',
    link: function(scope, element) {

      function createChart(title, data, targetDivId) {
        MG.data_graphic({
          title: title,
          data: data,
          chart_type: 'line',
          target: '#' + targetDivId,
          x_accessor: "date",
          y_accessor: "hours",
          width: 400,
          interpolate: 'linear'
        });
      }

      function createUnitedChart(data) {
        MG.data_graphic({
          title: 'United Chart of running hours',
          data: data,
          chart_type: 'line',
          target: '#unitedChart',
          x_accessor: 'date',
          y_accessor: 'hours',
          interpolate: 'linear',
          legend: ['GCP','AZURE','AWS'],
          legend_target: '.legend',
          width: 650,
          missing_is_zero: false,
          linked: true
        });
      }

      scope.$watch('dataOfCharts', function(value) {
        //delete previous diagrams
        element.empty();
        var convertToDate = function(item) { item.date = new Date(item.date)};
        //clone data arrays
        var gcp = value.gcp.slice();
        var azure = value.azure.slice();
        var aws = value.aws.slice();
        //transform date fields
        gcp.forEach(convertToDate);
        azure.forEach(convertToDate);
        aws.forEach(convertToDate);

        var unitedChartData = [];
        if (value.cloud == 'all' || value.cloud == 'GCC') {
          element.append('<div id="gcpChart" class="col-xs-6 col-sm-4 col-md-4"/>');
          createChart('GCP running hours', gcp, 'gcpChart');
          unitedChartData.push(gcp);
        }
        if (value.cloud == 'all' || value.cloud == 'AZURE') {
          element.append('<div id="azureChart" class="col-xs-6 col-sm-4 col-md-4"/>');
          createChart('AZURE running hours', azure, 'azureChart');
          unitedChartData.push(azure);
        }
        if (value.cloud == 'all' || value.cloud == 'AWS') {
          element.append('<div id="awsChart" class="col-xs-6 col-sm-4 col-md-4"/>');
          createChart('AWS running hours', aws, 'awsChart');
          unitedChartData.push(aws);
        }
        if (value.cloud == 'all') {
          element.append('<div id="unitedChartLegend" class="col-xs-2 col-sm-2 col-md-2 legend" />');
          element.append('<div id="unitedChart" class="col-xs-8 col-sm-8 col-md-8" />');
          createUnitedChart(unitedChartData);
        }
      } ,true);

    }
  };
});
