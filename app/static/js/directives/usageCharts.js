'use strict';

cloudbreakApp.directive('usagecharts', function() {
  return {
    restrict: 'A',
    link: function(scope, element) {

      function createChart(title, data, targetDivId, y_axis) {
        MG.data_graphic({
          title: title,
          data: data,
          full_width: true,
          chart_type: 'line',
          target: '#' + targetDivId,
          x_accessor: "date",
          y_accessor: "hours",
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
          legend: ['GCP','AZURE','AWS', 'OPENSTACK'],
          legend_target: '.legend',
          missing_is_zero: false,
          linked: true,
          full_width: true,
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
        var openstackUsage = value.openstack.slice();
        var gcpMaxHour = 0;
        var azureMaxHour = 0;
        var awsMaxHour = 0;
        var openstackMaxHour = 0;

        convertDatesAndSelectMax(gcpUsage, gcpMaxHour);
        convertDatesAndSelectMax(azureUsage, azureMaxHour);
        convertDatesAndSelectMax(awsUsage, awsMaxHour);
        convertDatesAndSelectMax(openstackUsage, openstackMaxHour);

        var unitedChartData = [];
        if (value.cloud == 'all' || value.cloud == 'GCP') {
          element.append('<div id="gcpChart" class="col-xs-12 col-sm-6 col-md-6"/>');
          createChart('GCP running hours', gcpUsage, 'gcpChart', (gcpMaxHour > 0));
          unitedChartData.push(gcpUsage);
        }
        if (value.cloud == 'all' || value.cloud == 'AZURE') {
          element.append('<div class="col-xs-12 col-sm-6 col-md-6"><div id="azureChart" /></div>');
          createChart('AZURE running hours', azureUsage, 'azureChart', (azureMaxHour > 0));
          unitedChartData.push(azureUsage);
        }
        if (value.cloud == 'all' || value.cloud == 'AWS') {
          element.append('<div id="awsChart" class="col-xs-12 col-sm-6 col-md-6"/>');
          createChart('AWS running hours', awsUsage, 'awsChart', (awsMaxHour > 0));
          unitedChartData.push(awsUsage);
        }
        if (value.cloud == 'all' || value.cloud == 'OPENSTACK') {
          element.append('<div id="openstackChart" class="col-xs-12 col-sm-6 col-md-6"/>');
          createChart('OpenStack running hours', openstackUsage, 'openstackChart', (openstackMaxHour > 0));
          unitedChartData.push(openstackUsage);
        }
        if (value.cloud == 'all') {
          element.append('<div class="col-xs-12 col-sm-12 col-md-12">' 
            + '<div id="unitedChartLegend" class="col-xs-1 col-sm-2 col-md-2 legend" />'
            + '<div id="unitedChart" class="col-xs-8 col-sm-10 col-md-10" /></div>');
          var y_axis = (gcpMaxHour > 0 || azureMaxHour > 0 || awsMaxHour > 0);
          createUnitedChart(unitedChartData, y_axis);
        }
      }

      function convertDatesAndSelectMax(collection, max) {
          collection.forEach(function(item) {
            item.date = new Date(item.date);
            if (item.hours > max) {
              max = item.hours;
            }
          });
        }
    }
  };
});
