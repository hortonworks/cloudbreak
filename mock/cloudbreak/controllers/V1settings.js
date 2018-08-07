'use strict';

var utils = require('../utils/writer.js');
var V1settings = require('../service/V1settingsService');

module.exports.getAllSettings = function getAllSettings (req, res, next) {
  V1settings.getAllSettings()
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.getDatabaseConfigSettings = function getDatabaseConfigSettings (req, res, next) {
  V1settings.getDatabaseConfigSettings()
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};

module.exports.getRecipeSettings = function getRecipeSettings (req, res, next) {
  V1settings.getRecipeSettings()
    .then(function (response) {
      utils.writeJson(res, response);
    })
    .catch(function (response) {
      utils.writeJson(res, response);
    });
};
