'use strict';

var url = require('url');

var V1recipes = require('./V1recipesService');

module.exports.deletePrivateRecipe = function deletePrivateRecipe (req, res, next) {
  V1recipes.deletePrivateRecipe(req.swagger.params, res, next);
};

module.exports.deletePublicRecipe = function deletePublicRecipe (req, res, next) {
  V1recipes.deletePublicRecipe(req.swagger.params, res, next);
};

module.exports.deleteRecipe = function deleteRecipe (req, res, next) {
  V1recipes.deleteRecipe(req.swagger.params, res, next);
};

module.exports.getPrivateRecipe = function getPrivateRecipe (req, res, next) {
  V1recipes.getPrivateRecipe(req.swagger.params, res, next);
};

module.exports.getPrivatesRecipe = function getPrivatesRecipe (req, res, next) {
  V1recipes.getPrivatesRecipe(req.swagger.params, res, next);
};

module.exports.getPublicRecipe = function getPublicRecipe (req, res, next) {
  V1recipes.getPublicRecipe(req.swagger.params, res, next);
};

module.exports.getPublicsRecipe = function getPublicsRecipe (req, res, next) {
  V1recipes.getPublicsRecipe(req.swagger.params, res, next);
};

module.exports.getRecipe = function getRecipe (req, res, next) {
  V1recipes.getRecipe(req.swagger.params, res, next);
};

module.exports.getRecipeRequestFromName = function getRecipeRequestFromName (req, res, next) {
  V1recipes.getRecipeRequestFromName(req.swagger.params, res, next);
};

module.exports.postPrivateRecipe = function postPrivateRecipe (req, res, next) {
  V1recipes.postPrivateRecipe(req.swagger.params, res, next);
};

module.exports.postPublicRecipe = function postPublicRecipe (req, res, next) {
  V1recipes.postPublicRecipe(req.swagger.params, res, next);
};
