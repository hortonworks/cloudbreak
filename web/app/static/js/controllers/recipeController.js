'use strict';

var log = log4javascript.getLogger("recipeController-logger");

angular.module('uluwatuControllers').controller('recipeController', ['$scope', '$rootScope', '$filter', '$base64', 'UserRecipe', 'AccountRecipe', 'GlobalRecipe', 'File', 'ErrorHandler',
    function($scope, $rootScope, $filter, $base64, UserRecipe, AccountRecipe, GlobalRecipe, File, ErrorHandler) {

        var decodeBase64Content = function(recipe) {
            recipe.decodedContent = $base64.decode(recipe.content);
        };

        $rootScope.recipes = AccountRecipe.query(function() {
            for (var i = 0; i < $rootScope.recipes.length; i++) {
                decodeBase64Content($rootScope.recipes[i]);
            }
        });
        initalizeRecipe();

        $scope.createRecipe = function() {
            var successHandler = function(result) {
                GlobalRecipe.get({
                    id: result.id
                }, function(success) {
                    decodeBase64Content(success);
                    $rootScope.recipes.push(success);
                    initalizeRecipe();
                    $scope.showSuccess($rootScope.msg.recipe_success1 + success.id + $rootScope.msg.recipe_success2);
                    angular.element(document.querySelector('#panel-create-recipes-collapse-btn')).click();
                });
            };
            var errorHandler = function(error) {
                $scope.showError(error, $rootScope.msg.recipe_failed);
            };

            $scope.recipe.content = $base64.encode($scope.recipeScript);
            if ($scope.recipePublicInAccount) {
                AccountRecipe.save($scope.recipe, successHandler, errorHandler);
            } else {
                UserRecipe.save($scope.recipe, successHandler, errorHandler);
            }

            $scope.recipeCreationForm.$setPristine();

        };

        $scope.deleteRecipe = function(recipe) {
            GlobalRecipe.delete({
                id: recipe.id
            }, function(success) {
                $rootScope.recipes.splice($rootScope.recipes.indexOf(recipe), 1);
                $scope.showSuccess($rootScope.msg.recipe_delete_success1 + recipe.id + $rootScope.msg.recipe_delete_success2);
            }, function(error) {
                $scope.showError(error, $rootScope.msg.recipe_delete_failed);
            });
        };

        $scope.isEmpty = function(obj) {
            for (var i in obj)
                if (obj.hasOwnProperty(i)) return false;
            return true;
        };

        $scope.generateStoredContentFromFile = function() {
            File.getBase64ContentById("recipeFile", function(content) {
                $scope.recipeScript = $base64.decode(content);
            });
        };

        function initalizeRecipe() {
            $scope.recipeContentType = 'SCRIPT';
            $scope.recipeScript = "";
            $scope.recipePublicInAccount = false;
            $scope.recipe = {
                name: "",
                description: "",
                recipeType: "PRE"
            }
        }

        $scope.keys = function(obj) {
            return obj ? Object.keys(obj) : [];
        }
    }
]);