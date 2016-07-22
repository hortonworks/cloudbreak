'use strict';

var log = log4javascript.getLogger("recipeController-logger");

angular.module('uluwatuControllers').controller('recipeController', ['$scope', '$rootScope', '$filter', '$base64', 'UserRecipe', 'AccountRecipe', 'GlobalRecipe', 'File', 'ErrorHandler',
    function($scope, $rootScope, $filter, $base64, UserRecipe, AccountRecipe, GlobalRecipe, File, ErrorHandler) {

        var decorateBase64Plugins = function(recipe) {
            recipe.pluginContents = {};
            recipe.plugins.forEach(function (p) {
                var lines = recipe.pluginContents[p] = {};
                var files = $base64.decode(p.substring(9)).split('\n');
                for (var f = 0; f < files.length; f++) {
                    var file = files[f];
                    if (file) {
                        var content = file.split(":");
                        if ("plugin.toml" != content[0]) {
                            lines[content[0]] = $base64.decode(content[1]);
                        }
                    }
                }
            });
        };

        $rootScope.recipes = AccountRecipe.query(function() {
            for (var i = 0; i < $rootScope.recipes.length; i++) {
                decorateBase64Plugins($rootScope.recipes[i]);
            }
        });
        initalizeRecipe();

        $scope.createRecipe = function() {
            $scope.recipeCreationForm.$setPristine();
            $scope.recipe.plugins = [];
            $scope.recipe.plugins.push($scope.recipePlugin.url);
            angular.forEach($scope.recipePropertyList, function(item) {
                $scope.recipe.properties[item.name] = item.value;
            });
            var successHandler = function(result) {
                GlobalRecipe.get({
                    id: result.id
                }, function(success) {
                    decorateBase64Plugins(success);
                    $rootScope.recipes.push(success);
                    initalizeRecipe();
                    $scope.showSuccess($rootScope.msg.recipe_success1 + success.id + $rootScope.msg.recipe_success2);
                    angular.element(document.querySelector('#panel-create-recipes-collapse-btn')).click();
                });
            };
            var errorHandler = function(error) {
                $scope.showError(error, $rootScope.msg.recipe_failed);
            };

            if ($scope.recipePublicInAccount) {
                AccountRecipe.save($scope.recipe, successHandler, errorHandler);
            } else {
                UserRecipe.save($scope.recipe, successHandler, errorHandler);
            }
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

        $scope.addRecipeProperty = function() {
            $scope.recipePropertyList.push({
                name: "",
                value: ""
            });
        };

        $scope.deleteRecipeProperty = function(index) {
            delete $scope.recipePropertyList.splice(index, 1);
        }

        $scope.isEmpty = function(obj) {
            for (var i in obj)
                if (obj.hasOwnProperty(i)) return false;
            return true;
        };

        $scope.generateStoredPluginFromFile = function() {
            $scope.recipePlugin.url = "";
            var plugin = "";
            File.getBase64ContentById("preInstallFile", function(content) {
                if (content) {
                    plugin += "recipe-pre-install:" + content + "\n";
                }
                File.getBase64ContentById("postInstallFile", function(content) {
                    if (content) {
                        plugin += "recipe-post-install:" + content + "\n";
                    }
                    if (plugin) {
                        plugin += "plugin.toml:" + $base64.encode('[plugin]\nname="' + $scope.recipe.name + '"\ndescription="' + $scope.recipe.description + '"\nversion="1.0"') + "\n";
                        $scope.recipePlugin.url = "base64://" + $base64.encode(plugin);
                    }
                    $scope.$apply();
                });
            });
            $scope.$apply();
        };

        $scope.generateStoredPluginFromText = function() {
            $scope.recipePlugin.url = "";
            var plugin = "";
            if ($scope.preInstallScript) {
                plugin += "recipe-pre-install:" + $base64.encode($scope.preInstallScript + "\n") + "\n";
            }
            if ($scope.postInstallScript) {
                plugin += "recipe-post-install:" + $base64.encode($scope.postInstallScript + "\n") + "\n";
            }
            if (plugin) {
                plugin += "plugin.toml:" + $base64.encode('[plugin]\nname="' + $scope.recipe.name + '"\ndescription="' + $scope.recipe.description + '"\nversion="1.0"') + "\n";
                $scope.recipePlugin.url = "base64://" + $base64.encode(plugin);
            }
        };

        function initalizeRecipe() {
            $scope.recipeType = 'SCRIPT';
            $scope.preInstallScript = "";
            $scope.postInstallScript = "";
            $scope.recipePropertyList = [];
            $scope.recipePublicInAccount = false;
            $scope.recipePlugin = {
                url: ""
            };
            $scope.recipe = {
                name: "",
                description: "",
                timeout: 15,
                properties: {},
                plugins: {}
            }
        }
    }
]);