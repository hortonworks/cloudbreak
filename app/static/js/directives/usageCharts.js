'use strict';

cloudbreakApp.directive('usagecharts', function() {
  return {
    restrict: 'A',
    link: function(scope, element) {

      function createChart(title, data, targetDivId, y_axis) {
        MG.data_graphic({
          title: title,
          data: data,
          chart_type: 'line',
          target: '#' + targetDivId,
          x_accessor: "date",
          y_accessor: "hours",
          width: 400,
          interpolate: 'linear',
          y_axis: y_axis
        });
      }

      function createUnitedChart(data, y_axis) {
        MG.data_graphic({
          title: 'Combined running hours',
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
          linked: true,
          y_axis: y_axis
        });
      }

      scope.$watch('dataOfCharts', function(value) {
        if (value != undefined) {
          createUsageCharts(value);
        }
      }, false);

      function createUsageCharts(value) {
        //delete previous diagrams
        element.empty();
        //clone data arrays
        var gcpUsage = value.gcp.slice();
        var azureUsage = value.azure.slice();
        var awsUsage = value.aws.slice();
        var gcpMaxHour = 0;
        var azureMaxHour = 0;
        var awsMaxHour = 0;
        gcpUsage.forEach(function(item) {
          item.date = new Date(item.date);
          if (item.hours > gcpMaxHour) {gcpMaxHour = item.hours;}
        });
        azureUsage.forEach(function(item) {
          item.date = new Date(item.date);
          if (item.hours > azureMaxHour) {azureMaxHour = item.hours;}
        });
        awsUsage.forEach(function(item) {
          item.date = new Date(item.date);
          if (item.hours > awsMaxHour) {awsMaxHour = item.hours;}
        });

        var unitedChartData = [];
        if (value.cloud == 'all' || value.cloud == 'GCC') {
          element.append('<div id="gcpChart" class="col-xs-6 col-sm-4 col-md-4"/>');
          createChart('GCP running hours', gcpUsage, 'gcpChart', (gcpMaxHour > 0));
          unitedChartData.push(gcpUsage);
        }
        if (value.cloud == 'all' || value.cloud == 'AZURE') {
          element.append('<div id="azureChart" class="col-xs-6 col-sm-4 col-md-4"/>');
          createChart('AZURE running hours', azureUsage, 'azureChart', (azureMaxHour > 0));
          unitedChartData.push(azureUsage);
        }
        if (value.cloud == 'all' || value.cloud == 'AWS') {
          element.append('<div id="awsChart" class="col-xs-6 col-sm-4 col-md-4"/>');
          createChart('AWS running hours', awsUsage, 'awsChart', (awsMaxHour > 0));
          unitedChartData.push(awsUsage);
        }
        if (value.cloud == 'all') {
          element.append('<div id="unitedChartLegend" class="col-xs-2 col-sm-2 col-md-2 legend" />');
          element.append('<div id="unitedChart" class="col-xs-8 col-sm-8 col-md-8" />');
          var y_axis = (gcpMaxHour > 0 || azureMaxHour > 0 || awsMaxHour > 0);
          createUnitedChart(unitedChartData, y_axis);
        }
      }
    }
  };
});
