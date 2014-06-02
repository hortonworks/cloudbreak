'use strict';

/* App Module */

var cloudbreakApp = angular.module('cloudbreakApp', ['ngRoute', 'cloudbreakControllers', 'cloudbreakServices']);

cloudbreakApp.config([ '$routeProvider', function($routeProvider) {
    $routeProvider.when('/', {
        templateUrl : 'partials/login.html',
        controller: 'cloudbreakController'
    }) .otherwise({
        redirectTo : '/'
    });
 /*       .when('/console', {
        templateUrl : 'partials/console.html',
        controller: 'consoleController'
    })*/

} ]);