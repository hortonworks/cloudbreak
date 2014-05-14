'use strict';

/* Services */

var provisioningServices = angular.module('provisioningServices', ['ngResource']);

provisioningServices.factory('Templates', ['$resource',
  	function($resource){
      	return $resource('templates');
  	}]);