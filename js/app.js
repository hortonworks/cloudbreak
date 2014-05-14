'use strict';

/* App Module */

var provisioningApp = angular.module('provisioningApp', ['ngRoute', 'provisioningControllers', 'provisioningServices']);

provisioningApp.config([ '$routeProvider', function($routeProvider) {
    $routeProvider.when('/', {
        templateUrl : 'partials/aws.html',
        controller: 'AwsController'
    }).when('/aws', {
        templateUrl : 'partials/aws.html',
        controller: 'AwsController'
    }).when('/azure', {
        templateUrl : 'partials/azure.html',
        controller: 'AzureController'
    }).otherwise({
        redirectTo : '/'
    });
} ]);