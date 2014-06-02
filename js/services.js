'use strict';

/* Services */

var cloudbreakServices = angular.module('cloudbreakServices', ['ngResource']);

cloudbreakServices.factory('Templates', ['$resource',
  	function($resource){
      	return $resource('templates');
  	}
]);