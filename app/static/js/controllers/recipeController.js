'use strict';

var log = log4javascript.getLogger("recipeController-logger");

angular.module('uluwatuControllers').controller('recipeController', ['$scope', '$rootScope', '$filter','UserRecipe', 'AccountRecipe', 'ErrorHandler',
    function ($scope, $rootScope, $filter, UserRecipe, AccountRecipe, ErrorHandler) {
        $rootScope.recipes = AccountRecipe.query();
    }
]);
