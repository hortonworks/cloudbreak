'use strict';

var url = require('url');

var V1settings = require('./V1settingsService');

module.exports.getAllSettings = function getAllSettings (req, res, next) {
  V1settings.getAllSettings(req.swagger.params, res, next);
};

module.exports.getDatabaseConfigSettings = function getDatabaseConfigSettings (req, res, next) {
  V1settings.getDatabaseConfigSettings(req.swagger.params, res, next);
};

module.exports.getRecipeSettings = function getRecipeSettings (req, res, next) {
  V1settings.getRecipeSettings(req.swagger.params, res, next);
};
