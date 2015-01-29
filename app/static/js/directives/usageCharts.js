'use strict';

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
        if (value != undefined) {
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

          console.log(value)
          console.log('watch callback called.....')

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
        }
      }, false);

    }
  };
});
