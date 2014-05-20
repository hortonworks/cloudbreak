'use strict';

/* App Module */

var provisioningApp = angular.module('provisioningApp', ['ngRoute', 'provisioningControllers', 'provisioningServices']);

provisioningApp.config([ '$routeProvider', function($routeProvider) {
    $routeProvider.when('/', {
        templateUrl : 'partials/list.html',
        controller: 'ProvisioningController'
    }).when('/AWS', {
        templateUrl : 'partials/AWS.html',
        controller: 'AwsController'
    }).when('/AZURE', {
        templateUrl : 'partials/AZURE.html',
        controller: 'AzureController'
    }).when('/add_provider', {
        templateUrl : 'partials/add_provider.html',
        controller: 'CloudProviderController'
    }).when('/cloud', {
        templateUrl : 'partials/cloud.html',
        controller: 'CloudInstanceController'
    }).otherwise({
        redirectTo : '/'
    });
} ]);
